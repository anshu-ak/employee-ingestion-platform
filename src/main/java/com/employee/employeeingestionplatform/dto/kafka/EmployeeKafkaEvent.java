package com.employee.employeeingestionplatform.dto.kafka;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EmployeeKafkaEvent(

        @NotBlank
        @Size(max = 50)
        String empId,

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(max = 100)
        String department,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal salary
) {
}