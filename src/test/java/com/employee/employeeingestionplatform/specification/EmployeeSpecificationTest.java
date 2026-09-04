package com.employee.employeeingestionplatform.specification;

import com.employee.employeeingestionplatform.entity.Employee;
import com.employee.employeeingestionplatform.entity.EmployeeSource;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeSpecificationTest {

    @Mock
    private Root<Employee> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Predicate combinedPredicate;

    @BeforeEach
    void setUp() {
        when(criteriaBuilder.and(any(Predicate[].class)))
                .thenReturn(combinedPredicate);
    }

    @Test
    void shouldCreateEmptyPredicateWhenAllFiltersAreNull() {
        Specification<Employee> specification =
                EmployeeSpecification.withFilters(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Predicate result = specification.toPredicate(
                root,
                query,
                criteriaBuilder
        );

        assertSame(combinedPredicate, result);

        ArgumentCaptor<Predicate[]> captor =
                ArgumentCaptor.forClass(Predicate[].class);

        verify(criteriaBuilder).and(captor.capture());

        assertEquals(0, captor.getValue().length);
        verifyNoInteractions(root);
    }

    @Test
    void shouldIgnoreBlankDepartment() {
        Specification<Employee> specification =
                EmployeeSpecification.withFilters(
                        null,
                        "   ",
                        null,
                        null,
                        null,
                        null
                );

        Predicate result = specification.toPredicate(
                root,
                query,
                criteriaBuilder
        );

        assertSame(combinedPredicate, result);

        ArgumentCaptor<Predicate[]> captor =
                ArgumentCaptor.forClass(Predicate[].class);

        verify(criteriaBuilder).and(captor.capture());

        assertEquals(0, captor.getValue().length);
        verifyNoInteractions(root);
    }

    @Test
    void shouldCreateDepartmentPredicateUsingTrimmedLowercaseValue() {
        Path<String> departmentPath = mock(Path.class);
        Expression<String> lowercaseDepartment = mock(Expression.class);
        Predicate departmentPredicate = mock(Predicate.class);

        when(root.<String>get("department"))
                .thenReturn(departmentPath);
        when(criteriaBuilder.lower(departmentPath))
                .thenReturn(lowercaseDepartment);
        when(criteriaBuilder.equal(
                lowercaseDepartment,
                "engineering"
        )).thenReturn(departmentPredicate);

        Specification<Employee> specification =
                EmployeeSpecification.withFilters(
                        null,
                        "  Engineering  ",
                        null,
                        null,
                        null,
                        null
                );

        Predicate result = specification.toPredicate(
                root,
                query,
                criteriaBuilder
        );

        assertSame(combinedPredicate, result);

        verify(criteriaBuilder).lower(departmentPath);
        verify(criteriaBuilder).equal(
                lowercaseDepartment,
                "engineering"
        );

        assertCombinedPredicates(departmentPredicate);
    }

    @Test
    void shouldCreateSourcePredicate() {
        Path<EmployeeSource> sourcePath = mock(Path.class);
        Predicate sourcePredicate = mock(Predicate.class);

        when(root.<EmployeeSource>get("source"))
                .thenReturn(sourcePath);
        when(criteriaBuilder.equal(
                sourcePath,
                EmployeeSource.KAFKA
        )).thenReturn(sourcePredicate);

        Specification<Employee> specification =
                EmployeeSpecification.withFilters(
                        null,
                        null,
                        null,
                        EmployeeSource.KAFKA,
                        null,
                        null
                );

        Predicate result = specification.toPredicate(
                root,
                query,
                criteriaBuilder
        );

        assertSame(combinedPredicate, result);

        verify(criteriaBuilder).equal(
                sourcePath,
                EmployeeSource.KAFKA
        );

        assertCombinedPredicates(sourcePredicate);
    }

    @Test
    void shouldCreateMinimumSalaryPredicate() {
        Path<BigDecimal> salaryPath = mock(Path.class);
        Predicate minimumSalaryPredicate = mock(Predicate.class);
        BigDecimal minimumSalary = new BigDecimal("50000");

        when(root.<BigDecimal>get("salary"))
                .thenReturn(salaryPath);
        when(criteriaBuilder.greaterThanOrEqualTo(
                salaryPath,
                minimumSalary
        )).thenReturn(minimumSalaryPredicate);

        Specification<Employee> specification =
                EmployeeSpecification.withFilters(
                        null,
                        null,
                        null,
                        null,
                        minimumSalary,
                        null
                );

        Predicate result = specification.toPredicate(
                root,
                query,
                criteriaBuilder
        );

        assertSame(combinedPredicate, result);

        verify(criteriaBuilder).greaterThanOrEqualTo(
                salaryPath,
                minimumSalary
        );

        assertCombinedPredicates(minimumSalaryPredicate);
    }

    @Test
    void shouldCreateMaximumSalaryPredicate() {
        Path<BigDecimal> salaryPath = mock(Path.class);
        Predicate maximumSalaryPredicate = mock(Predicate.class);
        BigDecimal maximumSalary = new BigDecimal("150000");

        when(root.<BigDecimal>get("salary"))
                .thenReturn(salaryPath);
        when(criteriaBuilder.lessThanOrEqualTo(
                salaryPath,
                maximumSalary
        )).thenReturn(maximumSalaryPredicate);

        Specification<Employee> specification =
                EmployeeSpecification.withFilters(
                        null,
                        null,
                        null,
                        null,
                        null,
                        maximumSalary
                );

        Predicate result = specification.toPredicate(
                root,
                query,
                criteriaBuilder
        );

        assertSame(combinedPredicate, result);

        verify(criteriaBuilder).lessThanOrEqualTo(
                salaryPath,
                maximumSalary
        );

        assertCombinedPredicates(maximumSalaryPredicate);
    }

    @Test
    void shouldCreateEmpIdPredicateUsingTrimmedLowercaseValue() {
        Path<String> empIdPath = mock(Path.class);
        Expression<String> lowercaseEmpId = mock(Expression.class);
        Predicate empIdPredicate = mock(Predicate.class);

        when(root.<String>get("empId"))
                .thenReturn(empIdPath);
        when(criteriaBuilder.lower(empIdPath))
                .thenReturn(lowercaseEmpId);
        when(criteriaBuilder.equal(
                lowercaseEmpId,
                "emp001"
        )).thenReturn(empIdPredicate);

        Specification<Employee> specification =
                EmployeeSpecification.withFilters(
                        "  EMP001  ",
                        null,
                        null,
                        null,
                        null,
                        null
                );

        Predicate result = specification.toPredicate(
                root,
                query,
                criteriaBuilder
        );

        assertSame(combinedPredicate, result);

        verify(criteriaBuilder).lower(empIdPath);
        verify(criteriaBuilder).equal(
                lowercaseEmpId,
                "emp001"
        );

        assertCombinedPredicates(empIdPredicate);
    }

    @Test
    void shouldCreateEmailPredicateUsingTrimmedLowercaseValue() {
        Path<String> emailPath = mock(Path.class);
        Expression<String> lowercaseEmail = mock(Expression.class);
        Predicate emailPredicate = mock(Predicate.class);

        when(root.<String>get("email"))
                .thenReturn(emailPath);
        when(criteriaBuilder.lower(emailPath))
                .thenReturn(lowercaseEmail);
        when(criteriaBuilder.equal(
                lowercaseEmail,
                "anshu@example.com"
        )).thenReturn(emailPredicate);

        Specification<Employee> specification =
                EmployeeSpecification.withFilters(
                        null,
                        null,
                        "  Anshu@Example.COM  ",
                        null,
                        null,
                        null
                );

        Predicate result = specification.toPredicate(
                root,
                query,
                criteriaBuilder
        );

        assertSame(combinedPredicate, result);

        verify(criteriaBuilder).lower(emailPath);
        verify(criteriaBuilder).equal(
                lowercaseEmail,
                "anshu@example.com"
        );

        assertCombinedPredicates(emailPredicate);
    }

    @Test
    void shouldIgnoreBlankEmpIdAndEmail() {
        Specification<Employee> specification =
                EmployeeSpecification.withFilters(
                        "   ",
                        null,
                        "   ",
                        null,
                        null,
                        null
                );

        Predicate result = specification.toPredicate(
                root,
                query,
                criteriaBuilder
        );

        assertSame(combinedPredicate, result);

        ArgumentCaptor<Predicate[]> captor =
                ArgumentCaptor.forClass(Predicate[].class);

        verify(criteriaBuilder).and(captor.capture());

        assertEquals(0, captor.getValue().length);
        verifyNoInteractions(root);
    }

    @Test
    void shouldCombineAllFiltersUsingAnd() {
        Path<String> empIdPath = mock(Path.class);
        Path<String> departmentPath = mock(Path.class);
        Path<String> emailPath = mock(Path.class);
        Path<EmployeeSource> sourcePath = mock(Path.class);
        Path<BigDecimal> salaryPath = mock(Path.class);

        Expression<String> lowercaseEmpId = mock(Expression.class);
        Expression<String> lowercaseDepartment = mock(Expression.class);
        Expression<String> lowercaseEmail = mock(Expression.class);

        Predicate empIdPredicate = mock(Predicate.class);
        Predicate departmentPredicate = mock(Predicate.class);
        Predicate emailPredicate = mock(Predicate.class);
        Predicate sourcePredicate = mock(Predicate.class);
        Predicate minimumSalaryPredicate = mock(Predicate.class);
        Predicate maximumSalaryPredicate = mock(Predicate.class);

        BigDecimal minimumSalary = new BigDecimal("50000");
        BigDecimal maximumSalary = new BigDecimal("150000");

        when(root.<String>get("empId"))
                .thenReturn(empIdPath);
        when(root.<String>get("department"))
                .thenReturn(departmentPath);
        when(root.<String>get("email"))
                .thenReturn(emailPath);
        when(root.<EmployeeSource>get("source"))
                .thenReturn(sourcePath);
        when(root.<BigDecimal>get("salary"))
                .thenReturn(salaryPath);

        when(criteriaBuilder.lower(empIdPath))
                .thenReturn(lowercaseEmpId);
        when(criteriaBuilder.lower(departmentPath))
                .thenReturn(lowercaseDepartment);
        when(criteriaBuilder.lower(emailPath))
                .thenReturn(lowercaseEmail);

        when(criteriaBuilder.equal(
                lowercaseEmpId,
                "emp001"
        )).thenReturn(empIdPredicate);

        when(criteriaBuilder.equal(
                lowercaseDepartment,
                "engineering"
        )).thenReturn(departmentPredicate);

        when(criteriaBuilder.equal(
                lowercaseEmail,
                "anshu@example.com"
        )).thenReturn(emailPredicate);

        when(criteriaBuilder.equal(
                sourcePath,
                EmployeeSource.EXCEL
        )).thenReturn(sourcePredicate);

        when(criteriaBuilder.greaterThanOrEqualTo(
                salaryPath,
                minimumSalary
        )).thenReturn(minimumSalaryPredicate);

        when(criteriaBuilder.lessThanOrEqualTo(
                salaryPath,
                maximumSalary
        )).thenReturn(maximumSalaryPredicate);

        Specification<Employee> specification =
                EmployeeSpecification.withFilters(
                        "EMP001",
                        "Engineering",
                        "Anshu@Example.com",
                        EmployeeSource.EXCEL,
                        minimumSalary,
                        maximumSalary
                );

        Predicate result = specification.toPredicate(
                root,
                query,
                criteriaBuilder
        );

        assertSame(combinedPredicate, result);

        assertCombinedPredicates(
                empIdPredicate,
                departmentPredicate,
                emailPredicate,
                sourcePredicate,
                minimumSalaryPredicate,
                maximumSalaryPredicate
        );
    }

    private void assertCombinedPredicates(
            Predicate... expectedPredicates
    ) {
        ArgumentCaptor<Predicate[]> captor =
                ArgumentCaptor.forClass(Predicate[].class);

        verify(criteriaBuilder).and(captor.capture());

        assertArrayEquals(
                expectedPredicates,
                captor.getValue()
        );
    }
}