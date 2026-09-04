package com.employee.employeeingestionplatform.service;

import com.employee.employeeingestionplatform.dto.employee.EmployeeResponse;
import com.employee.employeeingestionplatform.dto.employee.PageResponse;
import com.employee.employeeingestionplatform.entity.Employee;
import com.employee.employeeingestionplatform.repository.EmployeeRepository;
import com.employee.employeeingestionplatform.entity.EmployeeSource;
import com.employee.employeeingestionplatform.specification.EmployeeSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

import java.math.BigDecimal;

@Service
public class EmployeeService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "empId",
            "firstName",
            "lastName",
            "email",
            "department",
            "salary",
            "source",
            "createdAt",
            "updatedAt"
    );

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    private Sort createSort(String sortValue) {
        String effectiveSort =
                sortValue == null || sortValue.isBlank()
                        ? "id,asc"
                        : sortValue.trim();

        String[] parts = effectiveSort.split(",", -1);

        if (parts.length > 2) {
            throw invalidSort();
        }

        String field = parts[0].trim();

        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw invalidSort();
        }

        Sort.Direction direction = Sort.Direction.ASC;

        if (parts.length == 2) {
            String directionValue = parts[1]
                    .trim()
                    .toLowerCase(Locale.ROOT);

            if ("asc".equals(directionValue)) {
                direction = Sort.Direction.ASC;
            } else if ("desc".equals(directionValue)) {
                direction = Sort.Direction.DESC;
            } else {
                throw invalidSort();
            }
        }

        return Sort.by(direction, field);
    }

    private ResponseStatusException invalidSort() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "sort must use an allowed field and direction, "
                        + "for example: salary,desc"
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> getEmployees(
            int page,
            int size,
            String empId,
            String department,
            String email,
            EmployeeSource source,
            BigDecimal minSalary,
            BigDecimal maxSalary,
            String sort
    ) {
        if (minSalary != null
                && maxSalary != null
                && minSalary.compareTo(maxSalary) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "minSalary cannot be greater than maxSalary"
            );
        }

        Sort requestedSort = createSort(sort);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                requestedSort
        );

        Specification<Employee> specification =
                EmployeeSpecification.withFilters(
                        empId,
                        department,
                        email,
                        source,
                        minSalary,
                        maxSalary
                );

        Page<EmployeeResponse> result = employeeRepository
                .findAll(specification, pageRequest)
                .map(this::toResponse);

        return PageResponse.from(result);
    }
    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmpId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getDepartment(),
                employee.getSalary(),
                employee.getSource(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}