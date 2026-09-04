package com.employee.employeeingestionplatform.repository;

import com.employee.employeeingestionplatform.entity.UploadStatus;
import com.employee.employeeingestionplatform.entity.UploadTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UploadTrackingRepository
        extends JpaRepository<UploadTracking, UUID> {

    List<UploadTracking> findByStatusOrderByCreatedAtAsc(
            UploadStatus status
    );
}