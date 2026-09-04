package com.employee.employeeingestionplatform.batch.processor;

import com.employee.employeeingestionplatform.batch.exception.InvalidEmployeeRowException;
import com.employee.employeeingestionplatform.batch.model.EmployeeExcelRow;
import com.employee.employeeingestionplatform.batch.validation.EmployeeRowValidator;
import com.employee.employeeingestionplatform.entity.Employee;
import com.employee.employeeingestionplatform.entity.EmployeeSource;
import com.employee.employeeingestionplatform.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeItemProcessorTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private EmployeeItemProcessor processor;

    @BeforeEach
    void setUp() {
        EmployeeRowValidator validator =
                new EmployeeRowValidator();

        processor = new EmployeeItemProcessor(
                validator,
                employeeRepository
        );
    }

    @Test
    void shouldConvertValidRowToEmployee() {
        EmployeeExcelRow row = validRow();

        when(employeeRepository.existsByEmpId("EMP001"))
                .thenReturn(false);

        when(employeeRepository.existsByEmailIgnoreCase(
                "anshu@example.com"
        )).thenReturn(false);

        Employee employee = processor.process(row);

        assertAll(
                () -> assertEquals(
                        "EMP001",
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
                        "anshu@example.com",
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
                        EmployeeSource.EXCEL,
                        employee.getSource()
                )
        );

        verify(employeeRepository)
                .existsByEmpId("EMP001");

        verify(employeeRepository)
                .existsByEmailIgnoreCase(
                        "anshu@example.com"
                );
    }

    @Test
    void shouldTrimAndNormalizeEmployeeValues() {
        EmployeeExcelRow row = new EmployeeExcelRow(
                2,
                " EMP001 ",
                " Anshu ",
                " Kumari ",
                " ANSHU@EXAMPLE.COM ",
                " Engineering ",
                new BigDecimal("125000")
        );

        when(employeeRepository.existsByEmpId("EMP001"))
                .thenReturn(false);

        when(employeeRepository.existsByEmailIgnoreCase(
                "anshu@example.com"
        )).thenReturn(false);

        Employee employee = processor.process(row);

        assertAll(
                () -> assertEquals(
                        "EMP001",
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
                        "anshu@example.com",
                        employee.getEmail()
                ),
                () -> assertEquals(
                        "Engineering",
                        employee.getDepartment()
                )
        );
    }

    @Test
    void shouldRejectMissingRequiredFields() {
        EmployeeExcelRow row = new EmployeeExcelRow(
                2,
                null,
                "",
                " ",
                null,
                "",
                null
        );

        InvalidEmployeeRowException exception =
                assertThrows(
                        InvalidEmployeeRowException.class,
                        () -> processor.process(row)
                );

        assertAll(
                () -> assertEquals(
                        2,
                        exception.getRow().rowNumber()
                ),
                () -> assertEquals(
                        "Employee ID is required; "
                                + "First name is required; "
                                + "Last name is required; "
                                + "Email is required; "
                                + "Department is required; "
                                + "Salary is required",
                        exception.getMessage()
                )
        );

        verifyNoInteractions(employeeRepository);
    }

    @Test
    void shouldRejectInvalidEmailAndNegativeSalary() {
        EmployeeExcelRow row = new EmployeeExcelRow(
                2,
                "EMP002",
                "Test",
                "Employee",
                "invalid-email",
                "Finance",
                new BigDecimal("-1")
        );

        InvalidEmployeeRowException exception =
                assertThrows(
                        InvalidEmployeeRowException.class,
                        () -> processor.process(row)
                );

        assertEquals(
                "Email format is invalid; "
                        + "Salary cannot be negative",
                exception.getMessage()
        );

        verifyNoInteractions(employeeRepository);
    }

    @Test
    void shouldRejectDuplicateEmployeeId() {
        EmployeeExcelRow row = validRow();

        when(employeeRepository.existsByEmpId("EMP001"))
                .thenReturn(true);

        InvalidEmployeeRowException exception =
                assertThrows(
                        InvalidEmployeeRowException.class,
                        () -> processor.process(row)
                );

        assertEquals(
                "Employee ID already exists: EMP001",
                exception.getMessage()
        );

        verify(employeeRepository)
                .existsByEmpId("EMP001");

        verify(employeeRepository, never())
                .existsByEmailIgnoreCase("anshu@example.com");
    }

    @Test
    void shouldRejectDuplicateEmail() {
        EmployeeExcelRow row = validRow();

        when(employeeRepository.existsByEmpId("EMP001"))
                .thenReturn(false);

        when(employeeRepository.existsByEmailIgnoreCase(
                "anshu@example.com"
        )).thenReturn(true);

        InvalidEmployeeRowException exception =
                assertThrows(
                        InvalidEmployeeRowException.class,
                        () -> processor.process(row)
                );

        assertEquals(
                "Employee email already exists: "
                        + "anshu@example.com",
                exception.getMessage()
        );

        verify(employeeRepository)
                .existsByEmpId("EMP001");

        verify(employeeRepository)
                .existsByEmailIgnoreCase("anshu@example.com");
    }

    private EmployeeExcelRow validRow() {
        return new EmployeeExcelRow(
                2,
                "EMP001",
                "Anshu",
                "Kumari",
                "ANSHU@EXAMPLE.COM",
                "Engineering",
                new BigDecimal("125000")
        );
    }
}