package com.employee.employeeingestionplatform.batch.validation;

import com.employee.employeeingestionplatform.batch.model.EmployeeExcelRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmployeeRowValidatorTest {

    private EmployeeRowValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EmployeeRowValidator();
    }

    @Test
    void shouldAcceptValidEmployeeRow() {
        EmployeeExcelRow row = new EmployeeExcelRow(
                2,
                "EMP001",
                "Anshu",
                "Kumari",
                "anshu@example.com",
                "Engineering",
                new BigDecimal("125000")
        );

        List<String> errors = validator.validate(row);

        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldRejectMissingRequiredFields() {
        EmployeeExcelRow row = new EmployeeExcelRow(
                2,
                null,
                "",
                " ",
                null,
                "",
                null
        );

        List<String> errors = validator.validate(row);

        assertAll(
                () -> assertTrue(
                        errors.contains("Employee ID is required")
                ),
                () -> assertTrue(
                        errors.contains("First name is required")
                ),
                () -> assertTrue(
                        errors.contains("Last name is required")
                ),
                () -> assertTrue(
                        errors.contains("Email is required")
                ),
                () -> assertTrue(
                        errors.contains("Department is required")
                ),
                () -> assertTrue(
                        errors.contains("Salary is required")
                )
        );
    }

    @Test
    void shouldRejectInvalidEmail() {
        EmployeeExcelRow row = new EmployeeExcelRow(
                2,
                "EMP002",
                "Test",
                "Employee",
                "invalid-email",
                "Finance",
                new BigDecimal("75000")
        );

        List<String> errors = validator.validate(row);

        assertEquals(1, errors.size());
        assertEquals(
                "Email format is invalid",
                errors.getFirst()
        );
    }

    @Test
    void shouldRejectNegativeSalary() {
        EmployeeExcelRow row = new EmployeeExcelRow(
                2,
                "EMP003",
                "Test",
                "Employee",
                "employee@example.com",
                "Finance",
                new BigDecimal("-1")
        );

        List<String> errors = validator.validate(row);

        assertEquals(1, errors.size());
        assertEquals(
                "Salary cannot be negative",
                errors.getFirst()
        );
    }
}