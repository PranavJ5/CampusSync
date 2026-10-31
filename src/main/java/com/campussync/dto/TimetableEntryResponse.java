package com.campussync.dto;

import com.campussync.entity.TimetableEntry;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public record TimetableEntryResponse(
        Long id,
        Long subjectId,
        String subjectCode,
        String subjectName,
        Long facultyId,
        String facultyName,
        Long departmentId,
        String departmentName,
        Integer semester,
        String division,
        String labBatch,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String venue,
        String classType,
        String label,
        String academicYear,
        LocalDate effectiveFrom,
        boolean active) {

    public static TimetableEntryResponse from(TimetableEntry entry) {
        var subject = entry.getSubject();
        var faculty = entry.getFaculty();
        return new TimetableEntryResponse(
                entry.getId(),
                subject == null ? null : subject.getId(),
                subject == null ? null : subject.getCode(),
                subject == null ? null : subject.getName(),
                faculty == null ? null : faculty.getId(),
                faculty == null ? null : (faculty.getUser().getFirstName() + " " + faculty.getUser().getLastName()).trim(),
                entry.getDepartment().getId(),
                entry.getDepartment().getName(),
                entry.getSemester(),
                entry.getDivision(),
                "ALL".equalsIgnoreCase(entry.getLabBatch()) ? null : entry.getLabBatch(),
                entry.getDayOfWeek(),
                entry.getStartTime(),
                entry.getEndTime(),
                entry.getVenue(),
                entry.getClassType(),
                entry.getLabel(),
                entry.getAcademicYear(),
                entry.getEffectiveFrom(),
                entry.isActive());
    }
}
