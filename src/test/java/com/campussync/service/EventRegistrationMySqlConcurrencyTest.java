package com.campussync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campussync.entity.Committee;
import com.campussync.entity.Department;
import com.campussync.entity.Event;
import com.campussync.entity.Role;
import com.campussync.entity.Student;
import com.campussync.entity.User;
import com.campussync.repository.CommitteeRepository;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.EventRegistrationRepository;
import com.campussync.repository.EventRepository;
import com.campussync.repository.RoleRepository;
import com.campussync.repository.StudentRepository;
import com.campussync.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.DockerClientFactory;
import org.junit.jupiter.api.Assumptions;

/** Verifies that independent MySQL transactions serialize on the event row. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class EventRegistrationMySqlConcurrencyTest {
    private static final String EXTERNAL_URL = System.getenv("CAMPUSSYNC_TEST_MYSQL_URL");
    private static final String EXTERNAL_USERNAME = System.getenv("CAMPUSSYNC_TEST_MYSQL_USERNAME");
    private static final String EXTERNAL_PASSWORD = System.getenv("CAMPUSSYNC_TEST_MYSQL_PASSWORD");
    private static boolean useExternalDatabase;
    // Leave teardown to Testcontainers/Ryuk at JVM exit. Stopping in @AfterAll
    // races Spring's later shutdown hook, where Hibernate performs create-drop.
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("campussync_test")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void startIsolatedDatabase() {
        if (EXTERNAL_URL != null && !EXTERNAL_URL.isBlank()) {
            if (!EXTERNAL_URL.matches("jdbc:mysql://[^/]+/[^/?]*_test(?:\\?.*)?"))
                throw new IllegalArgumentException("External MySQL URL must target a dedicated database ending in _test.");
            if (EXTERNAL_USERNAME == null || EXTERNAL_PASSWORD == null)
                throw new IllegalArgumentException("External MySQL username and password environment variables are required.");
            useExternalDatabase = true;
            return;
        }
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Set up Docker or provide CAMPUSSYNC_TEST_MYSQL_URL/USERNAME/PASSWORD for a dedicated *_test database.");
        MYSQL.start();
    }

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        if (useExternalDatabase) {
            registry.add("spring.datasource.url", () -> EXTERNAL_URL);
            registry.add("spring.datasource.username", () -> EXTERNAL_USERNAME);
            registry.add("spring.datasource.password", () -> EXTERNAL_PASSWORD);
        } else {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
        }
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Autowired EventRegistrationService registrationService;
    @Autowired EventRegistrationRepository registrations;
    @Autowired EventRepository events;
    @Autowired RoleRepository roles;
    @Autowired DepartmentRepository departments;
    @Autowired CommitteeRepository committees;
    @Autowired UserRepository users;
    @Autowired StudentRepository students;

    private Long eventId;
    private List<Long> studentUserIds;

    @BeforeEach
    void createIsolatedTestData() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        Role role = new Role(); role.setCode("STUDENT"); role.setName("Student");
        role = roles.saveAndFlush(role);
        Department department = new Department(); department.setCode("D" + suffix.substring(0, 8));
        department.setName("Concurrency " + suffix); department = departments.saveAndFlush(department);
        Committee committee = new Committee(); committee.setDepartment(department); committee.setCode("C" + suffix.substring(0, 8));
        committee.setName("Concurrency Committee"); committee.setStatus("ACTIVE"); committee.setCategory("TECHNICAL"); committee = committees.saveAndFlush(committee);
        Event event = new Event(); event.setCommittee(committee); event.setTitle("Last seat " + suffix);
        event.setCategory("WORKSHOP");
        event.setStartsAt(LocalDateTime.now().plusDays(2)); event.setEndsAt(LocalDateTime.now().plusDays(2).plusHours(1));
        event.setRegistrationDeadline(LocalDateTime.now().plusDays(1)); event.setVenue("Test room");
        event.setCapacity(1); event.setStatus("PUBLISHED"); event = events.saveAndFlush(event); eventId = event.getId();

        java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
        for (int index = 0; index < 2; index++) {
            User user = new User(); user.setRole(role); user.setUsername("u" + index + suffix);
            user.setEmail("u" + index + suffix + "@example.test"); user.setPasswordHash("test-hash");
            user.setFirstName("Test"); user.setLastName("Student"); user = users.saveAndFlush(user);
            Student student = new Student(); student.setUser(user); student.setDepartment(department);
            student.setEnrollmentNumber("E" + index + suffix); student.setAdmissionDate(LocalDate.now());
            student.setAcademicStatus("ENROLLED"); students.saveAndFlush(student); ids.add(user.getId());
        }
        studentUserIds = List.copyOf(ids);
    }

    @Test
    void concurrentLastSeatRequestsCreateExactlyOneRegistration() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = pool.submit(() -> attempt(studentUserIds.get(0), ready, start));
            Future<Boolean> second = pool.submit(() -> attempt(studentUserIds.get(1), ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Both requests should reach the barrier.");
            start.countDown();
            List<Boolean> outcomes = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertEquals(1, outcomes.stream().filter(Boolean::booleanValue).count(), "Exactly one request should succeed.");
            assertEquals(1, registrations.countActiveByEventId(eventId), "Exactly one confirmed row should persist.");
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    private boolean attempt(Long userId, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent test did not start.");
        try {
            registrationService.register(eventId, userId);
            return true;
        } catch (IllegalStateException conflict) {
            if ("Event is full.".equals(conflict.getMessage())) return false;
            // The first request marks a newly full event REGISTRATION_CLOSED.
            // The serialized second request then sees that state before the
            // capacity check; count it only when the persisted capacity proves
            // that this status transition was caused by the winning request.
            if ("Event is not open for registration.".equals(conflict.getMessage())
                    && events.findById(eventId)
                            .filter(event -> "REGISTRATION_CLOSED".equalsIgnoreCase(event.getStatus()))
                            .filter(event -> registrations.countActiveByEventId(eventId) >= event.getCapacity())
                            .isPresent()) return false;
            throw conflict;
        }
    }
}
