CREATE TABLE user_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(16) NOT NULL,
    owner_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_accounts_email UNIQUE (email),
    CONSTRAINT uk_user_accounts_owner UNIQUE (owner_id),
    CONSTRAINT fk_user_accounts_owner
        FOREIGN KEY (owner_id) REFERENCES owners (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_accounts_role CHECK (role IN ('OWNER', 'STAFF')),
    CONSTRAINT chk_user_accounts_owner_role CHECK (
        (role = 'OWNER' AND owner_id IS NOT NULL)
        OR (role = 'STAFF' AND owner_id IS NULL)
    )
) ENGINE=InnoDB;
