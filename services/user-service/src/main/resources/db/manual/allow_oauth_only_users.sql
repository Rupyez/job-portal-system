-- Run once against job_portal_user_db before deploying OAuth2 login
-- to an existing database. Review your database's current schema first.
ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NULL;

ALTER TABLE users
    ADD COLUMN oauth_provider VARCHAR(30) NULL,
    ADD COLUMN oauth_subject VARCHAR(255) NULL,
    ADD CONSTRAINT uk_users_oauth_subject UNIQUE (oauth_subject);

CREATE TABLE oauth2_authorization_codes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    consumed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_oauth2_authorization_codes_code_hash UNIQUE (code_hash),
    CONSTRAINT fk_oauth2_authorization_codes_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_oauth2_code_user_id (user_id)
);