-- ============================================================
-- Card Management System - Database Schema
-- ============================================================

-- Drop tables if they exist (in reverse order of dependencies)
DROP TABLE IF EXISTS CardRequest;
DROP TABLE IF EXISTS Card;
DROP TABLE IF EXISTS User;
DROP TABLE IF EXISTS CardRequestType;
DROP TABLE IF EXISTS RequestStatus;
DROP TABLE IF EXISTS CardStatus;

-- ============================================================
-- Reference Tables
-- ============================================================

-- CardStatus table: Stores status codes for cards
CREATE TABLE CardStatus (
    StatusCode VARCHAR(20) PRIMARY KEY,
    Description VARCHAR(100) NOT NULL,
    CONSTRAINT chk_card_status_code CHECK (StatusCode IN ('IACT', 'CACT', 'DACT'))
);

-- RequestStatus table: Stores status codes for card requests
CREATE TABLE RequestStatus (
    StatusCode VARCHAR(20) PRIMARY KEY,
    Description VARCHAR(100) NOT NULL,
    CONSTRAINT chk_request_status_code CHECK (StatusCode IN ('PEND', 'APPR', 'RJCT'))
);

-- CardRequestType table: Stores types of card requests
CREATE TABLE CardRequestType (
    Code VARCHAR(20) PRIMARY KEY,
    Description VARCHAR(100) NOT NULL,
    CONSTRAINT chk_request_type CHECK (Code IN ('ACTI', 'CDCL'))
);

-- ============================================================
-- Main Tables
-- ============================================================

-- User table: Stores user information
CREATE TABLE User (
    UserName VARCHAR(50) PRIMARY KEY,
    Status VARCHAR(20) NOT NULL,
    Name VARCHAR(100) NOT NULL,
    Description VARCHAR(500),
    CreatedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    LastUpdateTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_status CHECK (Status IN ('ACT', 'DACT')),
    CONSTRAINT chk_username_not_empty CHECK (LENGTH(UserName) > 0),
    CONSTRAINT chk_name_not_empty CHECK (LENGTH(Name) > 0)
);

-- Card table: Stores card information
-- Note: CardNumber stores ENCRYPTED card numbers (Base64-encoded), so VARCHAR(255) is required
CREATE TABLE Card (
    CardNumber VARCHAR(255) PRIMARY KEY,
    ExpiryDate DATE NOT NULL,
    CardStatus VARCHAR(20) NOT NULL DEFAULT 'IACT',
    CreditLimit DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    CashLimit DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    AvailableCreditLimit DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    AvailableCashLimit DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    LastUpdatedUser VARCHAR(50) NOT NULL,
    LastUpdateTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_status FOREIGN KEY (CardStatus) REFERENCES CardStatus(StatusCode),
    CONSTRAINT fk_card_last_updated_user FOREIGN KEY (LastUpdatedUser) REFERENCES User(UserName),
    CONSTRAINT chk_credit_limit CHECK (CreditLimit >= 0),
    CONSTRAINT chk_cash_limit CHECK (CashLimit >= 0),
    CONSTRAINT chk_available_credit CHECK (AvailableCreditLimit >= 0 AND AvailableCreditLimit <= CreditLimit),
    CONSTRAINT chk_available_cash CHECK (AvailableCashLimit >= 0 AND AvailableCashLimit <= CashLimit)
);

-- CardRequest table: Stores card activation/deactivation requests
-- Note: CardNumber references encrypted card numbers, so VARCHAR(255) is required
CREATE TABLE CardRequest (
    RequestID BIGINT AUTO_INCREMENT PRIMARY KEY,
    CardNumber VARCHAR(255) NOT NULL,
    RequestStatusCode VARCHAR(20) NOT NULL DEFAULT 'PEND',
    RequestTypeCode VARCHAR(20) NOT NULL,
    Reason VARCHAR(500),
    RequestedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ProcessedAt TIMESTAMP NULL,
    RequestedUser VARCHAR(50),
    ApprovedUser VARCHAR(50),
    CONSTRAINT fk_request_card FOREIGN KEY (CardNumber) REFERENCES Card(CardNumber) ON DELETE CASCADE,
    CONSTRAINT fk_request_status FOREIGN KEY (RequestStatusCode) REFERENCES RequestStatus(StatusCode),
    CONSTRAINT fk_request_type FOREIGN KEY (RequestTypeCode) REFERENCES CardRequestType(Code),
    CONSTRAINT fk_request_requested_user FOREIGN KEY (RequestedUser) REFERENCES User(UserName),
    CONSTRAINT fk_request_approved_user FOREIGN KEY (ApprovedUser) REFERENCES User(UserName)
);

-- ============================================================
-- Indexes for better query performance
-- ============================================================

CREATE INDEX idx_card_status ON Card(CardStatus);
CREATE INDEX idx_card_update_time ON Card(LastUpdateTime);
CREATE INDEX idx_request_status ON CardRequest(RequestStatusCode);
CREATE INDEX idx_request_card ON CardRequest(CardNumber);
CREATE INDEX idx_request_created ON CardRequest(RequestedAt);
CREATE INDEX idx_user_status ON User(Status);
CREATE INDEX idx_user_update_time ON User(LastUpdateTime);

-- ============================================================
-- Initial Data
-- ============================================================

-- Insert CardStatus values
INSERT INTO CardStatus (StatusCode, Description) VALUES
('IACT', 'Card Inactive - Initial/Pending state'),
('CACT', 'Card Active - Normal active state'),
('DACT', 'Card Deactivated - Card has been deactivated');

-- Insert RequestStatus values
INSERT INTO RequestStatus (StatusCode, Description) VALUES
('PEND', 'Request Pending - Awaiting approval'),
('APPR', 'Request Approved - Request has been approved'),
('RJCT', 'Request Rejected - Request has been rejected');

-- Insert CardRequestType values
INSERT INTO CardRequestType (Code, Description) VALUES
('ACTI', 'Card Activation Request'),
('CDCL', 'Card Close Request');

-- ============================================================
-- Sample Users (Initial Data)
-- ============================================================

INSERT INTO User (UserName, Status, Name, Description) VALUES
('admin', 'ACT', 'Supun', 'I am the main admin'),
('admin1', 'DACT', 'Vidura', 'I am the second admin');

-- ============================================================
-- Sample Cards (Optional - for testing)
-- ============================================================
-- NOTE: Sample cards removed because CardNumber now stores ENCRYPTED values.
-- Cards must be created through the application API to ensure proper encryption.
-- Plain card numbers cannot be inserted directly into the database.

-- ============================================================
-- Sample Card Requests (Optional - for testing)
-- ============================================================
-- NOTE: Sample card requests removed because they reference encrypted CardNumbers.
-- Card requests must be created through the application API after creating cards.

