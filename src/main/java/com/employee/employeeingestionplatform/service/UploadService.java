package com.employee.employeeingestionplatform.service;

import com.employee.employeeingestionplatform.dto.upload.UploadResponse;
import com.employee.employeeingestionplatform.dto.upload.UploadStatusResponse;
import com.employee.employeeingestionplatform.entity.UploadStatus;
import com.employee.employeeingestionplatform.entity.UploadTracking;
import com.employee.employeeingestionplatform.repository.RejectedRecordRepository;
import com.employee.employeeingestionplatform.repository.UploadTrackingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.employee.employeeingestionplatform.dto.employee.PageResponse;
import com.employee.employeeingestionplatform.dto.upload.RejectedRecordResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class UploadService {

    private final UploadTrackingRepository uploadTrackingRepository;
    private final Path uploadDirectory;
    private final RejectedRecordRepository rejectedRecordRepository;

    public UploadService(
            UploadTrackingRepository uploadTrackingRepository,
            RejectedRecordRepository rejectedRecordRepository,
            @Value("${app.upload.directory}") String uploadDirectory
    ) {
        this.uploadTrackingRepository = uploadTrackingRepository;
        this.rejectedRecordRepository = rejectedRecordRepository;
        this.uploadDirectory = Path.of(uploadDirectory)
                .toAbsolutePath()
                .normalize();
    }

    public UploadResponse store(MultipartFile file) {
        validate(file);

        String originalFileName = file.getOriginalFilename();
        UUID trackingId = UUID.randomUUID();
        String storedFileName = trackingId + ".xlsx";
        Path targetPath = uploadDirectory.resolve(storedFileName);

        try {
            Files.createDirectories(uploadDirectory);

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            UploadTracking tracking = new UploadTracking();
            tracking.setTrackingId(trackingId);
            tracking.setOriginalFileName(originalFileName);
            tracking.setStoredFilePath(targetPath.toString());
            tracking.setStatus(UploadStatus.PENDING);

            uploadTrackingRepository.save(tracking);

            return new UploadResponse(
                    trackingId,
                    originalFileName,
                    UploadStatus.PENDING,
                    "File uploaded successfully and is awaiting processing"
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not store the uploaded file",
                    exception
            );
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "An Excel file is required"
            );
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null || fileName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The uploaded file must have a name"
            );
        }

        String normalizedFileName = fileName.toLowerCase(Locale.ROOT);

        if (!normalizedFileName.endsWith(".xlsx")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only .xlsx Excel files are supported"
            );
        }
    }

    public UploadStatusResponse getStatus(UUID trackingId) {
        UploadTracking tracking = uploadTrackingRepository
                .findById(trackingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Upload tracking record not found"
                ));

        return new UploadStatusResponse(
                tracking.getTrackingId(),
                tracking.getOriginalFileName(),
                tracking.getStatus(),
                tracking.getTotalRows(),
                tracking.getSuccessRows(),
                tracking.getRejectedRows(),
                tracking.getErrorSummary(),
                tracking.getCreatedAt(),
                tracking.getStartedAt(),
                tracking.getCompletedAt()
        );
    }

    public PageResponse<RejectedRecordResponse> getRejectedRecords(
            UUID trackingId,
            int page,
            int size
    ) {
        if (!uploadTrackingRepository.existsById(trackingId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Upload tracking record not found"
            );
        }

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "rowNumber")
        );

        Page<RejectedRecordResponse> result =
                rejectedRecordRepository
                        .findByTrackingId(trackingId, pageRequest)
                        .map(record -> new RejectedRecordResponse(
                                record.getId(),
                                record.getRowNumber(),
                                record.getEmpId(),
                                record.getReason(),
                                record.getCreatedAt()
                        ));

        return PageResponse.from(result);
    }
}