package com.employee.employeeingestionplatform.controller;

import com.employee.employeeingestionplatform.dto.employee.EmployeeResponse;
import com.employee.employeeingestionplatform.dto.employee.PageResponse;
import com.employee.employeeingestionplatform.service.EmployeeService;
import com.employee.employeeingestionplatform.entity.EmployeeSource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.employee.employeeingestionplatform.config.OpenApiConfig;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/employees")
@Validated
@SecurityRequirement(
        name = OpenApiConfig.SECURITY_SCHEME_NAME
)
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public PageResponse<EmployeeResponse> getEmployees(
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size,

            @RequestParam(required = false)
            String empId,

            @RequestParam(required = false)
            String department,

            @RequestParam(required = false)
            String email,

            @RequestParam(required = false)
            EmployeeSource source,

            @RequestParam(required = false)
            @DecimalMin(value = "0.0", inclusive = true)
            BigDecimal minSalary,

            @RequestParam(required = false)
            @DecimalMin(value = "0.0", inclusive = true)
            BigDecimal maxSalary,

            @RequestParam(defaultValue = "id,asc")
            String sort
    ) {
        return employeeService.getEmployees(
                page,
                size,
                empId,
                department,
                email,
                source,
                minSalary,
                maxSalary,
                sort
        );
    }
}