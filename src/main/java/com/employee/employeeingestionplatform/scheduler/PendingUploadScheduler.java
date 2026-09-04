package com.employee.employeeingestionplatform.scheduler;

import com.employee.employeeingestionplatform.entity.UploadStatus;
import com.employee.employeeingestionplatform.entity.UploadTracking;
import com.employee.employeeingestionplatform.repository.UploadTrackingRepository;
import com.employee.employeeingestionplatform.service.BatchJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PendingUploadScheduler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PendingUploadScheduler.class);

    private final UploadTrackingRepository uploadTrackingRepository;
    private final BatchJobService batchJobService;

    public PendingUploadScheduler(
            UploadTrackingRepository uploadTrackingRepository,
            BatchJobService batchJobService
    ) {
        this.uploadTrackingRepository = uploadTrackingRepository;
        this.batchJobService = batchJobService;
    }

    @Scheduled(
            initialDelayString =
                    "${app.upload.scheduler-initial-delay-ms:10000}",
            fixedDelayString =
                    "${app.upload.scheduler-delay-ms:300000}"
    )
    public void processPendingUploads() {
        List<UploadTracking> pendingUploads =
                uploadTrackingRepository
                        .findByStatusOrderByCreatedAtAsc(
                                UploadStatus.PENDING
                        );

        if (pendingUploads.isEmpty()) {
            LOGGER.debug("No pending uploads found");
            return;
        }

        LOGGER.info(
                "Found {} pending upload(s)",
                pendingUploads.size()
        );

        for (UploadTracking upload : pendingUploads) {
            try {
                LOGGER.info(
                        "Starting scheduled processing for upload {}",
                        upload.getTrackingId()
                );

                batchJobService.process(upload.getTrackingId());
            } catch (Exception exception) {
                LOGGER.error(
                        "Scheduled processing failed for upload {}",
                        upload.getTrackingId(),
                        exception
                );
            }
        }
    }
}