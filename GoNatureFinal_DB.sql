-- GoNature Database Schema Setup
-- Target Database: gonature

CREATE DATABASE IF NOT EXISTS gonature;
USE gonature;

-- Drop tables if they exist to start fresh
DROP TABLE IF EXISTS park_occupancy_log;
DROP TABLE IF EXISTS promotions;
DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS subscribers;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS parks;

-- Table 1: Parks
CREATE TABLE parks (
    park_id INT AUTO_INCREMENT PRIMARY KEY,
    park_name VARCHAR(100) UNIQUE NOT NULL,
    max_quota INT NOT NULL,
    current_quota INT NOT NULL,
    reserved_gap INT NOT NULL,
    stay_duration INT NOT NULL DEFAULT 4, -- Default in hours (1 hour = 30s in simulation)
    pending_max_quota INT DEFAULT NULL,
    pending_reserved_gap INT DEFAULT NULL,
    pending_stay_duration INT DEFAULT NULL,
    pending_changes_status VARCHAR(30) DEFAULT 'NONE' -- 'NONE', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED'
);

-- Table 2: Users (Employees and Guides)
CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(50) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role VARCHAR(30) NOT NULL, -- 'PARK_EMPLOYEE', 'PARK_MANAGER', 'DEPARTMENT_MANAGER', 'SERVICE_REPRESENTATIVE', 'GUIDE'
    email VARCHAR(100) NOT NULL,
    assigned_park_id INT DEFAULT NULL,
    is_logged_in BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (assigned_park_id) REFERENCES parks(park_id) ON DELETE SET NULL
);

-- Table 3: Subscribers (Traveler Club Members)
CREATE TABLE subscribers (
    subscriber_id INT AUTO_INCREMENT PRIMARY KEY,
    id_number VARCHAR(20) UNIQUE NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    family_size INT NOT NULL,
    credit_card_number VARCHAR(20) DEFAULT NULL
);

-- Table 4: Reservations
CREATE TABLE reservations (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    visitor_id VARCHAR(50) NOT NULL, -- Subscriber ID, Guide Username, or Guest ID
    park_id INT NOT NULL,
    visit_date_time DATETIME NOT NULL,
    number_of_visitors INT NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    reservation_type VARCHAR(30) NOT NULL, -- 'INDIVIDUAL', 'FAMILY_SUBSCRIBER', 'ORGANIZED_GROUP'
    status VARCHAR(30) NOT NULL, -- 'PENDING_CONFIRMATION', 'CONFIRMED', 'CANCELLED', 'WAITING_LIST', 'ACTIVE', 'COMPLETED'
    payment_status VARCHAR(30) NOT NULL, -- 'UNPAID', 'PAID_IN_ADVANCE', 'PAID_AT_ENTRANCE'
    price DOUBLE NOT NULL,
    created_at DATETIME NOT NULL,
    actual_entry_time DATETIME DEFAULT NULL,
    actual_exit_time DATETIME DEFAULT NULL,
    reminder_sent_time DATETIME DEFAULT NULL,
    spot_promoted_time DATETIME DEFAULT NULL,
    cancelled_at DATETIME DEFAULT NULL,
    is_no_show BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (park_id) REFERENCES parks(park_id) ON DELETE CASCADE
);

-- Table 5: Promotions (Campaigns)
CREATE TABLE promotions (
    promotion_id INT AUTO_INCREMENT PRIMARY KEY,
    park_id INT NOT NULL,
    promotion_name VARCHAR(100) NOT NULL,
    discount_percentage DOUBLE NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL, -- 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'EXPIRED'
    FOREIGN KEY (park_id) REFERENCES parks(park_id) ON DELETE CASCADE
);

-- Table 6: Park Occupancy Logs (For historical charts & reports)
CREATE TABLE park_occupancy_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    park_id INT NOT NULL,
    log_time DATETIME NOT NULL,
    current_visitors INT NOT NULL,
    FOREIGN KEY (park_id) REFERENCES parks(park_id) ON DELETE CASCADE
);

-- ==========================================
-- SEED INITIAL DATA
-- ==========================================

-- 1. Insert Parks
INSERT INTO parks (park_name, max_quota, current_quota, reserved_gap, stay_duration) VALUES
('Masada Nature Reserve', 200, 200, 30, 4),
('Banias Springs', 150, 150, 20, 3),
('Carmel Forest Park', 300, 300, 50, 4);

-- 2. Insert Employees
-- Roles: 'PARK_EMPLOYEE', 'PARK_MANAGER', 'DEPARTMENT_MANAGER', 'SERVICE_REPRESENTATIVE', 'GUIDE'
INSERT INTO users (username, password, first_name, last_name, role, email, assigned_park_id) VALUES
('emp_masada', 'password123', 'John', 'Doe', 'PARK_EMPLOYEE', 'john.masada@gonature.gov.il', 1),
('emp_banias', 'password123', 'Jane', 'Smith', 'PARK_EMPLOYEE', 'jane.banias@gonature.gov.il', 2),
('emp_carmel', 'password123', 'Tom', 'Brown', 'PARK_EMPLOYEE', 'tom.carmel@gonature.gov.il', 3),
('mgr_masada', 'password123', 'Robert', 'Miller', 'PARK_MANAGER', 'robert.masada@gonature.gov.il', 1),
('mgr_banias', 'password123', 'Emma', 'Davis', 'PARK_MANAGER', 'emma.banias@gonature.gov.il', 2),
('mgr_carmel', 'password123', 'Lucy', 'White', 'PARK_MANAGER', 'lucy.carmel@gonature.gov.il', 3),
('dept_mgr', 'password123', 'Sarah', 'Chief', 'DEPARTMENT_MANAGER', 'sarah.chief@gonature.gov.il', NULL),
('rep_service', 'password123', 'Alice', 'Rep', 'SERVICE_REPRESENTATIVE', 'alice.service@gonature.gov.il', NULL);

