-- ============================================================
-- Card Management System - Database Schema
-- ============================================================

-- Drop tables if they exist (in reverse order of dependencies)
DROP TABLE IF EXISTS CardRequest;
DROP TABLE IF EXISTS Card;
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

-- Card table: Stores card information
CREATE TABLE Card (
    CardNumber VARCHAR(16) PRIMARY KEY,
    ExpiryDate DATE NOT NULL,
    CardStatus VARCHAR(20) NOT NULL DEFAULT 'IACT',
    CreditLimit DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    CashLimit DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    AvailableCreditLimit DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    AvailableCashLimit DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    LastUpdateTime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_status FOREIGN KEY (CardStatus) REFERENCES CardStatus(StatusCode),
    CONSTRAINT chk_card_number CHECK (LENGTH(CardNumber) >= 13 AND LENGTH(CardNumber) <= 16),
    CONSTRAINT chk_credit_limit CHECK (CreditLimit >= 0),
    CONSTRAINT chk_cash_limit CHECK (CashLimit >= 0),
    CONSTRAINT chk_available_credit CHECK (AvailableCreditLimit >= 0 AND AvailableCreditLimit <= CreditLimit),
    CONSTRAINT chk_available_cash CHECK (AvailableCashLimit >= 0 AND AvailableCashLimit <= CashLimit)
);

-- CardRequest table: Stores card activation/deactivation requests
CREATE TABLE CardRequest (
    RequestID BIGINT AUTO_INCREMENT PRIMARY KEY,
    CardNumber VARCHAR(16) NOT NULL,
    RequestStatusCode VARCHAR(20) NOT NULL DEFAULT 'PEND',
    RequestTypeCode VARCHAR(20) NOT NULL,
    Reason VARCHAR(500),
    RequestedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ProcessedAt TIMESTAMP NULL,
    CONSTRAINT fk_request_card FOREIGN KEY (CardNumber) REFERENCES Card(CardNumber) ON DELETE CASCADE,
    CONSTRAINT fk_request_status FOREIGN KEY (RequestStatusCode) REFERENCES RequestStatus(StatusCode),
    CONSTRAINT fk_request_type FOREIGN KEY (RequestTypeCode) REFERENCES CardRequestType(Code)
);

-- ============================================================
-- Indexes for better query performance
-- ============================================================

CREATE INDEX idx_card_status ON Card(CardStatus);
CREATE INDEX idx_card_update_time ON Card(LastUpdateTime);
CREATE INDEX idx_request_status ON CardRequest(RequestStatusCode);
CREATE INDEX idx_request_card ON CardRequest(CardNumber);
CREATE INDEX idx_request_created ON CardRequest(RequestedAt);

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
-- Sample Cards (Optional - for testing)
-- ============================================================

INSERT INTO Card (CardNumber, ExpiryDate, CardStatus, CreditLimit, CashLimit, AvailableCreditLimit, AvailableCashLimit) VALUES
('4532015112830366', '2027-12-31', 'CACT', 100000.00, 50000.00, 75000.00, 40000.00),
('5425233430109903', '2028-06-30', 'CACT', 150000.00, 75000.00, 150000.00, 75000.00),
('6011111111111117', '2026-03-31', 'IACT', 80000.00, 40000.00, 80000.00, 40000.00),
('378282246310005', '2027-09-30', 'DACT', 200000.00, 100000.00, 120000.00, 60000.00);

-- ============================================================
-- Sample Card Requests (Optional - for testing)
-- ============================================================

INSERT INTO CardRequest (CardNumber, RequestStatusCode, RequestTypeCode, Reason) VALUES
('6011111111111117', 'PEND', 'ACTI', 'Customer requested to activate card for international travel'),
('4532015112830366', 'APPR', 'CDCL', 'Card close request approved after clearing all debts');
