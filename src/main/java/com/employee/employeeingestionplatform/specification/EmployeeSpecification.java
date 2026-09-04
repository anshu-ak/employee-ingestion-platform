package com.employee.employeeingestionplatform.specification;

import com.employee.employeeingestionplatform.entity.Employee;
import com.employee.employeeingestionplatform.entity.EmployeeSource;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class EmployeeSpecification {

    private EmployeeSpecification() {
    }

    public static Specification<Employee> withFilters(
            String empId,
            String department,
            String email,
            EmployeeSource source,
            BigDecimal minSalary,
            BigDecimal maxSalary
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (empId != null && !empId.isBlank()) {
                predicates.add(
                        criteriaBuilder.equal(
                                criteriaBuilder.lower(
                                        root.get("empId")
                                ),
                                empId.trim().toLowerCase()
                        )
                );
            }

            if (department != null && !department.isBlank()) {
                predicates.add(
                        criteriaBuilder.equal(
                                criteriaBuilder.lower(
                                        root.get("department")
                                ),
                                department.trim().toLowerCase()
                        )
                );
            }

            if (email != null && !email.isBlank()) {
                predicates.add(
                        criteriaBuilder.equal(
                                criteriaBuilder.lower(
                                        root.get("email")
                                ),
                                email.trim().toLowerCase()
                        )
                );
            }

            if (source != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("source"),
                                source
                        )
                );
            }

            if (minSalary != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("salary"),
                                minSalary
                        )
                );
            }

            if (maxSalary != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("salary"),
                                maxSalary
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}