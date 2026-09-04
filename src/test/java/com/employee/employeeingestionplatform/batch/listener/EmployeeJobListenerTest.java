package com.employee.employeeingestionplatform.batch.listener;

import com.employee.employeeingestionplatform.entity.UploadStatus;
import com.employee.employeeingestionplatform.entity.UploadTracking;
import com.employee.employeeingestionplatform.repository.UploadTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeJobListenerTest {

    @Mock
    private UploadTrackingRepository uploadTrackingRepository;

    private EmployeeJobListener listener;
    private UUID trackingId;
    private UploadTracking tracking;

    @BeforeEach
    void setUp() {
        listener = new EmployeeJobListener(uploadTrackingRepository);

        trackingId = UUID.randomUUID();

        tracking = new UploadTracking();
        tracking.setTrackingId(trackingId);
        tracking.setStatus(UploadStatus.PENDING);
    }

    @Test
    void shouldMarkUploadAsProcessingBeforeJob() {
        JobExecution jobExecution = createJobExecution();

        when(uploadTrackingRepository.findById(trackingId))
                .thenReturn(Optional.of(tracking));

        listener.beforeJob(jobExecution);

        assertAll(
                () -> assertEquals(
                        UploadStatus.PROCESSING,
                        tracking.getStatus()
                ),
                () -> assertNotNull(tracking.getStartedAt()),
                () -> assertNull(tracking.getCompletedAt()),
                () -> assertNull(tracking.getErrorSummary())
        );

        verify(uploadTrackingRepository).findById(trackingId);
        verify(uploadTrackingRepository).save(tracking);
    }

    @Test
    void shouldMarkUploadAsCompletedWithoutSkippedRows() {
        JobExecution jobExecution = createJobExecution();
        StepExecution stepExecution = mock(StepExecution.class);

        when(jobExecution.getStatus())
                .thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStepExecutions())
                .thenReturn(Set.of(stepExecution));

        when(stepExecution.getReadCount()).thenReturn(10L);
        when(stepExecution.getWriteCount()).thenReturn(10L);
        when(stepExecution.getSkipCount()).thenReturn(0L);

        when(uploadTrackingRepository.findById(trackingId))
                .thenReturn(Optional.of(tracking));

        listener.afterJob(jobExecution);

        assertAll(
                () -> assertEquals(
                        UploadStatus.COMPLETED,
                        tracking.getStatus()
                ),
                () -> assertEquals(10, tracking.getTotalRows()),
                () -> assertEquals(10, tracking.getSuccessRows()),
                () -> assertEquals(0, tracking.getRejectedRows()),
                () -> assertNotNull(tracking.getCompletedAt()),
                () -> assertNull(tracking.getErrorSummary())
        );

        verify(uploadTrackingRepository).save(tracking);
    }

    @Test
    void shouldMarkUploadAsPartiallyCompletedWhenRowsAreSkipped() {
        JobExecution jobExecution = createJobExecution();
        StepExecution stepExecution = mock(StepExecution.class);

        when(jobExecution.getStatus())
                .thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStepExecutions())
                .thenReturn(Set.of(stepExecution));

        when(stepExecution.getReadCount()).thenReturn(10L);
        when(stepExecution.getWriteCount()).thenReturn(8L);
        when(stepExecution.getSkipCount()).thenReturn(2L);

        when(uploadTrackingRepository.findById(trackingId))
                .thenReturn(Optional.of(tracking));

        listener.afterJob(jobExecution);

        assertAll(
                () -> assertEquals(
                        UploadStatus.PARTIALLY_COMPLETED,
                        tracking.getStatus()
                ),
                () -> assertEquals(10, tracking.getTotalRows()),
                () -> assertEquals(8, tracking.getSuccessRows()),
                () -> assertEquals(2, tracking.getRejectedRows()),
                () -> assertNotNull(tracking.getCompletedAt())
        );

        verify(uploadTrackingRepository).save(tracking);
    }

    @Test
    void shouldCombineCountsFromMultipleSteps() {
        JobExecution jobExecution = createJobExecution();

        StepExecution firstStep = mock(StepExecution.class);
        StepExecution secondStep = mock(StepExecution.class);

        when(jobExecution.getStatus())
                .thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getStepExecutions())
                .thenReturn(Set.of(firstStep, secondStep));

        when(firstStep.getReadCount()).thenReturn(5L);
        when(firstStep.getWriteCount()).thenReturn(4L);
        when(firstStep.getSkipCount()).thenReturn(1L);

        when(secondStep.getReadCount()).thenReturn(7L);
        when(secondStep.getWriteCount()).thenReturn(7L);
        when(secondStep.getSkipCount()).thenReturn(0L);

        when(uploadTrackingRepository.findById(trackingId))
                .thenReturn(Optional.of(tracking));

        listener.afterJob(jobExecution);

        assertAll(
                () -> assertEquals(
                        UploadStatus.PARTIALLY_COMPLETED,
                        tracking.getStatus()
                ),
                () -> assertEquals(12, tracking.getTotalRows()),
                () -> assertEquals(11, tracking.getSuccessRows()),
                () -> assertEquals(1, tracking.getRejectedRows())
        );
    }

    @Test
    void shouldMarkUploadAsFailedAndStoreFailureMessage() {
        JobExecution jobExecution = createJobExecution();

        when(jobExecution.getStatus())
                .thenReturn(BatchStatus.FAILED);
        when(jobExecution.getStepExecutions())
                .thenReturn(Set.of());
        when(jobExecution.getAllFailureExceptions())
                .thenReturn(List.of(
                        new RuntimeException("Excel processing failed")
                ));

        when(uploadTrackingRepository.findById(trackingId))
                .thenReturn(Optional.of(tracking));

        listener.afterJob(jobExecution);

        assertAll(
                () -> assertEquals(
                        UploadStatus.FAILED,
                        tracking.getStatus()
                ),
                () -> assertEquals(
                        "Excel processing failed",
                        tracking.getErrorSummary()
                ),
                () -> assertNotNull(tracking.getCompletedAt())
        );

        verify(uploadTrackingRepository).save(tracking);
    }

    @Test
    void shouldUseDefaultMessageWhenFailureHasNoMessage() {
        JobExecution jobExecution = createJobExecution();

        when(jobExecution.getStatus())
                .thenReturn(BatchStatus.FAILED);
        when(jobExecution.getStepExecutions())
                .thenReturn(Set.of());
        when(jobExecution.getAllFailureExceptions())
                .thenReturn(List.of(new RuntimeException()));

        when(uploadTrackingRepository.findById(trackingId))
                .thenReturn(Optional.of(tracking));

        listener.afterJob(jobExecution);

        assertEquals(
                "Batch job failed",
                tracking.getErrorSummary()
        );
    }

    @Test
    void shouldLimitFailureSummaryToTwoThousandCharacters() {
        JobExecution jobExecution = createJobExecution();
        String longMessage = "x".repeat(2_100);

        when(jobExecution.getStatus())
                .thenReturn(BatchStatus.FAILED);
        when(jobExecution.getStepExecutions())
                .thenReturn(Set.of());
        when(jobExecution.getAllFailureExceptions())
                .thenReturn(List.of(
                        new RuntimeException(longMessage)
                ));

        when(uploadTrackingRepository.findById(trackingId))
                .thenReturn(Optional.of(tracking));

        listener.afterJob(jobExecution);

        assertEquals(2_000, tracking.getErrorSummary().length());
        verify(uploadTrackingRepository).save(tracking);
    }

    @Test
    void shouldRejectJobWithoutTrackingIdParameter() {
        JobExecution jobExecution = mock(JobExecution.class);
        JobParameters jobParameters = mock(JobParameters.class);

        when(jobExecution.getJobParameters())
                .thenReturn(jobParameters);
        when(jobParameters.getString("trackingId"))
                .thenReturn(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> listener.beforeJob(jobExecution)
        );

        assertEquals(
                "trackingId job parameter is missing",
                exception.getMessage()
        );

        verifyNoInteractions(uploadTrackingRepository);
    }

    @Test
    void shouldRejectUnknownTrackingId() {
        JobExecution jobExecution = createJobExecution();

        when(uploadTrackingRepository.findById(trackingId))
                .thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> listener.beforeJob(jobExecution)
        );

        assertEquals(
                "Upload tracking record not found: " + trackingId,
                exception.getMessage()
        );

        verify(uploadTrackingRepository).findById(trackingId);
        verify(uploadTrackingRepository, never())
                .save(any(UploadTracking.class));
    }

    private JobExecution createJobExecution() {
        JobExecution jobExecution = mock(JobExecution.class);
        JobParameters jobParameters = mock(JobParameters.class);

        when(jobExecution.getJobParameters())
                .thenReturn(jobParameters);
        when(jobParameters.getString("trackingId"))
                .thenReturn(trackingId.toString());

        return jobExecution;
    }
}