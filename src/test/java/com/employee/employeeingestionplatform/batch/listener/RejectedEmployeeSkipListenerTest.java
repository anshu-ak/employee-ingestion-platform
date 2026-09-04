package com.employee.employeeingestionplatform.batch.listener;

import com.employee.employeeingestionplatform.batch.exception.InvalidEmployeeRowException;
import com.employee.employeeingestionplatform.batch.model.EmployeeExcelRow;
import com.employee.employeeingestionplatform.entity.Employee;
import com.employee.employeeingestionplatform.entity.RejectedRecord;
import com.employee.employeeingestionplatform.repository.RejectedRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RejectedEmployeeSkipListenerTest {

    @Mock
    private RejectedRecordRepository rejectedRecordRepository;

    private UUID trackingId;
    private RejectedEmployeeSkipListener listener;

    @BeforeEach
    void setUp() {
        trackingId = UUID.randomUUID();

        listener = new RejectedEmployeeSkipListener(
                trackingId,
                rejectedRecordRepository
        );
    }

    @Test
    void shouldSaveRejectedRecordForInvalidEmployeeRow() {
        EmployeeExcelRow row = mock(EmployeeExcelRow.class);
        InvalidEmployeeRowException exception =
                mock(InvalidEmployeeRowException.class);

        when(row.rowNumber()).thenReturn(2);
        when(row.empId()).thenReturn("EMP001");
        when(exception.getMessage())
                .thenReturn("Email format is invalid");

        listener.onSkipInProcess(row, exception);

        ArgumentCaptor<RejectedRecord> captor =
                ArgumentCaptor.forClass(RejectedRecord.class);

        verify(rejectedRecordRepository).save(captor.capture());

        RejectedRecord savedRecord = captor.getValue();

        assertAll(
                () -> assertEquals(
                        trackingId,
                        savedRecord.getTrackingId()
                ),
                () -> assertEquals(
                        2,
                        savedRecord.getRowNumber()
                ),
                () -> assertEquals(
                        "EMP001",
                        savedRecord.getEmpId()
                ),
                () -> assertEquals(
                        "Email format is invalid",
                        savedRecord.getReason()
                )
        );
    }

    @Test
    void shouldUseRegularExceptionMessageAsReason() {
        EmployeeExcelRow row = mock(EmployeeExcelRow.class);
        RuntimeException exception =
                new RuntimeException("Unexpected processing error");

        when(row.rowNumber()).thenReturn(5);
        when(row.empId()).thenReturn("EMP005");

        listener.onSkipInProcess(row, exception);

        ArgumentCaptor<RejectedRecord> captor =
                ArgumentCaptor.forClass(RejectedRecord.class);

        verify(rejectedRecordRepository).save(captor.capture());

        assertEquals(
                "Unexpected processing error",
                captor.getValue().getReason()
        );
    }

    @Test
    void shouldUseDefaultReasonWhenExceptionMessageIsNull() {
        EmployeeExcelRow row = mock(EmployeeExcelRow.class);
        RuntimeException exception = new RuntimeException();

        when(row.rowNumber()).thenReturn(7);
        when(row.empId()).thenReturn("EMP007");

        listener.onSkipInProcess(row, exception);

        ArgumentCaptor<RejectedRecord> captor =
                ArgumentCaptor.forClass(RejectedRecord.class);

        verify(rejectedRecordRepository).save(captor.capture());

        assertEquals(
                "Unknown validation error",
                captor.getValue().getReason()
        );
    }

    @Test
    void shouldUseDefaultReasonWhenExceptionMessageIsBlank() {
        EmployeeExcelRow row = mock(EmployeeExcelRow.class);
        RuntimeException exception = new RuntimeException("   ");

        when(row.rowNumber()).thenReturn(8);
        when(row.empId()).thenReturn("EMP008");

        listener.onSkipInProcess(row, exception);

        ArgumentCaptor<RejectedRecord> captor =
                ArgumentCaptor.forClass(RejectedRecord.class);

        verify(rejectedRecordRepository).save(captor.capture());

        assertEquals(
                "Unknown validation error",
                captor.getValue().getReason()
        );
    }

    @Test
    void shouldLimitReasonToOneThousandCharacters() {
        EmployeeExcelRow row = mock(EmployeeExcelRow.class);
        String longMessage = "x".repeat(1_100);
        RuntimeException exception =
                new RuntimeException(longMessage);

        when(row.rowNumber()).thenReturn(9);
        when(row.empId()).thenReturn("EMP009");

        listener.onSkipInProcess(row, exception);

        ArgumentCaptor<RejectedRecord> captor =
                ArgumentCaptor.forClass(RejectedRecord.class);

        verify(rejectedRecordRepository).save(captor.capture());

        assertAll(
                () -> assertEquals(
                        1_000,
                        captor.getValue().getReason().length()
                ),
                () -> assertEquals(
                        longMessage.substring(0, 1_000),
                        captor.getValue().getReason()
                )
        );
    }

    @Test
    void shouldNotSaveRejectedRecordForReadSkip() {
        RuntimeException exception =
                new RuntimeException("Excel row cannot be read");

        listener.onSkipInRead(exception);

        verifyNoInteractions(rejectedRecordRepository);
    }

    @Test
    void shouldNotSaveRejectedRecordForWriteSkip() {
        Employee employee = new Employee();
        employee.setEmpId("EMP010");

        RuntimeException exception =
                new RuntimeException("Database write failed");

        listener.onSkipInWrite(employee, exception);

        verifyNoInteractions(rejectedRecordRepository);
    }
}