package com.employee.employeeingestionplatform.service;

import com.employee.employeeingestionplatform.dto.upload.UploadResponse;
import com.employee.employeeingestionplatform.entity.UploadStatus;
import com.employee.employeeingestionplatform.entity.UploadTracking;
import com.employee.employeeingestionplatform.repository.RejectedRecordRepository;
import com.employee.employeeingestionplatform.repository.UploadTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Mock
    private UploadTrackingRepository uploadTrackingRepository;

    @Mock
    private RejectedRecordRepository rejectedRecordRepository;

    private UploadService uploadService;

    @BeforeEach
    void setUp() {
        uploadService = new UploadService(
                uploadTrackingRepository,
                rejectedRecordRepository,
                temporaryDirectory.toString()
        );
    }

    @Test
    void shouldStoreValidExcelFileAndCreateTrackingRecord()
            throws Exception {

        byte[] content = "test Excel content".getBytes();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employees.xlsx",
                "application/vnd.openxmlformats-officedocument"
                        + ".spreadsheetml.sheet",
                content
        );

        UploadResponse response = uploadService.store(file);

        ArgumentCaptor<UploadTracking> trackingCaptor =
                ArgumentCaptor.forClass(UploadTracking.class);

        verify(uploadTrackingRepository).save(
                trackingCaptor.capture()
        );

        UploadTracking tracking = trackingCaptor.getValue();
        Path storedFile = Path.of(
                tracking.getStoredFilePath()
        );

        assertAll(
                () -> assertEquals(
                        "employees.xlsx",
                        response.fileName()
                ),
                () -> assertEquals(
                        UploadStatus.PENDING,
                        response.status()
                ),
                () -> assertEquals(
                        response.trackingId(),
                        tracking.getTrackingId()
                ),
                () -> assertEquals(
                        "employees.xlsx",
                        tracking.getOriginalFileName()
                ),
                () -> assertEquals(
                        UploadStatus.PENDING,
                        tracking.getStatus()
                ),
                () -> assertTrue(
                        Files.exists(storedFile)
                ),
                () -> assertEquals(
                        "test Excel content",
                        Files.readString(storedFile)
                )
        );
    }

    @Test
    void shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employees.xlsx",
                "application/vnd.openxmlformats-officedocument"
                        + ".spreadsheetml.sheet",
                new byte[0]
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> uploadService.store(file)
                );

        assertAll(
                () -> assertEquals(
                        HttpStatus.BAD_REQUEST,
                        exception.getStatusCode()
                ),
                () -> assertEquals(
                        "An Excel file is required",
                        exception.getReason()
                )
        );

        verify(uploadTrackingRepository, never())
                .save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectNonExcelFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employees.csv",
                "text/csv",
                "EMP001,Anshu".getBytes()
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> uploadService.store(file)
                );

        assertAll(
                () -> assertEquals(
                        HttpStatus.BAD_REQUEST,
                        exception.getStatusCode()
                ),
                () -> assertEquals(
                        "Only .xlsx Excel files are supported",
                        exception.getReason()
                )
        );

        verify(uploadTrackingRepository, never())
                .save(org.mockito.ArgumentMatchers.any());
    }
}