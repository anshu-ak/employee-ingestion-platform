package com.employee.employeeingestionplatform.controller;

import com.employee.employeeingestionplatform.dto.upload.BatchLaunchResponse;
import com.employee.employeeingestionplatform.dto.upload.UploadResponse;
import com.employee.employeeingestionplatform.dto.upload.UploadStatusResponse;
import com.employee.employeeingestionplatform.service.BatchJobService;
import com.employee.employeeingestionplatform.service.UploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.employee.employeeingestionplatform.dto.employee.PageResponse;
import com.employee.employeeingestionplatform.dto.upload.RejectedRecordResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/employees")
public class UploadController {

    private final UploadService uploadService;
    private final BatchJobService batchJobService;

    public UploadController(
            UploadService uploadService,
            BatchJobService batchJobService
    ) {
        this.uploadService = uploadService;
        this.batchJobService = batchJobService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('ADMIN')")
    public UploadResponse upload(
            @RequestParam("file") MultipartFile file
    ) {
        return uploadService.store(file);
    }

    @GetMapping("/uploads/{trackingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UploadStatusResponse getStatus(
            @PathVariable UUID trackingId
    ) {
        return uploadService.getStatus(trackingId);
    }

    @PostMapping("/uploads/{trackingId}/process")
    @PreAuthorize("hasRole('ADMIN')")
    public BatchLaunchResponse process(
            @PathVariable UUID trackingId
    ) {
        return batchJobService.process(trackingId);
    }

    @GetMapping("/uploads/{trackingId}/rejections")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<RejectedRecordResponse> getRejectedRecords(
            @PathVariable UUID trackingId,

            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size
    ) {
        return uploadService.getRejectedRecords(
                trackingId,
                page,
                size
        );
    }
}