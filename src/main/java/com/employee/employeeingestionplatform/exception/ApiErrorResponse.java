package com.employee.employeeingestionplatform.exception;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId
) {
}