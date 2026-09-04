package com.employee.employeeingestionplatform.repository;

import com.employee.employeeingestionplatform.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface EmployeeRepository
        extends JpaRepository<Employee, Long>,
        JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByEmpId(String empId);

    boolean existsByEmpId(String empId);

    boolean existsByEmailIgnoreCase(String email);
}