package com.campussync.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record StudentAttendanceResponse(
        int thresholdPercent,
        long attendedClasses,
        long heldClasses,
        BigDecimal overallPercentage,
        boolean belowThreshold,
        List<SubjectAttendance> subjects,
        List<AttendanceHistory> history
) {
    public record SubjectAttendance(Long subjectId, String subjectCode, String subjectName,
                                    long attendedClasses, long heldClasses,
                                    BigDecimal percentage, boolean belowThreshold) {
    }

    public record AttendanceHistory(Long recordId, Long sessionId, LocalDate sessionDate,
                                    LocalTime startTime, String subjectCode, String subjectName, String status) {
    }
}
