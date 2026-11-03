-- V4: Add Multi-Role System, Applications, and Moderator Reports

-- 1. Add role column to users
ALTER TABLE users ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT 'STUDENT';

-- Make sure admin accounts have ADMIN role
UPDATE users SET role = 'ADMIN' WHERE is_admin = 1;

-- 2. Create role_applications table
CREATE TABLE IF NOT EXISTS role_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    requested_role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reason TEXT,
    alternative_phone VARCHAR(30),
    bkash_number VARCHAR(30),
    estimated_return_time VARCHAR(100),
    active_honest_commitment BOOLEAN DEFAULT FALSE,
    agreed_to_rules BOOLEAN DEFAULT TRUE,
    admin_notes TEXT,
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_apps_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_apps_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_role_apps_user ON role_applications(user_id);
CREATE INDEX idx_role_apps_status ON role_applications(status);
CREATE INDEX idx_role_apps_role ON role_applications(requested_role);

-- 3. Create moderator_reports table
CREATE TABLE IF NOT EXISTS moderator_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    moderator_id BIGINT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    target_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mod_reports_moderator FOREIGN KEY (moderator_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_mod_reports_mod ON moderator_reports(moderator_id);
CREATE INDEX idx_mod_reports_action ON moderator_reports(action_type);
