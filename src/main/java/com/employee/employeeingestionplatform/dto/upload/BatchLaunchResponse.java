package com.employee.employeeingestionplatform.dto.upload;

import com.employee.employeeingestionplatform.entity.UploadStatus;

import java.util.UUID;

public record BatchLaunchResponse(
        UUID trackingId,
        long jobExecutionId,
        String batchStatus,
        UploadStatus uploadStatus
) {
}