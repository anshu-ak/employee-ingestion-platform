package com.employee.employeeingestionplatform.batch.writer;

import com.employee.employeeingestionplatform.entity.Employee;
import com.employee.employeeingestionplatform.repository.EmployeeRepository;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class EmployeeItemWriter implements ItemWriter<Employee> {

    private final EmployeeRepository employeeRepository;

    public EmployeeItemWriter(
            EmployeeRepository employeeRepository
    ) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public void write(Chunk<? extends Employee> chunk) {
        employeeRepository.saveAll(chunk.getItems());
    }
}