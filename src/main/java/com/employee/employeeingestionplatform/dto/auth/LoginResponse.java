package com.employee.employeeingestionplatform.dto.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}