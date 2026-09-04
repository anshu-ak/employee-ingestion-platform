package com.employee.employeeingestionplatform.service;

import com.employee.employeeingestionplatform.dto.kafka.EmployeeKafkaEvent;
import com.employee.employeeingestionplatform.entity.Employee;
import com.employee.employeeingestionplatform.entity.EmployeeSource;
import com.employee.employeeingestionplatform.repository.EmployeeRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaEmployeeServiceTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @Mock
    private EmployeeRepository employeeRepository;

    private KafkaEmployeeService kafkaEmployeeService;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation
                .buildDefaultValidatorFactory();

        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        kafkaEmployeeService = new KafkaEmployeeService(
                employeeRepository,
                validator
        );
    }

    @Test
    void shouldSaveValidKafkaEmployee() {
        EmployeeKafkaEvent event = validEvent();

        when(employeeRepository.existsByEmpId("KAFKA001"))
                .thenReturn(false);

        when(employeeRepository.existsByEmailIgnoreCase(
                "kafka001@example.com"
        )).thenReturn(false);

        kafkaEmployeeService.ingest(event);

        ArgumentCaptor<Employee> employeeCaptor =
                ArgumentCaptor.forClass(Employee.class);

        verify(employeeRepository).save(
                employeeCaptor.capture()
        );

        Employee employee = employeeCaptor.getValue();

        assertAll(
                () -> assertEquals(
                        "KAFKA001",
                        employee.getEmpId()
                ),
                () -> assertEquals(
                        "Anshu",
                        employee.getFirstName()
                ),
                () -> assertEquals(
                        "Kumari",
                        employee.getLastName()
                ),
                () -> assertEquals(
                        "kafka001@example.com",
                        employee.getEmail()
                ),
                () -> assertEquals(
                        "Engineering",
                        employee.getDepartment()
                ),
                () -> assertEquals(
                        new BigDecimal("125000"),
                        employee.getSalary()
                ),
                () -> assertEquals(
                        EmployeeSource.KAFKA,
                        employee.getSource()
                )
        );
    }

    @Test
    void shouldRejectInvalidKafkaEvent() {
        EmployeeKafkaEvent event = new EmployeeKafkaEvent(
                "",
                "Test",
                "Employee",
                "invalid-email",
                "Engineering",
                new BigDecimal("-1")
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> kafkaEmployeeService.ingest(event)
                );

        assertAll(
                () -> assertTrue(
                        exception.getMessage().contains("empId")
                ),
                () -> assertTrue(
                        exception.getMessage().contains("email")
                ),
                () -> assertTrue(
                        exception.getMessage().contains("salary")
                )
        );

        verifyNoInteractions(employeeRepository);
    }

    @Test
    void shouldRejectDuplicateEmployeeId() {
        EmployeeKafkaEvent event = validEvent();

        when(employeeRepository.existsByEmpId("KAFKA001"))
                .thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> kafkaEmployeeService.ingest(event)
                );

        assertEquals(
                "Employee ID already exists: KAFKA001",
                exception.getMessage()
        );

        verify(employeeRepository, never())
                .save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectDuplicateEmailIgnoringCase() {
        EmployeeKafkaEvent event = validEvent();

        when(employeeRepository.existsByEmpId("KAFKA001"))
                .thenReturn(false);

        when(employeeRepository.existsByEmailIgnoreCase(
                "kafka001@example.com"
        )).thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> kafkaEmployeeService.ingest(event)
                );

        assertEquals(
                "Employee email already exists: "
                        + "kafka001@example.com",
                exception.getMessage()
        );

        verify(employeeRepository, never())
                .save(org.mockito.ArgumentMatchers.any());
    }

    private EmployeeKafkaEvent validEvent() {
        return new EmployeeKafkaEvent(
                " KAFKA001 ",
                " Anshu ",
                " Kumari ",
                " KAFKA001@EXAMPLE.COM ",
                " Engineering ",
                new BigDecimal("125000")
        );
    }
}