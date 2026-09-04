package com.employee.employeeingestionplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "rejected_records")
@Getter
@Setter
@NoArgsConstructor
public class RejectedRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_id", nullable = false)
    private UUID trackingId;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(name = "emp_id", length = 50)
    private String empId;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}