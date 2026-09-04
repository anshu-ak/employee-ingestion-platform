package com.employee.employeeingestionplatform.batch.exception;

import com.employee.employeeingestionplatform.batch.model.EmployeeExcelRow;

import java.util.List;

public class EmployeeRowValidationException extends RuntimeException {

    private final EmployeeExcelRow row;
    private final List<String> validationErrors;

    public EmployeeRowValidationException(
            EmployeeExcelRow row,
            List<String> validationErrors
    ) {
        super(String.join("; ", validationErrors));
        this.row = row;
        this.validationErrors = List.copyOf(validationErrors);
    }

    public EmployeeExcelRow getRow() {
        return row;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}