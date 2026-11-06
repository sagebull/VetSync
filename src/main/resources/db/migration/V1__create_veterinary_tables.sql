CREATE TABLE owners (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(160) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    email VARCHAR(254) NOT NULL,
    address VARCHAR(500) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE pets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    species VARCHAR(80) NOT NULL,
    breed VARCHAR(120) NOT NULL,
    sex VARCHAR(16) NOT NULL,
    birth_date DATE NOT NULL,
    weight DECIMAL(7, 3) NOT NULL,
    observations VARCHAR(2000),
    owner_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pets_owner FOREIGN KEY (owner_id) REFERENCES owners (id) ON DELETE RESTRICT,
    INDEX idx_pets_owner_id (owner_id),
    INDEX idx_pets_name (name)
) ENGINE=InnoDB;

CREATE TABLE medical_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pet_id BIGINT NOT NULL,
    visit_date DATE NOT NULL,
    consultation VARCHAR(2000) NOT NULL,
    diagnosis VARCHAR(2000),
    treatment VARCHAR(2000),
    medication VARCHAR(2000),
    vaccines VARCHAR(2000),
    veterinary_observations VARCHAR(4000),
    PRIMARY KEY (id),
    CONSTRAINT fk_medical_records_pet FOREIGN KEY (pet_id) REFERENCES pets (id) ON DELETE RESTRICT,
    INDEX idx_medical_records_pet_date (pet_id, visit_date)
) ENGINE=InnoDB;
