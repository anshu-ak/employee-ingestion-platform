package com.employee.employeeingestionplatform.batch.model;

import java.math.BigDecimal;

public record EmployeeExcelRow(
        int rowNumber,
        String empId,
        String firstName,
        String lastName,
        String email,
        String department,
        BigDecimal salary
) {
}