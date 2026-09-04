package com.employee.employeeingestionplatform.dto.upload;

import com.employee.employeeingestionplatform.entity.UploadStatus;

import java.util.UUID;

public record UploadResponse(
        UUID trackingId,
        String fileName,
        UploadStatus status,
        String message
) {
}