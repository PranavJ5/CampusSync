package com.campussync.service;

import com.campussync.dto.StudentRequest;
import com.campussync.dto.StudentResponse;
import com.campussync.dto.StudentContactRequest;
import com.campussync.dto.StudentLabBatchRequest;
import com.campussync.entity.Student;
import com.campussync.entity.User;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.StudentRepository;
import com.campussync.repository.UserRepository;
import com.campussync.repository.FacultyRepository;
import java.security.Principal;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Service
public class StudentService {
    private final StudentRepository students;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final AuthorizationService authorization;
    private final FacultyRepository faculty;

    public StudentService(StudentRepository students, UserRepository users, DepartmentRepository departments,
                          AuthorizationService authorization, FacultyRepository faculty) {
        this.students = students; this.users = users; this.departments = departments; this.authorization = authorization; this.faculty = faculty;
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> list(Principal principal, Pageable pageable, Long departmentId,
                                      Integer semester, Integer admissionYear, String search) {
        User actor = authorization.authenticatedUser(principal);
        boolean admin = actor.getRole() != null && "ADMINISTRATOR".equalsIgnoreCase(actor.getRole().getCode());
        Long scopedDepartment = departmentId;
        if (!admin) {
            if (actor.getRole() == null || !"FACULTY".equalsIgnoreCase(actor.getRole().getCode()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator or faculty role is required.");
            var profile = faculty.findByUserId(actor.getId()).orElseThrow(() -> new NoSuchElementException("Faculty profile not found."));
            if (departmentId != null && !departmentId.equals(profile.getDepartment().getId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Faculty may list students from their own department only.");
            scopedDepartment = profile.getDepartment().getId();
        }
        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.max(1, Math.min(25, pageable.getPageSize()));
        Specification<Student> spec = (root, query, cb) -> cb.conjunction();
        if (scopedDepartment != null) {
            Long id = scopedDepartment;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("department").get("id"), id));
        }
        if (semester != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("currentSemester"), semester));
        if (admissionYear != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("admissionYear"), admissionYear));
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.join("user").get("firstName")), pattern),
                    cb.like(cb.lower(root.join("user").get("lastName")), pattern),
                    cb.like(cb.lower(root.get("enrollmentNumber")), pattern)));
        }
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by("id").ascending();
        return students.findAll(spec, PageRequest.of(page, size, sort)).map(StudentResponse::from);
    }

    @Transactional(readOnly = true)
    public StudentResponse get(Long id, Principal principal) {
        User actor = authorization.authenticatedUser(principal);
        Student profile = find(id);
        requireSelfOrAdmin(actor, profile);
        return StudentResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public StudentResponse me(Principal principal) {
        User actor = authorization.requireRole(principal, "STUDENT");
        Student profile = students.findByUserId(actor.getId())
                .orElseThrow(() -> new NoSuchElementException("Student profile not found."));
        return StudentResponse.from(profile);
    }

    @Transactional
    public StudentResponse updateOwnContact(StudentContactRequest request, Principal principal) {
        User actor = authorization.requireRole(principal, "STUDENT");
        Student student = students.findByUserId(actor.getId())
                .orElseThrow(() -> new NoSuchElementException("Student profile not found."));
        student.setContactNumber(request.contactNumber()); student.setAlternateEmail(request.alternateEmail());
        student.setPhotoPath(request.photoPath());
        return StudentResponse.from(students.saveAndFlush(student));
    }

    @Transactional
    public StudentResponse create(StudentRequest request, Principal principal) {
        authorization.requireRole(principal, "ADMINISTRATOR");
        User user = findStudentUser(request.userId());
        if (students.findByUserId(user.getId()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A student profile already exists for this account.");
        Student student = new Student();
        apply(student, request, user);
        return StudentResponse.from(students.saveAndFlush(student));
    }

    @Transactional
    public StudentResponse update(Long id, StudentRequest request, Principal principal) {
        authorization.requireRole(principal, "ADMINISTRATOR");
        Student student = find(id);
        User user = findStudentUser(request.userId());
        if (!student.getUser().getId().equals(user.getId()) && students.findByUserId(user.getId()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A student profile already exists for this account.");
        apply(student, request, user);
        return StudentResponse.from(students.saveAndFlush(student));
    }

    @Transactional
    public StudentResponse updateLabBatch(Long id, StudentLabBatchRequest request, Principal principal) {
        authorization.requireRole(principal, "ADMINISTRATOR");
        Student student = find(id);
        student.setLabBatch(request.labBatch() == null
                ? null : request.labBatch().toUpperCase(java.util.Locale.ROOT));
        return StudentResponse.from(students.saveAndFlush(student));
    }

    private void requireSelfOrAdmin(User actor, Student profile) {
        boolean admin = actor.getRole() != null && "ADMINISTRATOR".equalsIgnoreCase(actor.getRole().getCode());
        boolean self = actor.getRole() != null && "STUDENT".equalsIgnoreCase(actor.getRole().getCode())
                && actor.getId().equals(profile.getUser().getId());
        if (!admin && !self) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student profile access is restricted.");
    }

    private User findStudentUser(Long id) {
        User user = users.findById(id).orElseThrow(() -> new NoSuchElementException("User not found."));
        if (user.getRole() == null || !"STUDENT".equalsIgnoreCase(user.getRole().getCode()))
            throw new IllegalArgumentException("Student records must reference a STUDENT account.");
        return user;
    }

    private Student find(Long id) {
        return students.findById(id).orElseThrow(() -> new NoSuchElementException("Student record not found."));
    }

    private void apply(Student target, StudentRequest request, User user) {
        target.setUser(user);
        target.setDepartment(departments.findById(request.departmentId())
                .orElseThrow(() -> new NoSuchElementException("Department not found.")));
        target.setEnrollmentNumber(request.enrollmentNumber().trim());
        target.setAdmissionDate(request.admissionDate());
        target.setAcademicStatus(request.academicStatus().trim());
        target.setCurrentSemester(request.currentSemester()); target.setAdmissionYear(request.admissionYear());
        target.setDivision(request.division() == null || request.division().isBlank()
                ? null : request.division().trim());
        target.setContactNumber(request.contactNumber()); target.setAlternateEmail(request.alternateEmail());
        target.setDateOfBirth(request.dateOfBirth()); target.setGender(request.gender()); target.setPhotoPath(request.photoPath());
    }
}
