package com.employee.employeeingestionplatform.batch.listener;

import com.employee.employeeingestionplatform.entity.UploadStatus;
import com.employee.employeeingestionplatform.entity.UploadTracking;
import com.employee.employeeingestionplatform.repository.UploadTrackingRepository;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class EmployeeJobListener
        implements JobExecutionListener {

    private final UploadTrackingRepository uploadTrackingRepository;

    public EmployeeJobListener(
            UploadTrackingRepository uploadTrackingRepository
    ) {
        this.uploadTrackingRepository = uploadTrackingRepository;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        UploadTracking tracking = getTracking(jobExecution);

        tracking.setStatus(UploadStatus.PROCESSING);
        tracking.setStartedAt(OffsetDateTime.now());
        tracking.setCompletedAt(null);
        tracking.setErrorSummary(null);

        uploadTrackingRepository.save(tracking);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        UploadTracking tracking = getTracking(jobExecution);

        long totalRows = 0;
        long successRows = 0;
        long rejectedRows = 0;

        for (StepExecution stepExecution
                : jobExecution.getStepExecutions()) {
            totalRows += stepExecution.getReadCount();
            successRows += stepExecution.getWriteCount();
            rejectedRows += stepExecution.getSkipCount();
        }

        tracking.setTotalRows(toInt(totalRows));
        tracking.setSuccessRows(toInt(successRows));
        tracking.setRejectedRows(toInt(rejectedRows));
        tracking.setCompletedAt(OffsetDateTime.now());

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            tracking.setStatus(
                    rejectedRows > 0
                            ? UploadStatus.PARTIALLY_COMPLETED
                            : UploadStatus.COMPLETED
            );
        } else {
            tracking.setStatus(UploadStatus.FAILED);
            tracking.setErrorSummary(
                    getFailureSummary(jobExecution)
            );
        }

        uploadTrackingRepository.save(tracking);
    }

    private UploadTracking getTracking(
            JobExecution jobExecution
    ) {
        String trackingId = jobExecution
                .getJobParameters()
                .getString("trackingId");

        if (trackingId == null) {
            throw new IllegalStateException(
                    "trackingId job parameter is missing"
            );
        }

        return uploadTrackingRepository
                .findById(UUID.fromString(trackingId))
                .orElseThrow(() -> new IllegalStateException(
                        "Upload tracking record not found: "
                                + trackingId
                ));
    }

    private String getFailureSummary(
            JobExecution jobExecution
    ) {
        String summary = jobExecution
                .getAllFailureExceptions()
                .stream()
                .map(Throwable::getMessage)
                .filter(message ->
                        message != null && !message.isBlank()
                )
                .findFirst()
                .orElse("Batch job failed");

        return summary.length() <= 2000
                ? summary
                : summary.substring(0, 2000);
    }

    private int toInt(long value) {
        return Math.toIntExact(value);
    }
}