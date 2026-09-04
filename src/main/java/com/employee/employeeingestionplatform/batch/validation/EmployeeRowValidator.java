package com.employee.employeeingestionplatform.batch.validation;

import com.employee.employeeingestionplatform.batch.model.EmployeeExcelRow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class EmployeeRowValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    public List<String> validate(EmployeeExcelRow row) {
        List<String> errors = new ArrayList<>();

        if (isBlank(row.empId())) {
            errors.add("Employee ID is required");
        } else if (row.empId().length() > 50) {
            errors.add("Employee ID cannot exceed 50 characters");
        }

        if (isBlank(row.firstName())) {
            errors.add("First name is required");
        } else if (row.firstName().length() > 100) {
            errors.add("First name cannot exceed 100 characters");
        }

        if (isBlank(row.lastName())) {
            errors.add("Last name is required");
        } else if (row.lastName().length() > 100) {
            errors.add("Last name cannot exceed 100 characters");
        }

        if (isBlank(row.email())) {
            errors.add("Email is required");
        } else if (row.email().length() > 255) {
            errors.add("Email cannot exceed 255 characters");
        } else if (!EMAIL_PATTERN.matcher(row.email()).matches()) {
            errors.add("Email format is invalid");
        }

        if (isBlank(row.department())) {
            errors.add("Department is required");
        } else if (row.department().length() > 100) {
            errors.add("Department cannot exceed 100 characters");
        }

        if (row.salary() == null) {
            errors.add("Salary is required");
        } else if (row.salary().signum() < 0) {
            errors.add("Salary cannot be negative");
        }

        return errors;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}