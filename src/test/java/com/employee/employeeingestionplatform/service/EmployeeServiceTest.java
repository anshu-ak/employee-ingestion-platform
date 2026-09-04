package com.employee.employeeingestionplatform.service;

import com.employee.employeeingestionplatform.dto.employee.EmployeeResponse;
import com.employee.employeeingestionplatform.dto.employee.PageResponse;
import com.employee.employeeingestionplatform.entity.Employee;
import com.employee.employeeingestionplatform.entity.EmployeeSource;
import com.employee.employeeingestionplatform.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(
                employeeRepository
        );
    }

    @Test
    void shouldReturnPaginatedEmployees() {
        Employee employee = createEmployee();

        Page<Employee> employeePage = new PageImpl<>(
                List.of(employee)
        );

        when(employeeRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(employeePage);

        PageResponse<EmployeeResponse> response =
                employeeService.getEmployees(
                        0,
                        10,
                        "EMP001",
                        "Engineering",
                        "anshu@example.com",
                        EmployeeSource.EXCEL,
                        new BigDecimal("50000"),
                        new BigDecimal("150000"),
                        "salary,desc"
                );

        assertAll(
                () -> assertEquals(0, response.page()),
                () -> assertEquals(1, response.content().size()),
                () -> assertEquals(
                        1,
                        response.totalElements()
                ),
                () -> assertEquals(
                        "EMP001",
                        response.content().getFirst().empId()
                ),
                () -> assertEquals(
                        "Anshu",
                        response.content().getFirst().firstName()
                ),
                () -> assertEquals(
                        "anshu@example.com",
                        response.content().getFirst().email()
                ),
                () -> assertEquals(
                        EmployeeSource.EXCEL,
                        response.content().getFirst().source()
                )
        );
    }

    @Test
    void shouldCreateExpectedPageRequestWithDefaultSort() {
        when(employeeRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        employeeService.getEmployees(
                2,
                25,
                null,
                null,
                null,
                null,
                null,
                null,
                "id,asc"
        );

        Pageable pageable = capturePageable();

        assertAll(
                () -> assertEquals(
                        2,
                        pageable.getPageNumber()
                ),
                () -> assertEquals(
                        25,
                        pageable.getPageSize()
                ),
                () -> assertEquals(
                        "id: ASC",
                        pageable.getSort().toString()
                )
        );
    }

    @Test
    void shouldCreateDescendingSort() {
        when(employeeRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        employeeService.getEmployees(
                0,
                20,
                null,
                null,
                null,
                null,
                null,
                null,
                "salary,desc"
        );

        Pageable pageable = capturePageable();

        assertEquals(
                "salary: DESC",
                pageable.getSort().toString()
        );
    }

    @Test
    void shouldUseAscendingDirectionWhenDirectionIsOmitted() {
        when(employeeRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        employeeService.getEmployees(
                0,
                20,
                null,
                null,
                null,
                null,
                null,
                null,
                "createdAt"
        );

        Pageable pageable = capturePageable();

        assertEquals(
                "createdAt: ASC",
                pageable.getSort().toString()
        );
    }

    @Test
    void shouldUseDefaultSortWhenSortIsBlank() {
        when(employeeRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        employeeService.getEmployees(
                0,
                20,
                null,
                null,
                null,
                null,
                null,
                null,
                "   "
        );

        Pageable pageable = capturePageable();

        assertEquals(
                "id: ASC",
                pageable.getSort().toString()
        );
    }

    @Test
    void shouldUseDefaultSortWhenSortIsNull() {
        when(employeeRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        employeeService.getEmployees(
                0,
                20,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Pageable pageable = capturePageable();

        assertEquals(
                "id: ASC",
                pageable.getSort().toString()
        );
    }

    @Test
    void shouldAcceptCaseInsensitiveSortDirection() {
        when(employeeRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        employeeService.getEmployees(
                0,
                20,
                null,
                null,
                null,
                null,
                null,
                null,
                "email,DESC"
        );

        Pageable pageable = capturePageable();

        assertEquals(
                "email: DESC",
                pageable.getSort().toString()
        );
    }

    @Test
    void shouldRejectUnsupportedSortField() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.getEmployees(
                        0,
                        20,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "password,asc"
                )
        );

        assertInvalidSort(exception);
        verifyNoRepositorySearch();
    }

    @Test
    void shouldRejectUnsupportedSortDirection() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.getEmployees(
                        0,
                        20,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "salary,sideways"
                )
        );

        assertInvalidSort(exception);
        verifyNoRepositorySearch();
    }

    @Test
    void shouldRejectSortWithTooManyParts() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.getEmployees(
                        0,
                        20,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "salary,desc,extra"
                )
        );

        assertInvalidSort(exception);
        verifyNoRepositorySearch();
    }

    @Test
    void shouldRejectInvalidSalaryRange() {
        BigDecimal minimum = new BigDecimal("150000");
        BigDecimal maximum = new BigDecimal("50000");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> employeeService.getEmployees(
                        0,
                        20,
                        null,
                        null,
                        null,
                        null,
                        minimum,
                        maximum,
                        "id,asc"
                )
        );

        assertAll(
                () -> assertEquals(
                        HttpStatus.BAD_REQUEST,
                        exception.getStatusCode()
                ),
                () -> assertEquals(
                        "minSalary cannot be greater than maxSalary",
                        exception.getReason()
                )
        );

        verifyNoRepositorySearch();
    }

    private Pageable capturePageable() {
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(employeeRepository).findAll(
                any(Specification.class),
                pageableCaptor.capture()
        );

        return pageableCaptor.getValue();
    }

    private void assertInvalidSort(
            ResponseStatusException exception
    ) {
        assertAll(
                () -> assertEquals(
                        HttpStatus.BAD_REQUEST,
                        exception.getStatusCode()
                ),
                () -> assertEquals(
                        "sort must use an allowed field and direction, "
                                + "for example: salary,desc",
                        exception.getReason()
                )
        );
    }

    private void verifyNoRepositorySearch() {
        verify(
                employeeRepository,
                never()
        ).findAll(
                any(Specification.class),
                any(Pageable.class)
        );
    }

    private Employee createEmployee() {
        OffsetDateTime now = OffsetDateTime.now();

        Employee employee = new Employee();
        employee.setId(1L);
        employee.setEmpId("EMP001");
        employee.setFirstName("Anshu");
        employee.setLastName("Kumari");
        employee.setEmail("anshu@example.com");
        employee.setDepartment("Engineering");
        employee.setSalary(new BigDecimal("125000"));
        employee.setSource(EmployeeSource.EXCEL);
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);

        return employee;
    }
}