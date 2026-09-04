package com.employee.employeeingestionplatform.batch.listener;

import com.employee.employeeingestionplatform.batch.exception.InvalidEmployeeRowException;
import com.employee.employeeingestionplatform.batch.model.EmployeeExcelRow;
import com.employee.employeeingestionplatform.entity.Employee;
import com.employee.employeeingestionplatform.entity.RejectedRecord;
import com.employee.employeeingestionplatform.repository.RejectedRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;

import java.util.UUID;

public class RejectedEmployeeSkipListener
        implements SkipListener<EmployeeExcelRow, Employee> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RejectedEmployeeSkipListener.class);

    private final UUID trackingId;
    private final RejectedRecordRepository rejectedRecordRepository;

    public RejectedEmployeeSkipListener(
            UUID trackingId,
            RejectedRecordRepository rejectedRecordRepository
    ) {
        this.trackingId = trackingId;
        this.rejectedRecordRepository = rejectedRecordRepository;
    }

    @Override
    public void onSkipInRead(Throwable throwable) {
        LOGGER.error(
                "Could not read an Excel row for upload {}",
                trackingId,
                throwable
        );
    }

    @Override
    public void onSkipInProcess(
            EmployeeExcelRow row,
            Throwable throwable
    ) {
        RejectedRecord rejectedRecord = new RejectedRecord();
        rejectedRecord.setTrackingId(trackingId);
        rejectedRecord.setRowNumber(row.rowNumber());
        rejectedRecord.setEmpId(row.empId());
        rejectedRecord.setReason(resolveReason(throwable));

        rejectedRecordRepository.save(rejectedRecord);

        LOGGER.warn(
                "Rejected Excel row {} for upload {}: {}",
                row.rowNumber(),
                trackingId,
                throwable.getMessage()
        );
    }

    @Override
    public void onSkipInWrite(
            Employee employee,
            Throwable throwable
    ) {
        LOGGER.error(
                "Could not write employee {} for upload {}",
                employee.getEmpId(),
                trackingId,
                throwable
        );
    }

    private String resolveReason(Throwable throwable) {
        if (throwable instanceof InvalidEmployeeRowException) {
            return throwable.getMessage();
        }

        String message = throwable.getMessage();

        if (message == null || message.isBlank()) {
            return "Unknown validation error";
        }

        return message.length() <= 1000
                ? message
                : message.substring(0, 1000);
    }
}