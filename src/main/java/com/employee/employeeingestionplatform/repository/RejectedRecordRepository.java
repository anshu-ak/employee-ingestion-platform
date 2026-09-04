package com.employee.employeeingestionplatform.repository;

import com.employee.employeeingestionplatform.entity.RejectedRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RejectedRecordRepository
        extends JpaRepository<RejectedRecord, Long> {

    List<RejectedRecord> findByTrackingIdOrderByRowNumberAsc(
            UUID trackingId
    );

    Page<RejectedRecord> findByTrackingId(
            UUID trackingId,
            Pageable pageable
    );

    long countByTrackingId(UUID trackingId);
}