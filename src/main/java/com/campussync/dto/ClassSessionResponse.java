package com.campussync.dto;

import com.campussync.entity.ClassSession;
import java.time.LocalDate;
import java.time.LocalTime;

public record ClassSessionResponse(
        Long id,
        Long timetableEntryId,
        Long subjectId,
        String subjectCode,
        String subjectName,
        Long departmentId,
        Integer semester,
        String division,
        String labBatch,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        String status
) {
    public static ClassSessionResponse from(ClassSession session) {
        return new ClassSessionResponse(session.getId(),
                session.getTimetableEntry() == null ? null : session.getTimetableEntry().getId(),
                session.getSubject().getId(),
                session.getSubject().getCode(), session.getSubject().getName(),
                session.getDepartment().getId(), session.getSemester(), session.getDivision(),
                session.getLabBatch(),
                session.getSessionDate(), session.getStartTime(), session.getEndTime(), session.getStatus());
    }
}
