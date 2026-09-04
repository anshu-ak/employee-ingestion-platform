CREATE TABLE application_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_application_users_username UNIQUE (username),
    CONSTRAINT chk_application_users_role
        CHECK (role IN ('ADMIN', 'USER'))
);