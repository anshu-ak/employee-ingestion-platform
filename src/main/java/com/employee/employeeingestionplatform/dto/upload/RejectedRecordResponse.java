package com.employee.employeeingestionplatform.dto.upload;

import java.time.OffsetDateTime;

public record RejectedRecordResponse(
        Long id,
        int rowNumber,
        String empId,
        String reason,
        OffsetDateTime createdAt
) {
}