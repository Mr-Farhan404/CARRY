-- V5: Add moderator_name column and reset admin demo password
ALTER TABLE moderator_reports ADD COLUMN moderator_name VARCHAR(100) NOT NULL DEFAULT 'Moderator';

-- Ensure farhan@kuet.ac.bd password is password123 and role is ADMIN
UPDATE users SET password_hash = '$2a$10$tCNIO0GPJdCQQ/chloUQA.UUmjKZO69eZVdMzwHBbkX/AEGNx5lxW', role = 'ADMIN', is_admin = 1 WHERE email = 'farhan@kuet.ac.bd';
