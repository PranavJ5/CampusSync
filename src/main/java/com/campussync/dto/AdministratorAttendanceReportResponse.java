package com.campussync.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdministratorAttendanceReportResponse(int thresholdPercent, List<Row> rows) {
    public record Row(Long departmentId, String departmentName, String division,
                      Long subjectId, String subjectCode, String subjectName,
                      Long facultyId, String facultyName,
                      long heldClasses, long recordedStudents, long attendedRecords,
                      BigDecimal percentage, boolean belowThreshold) {
    }
}
