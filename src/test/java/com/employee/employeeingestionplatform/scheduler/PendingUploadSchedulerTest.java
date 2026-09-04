package com.employee.employeeingestionplatform.scheduler;

import com.employee.employeeingestionplatform.entity.UploadStatus;
import com.employee.employeeingestionplatform.entity.UploadTracking;
import com.employee.employeeingestionplatform.repository.UploadTrackingRepository;
import com.employee.employeeingestionplatform.service.BatchJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PendingUploadSchedulerTest {

    @Mock
    private UploadTrackingRepository uploadTrackingRepository;

    @Mock
    private BatchJobService batchJobService;

    private PendingUploadScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PendingUploadScheduler(
                uploadTrackingRepository,
                batchJobService
        );
    }

    @Test
    void shouldDoNothingWhenNoPendingUploadsExist() {
        when(uploadTrackingRepository
                .findByStatusOrderByCreatedAtAsc(UploadStatus.PENDING))
                .thenReturn(List.of());

        scheduler.processPendingUploads();

        verify(uploadTrackingRepository)
                .findByStatusOrderByCreatedAtAsc(UploadStatus.PENDING);

        verifyNoInteractions(batchJobService);
    }

    @Test
    void shouldProcessOnePendingUpload() {
        UUID trackingId = UUID.randomUUID();
        UploadTracking upload = createUpload(trackingId);

        when(uploadTrackingRepository
                .findByStatusOrderByCreatedAtAsc(UploadStatus.PENDING))
                .thenReturn(List.of(upload));

        scheduler.processPendingUploads();

        verify(batchJobService).process(trackingId);
    }

    @Test
    void shouldProcessAllPendingUploadsInRepositoryOrder() {
        UUID firstTrackingId = UUID.randomUUID();
        UUID secondTrackingId = UUID.randomUUID();
        UUID thirdTrackingId = UUID.randomUUID();

        UploadTracking firstUpload =
                createUpload(firstTrackingId);
        UploadTracking secondUpload =
                createUpload(secondTrackingId);
        UploadTracking thirdUpload =
                createUpload(thirdTrackingId);

        when(uploadTrackingRepository
                .findByStatusOrderByCreatedAtAsc(UploadStatus.PENDING))
                .thenReturn(List.of(
                        firstUpload,
                        secondUpload,
                        thirdUpload
                ));

        scheduler.processPendingUploads();

        InOrder processingOrder = inOrder(batchJobService);

        processingOrder.verify(batchJobService)
                .process(firstTrackingId);
        processingOrder.verify(batchJobService)
                .process(secondTrackingId);
        processingOrder.verify(batchJobService)
                .process(thirdTrackingId);

        processingOrder.verifyNoMoreInteractions();
    }

    @Test
    void shouldContinueProcessingWhenOneUploadFails() {
        UUID firstTrackingId = UUID.randomUUID();
        UUID secondTrackingId = UUID.randomUUID();
        UUID thirdTrackingId = UUID.randomUUID();

        UploadTracking firstUpload =
                createUpload(firstTrackingId);
        UploadTracking secondUpload =
                createUpload(secondTrackingId);
        UploadTracking thirdUpload =
                createUpload(thirdTrackingId);

        when(uploadTrackingRepository
                .findByStatusOrderByCreatedAtAsc(UploadStatus.PENDING))
                .thenReturn(List.of(
                        firstUpload,
                        secondUpload,
                        thirdUpload
                ));

        doThrow(new RuntimeException("Batch launch failed"))
                .when(batchJobService)
                .process(secondTrackingId);

        scheduler.processPendingUploads();

        InOrder processingOrder = inOrder(batchJobService);

        processingOrder.verify(batchJobService)
                .process(firstTrackingId);
        processingOrder.verify(batchJobService)
                .process(secondTrackingId);
        processingOrder.verify(batchJobService)
                .process(thirdTrackingId);
    }

    private UploadTracking createUpload(UUID trackingId) {
        UploadTracking upload = new UploadTracking();
        upload.setTrackingId(trackingId);
        upload.setStatus(UploadStatus.PENDING);

        return upload;
    }
}