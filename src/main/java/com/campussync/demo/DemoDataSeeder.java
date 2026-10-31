package com.campussync.demo;

import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class DemoDataSeeder {
    private static final String DATABASE_NAME = "campussync_demo";
    private static final String DATABASE_URL = "jdbc:mysql://127.0.0.1:3306/campussync_demo";
    private static final int DATABASE_PORT = 3306;
    private static final String CONFIRMATION = "--confirm=" + DATABASE_NAME;
    private static final String COMMITTEE_REFERENCE_MODE = "--ensure-demo-committee-references";
    private static final LocalDate EFFECTIVE_FROM = LocalDate.of(2026, 8, 10);
    private static final String ACADEMIC_YEAR = "2026-27";

    private static final Set<String> APPLICATION_TABLES = Set.of(
            "role", "department", "user", "student", "faculty", "committee", "subject",
            "committee_member", "event", "event_registration", "notification",
            "notification_recipient", "class_session", "attendance_record",
            "attendance_change", "timetable_entry");
    private static final Set<String> EXPECTED_TABLES;
    private static final Set<String> VERSIONED_MIGRATIONS = Set.of("1000", "1004", "1005", "1006");
    private static final Set<String> INSERT_TABLES = Set.of(
            "role", "department", "user", "student", "faculty", "subject", "timetable_entry",
            "committee", "committee_member");
    static final Set<String> COMMITTEE_REFERENCE_INSERT_TABLES =
            Set.of("committee", "committee_member");
    private static final Pattern GRANT_PATTERN = Pattern.compile(
            "(?i)^GRANT\\s+(.+?)\\s+ON\\s+(.+?)\\s+TO\\s+.+$");

    static {
        Set<String> tables = new LinkedHashSet<>(APPLICATION_TABLES);
        tables.add("flyway_schema_history");
        EXPECTED_TABLES = Set.copyOf(tables);
    }

    private DemoDataSeeder() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--verify-console".equals(args[0])) {
            requireInteractiveConsole();
            System.out.println("Interactive Java console is available.");
            return;
        }
        if (args.length == 2 && COMMITTEE_REFERENCE_MODE.equals(args[0])
                && CONFIRMATION.equals(args[1])) {
            seedCommitteeReferencesOnly();
            return;
        }
        if (args.length != 1 || !CONFIRMATION.equals(args[0])) {
            throw new IllegalArgumentException(
                    "Refusing to seed. Pass the exact confirmation argument: " + CONFIRMATION);
        }
        requireInteractiveConsole();
        if (System.getenv("CAMPSYNC_DEMO_SEED_DB_URL") != null) {
            throw new IllegalStateException("CAMPSYNC_DEMO_SEED_DB_URL is not supported; the target URL is fixed.");
        }

        String username = requiredEnvironmentVariable("CAMPSYNC_DEMO_SEED_DB_USERNAME");
        String password = requiredEnvironmentVariable("CAMPSYNC_DEMO_SEED_DB_PASSWORD");
        String expectedServerHostname =
                requiredEnvironmentVariable("CAMPSYNC_DEMO_EXPECTED_DB_SERVER_HOSTNAME");

        List<UserSeed> users = syntheticUsers();
        validateFixture();
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, username, password)) {
            String targetIdentity = verifyKnownEmptyTarget(connection, expectedServerHostname);
            confirmConnectedTarget(targetIdentity);

            Map<String, String> passwordHashes = readPasswordHashes(users);
            connection.setAutoCommit(false);
            try {
                verifyKnownEmptyTarget(connection, expectedServerHostname);
                seed(connection, users, passwordHashes);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                passwordHashes.clear();
            }
        }
        System.out.println("DEMO seed completed: 4 roles, 1 department, 17 accounts, "
                + "8 faculty profiles, 8 student profiles, 8 subjects, 5 committees, "
                + "13 coordinator memberships, and 27 timetable entries.");
        System.out.println("No class sessions or attendance records were created.");
    }

    private static void seedCommitteeReferencesOnly() throws Exception {
        requireInteractiveConsole();
        if (System.getenv("CAMPSYNC_DEMO_SEED_DB_URL") != null) {
            throw new IllegalStateException("CAMPSYNC_DEMO_SEED_DB_URL is not supported; the target URL is fixed.");
        }
        String username = requiredEnvironmentVariable("CAMPSYNC_DEMO_SEED_DB_USERNAME");
        String password = requiredEnvironmentVariable("CAMPSYNC_DEMO_SEED_DB_PASSWORD");
        String expectedServerHostname =
                requiredEnvironmentVariable("CAMPSYNC_DEMO_EXPECTED_DB_SERVER_HOSTNAME");

        try (Connection connection = DriverManager.getConnection(DATABASE_URL, username, password)) {
            String targetIdentity = verifyKnownDemoTarget(
                    connection, expectedServerHostname, COMMITTEE_REFERENCE_INSERT_TABLES);
            verifyDemoSeedAccounts(connection);
            confirmConnectedTarget(targetIdentity);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                verifyKnownDemoTarget(connection, expectedServerHostname, COMMITTEE_REFERENCE_INSERT_TABLES);
                verifyDemoSeedAccounts(connection);
                ensureDemoCommitteeReferences(connection);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
        System.out.println("DEMO reference data ensured: 5 canonical committees and 13 coordinator memberships.");
    }

    private static Console requireInteractiveConsole() {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException(
                    "An interactive Java console is required; run the seeder directly in a terminal.");
        }
        return console;
    }

    private static String verifyKnownEmptyTarget(Connection connection, String expectedServerHostname)
            throws SQLException {
        String targetIdentity = verifyKnownDemoTarget(connection, expectedServerHostname);
        verifyApplicationTablesEmpty(connection);
        return targetIdentity;
    }

    private static String verifyKnownDemoTarget(Connection connection, String expectedServerHostname)
            throws SQLException {
        return verifyKnownDemoTarget(connection, expectedServerHostname, INSERT_TABLES);
    }

    private static String verifyKnownDemoTarget(
            Connection connection, String expectedServerHostname, Set<String> requiredInsertTables)
            throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT DATABASE(), @@hostname, @@port")) {
            if (!result.next() || !DATABASE_NAME.equals(result.getString(1))) {
                throw new IllegalStateException("Connected schema was not " + DATABASE_NAME + ".");
            }
            String serverHostname = result.getString(2);
            int serverPort = result.getInt(3);
            if (!expectedServerHostname.equalsIgnoreCase(serverHostname) || serverPort != DATABASE_PORT) {
                throw new IllegalStateException(
                        "Connected MySQL server identity did not match the independently verified demo server.");
            }
            verifyExactTableSet(connection);
            verifyMigrationHistory(connection);
            verifySeederGrants(connection, requiredInsertTables);
            return DATABASE_NAME + "@" + serverHostname + ":" + serverPort;
        }
    }

    private static void verifyExactTableSet(Connection connection) throws SQLException {
        Set<String> actualTables = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT TABLE_NAME, TABLE_TYPE FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?")) {
            statement.setString(1, DATABASE_NAME);
            try (ResultSet tables = statement.executeQuery()) {
                while (tables.next()) {
                    if (!"BASE TABLE".equalsIgnoreCase(tables.getString("TABLE_TYPE"))) {
                        throw new IllegalStateException("Unexpected non-table object in " + DATABASE_NAME + ".");
                    }
                    actualTables.add(tables.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
                }
            }
        }
        if (!EXPECTED_TABLES.equals(actualTables)) {
            throw new IllegalStateException(
                    "Refusing to seed: schema tables do not match the expected migrated demo schema.");
        }
    }

    private static void verifyMigrationHistory(Connection connection) throws SQLException {
        Set<String> successfulVersions = new LinkedHashSet<>();
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "SELECT version, success FROM `flyway_schema_history` ORDER BY installed_rank")) {
            while (rows.next()) {
                if (!rows.getBoolean("success")) {
                    throw new IllegalStateException("Refusing to seed: Flyway history contains a failed migration.");
                }
                successfulVersions.add(rows.getString("version"));
            }
        }
        if (!VERSIONED_MIGRATIONS.equals(successfulVersions)) {
            throw new IllegalStateException(
                    "Refusing to seed: successful Flyway versions must be exactly V1000, V1004, V1005, and V1006.");
        }
    }

    private static void verifyApplicationTablesEmpty(Connection connection) throws SQLException {
        for (String table : APPLICATION_TABLES) {
            try (Statement statement = connection.createStatement();
                    ResultSet count = statement.executeQuery("SELECT COUNT(*) FROM `" + table + "`")) {
                if (!count.next() || count.getLong(1) != 0) {
                    throw new IllegalStateException(
                            "Refusing to seed: application table `" + table + "` is not empty.");
                }
            }
        }
    }

    private static void verifySeederGrants(Connection connection, Set<String> requiredInsertTables)
            throws SQLException {
        List<String> grants = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SHOW GRANTS FOR CURRENT_USER()")) {
            while (rows.next()) {
                grants.add(rows.getString(1));
            }
        }
        validateSeederGrants(grants, requiredInsertTables);
    }

    static void validateSeederGrants(List<String> grants, Set<String> requiredInsertTables) {
        if (!INSERT_TABLES.containsAll(requiredInsertTables)) {
            throw new IllegalArgumentException("Required seeder grants must be within the approved INSERT allowlist.");
        }
        boolean schemaSelect = false;
        Set<String> insertTables = new LinkedHashSet<>();
        for (String grant : grants) {
            Matcher matcher = GRANT_PATTERN.matcher(grant);
            if (!matcher.matches()) {
                throw new IllegalStateException("Refusing to seed: seeder account has an unsupported grant.");
            }
            Set<String> privileges = new LinkedHashSet<>();
            for (String privilege : matcher.group(1).split(",")) {
                privileges.add(privilege.trim().toUpperCase(Locale.ROOT));
            }
            String scope = matcher.group(2).replace("`", "").toLowerCase(Locale.ROOT);

            if ("*.*".equals(scope) && Set.of("USAGE").equals(privileges)) {
                continue;
            }
            if ((DATABASE_NAME + ".*").equals(scope) && Set.of("SELECT").equals(privileges)) {
                schemaSelect = true;
                continue;
            }
            String prefix = DATABASE_NAME + ".";
            if (scope.startsWith(prefix)) {
                String table = scope.substring(prefix.length());
                if (APPLICATION_TABLES.contains(table)
                        && Set.of("INSERT").equals(privileges)
                        && INSERT_TABLES.contains(table)) {
                    insertTables.add(table);
                    continue;
                }
            }
            throw new IllegalStateException(
                    "Refusing to seed: seeder account has grants outside the approved demo-only scope.");
        }
        if (!schemaSelect || !insertTables.containsAll(requiredInsertTables)) {
            throw new IllegalStateException(
                    "Seeder account must have SELECT on campussync_demo.* and INSERT on each required approved seed table.");
        }
    }

    private static void seed(
            Connection connection, List<UserSeed> users, Map<String, String> passwordHashes) throws SQLException {
        Map<String, Long> roleIds = insertRoles(connection);
        long departmentId = insertDepartment(connection);
        Map<String, Long> userIds = insertUsers(connection, users, roleIds, passwordHashes);
        Map<String, Long> facultyIds = insertFacultyProfiles(connection, userIds, departmentId);
        insertStudentProfiles(connection, userIds, departmentId);
        seedDemoCommitteeReferences(connection);
        Map<String, Long> subjectIds = insertSubjects(connection, departmentId, facultyIds);
        insertTimetable(connection, departmentId, subjectIds, facultyIds);
    }

    private static void verifyDemoSeedAccounts(Connection connection) throws SQLException {
        for (int index = 1; index <= 8; index++) {
            findDemoFacultyId(connection, facultyUsername(index));
        }
        for (int index = 1; index <= 8; index++) {
            findDemoStudentUserId(connection, studentUsername(index));
        }
    }

    private static void seedDemoCommitteeReferences(Connection connection) throws SQLException {
        for (CommitteeSeed committee : committeeSeeds()) {
            long facultyId = findDemoFacultyId(connection, committee.facultyUsername());
            String sql = "INSERT INTO committee (department_id, committee_code, committee_name, category, "
                    + "faculty_coordinator_id, is_active, status) VALUES (NULL, ?, ?, ?, ?, TRUE, 'ACTIVE')";
            long committeeId;
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, committee.code());
                statement.setString(2, committee.name());
                statement.setString(3, committee.category());
                statement.setLong(4, facultyId);
                statement.executeUpdate();
                committeeId = generatedId(statement);
            }
            insertCoordinatorMembership(connection, committeeId, findDemoFacultyUserId(connection, committee.facultyUsername()));
            for (String studentUsername : committee.studentUsernames()) {
                insertCoordinatorMembership(connection, committeeId, findDemoStudentUserId(connection, studentUsername));
            }
        }
    }

    private static void ensureDemoCommitteeReferences(Connection connection) throws SQLException {
        for (CommitteeSeed seed : committeeSeeds()) {
            long expectedFacultyId = findDemoFacultyId(connection, seed.facultyUsername());
            long committeeId = findOrVerifyCommittee(connection, seed, expectedFacultyId);
            long facultyUserId = findDemoFacultyUserId(connection, seed.facultyUsername());
            ensureCoordinatorMembership(connection, committeeId, facultyUserId);
            for (String username : seed.studentUsernames()) {
                ensureCoordinatorMembership(connection, committeeId, findDemoStudentUserId(connection, username));
            }
        }
    }

    static long findOrVerifyCommittee(
            Connection connection, CommitteeSeed expected, long expectedFacultyId) throws SQLException {
        Long existingId = findAndVerifyCommittee(connection, expected, expectedFacultyId);
        if (existingId != null) {
            return existingId;
        }
        try {
            return insertDemoCommittee(connection, expected, expectedFacultyId);
        } catch (SQLException exception) {
            if (!isDuplicateKey(exception)) {
                throw exception;
            }
            Long concurrentId = findAndVerifyCommittee(connection, expected, expectedFacultyId);
            if (concurrentId == null) {
                throw exception;
            }
            return concurrentId;
        }
    }

    private static Long findAndVerifyCommittee(
            Connection connection, CommitteeSeed expected, long expectedFacultyId) throws SQLException {
        String sql = "SELECT committee_id, committee_code, committee_name, category, faculty_coordinator_id, "
                + "is_active, status FROM committee WHERE committee_code = ? OR committee_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, expected.code());
            statement.setString(2, expected.name());
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }
                long id = results.getLong("committee_id");
                Long facultyId = results.getObject("faculty_coordinator_id", Long.class);
                boolean matches = expected.code().equals(results.getString("committee_code"))
                        && expected.name().equals(results.getString("committee_name"))
                        && expected.category().equals(results.getString("category"))
                        && facultyId != null && facultyId == expectedFacultyId
                        && results.getBoolean("is_active")
                        && "ACTIVE".equalsIgnoreCase(results.getString("status"));
                if (results.next() || !matches) {
                    throw new IllegalStateException(
                            "Existing demo committee reference conflicts with the approved seed definition: "
                                    + expected.code() + ".");
                }
                return id;
            }
        }
    }

    private static long insertDemoCommittee(
            Connection connection, CommitteeSeed seed, long facultyId) throws SQLException {
        String sql = "INSERT INTO committee (department_id, committee_code, committee_name, category, "
                + "faculty_coordinator_id, is_active, status) VALUES (NULL, ?, ?, ?, ?, TRUE, 'ACTIVE')";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, seed.code());
            statement.setString(2, seed.name());
            statement.setString(3, seed.category());
            statement.setLong(4, facultyId);
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private static void insertCoordinatorMembership(Connection connection, long committeeId, long userId)
            throws SQLException {
        String sql = "INSERT INTO committee_member (committee_id, user_id, member_role, joined_on, left_on, "
                + "membership_status) VALUES (?, ?, 'COORDINATOR', ?, NULL, 'ACTIVE')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, committeeId);
            statement.setLong(2, userId);
            statement.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
            statement.executeUpdate();
        }
    }

    static void ensureCoordinatorMembership(Connection connection, long committeeId, long userId)
            throws SQLException {
        String sql = "SELECT member_role FROM committee_member WHERE committee_id = ? AND user_id = ? "
                + "AND UPPER(TRIM(membership_status)) = 'ACTIVE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, committeeId);
            statement.setLong(2, userId);
            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    verifyCoordinatorMembership(results);
                    return;
                }
            }
        }
        try {
            insertCoordinatorMembership(connection, committeeId, userId);
        } catch (SQLException exception) {
            if (!isDuplicateKey(exception)) {
                throw exception;
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, committeeId);
                statement.setLong(2, userId);
                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next()) {
                        throw exception;
                    }
                    verifyCoordinatorMembership(results);
                }
            }
        }
    }

    private static void verifyCoordinatorMembership(ResultSet results) throws SQLException {
        if (!"COORDINATOR".equalsIgnoreCase(results.getString("member_role")) || results.next()) {
            throw new IllegalStateException(
                    "An active non-coordinator or duplicate membership conflicts with the demo coordinator assignment.");
        }
    }

    private static boolean isDuplicateKey(SQLException exception) {
        return exception.getErrorCode() == 1062;
    }

    private static long findDemoFacultyId(Connection connection, String username) throws SQLException {
        String sql = "SELECT f.faculty_id FROM faculty f JOIN `user` u ON u.user_id = f.user_id "
                + "JOIN `role` r ON r.role_id = u.role_id WHERE u.username = ? AND u.is_active = TRUE "
                + "AND r.role_code = 'FACULTY'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException("Required active synthetic faculty profile is missing: " + username);
                }
                long id = result.getLong(1);
                if (result.next()) {
                    throw new IllegalStateException("Synthetic faculty username is not unique: " + username);
                }
                return id;
            }
        }
    }

    private static long findDemoFacultyUserId(Connection connection, String username) throws SQLException {
        String sql = "SELECT u.user_id FROM faculty f JOIN `user` u ON u.user_id = f.user_id "
                + "JOIN `role` r ON r.role_id = u.role_id WHERE u.username = ? AND u.is_active = TRUE "
                + "AND r.role_code = 'FACULTY'";
        return requireSingleId(connection, sql, username, "faculty");
    }

    private static long findDemoStudentUserId(Connection connection, String username) throws SQLException {
        String sql = "SELECT u.user_id FROM student s JOIN `user` u ON u.user_id = s.user_id "
                + "JOIN `role` r ON r.role_id = u.role_id WHERE u.username = ? AND u.is_active = TRUE "
                + "AND r.role_code = 'STUDENT'";
        return requireSingleId(connection, sql, username, "student");
    }

    private static long requireSingleId(Connection connection, String sql, String username, String role)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException(
                            "Required active synthetic " + role + " profile is missing: " + username);
                }
                long id = result.getLong(1);
                if (result.next()) {
                    throw new IllegalStateException("Synthetic " + role + " username is not unique: " + username);
                }
                return id;
            }
        }
    }

    private static void validateFixture() {
        Map<String, SubjectSeed> subjectByKey = new LinkedHashMap<>();
        Set<String> subjectCodes = new LinkedHashSet<>();
        for (SubjectSeed subject : subjects()) {
            if (subjectByKey.put(subject.key(), subject) != null || !subjectCodes.add(subject.code())) {
                throw new IllegalStateException("Synthetic subject fixture contains duplicate keys or codes.");
            }
        }

        Set<String> slotKeys = new LinkedHashSet<>();
        Set<String> scheduledBatches = new LinkedHashSet<>();
        Map<String, List<ScheduleSeed>> byFaculty = new HashMap<>();
        List<ScheduleSeed> entries = timetable();
        for (ScheduleSeed entry : entries) {
            if (entry.day() == DayOfWeek.SATURDAY || entry.day() == DayOfWeek.SUNDAY
                    || !entry.end().isAfter(entry.start())) {
                throw new IllegalStateException("Synthetic timetable contains an invalid day or time range.");
            }
            String slotKey = entry.day() + "|" + entry.start() + "|" + entry.labBatch();
            if (!slotKeys.add(slotKey)) {
                throw new IllegalStateException("Synthetic timetable contains a duplicate slot.");
            }
            if ("BREAK".equals(entry.classType())) {
                if (entry.subjectKey() != null || entry.label() == null || entry.label().isBlank()) {
                    throw new IllegalStateException("Synthetic break entries require a label and no subject.");
                }
                continue;
            }
            SubjectSeed subject = subjectByKey.get(entry.subjectKey());
            if (subject == null || entry.venue() == null || entry.label() == null
                    || !entry.venue().startsWith("DEMO ") || !entry.label().startsWith("DEMO ")) {
                throw new IllegalStateException("Synthetic class entry has an invalid subject or missing DEMO label.");
            }
            if ("LAB".equals(entry.classType())) {
                if (!Set.of("B1", "B2", "B3", "B4").contains(entry.labBatch())) {
                    throw new IllegalStateException("Synthetic lab entries must target batches B1 through B4.");
                }
                scheduledBatches.add(entry.labBatch());
            } else if (!"LECTURE".equals(entry.classType()) || !"ALL".equals(entry.labBatch())) {
                throw new IllegalStateException("Synthetic lecture entry has an invalid type or batch.");
            }
            byFaculty.computeIfAbsent(subject.facultyKey(), ignored -> new ArrayList<>()).add(entry);
        }
        if (!scheduledBatches.equals(Set.of("B1", "B2", "B3", "B4"))) {
            throw new IllegalStateException("Synthetic timetable must include lab entries for B1 through B4.");
        }
        if (!subjectByKey.keySet().containsAll(entries.stream()
                .filter(entry -> entry.subjectKey() != null)
                .map(ScheduleSeed::subjectKey)
                .collect(java.util.stream.Collectors.toSet()))) {
            throw new IllegalStateException("Synthetic timetable references an unknown subject.");
        }
        for (List<ScheduleSeed> facultyEntries : byFaculty.values()) {
            for (int first = 0; first < facultyEntries.size(); first++) {
                for (int second = first + 1; second < facultyEntries.size(); second++) {
                    ScheduleSeed left = facultyEntries.get(first);
                    ScheduleSeed right = facultyEntries.get(second);
                    if (left.day() == right.day() && left.start().isBefore(right.end())
                            && right.start().isBefore(left.end())) {
                        throw new IllegalStateException("Synthetic faculty assignments overlap.");
                    }
                }
            }
        }
        for (int first = 0; first < entries.size(); first++) {
            for (int second = first + 1; second < entries.size(); second++) {
                ScheduleSeed left = entries.get(first);
                ScheduleSeed right = entries.get(second);
                if (left.day() == right.day() && left.start().isBefore(right.end())
                        && right.start().isBefore(left.end())) {
                    throw new IllegalStateException("Synthetic division timetable contains overlapping entries.");
                }
            }
        }
    }

    private static Map<String, Long> insertRoles(Connection connection) throws SQLException {
        Map<String, Long> ids = new LinkedHashMap<>();
        String sql = "INSERT INTO `role` (role_code, role_name, description) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            addRole(statement, ids, "ADMINISTRATOR", "Administrator");
            addRole(statement, ids, "FACULTY", "Faculty");
            addRole(statement, ids, "STUDENT", "Student");
            addRole(statement, ids, "COMMITTEE_COORDINATOR", "Committee Coordinator");
        }
        return ids;
    }

    private static void addRole(
            PreparedStatement statement, Map<String, Long> ids, String code, String name) throws SQLException {
        statement.setString(1, code);
        statement.setString(2, name);
        statement.setString(3, "DEMO authorization role.");
        statement.executeUpdate();
        ids.put(code, generatedId(statement));
    }

    private static long insertDepartment(Connection connection) throws SQLException {
        String sql = "INSERT INTO department (department_code, department_name, is_active) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "DEMO_CE");
            statement.setString(2, "DEMO Computer Engineering");
            statement.setBoolean(3, true);
            statement.executeUpdate();
            return generatedId(statement);
        }
    }

    private static Map<String, Long> insertUsers(
            Connection connection, List<UserSeed> users, Map<String, Long> roleIds,
            Map<String, String> passwordHashes) throws SQLException {
        Map<String, Long> ids = new LinkedHashMap<>();
        String sql = "INSERT INTO `user` (role_id, username, email, password_hash, first_name, last_name, "
                + "is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP(6), "
                + "CURRENT_TIMESTAMP(6))";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (UserSeed user : users) {
                statement.setLong(1, roleIds.get(user.roleCode()));
                statement.setString(2, user.username());
                statement.setString(3, user.email());
                statement.setString(4, passwordHashes.get(user.key()));
                statement.setString(5, user.firstName());
                statement.setString(6, user.lastName());
                statement.executeUpdate();
                ids.put(user.key(), generatedId(statement));
            }
        }
        return ids;
    }

    private static Map<String, Long> insertFacultyProfiles(
            Connection connection, Map<String, Long> userIds, long departmentId) throws SQLException {
        Map<String, Long> ids = new LinkedHashMap<>();
        String sql = "INSERT INTO faculty (user_id, department_id, employee_number, designation, "
                + "employment_status, qualification) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int index = 1; index <= 8; index++) {
                String key = facultyKey(index);
                statement.setLong(1, userIds.get(key));
                statement.setLong(2, departmentId);
                statement.setString(3, String.format(Locale.ROOT, "DEMO-EMP-%03d", index));
                statement.setString(4, "DEMO Instructor");
                statement.setString(5, "ACTIVE");
                statement.setString(6, "DEMO synthetic qualification");
                statement.executeUpdate();
                ids.put(key, generatedId(statement));
            }
        }
        return ids;
    }

    private static void insertStudentProfiles(
            Connection connection, Map<String, Long> userIds, long departmentId) throws SQLException {
        String sql = "INSERT INTO student (user_id, department_id, enrollment_number, admission_date, "
                + "current_semester, division, lab_batch, admission_year, academic_status) "
                + "VALUES (?, ?, ?, ?, 5, 'B', ?, 2024, 'ACTIVE')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 8; index++) {
                statement.setLong(1, userIds.get(studentKey(index)));
                statement.setLong(2, departmentId);
                statement.setString(3, String.format(Locale.ROOT, "DEMO-ENR-%03d", index));
                statement.setDate(4, java.sql.Date.valueOf(LocalDate.of(2024, 8, 1)));
                statement.setString(5, "B" + ((index - 1) / 2 + 1));
                statement.executeUpdate();
            }
        }
    }

    private static Map<String, Long> insertSubjects(
            Connection connection, long departmentId, Map<String, Long> facultyIds) throws SQLException {
        Map<String, Long> ids = new LinkedHashMap<>();
        String sql = "INSERT INTO subject (department_id, subject_code, subject_name, credit_value, semester, "
                + "academic_year, faculty_id, division) VALUES (?, ?, ?, ?, 5, ?, ?, 'B')";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (SubjectSeed subject : subjects()) {
                statement.setLong(1, departmentId);
                statement.setString(2, subject.code());
                statement.setString(3, subject.name());
                statement.setBigDecimal(4, java.math.BigDecimal.valueOf(subject.creditValue()));
                statement.setString(5, ACADEMIC_YEAR);
                statement.setLong(6, facultyIds.get(subject.facultyKey()));
                statement.executeUpdate();
                ids.put(subject.key(), generatedId(statement));
            }
        }
        return ids;
    }

    private static void insertTimetable(
            Connection connection, long departmentId, Map<String, Long> subjectIds,
            Map<String, Long> facultyIds) throws SQLException {
        String sql = "INSERT INTO timetable_entry (subject_id, faculty_id, department_id, semester, division, "
                + "lab_batch, day_of_week, start_time, end_time, venue, class_type, label, academic_year, "
                + "effective_from, is_active) VALUES (?, ?, ?, 5, 'B', ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (ScheduleSeed entry : timetable()) {
                if (entry.subjectKey() == null) {
                    statement.setNull(1, java.sql.Types.BIGINT);
                    statement.setNull(2, java.sql.Types.BIGINT);
                } else {
                    statement.setLong(1, subjectIds.get(entry.subjectKey()));
                    statement.setLong(2, facultyIds.get(subjectFacultyKey(entry.subjectKey())));
                }
                statement.setLong(3, departmentId);
                statement.setString(4, entry.labBatch());
                statement.setString(5, entry.day().name());
                statement.setTime(6, java.sql.Time.valueOf(entry.start()));
                statement.setTime(7, java.sql.Time.valueOf(entry.end()));
                if (entry.venue() == null) {
                    statement.setNull(8, java.sql.Types.VARCHAR);
                } else {
                    statement.setString(8, entry.venue());
                }
                statement.setString(9, entry.classType());
                statement.setString(10, entry.label());
                statement.setString(11, ACADEMIC_YEAR);
                statement.setDate(12, java.sql.Date.valueOf(EFFECTIVE_FROM));
                statement.executeUpdate();
            }
        }
    }

    static List<CommitteeSeed> committeeSeeds() {
        return List.of(
                new CommitteeSeed("SPORTS", "Sports Committee", "SPORTS", "demo.faculty01",
                        List.of("demo.student01", "demo.student02")),
                new CommitteeSeed("OCULUS", "Oculus Committee", "CULTURAL", "demo.faculty02",
                        List.of("demo.student03", "demo.student04")),
                new CommitteeSeed("E_CELL", "E-Cell", "ENTREPRENEURSHIP", "demo.faculty03",
                        List.of("demo.student05")),
                new CommitteeSeed("ROTARACT", "Rotaract", "SERVICE", "demo.faculty04",
                        List.of("demo.student06")),
                new CommitteeSeed("IEEE_CS", "IEEE CS", "TECHNICAL", "demo.faculty05",
                        List.of("demo.student07", "demo.student08")));
    }

    private static String facultyUsername(int index) {
        return "demo." + facultyKey(index);
    }

    private static String studentUsername(int index) {
        return "demo." + studentKey(index);
    }

    private static long generatedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("Database did not return a generated identifier.");
            }
            return keys.getLong(1);
        }
    }

    private static List<UserSeed> syntheticUsers() {
        List<UserSeed> users = new ArrayList<>();
        users.add(new UserSeed("admin", "ADMINISTRATOR", "demo.admin",
                "demo.admin@campussync.invalid", "DEMO", "Administrator"));
        for (int index = 1; index <= 8; index++) {
            String suffix = String.format(Locale.ROOT, "%02d", index);
            users.add(new UserSeed(facultyKey(index), "FACULTY", "demo.faculty" + suffix,
                    "demo.faculty" + suffix + "@campussync.invalid", "DEMO", "Faculty" + suffix));
        }
        for (int index = 1; index <= 8; index++) {
            String suffix = String.format(Locale.ROOT, "%02d", index);
            users.add(new UserSeed(studentKey(index), "STUDENT", "demo.student" + suffix,
                    "demo.student" + suffix + "@campussync.invalid", "DEMO", "Student" + suffix));
        }
        return List.copyOf(users);
    }

    private static Map<String, String> readPasswordHashes(List<UserSeed> users) {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException(
                    "A real interactive console is required so passwords are not echoed or supplied as arguments.");
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Set<String> fingerprints = new LinkedHashSet<>();
        Map<String, String> hashes = new LinkedHashMap<>();
        for (UserSeed user : users) {
            char[] password = console.readPassword("Enter unique password for %s (%s): ",
                    user.username(), user.email());
            char[] confirmation = console.readPassword("Confirm password for %s: ", user.username());
            try {
                if (password == null || confirmation == null
                        || !MessageDigest.isEqual(
                                new String(password).getBytes(StandardCharsets.UTF_8),
                                new String(confirmation).getBytes(StandardCharsets.UTF_8))) {
                    throw new IllegalArgumentException("Password confirmation failed.");
                }
                byte[] encoded = new String(password).getBytes(StandardCharsets.UTF_8);
                if (encoded.length < 8 || encoded.length > 72) {
                    throw new IllegalArgumentException(
                            "Each demo password must be between 8 and 72 UTF-8 bytes.");
                }
                String fingerprint = passwordFingerprint(encoded);
                if (!fingerprints.add(fingerprint)) {
                    throw new IllegalArgumentException("Each synthetic account must use a unique password.");
                }
                hashes.put(user.key(), encoder.encode(new String(password)));
            } finally {
                if (password != null) {
                    java.util.Arrays.fill(password, '\0');
                }
                if (confirmation != null) {
                    java.util.Arrays.fill(confirmation, '\0');
                }
            }
        }
        return hashes;
    }

    private static String passwordFingerprint(byte[] password) {
        try {
            return Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(password));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required to validate unique demo passwords.", exception);
        }
    }

    private static List<SubjectSeed> subjects() {
        return List.of(
                new SubjectSeed("DC", "DEMO-DC", "DEMO Distributed Computing", 3, "faculty01"),
                new SubjectSeed("SE", "DEMO-SE", "DEMO Software Engineering", 3, "faculty02"),
                new SubjectSeed("AISC", "DEMO-AISC", "DEMO Artificial Intelligence & Soft Computing", 3, "faculty03"),
                new SubjectSeed("TOC", "DEMO-TOC", "DEMO Theory of Computation", 3, "faculty04"),
                new SubjectSeed("CNS", "DEMO-CNS", "DEMO Cryptography & Network Security", 3, "faculty05"),
                new SubjectSeed("MDM_DSA", "DEMO-MDM-DSA", "DEMO MDM-II (DSA)", 3, "faculty06"),
                new SubjectSeed("MDM_IOT", "DEMO-MDM-IOT", "DEMO MDM-II (IOT)", 3, "faculty07"),
                new SubjectSeed("DSA", "DEMO-DSA", "DEMO Data Structures & Algorithms (standalone)", 3, "faculty08"));
    }

    private static List<ScheduleSeed> timetable() {
        List<ScheduleSeed> entries = new ArrayList<>();
        addDayBreaks(entries, DayOfWeek.MONDAY);
        add(entries, DayOfWeek.MONDAY, "09:00", "10:00", "DC", "ALL", "DEMO Room 101", "LECTURE", "DEMO lecture");
        add(entries, DayOfWeek.MONDAY, "10:00", "11:00", "SE", "ALL", "DEMO Room 101", "LECTURE", "DEMO lecture");
        add(entries, DayOfWeek.MONDAY, "11:15", "12:15", "AISC", "ALL", "DEMO Room 102", "LECTURE", "DEMO lecture");
        add(entries, DayOfWeek.MONDAY, "13:15", "14:15", "MDM_DSA", "ALL", "DEMO Room 102", "LECTURE", "DEMO lecture");

        addDayBreaks(entries, DayOfWeek.TUESDAY);
        add(entries, DayOfWeek.TUESDAY, "09:00", "11:00", "DSA", "B1", "DEMO Lab 1", "LAB", "DEMO B1 lab");
        add(entries, DayOfWeek.TUESDAY, "11:15", "12:15", "TOC", "ALL", "DEMO Room 101", "LECTURE", "DEMO lecture");
        add(entries, DayOfWeek.TUESDAY, "13:15", "14:15", "CNS", "ALL", "DEMO Room 102", "LECTURE", "DEMO lecture");

        addDayBreaks(entries, DayOfWeek.WEDNESDAY);
        add(entries, DayOfWeek.WEDNESDAY, "09:00", "11:00", "MDM_DSA", "B2", "DEMO Lab 2", "LAB", "DEMO B2 lab");
        add(entries, DayOfWeek.WEDNESDAY, "11:15", "12:15", "MDM_IOT", "ALL", "DEMO Room 101", "LECTURE", "DEMO lecture");
        add(entries, DayOfWeek.WEDNESDAY, "13:15", "14:15", "DC", "ALL", "DEMO Room 102", "LECTURE", "DEMO lecture");

        addDayBreaks(entries, DayOfWeek.THURSDAY);
        add(entries, DayOfWeek.THURSDAY, "09:00", "11:00", "MDM_IOT", "B3", "DEMO Lab 3", "LAB", "DEMO B3 lab");
        add(entries, DayOfWeek.THURSDAY, "11:15", "12:15", "SE", "ALL", "DEMO Room 101", "LECTURE", "DEMO lecture");
        add(entries, DayOfWeek.THURSDAY, "13:15", "14:15", "AISC", "ALL", "DEMO Room 102", "LECTURE", "DEMO lecture");

        addDayBreaks(entries, DayOfWeek.FRIDAY);
        add(entries, DayOfWeek.FRIDAY, "09:00", "11:00", "CNS", "B4", "DEMO Lab 4", "LAB", "DEMO B4 lab");
        add(entries, DayOfWeek.FRIDAY, "11:15", "12:15", "TOC", "ALL", "DEMO Room 101", "LECTURE", "DEMO lecture");
        add(entries, DayOfWeek.FRIDAY, "13:15", "14:15", "MDM_IOT", "ALL", "DEMO Room 102", "LECTURE", "DEMO lecture");
        add(entries, DayOfWeek.FRIDAY, "14:15", "15:15", "DSA", "ALL", "DEMO Room 101", "LECTURE", "DEMO lecture");
        return List.copyOf(entries);
    }

    private static void addDayBreaks(List<ScheduleSeed> entries, DayOfWeek day) {
        entries.add(new ScheduleSeed(day, LocalTime.of(11, 0), LocalTime.of(11, 15),
                null, "ALL", null, "BREAK", "DEMO short break"));
        entries.add(new ScheduleSeed(day, LocalTime.of(12, 15), LocalTime.of(13, 15),
                null, "ALL", null, "BREAK", "DEMO lunch break"));
    }

    private static void add(
            List<ScheduleSeed> entries, DayOfWeek day, String start, String end, String subject,
            String batch, String venue, String type, String label) {
        entries.add(new ScheduleSeed(day, LocalTime.parse(start), LocalTime.parse(end),
                subject, batch, venue, type, label));
    }

    private static String subjectFacultyKey(String subjectKey) {
        return subjects().stream()
                .filter(subject -> subject.key().equals(subjectKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown synthetic subject: " + subjectKey))
                .facultyKey();
    }

    private static String facultyKey(int index) {
        return "faculty%02d".formatted(index);
    }

    private static String studentKey(int index) {
        return "student%02d".formatted(index);
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable is not set: " + name);
        }
        return value;
    }

    private static void confirmConnectedTarget(String targetIdentity) {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("An interactive console is required for target confirmation.");
        }
        String confirmation = console.readLine(
                "Verify this is the isolated demo MySQL server. Type %s to proceed: ", targetIdentity);
        if (!targetIdentity.equals(confirmation)) {
            throw new IllegalStateException("Target confirmation did not match; no data was seeded.");
        }
    }

    private record UserSeed(
            String key, String roleCode, String username, String email, String firstName, String lastName) {
    }

    record CommitteeSeed(String code, String name, String category, String facultyUsername,
                         List<String> studentUsernames) {
    }

    private record SubjectSeed(String key, String code, String name, int creditValue, String facultyKey) {
    }

    private record ScheduleSeed(
            DayOfWeek day, LocalTime start, LocalTime end, String subjectKey, String labBatch,
            String venue, String classType, String label) {
    }
}
