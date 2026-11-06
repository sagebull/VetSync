CREATE TABLE owner_registration_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(160) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    email VARCHAR(254) NOT NULL,
    address VARCHAR(500) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_owner_registration_requests_email (email),
    INDEX idx_owner_registration_requests_created_at (created_at)
) ENGINE=InnoDB;
