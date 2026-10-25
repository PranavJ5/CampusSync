package com.campussync.service;

import com.campussync.dto.SubjectRequest;
import com.campussync.dto.SubjectResponse;
import com.campussync.entity.Subject;
import com.campussync.repository.DepartmentRepository;
import com.campussync.repository.SubjectRepository;
import com.campussync.repository.FacultyRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {
    private final SubjectRepository subjects;
    private final DepartmentRepository departments;
    private final FacultyRepository faculty;

    public SubjectService(SubjectRepository subjects, DepartmentRepository departments, FacultyRepository faculty) {
        this.subjects = subjects; this.departments = departments; this.faculty = faculty;
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> list() { return subjects.findAll().stream().map(SubjectResponse::from).toList(); }

    @Transactional(readOnly = true)
    public SubjectResponse get(Long id) {
        Subject subject = subjects.findById(id).orElseThrow(() -> new NoSuchElementException("Subject not found."));
        return SubjectResponse.from(subject);
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> listByDepartment(Long departmentId) {
        if (!departments.existsById(departmentId)) throw new NoSuchElementException("Department not found.");
        return subjects.findByDepartmentId(departmentId).stream().map(SubjectResponse::from).toList();
    }

    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        Subject subject = new Subject(); apply(subject, request);
        return SubjectResponse.from(subjects.saveAndFlush(subject));
    }

    @Transactional
    public SubjectResponse update(Long id, SubjectRequest request) {
        Subject subject = subjects.findById(id).orElseThrow(() -> new NoSuchElementException("Subject not found."));
        apply(subject, request);
        return SubjectResponse.from(subjects.saveAndFlush(subject));
    }

    private void apply(Subject subject, SubjectRequest request) {
        subject.setDepartment(departments.findById(request.departmentId()).orElseThrow(() -> new NoSuchElementException("Department not found.")));
        subject.setCode(request.code().trim()); subject.setName(request.name().trim()); subject.setCreditValue(request.creditValue());
        subject.setSemester(request.semester()); subject.setAcademicYear(request.academicYear().trim());
        subject.setFaculty(request.facultyId() == null ? null : faculty.findById(request.facultyId())
                .orElseThrow(() -> new NoSuchElementException("Faculty record not found.")));
        subject.setDivision(request.division());
    }
}
