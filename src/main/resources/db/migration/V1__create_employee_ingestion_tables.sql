CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    emp_id VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    department VARCHAR(100) NOT NULL,
    salary NUMERIC(19, 2) NOT NULL,
    source VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_employees_emp_id UNIQUE (emp_id),
    CONSTRAINT chk_employees_salary CHECK (salary >= 0),
    CONSTRAINT chk_employees_source CHECK (source IN ('EXCEL', 'KAFKA'))
);

CREATE INDEX idx_employees_department
    ON employees (department);

CREATE INDEX idx_employees_email
    ON employees (email);

CREATE INDEX idx_employees_source
    ON employees (source);


CREATE TABLE upload_tracking (
    tracking_id UUID PRIMARY KEY,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_path VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_rows INTEGER NOT NULL DEFAULT 0,
    success_rows INTEGER NOT NULL DEFAULT 0,
    rejected_rows INTEGER NOT NULL DEFAULT 0,
    error_summary VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_upload_tracking_status CHECK (
        status IN (
            'PENDING',
            'PROCESSING',
            'COMPLETED',
            'PARTIALLY_COMPLETED',
            'FAILED'
        )
    ),
    CONSTRAINT chk_upload_tracking_counts CHECK (
        total_rows >= 0
        AND success_rows >= 0
        AND rejected_rows >= 0
    )
);

CREATE INDEX idx_upload_tracking_status_created_at
    ON upload_tracking (status, created_at);


CREATE TABLE rejected_records (
    id BIGSERIAL PRIMARY KEY,
    tracking_id UUID NOT NULL,
    row_number INTEGER NOT NULL,
    emp_id VARCHAR(50),
    reason VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_rejected_records_upload
        FOREIGN KEY (tracking_id)
        REFERENCES upload_tracking (tracking_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_rejected_records_row_number
        CHECK (row_number > 0)
);

CREATE INDEX idx_rejected_records_tracking_id
    ON rejected_records (tracking_id);