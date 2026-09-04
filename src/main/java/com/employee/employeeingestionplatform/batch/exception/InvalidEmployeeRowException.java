package com.employee.employeeingestionplatform.batch.exception;

import com.employee.employeeingestionplatform.batch.model.EmployeeExcelRow;

public class InvalidEmployeeRowException extends RuntimeException {

    private final EmployeeExcelRow row;

    public InvalidEmployeeRowException(
            EmployeeExcelRow row,
            String message
    ) {
        super(message);
        this.row = row;
    }

    public EmployeeExcelRow getRow() {
        return row;
    }
}