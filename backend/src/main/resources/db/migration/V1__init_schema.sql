CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    student_id VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone VARCHAR(20),
    department VARCHAR(50),
    password_hash VARCHAR(255) NOT NULL,
    is_admin BOOLEAN DEFAULT FALSE,
    avg_rating DECIMAL(3,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE trips (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    partner_id BIGINT NOT NULL,
    departure_time DATETIME NOT NULL,
    expected_return_time DATETIME NOT NULL,
    actual_return_time DATETIME,
    destination_area VARCHAR(50) NOT NULL,
    capacity_notes VARCHAR(255),
    status ENUM('PLANNED', 'IN_CITY', 'RETURNING', 'COMPLETED', 'CANCELLED') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trips_partner FOREIGN KEY (partner_id) REFERENCES users(id)
);

CREATE TABLE requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    category VARCHAR(50),
    quantity INT DEFAULT 1 CHECK (quantity >= 1),
    preferred_shop VARCHAR(150),
    pickup_area VARCHAR(50) NOT NULL,
    budget DECIMAL(10,2) CHECK (budget >= 0),
    instructions TEXT,
    status ENUM('REQUESTED', 'ACCEPTED', 'COLLECTED', 'RETURNING', 'READY_FOR_DELIVERY', 'DELIVERED', 'CANCELLED') NOT NULL,
    matched_trip_id BIGINT,
    version INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_requests_customer FOREIGN KEY (customer_id) REFERENCES users(id),
    CONSTRAINT fk_requests_matched_trip FOREIGN KEY (matched_trip_id) REFERENCES trips(id)
);

CREATE TABLE status_updates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    note VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_status_updates_request FOREIGN KEY (request_id) REFERENCES requests(id)
);

CREATE TABLE ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL UNIQUE,
    rated_by BIGINT NOT NULL,
    rated_user BIGINT NOT NULL,
    score TINYINT NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ratings_request FOREIGN KEY (request_id) REFERENCES requests(id),
    CONSTRAINT fk_ratings_rated_by FOREIGN KEY (rated_by) REFERENCES users(id),
    CONSTRAINT fk_ratings_rated_user FOREIGN KEY (rated_user) REFERENCES users(id)
);

CREATE TABLE complaints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT,
    raised_by BIGINT NOT NULL,
    against_user BIGINT,
    description TEXT NOT NULL,
    status ENUM('OPEN', 'IN_REVIEW', 'RESOLVED') NOT NULL,
    admin_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    CONSTRAINT fk_complaints_request FOREIGN KEY (request_id) REFERENCES requests(id),
    CONSTRAINT fk_complaints_raised_by FOREIGN KEY (raised_by) REFERENCES users(id),
    CONSTRAINT fk_complaints_against_user FOREIGN KEY (against_user) REFERENCES users(id)
);

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL UNIQUE,
    product_cost DECIMAL(10,2) NOT NULL,
    delivery_fee DECIMAL(10,2) NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    status ENUM('SIMULATED_PAID') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_request FOREIGN KEY (request_id) REFERENCES requests(id)
);

-- Indexes
CREATE INDEX idx_requests_status ON requests(status);
CREATE INDEX idx_requests_customer_id ON requests(customer_id);
CREATE INDEX idx_requests_matched_trip_id ON requests(matched_trip_id);

CREATE INDEX idx_trips_partner_id ON trips(partner_id);
CREATE INDEX idx_trips_status ON trips(status);

CREATE INDEX idx_ratings_rated_user ON ratings(rated_user);
