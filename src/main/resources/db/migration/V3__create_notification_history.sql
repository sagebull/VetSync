CREATE TABLE notification_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    reminder_minutes BIGINT NOT NULL,
    recipient_email VARCHAR(254) NOT NULL,
    pet_name VARCHAR(120) NOT NULL,
    veterinarian_name VARCHAR(160) NOT NULL,
    appointment_start DATETIME(6) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    sent_at DATETIME(6) NULL,
    error_message VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_history_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments (id) ON DELETE RESTRICT,
    CONSTRAINT uk_notification_history_appointment_reminder
        UNIQUE (appointment_id, reminder_minutes),
    CONSTRAINT chk_notification_history_reminder CHECK (reminder_minutes > 0),
    CONSTRAINT chk_notification_history_status CHECK (status IN ('PROCESSING', 'SENT', 'FAILED')),
    INDEX idx_notification_history_created_at (created_at)
) ENGINE=InnoDB;
