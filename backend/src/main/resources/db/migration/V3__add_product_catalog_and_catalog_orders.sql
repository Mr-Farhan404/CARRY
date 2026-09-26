-- V3: Add Product catalog and extend requests for catalog-based orders

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT,
    image_url VARCHAR(500),
    estimated_price DECIMAL(10,2) NOT NULL,
    weight_class VARCHAR(30) NOT NULL,
    size_class VARCHAR(30) NOT NULL,
    is_sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_is_active ON products(is_active);

-- Extend requests table for catalog orders
ALTER TABLE requests ADD COLUMN order_type VARCHAR(30) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE requests ADD COLUMN product_id BIGINT DEFAULT NULL;
ALTER TABLE requests ADD COLUMN unit_price_snapshot DECIMAL(10,2) DEFAULT NULL;
ALTER TABLE requests ADD COLUMN delivery_charge DECIMAL(10,2) DEFAULT NULL;

ALTER TABLE requests ADD CONSTRAINT fk_requests_product FOREIGN KEY (product_id) REFERENCES products(id);

CREATE INDEX idx_requests_order_type ON requests(order_type);
CREATE INDEX idx_requests_product_id ON requests(product_id);

-- Seed 8 demo products (3 FOOD, 3 ELECTRONICS, 2 OTHERS)
INSERT INTO products (name, category, description, image_url, estimated_price, weight_class, size_class, is_sensitive, is_active)
VALUES
('Hyderabadi Kacchi Biryani', 'FOOD', 'Authentic spiced mutton kacchi biryani with potato and boiled egg', 'https://picsum.photos/seed/kacchi/400/300', 320.00, 'MEDIUM', 'MEDIUM', FALSE, TRUE),
('Crispy Fried Chicken Box (4 pcs)', 'FOOD', '4 pieces of crunchy golden fried chicken with garlic mayo dip', 'https://picsum.photos/seed/friedchicken/400/300', 280.00, 'LIGHT', 'SMALL', FALSE, TRUE),
('Ghol & Roshogolla Treat Box (1 kg)', 'FOOD', 'Fresh sour-sweet ghol drink and spongy cottage cheese roshogollas', 'https://picsum.photos/seed/roshogolla/400/300', 450.00, 'HEAVY', 'MEDIUM', TRUE, TRUE),
('Arduino Uno R3 Microcontroller Board', 'ELECTRONICS', 'ATmega328P development board for robotics and embedded engineering projects', 'https://picsum.photos/seed/arduino/400/300', 850.00, 'LIGHT', 'SMALL', TRUE, TRUE),
('RGB Mechanical Gaming Keyboard (Blue Switch)', 'ELECTRONICS', 'Clicky tactile mechanical keyboard with customizable RGB backlighting', 'https://picsum.photos/seed/keyboard/400/300', 1950.00, 'MEDIUM', 'MEDIUM', TRUE, TRUE),
('20000mAh Fast Charging Power Bank', 'ELECTRONICS', 'Dual USB-C PD 22.5W high capacity portable charger for campus days', 'https://picsum.photos/seed/powerbank/400/300', 1600.00, 'HEAVY', 'SMALL', TRUE, TRUE),
('Architectural Drafting T-Scale & Triangle Set', 'OTHERS', 'Precision drafting T-square with 30/60 and 45 degree engineering set squares', 'https://picsum.photos/seed/drafting/400/300', 550.00, 'LIGHT', 'LARGE', TRUE, TRUE),
('Heavy Duty Windproof Umbrella', 'OTHERS', 'Double-canopy stormproof folding umbrella for monsoon rain protection', 'https://picsum.photos/seed/umbrella/400/300', 650.00, 'MEDIUM', 'LARGE', FALSE, TRUE);
