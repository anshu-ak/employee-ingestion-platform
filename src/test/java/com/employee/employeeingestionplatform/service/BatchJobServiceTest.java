package com.employee.employeeingestionplatform.service;

import com.employee.employeeingestionplatform.dto.upload.BatchLaunchResponse;
import com.employee.employeeingestionplatform.entity.UploadStatus;
import com.employee.employeeingestionplatform.entity.UploadTracking;
import com.employee.employeeingestionplatform.repository.UploadTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchJobServiceTest {

    @Mock
    private JobOperator jobOperator;

    @Mock
    private Job employeeImportJob;

    @Mock
    private UploadTrackingRepository uploadTrackingRepository;

    @TempDir
    Path temporaryDirectory;

    private BatchJobService batchJobService;
    private UUID trackingId;

    @BeforeEach
    void setUp() {
        batchJobService = new BatchJobService(
                jobOperator,
                employeeImportJob,
                uploadTrackingRepository
        );

        trackingId = UUID.randomUUID();
    }

    @Test
    void shouldRejectUnknownTrackingId() {
        when(uploadTrackingRepository.findById(trackingId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> batchJobService.process(trackingId)
        );

        assertAll(
                () -> assertEquals(
                        HttpStatus.NOT_FOUND,
                        exception.getStatusCode()
                ),
                () -> assertEquals(
                        "Upload tracking record not found",
                        exception.getReason()
                )
        );

        verifyNoInteractions(jobOperator);
    }

    @Test
    void shouldRejectUploadThatIsNotPending() {
        UploadTracking tracking = createTracking(
                UploadStatus.COMPLETED,
                temporaryDirectory.resolve("employees.xlsx")
        );

        when(uploadTrackingRepository.findById(trackingId))
                .thenReturn(Optional.of(tracking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> batchJobService.process(trackingId)
        );

        assertAll(
                () -> assertEquals(
                        HttpStatus.CONFLICT,
                        exception.getStatusCode()
                ),
                () -> assertEquals(
                        "Only PENDING uploads can be processed",
                        exception.getReason()
                )
        );

        verifyNoInteractions(jobOperator);
    }

    @Test
    void shouldRejectUploadWhenExcelFileDoesNotExist() {
        Path missingFile =
                temporaryDirectory.resolve("missing.xlsx");

        UploadTracking tracking = createTracking(
                UploadStatus.PENDING,
                missingFile
        );

        when(uploadTrackingRepository.findById(trackingId))
                .thenReturn(Optional.of(tracking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> batchJobService.process(trackingId)
        );

        assertAll(
                () -> assertEquals(
                        HttpStatus.NOT_FOUND,
                        exception.getStatusCode()
                ),
                () -> assertEquals(
                        "The uploaded Excel file could not be found",
                        exception.getReason()
                )
        );

        verifyNoInteractions(jobOperator);
    }

    @Test
    void shouldLaunchBatchJobForPendingUpload()
            throws Exception {

        Path excelFile =
                temporaryDirectory.resolve("employees.xlsx");
        Files.writeString(excelFile, "test file");

        UploadTracking pendingTracking = createTracking(
                UploadStatus.PENDING,
                excelFile
        );

        UploadTracking completedTracking = createTracking(
                UploadStatus.COMPLETED,
                excelFile
        );

        JobExecution jobExecution = mock(JobExecution.class);

        when(uploadTrackingRepository.findById(trackingId))
                .thenReturn(
                        Optional.of(pendingTracking),
                        Optional.of(completedTracking)
                );

        when(jobOperator.start(
                eq(employeeImportJob),
                any(JobParameters.class)
        )).thenReturn(jobExecution);

        when(jobExecution.getId()).thenReturn(101L);
        when(jobExecution.getStatus())
                .thenReturn(BatchStatus.COMPLETED);

        BatchLaunchResponse response =
                batchJobService.process(trackingId);

        assertAll(
                () -> assertEquals(
                        trackingId,
                        response.trackingId()
                ),
                () -> assertEquals(
                        101L,
                        response.jobExecutionId()
                ),
                () -> assertEquals(
                        "COMPLETED",
                        response.batchStatus()
                ),
                () -> assertEquals(
                        UploadStatus.COMPLETED,
                        response.uploadStatus()
                )
        );

        verify(jobOperator).start(
                eq(employeeImportJob),
                argThat(parameters ->
                        trackingId.toString().equals(
                                parameters.getString("trackingId")
                        )
                                && excelFile
                                .toAbsolutePath()
                                .normalize()
                                .toString()
                                .equals(
                                        parameters.getString("filePath")
                                )
                )
        );

        verify(uploadTrackingRepository, times(2))
                .findById(trackingId);
    }

    @Test
    void shouldReturnInternalServerErrorWhenJobLaunchFails()
            throws Exception {

        Path excelFile =
                temporaryDirectory.resolve("employees.xlsx");
        Files.writeString(excelFile, "test file");

        UploadTracking tracking = createTracking(
                UploadStatus.PENDING,
                excelFile
        );

        RuntimeException launchFailure =
                new RuntimeException("Job repository unavailable");

        when(uploadTrackingRepository.findById(trackingId))
                .thenReturn(Optional.of(tracking));

        when(jobOperator.start(
                eq(employeeImportJob),
                any(JobParameters.class)
        )).thenThrow(launchFailure);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> batchJobService.process(trackingId)
        );

        assertAll(
                () -> assertEquals(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        exception.getStatusCode()
                ),
                () -> assertEquals(
                        "Could not launch the employee import job",
                        exception.getReason()
                ),
                () -> assertSame(
                        launchFailure,
                        exception.getCause()
                )
        );
    }

    @Test
    void shouldReturnInternalServerErrorWhenUpdatedTrackingIsMissing()
            throws Exception {

        Path excelFile =
                temporaryDirectory.resolve("employees.xlsx");
        Files.writeString(excelFile, "test file");

        UploadTracking tracking = createTracking(
                UploadStatus.PENDING,
                excelFile
        );

        JobExecution jobExecution = mock(JobExecution.class);

        when(uploadTrackingRepository.findById(trackingId))
                .thenReturn(
                        Optional.of(tracking),
                        Optional.empty()
                );

        when(jobOperator.start(
                eq(employeeImportJob),
                any(JobParameters.class)
        )).thenReturn(jobExecution);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> batchJobService.process(trackingId)
        );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getStatusCode()
        );

        assertEquals(
                "Could not launch the employee import job",
                exception.getReason()
        );
    }

    private UploadTracking createTracking(
            UploadStatus status,
            Path storedFile
    ) {
        UploadTracking tracking = new UploadTracking();
        tracking.setTrackingId(trackingId);
        tracking.setStatus(status);
        tracking.setStoredFilePath(storedFile.toString());

        return tracking;
    }
}