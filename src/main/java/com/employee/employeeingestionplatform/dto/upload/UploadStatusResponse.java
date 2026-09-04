package com.employee.employeeingestionplatform.dto.upload;

import com.employee.employeeingestionplatform.entity.UploadStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UploadStatusResponse(
        UUID trackingId,
        String fileName,
        UploadStatus status,
        int totalRows,
        int successRows,
        int rejectedRows,
        String errorSummary,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt
) {
}