CREATE TABLE veterinarians (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL,
    specialty VARCHAR(120) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_veterinarians_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE appointments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    pet_id BIGINT NOT NULL,
    veterinarian_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_appointments_owner FOREIGN KEY (owner_id) REFERENCES owners (id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_pet FOREIGN KEY (pet_id) REFERENCES pets (id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_veterinarian FOREIGN KEY (veterinarian_id) REFERENCES veterinarians (id) ON DELETE RESTRICT,
    CONSTRAINT chk_appointments_time CHECK (end_time > start_time),
    CONSTRAINT chk_appointments_status CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
    INDEX idx_appointments_veterinarian_slot (veterinarian_id, appointment_date, start_time, end_time, status),
    INDEX idx_appointments_pet (pet_id, appointment_date),
    INDEX idx_appointments_owner (owner_id, appointment_date)
) ENGINE=InnoDB;