-- 3. Insert Some Initial Guides (as Users)
INSERT INTO users (username, password, first_name, last_name, role, email, assigned_park_id) VALUES
('guide_dan', 'password123', 'Dan', 'TheGuide', 'GUIDE', 'dan.guide@gmail.com', NULL),
('guide_yuri', 'password123', 'Yuri', 'Explorer', 'GUIDE', 'yuri.explorer@gmail.com', NULL);

-- 4. Insert Initial Subscribers
INSERT INTO subscribers (id_number, first_name, last_name, email, phone_number, family_size, credit_card_number) VALUES
('312345678', 'Michael', 'Green', 'michael.green@gmail.com', '054-1234567', 4, '1234-5678-9012-3456'),
('203456789', 'Jessica', 'Alba', 'jessica.alba@gmail.com', '052-7654321', 1, NULL),
('305678901', 'David', 'Beckham', 'david.b@gmail.com', '050-9876543', 6, '9876-5432-1098-7654');

-- 5. Seed Park Occupancy Logs for Monthly Reports (Mock data for May 2026)
INSERT INTO park_occupancy_log (park_id, log_time, current_visitors) VALUES
(1, '2026-05-15 09:00:00', 45),
(1, '2026-05-15 10:00:00', 80),
(1, '2026-05-15 11:00:00', 120),
(1, '2026-05-15 12:00:00', 140),
(1, '2026-05-15 13:00:00', 135),
(1, '2026-05-15 14:00:00', 95),
(1, '2026-05-15 15:00:00', 50),
(2, '2026-05-15 09:00:00', 20),
(2, '2026-05-15 11:00:00', 60),
(2, '2026-05-15 14:00:00', 95),
(3, '2026-05-15 10:00:00', 50),
(3, '2026-05-15 12:00:00', 110),
(3, '2026-05-15 14:00:00', 80);

-- 6. Seed Reservations (Completed & Canceled mock data for report analytics)
INSERT INTO reservations (visitor_id, park_id, visit_date_time, number_of_visitors, email, phone_number, reservation_type, status, payment_status, price, created_at, actual_entry_time, actual_exit_time, cancelled_at, is_no_show) VALUES
('1', 1, '2026-05-10 10:00:00', 4, 'michael.green@gmail.com', '054-1234567', 'FAMILY_SUBSCRIBER', 'COMPLETED', 'PAID_AT_ENTRANCE', 244.8, '2026-05-05 14:00:00', '2026-05-10 09:55:00', '2026-05-10 13:45:00', NULL, FALSE),
('guest123', 1, '2026-05-11 11:00:00', 2, 'guest.one@gmail.com', '053-1112222', 'INDIVIDUAL', 'COMPLETED', 'PAID_AT_ENTRANCE', 136.0, '2026-05-09 09:00:00', '2026-05-11 11:05:00', '2026-05-11 15:10:00', NULL, FALSE),
('guide_dan', 2, '2026-05-12 09:00:00', 12, 'dan.guide@gmail.com', '054-1234567', 'ORGANIZED_GROUP', 'COMPLETED', 'PAID_IN_ADVANCE', 594.0, '2026-05-01 10:00:00', '2026-05-12 08:58:00', '2026-05-12 12:00:00', NULL, FALSE),
('203456789', 2, '2026-05-15 14:00:00', 4, 'jessica.alba@gmail.com', '052-7654321', 'FAMILY_SUBSCRIBER', 'COMPLETED', 'PAID_AT_ENTRANCE', 244.8, '2026-05-10 11:00:00', '2026-05-15 13:58:00', '2026-05-15 17:50:00', NULL, FALSE),
('guest456', 2, '2026-05-14 14:00:00', 3, 'guest.two@gmail.com', '053-2223333', 'INDIVIDUAL', 'CANCELLED', 'UNPAID', 204.0, '2026-05-13 12:00:00', NULL, NULL, '2026-05-14 08:30:00', FALSE),
('guest101', 3, '2026-05-20 12:00:00', 2, 'guest.one@gmail.com', '053-1112222', 'INDIVIDUAL', 'COMPLETED', 'PAID_AT_ENTRANCE', 136.0, '2026-05-18 10:00:00', '2026-05-20 12:02:00', '2026-05-20 15:45:00', NULL, FALSE),
('guide_yuri', 3, '2026-05-22 10:00:00', 15, 'yuri.explorer@gmail.com', '050-9876543', 'ORGANIZED_GROUP', 'COMPLETED', 'PAID_IN_ADVANCE', 742.5, '2026-05-15 12:00:00', '2026-05-22 09:55:00', '2026-05-22 13:50:00', NULL, FALSE),
('guest789', 3, '2026-05-16 10:00:00', 5, 'guest.three@gmail.com', '053-4445555', 'INDIVIDUAL', 'CANCELLED', 'UNPAID', 340.0, '2026-05-14 11:00:00', NULL, NULL, NULL, TRUE);

