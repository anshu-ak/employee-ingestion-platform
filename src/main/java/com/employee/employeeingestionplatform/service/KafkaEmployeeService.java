package com.employee.employeeingestionplatform.service;

import com.employee.employeeingestionplatform.dto.kafka.EmployeeKafkaEvent;
import com.employee.employeeingestionplatform.entity.Employee;
import com.employee.employeeingestionplatform.entity.EmployeeSource;
import com.employee.employeeingestionplatform.repository.EmployeeRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KafkaEmployeeService {

    private final EmployeeRepository employeeRepository;
    private final Validator validator;

    public KafkaEmployeeService(
            EmployeeRepository employeeRepository,
            Validator validator
    ) {
        this.employeeRepository = employeeRepository;
        this.validator = validator;
    }

    @Transactional
    public void ingest(EmployeeKafkaEvent originalEvent) {
        EmployeeKafkaEvent event = normalize(originalEvent);

        validate(event);

        if (employeeRepository.existsByEmpId(event.empId())) {
            throw new IllegalArgumentException(
                    "Employee ID already exists: " + event.empId()
            );
        }

        if (employeeRepository.existsByEmailIgnoreCase(
                event.email()
        )) {
            throw new IllegalArgumentException(
                    "Employee email already exists: "
                            + event.email()
            );
        }

        Employee employee = new Employee();
        employee.setEmpId(event.empId());
        employee.setFirstName(event.firstName());
        employee.setLastName(event.lastName());
        employee.setEmail(event.email());
        employee.setDepartment(event.department());
        employee.setSalary(event.salary());
        employee.setSource(EmployeeSource.KAFKA);

        employeeRepository.save(employee);
    }

    private EmployeeKafkaEvent normalize(
            EmployeeKafkaEvent event
    ) {
        return new EmployeeKafkaEvent(
                trim(event.empId()),
                trim(event.firstName()),
                trim(event.lastName()),
                normalizeEmail(event.email()),
                trim(event.department()),
                event.salary()
        );
    }

    private void validate(EmployeeKafkaEvent event) {
        Set<ConstraintViolation<EmployeeKafkaEvent>> violations =
                validator.validate(event);

        if (violations.isEmpty()) {
            return;
        }

        String message = violations.stream()
                .map(violation ->
                        violation.getPropertyPath()
                                + ": "
                                + violation.getMessage()
                )
                .sorted()
                .collect(Collectors.joining("; "));

        throw new IllegalArgumentException(message);
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