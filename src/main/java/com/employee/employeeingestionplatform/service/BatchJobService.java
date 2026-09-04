package com.employee.employeeingestionplatform.service;

import com.employee.employeeingestionplatform.dto.upload.BatchLaunchResponse;
import com.employee.employeeingestionplatform.entity.UploadStatus;
import com.employee.employeeingestionplatform.entity.UploadTracking;
import com.employee.employeeingestionplatform.repository.UploadTrackingRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class BatchJobService {

    private final JobOperator jobOperator;
    private final Job employeeImportJob;
    private final UploadTrackingRepository uploadTrackingRepository;

    public BatchJobService(
            JobOperator jobOperator,
            Job employeeImportJob,
            UploadTrackingRepository uploadTrackingRepository
    ) {
        this.jobOperator = jobOperator;
        this.employeeImportJob = employeeImportJob;
        this.uploadTrackingRepository = uploadTrackingRepository;
    }

    public BatchLaunchResponse process(UUID trackingId) {
        UploadTracking tracking = uploadTrackingRepository
                .findById(trackingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Upload tracking record not found"
                ));

        if (tracking.getStatus() != UploadStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only PENDING uploads can be processed"
            );
        }

        Path filePath = Path.of(
                tracking.getStoredFilePath()
        ).toAbsolutePath().normalize();

        if (!Files.isRegularFile(filePath)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "The uploaded Excel file could not be found"
            );
        }

        JobParameters jobParameters =
                new JobParametersBuilder()
                        .addString(
                                "trackingId",
                                trackingId.toString()
                        )
                        .addString(
                                "filePath",
                                filePath.toString()
                        )
                        .toJobParameters();

        try {
            JobExecution execution = jobOperator.start(
                    employeeImportJob,
                    jobParameters
            );

            UploadTracking updatedTracking =
                    uploadTrackingRepository
                            .findById(trackingId)
                            .orElseThrow();

            return new BatchLaunchResponse(
                    trackingId,
                    execution.getId(),
                    execution.getStatus().name(),
                    updatedTracking.getStatus()
            );
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not launch the employee import job",
                    exception
            );
        }
    }
}