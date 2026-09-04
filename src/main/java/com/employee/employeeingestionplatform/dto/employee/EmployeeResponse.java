package com.employee.employeeingestionplatform.dto.employee;

import com.employee.employeeingestionplatform.entity.EmployeeSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EmployeeResponse(
        Long id,
        String empId,
        String firstName,
        String lastName,
        String email,
        String department,
        BigDecimal salary,
        EmployeeSource source,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}