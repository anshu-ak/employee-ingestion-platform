package com.employee.employeeingestionplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "upload_tracking")
@Getter
@Setter
@NoArgsConstructor
public class UploadTracking {

    @Id
    @Column(name = "tracking_id", nullable = false, updatable = false)
    private UUID trackingId;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "stored_file_path", nullable = false, length = 1000)
    private String storedFilePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UploadStatus status;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "success_rows", nullable = false)
    private int successRows;

    @Column(name = "rejected_rows", nullable = false)
    private int rejectedRows;

    @Column(name = "error_summary", length = 2000)
    private String errorSummary;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (trackingId == null) {
            trackingId = UUID.randomUUID();
        }

        if (status == null) {
            status = UploadStatus.PENDING;
        }

        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}