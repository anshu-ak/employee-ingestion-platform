package com.employee.employeeingestionplatform.batch.processor;

import com.employee.employeeingestionplatform.batch.exception.InvalidEmployeeRowException;
import com.employee.employeeingestionplatform.batch.model.EmployeeExcelRow;
import com.employee.employeeingestionplatform.batch.validation.EmployeeRowValidator;
import com.employee.employeeingestionplatform.entity.Employee;
import com.employee.employeeingestionplatform.entity.EmployeeSource;
import com.employee.employeeingestionplatform.repository.EmployeeRepository;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.util.List;
import java.util.Locale;

public class EmployeeItemProcessor
        implements ItemProcessor<EmployeeExcelRow, Employee> {

    private final EmployeeRowValidator rowValidator;
    private final EmployeeRepository employeeRepository;

    public EmployeeItemProcessor(
            EmployeeRowValidator rowValidator,
            EmployeeRepository employeeRepository
    ) {
        this.rowValidator = rowValidator;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public Employee process(EmployeeExcelRow originalRow) {
        EmployeeExcelRow normalizedRow = normalize(originalRow);

        List<String> validationErrors =
                rowValidator.validate(normalizedRow);

        if (!validationErrors.isEmpty()) {
            throw new InvalidEmployeeRowException(
                    originalRow,
                    String.join("; ", validationErrors)
            );
        }

        if (employeeRepository.existsByEmpId(
                normalizedRow.empId()
        )) {
            throw new InvalidEmployeeRowException(
                    originalRow,
                    "Employee ID already exists: "
                            + normalizedRow.empId()
            );
        }

        if (employeeRepository.existsByEmailIgnoreCase(
                normalizedRow.email()
        )) {
            throw new InvalidEmployeeRowException(
                    originalRow,
                    "Employee email already exists: "
                            + normalizedRow.email()
            );
        }

        Employee employee = new Employee();
        employee.setEmpId(normalizedRow.empId());
        employee.setFirstName(normalizedRow.firstName());
        employee.setLastName(normalizedRow.lastName());
        employee.setEmail(normalizedRow.email());
        employee.setDepartment(normalizedRow.department());
        employee.setSalary(normalizedRow.salary());
        employee.setSource(EmployeeSource.EXCEL);

        return employee;
    }

    private EmployeeExcelRow normalize(EmployeeExcelRow row) {
        return new EmployeeExcelRow(
                row.rowNumber(),
                trim(row.empId()),
                trim(row.firstName()),
                trim(row.lastName()),
                normalizeEmail(row.email()),
                trim(row.department()),
                row.salary()
        );
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeEmail(String email) {
        String trimmedEmail = trim(email);

        return trimmedEmail == null
                ? null
                : trimmedEmail.toLowerCase(Locale.ROOT);
    }
}