-- ============================================================
-- Migration: Add LastUpdatedUser column to Card table
-- Date: 2026-02-23
-- Purpose: Track which user last modified each card
-- ============================================================

USE cms_db;

-- Step 1: Add LastUpdatedUser column to Card table (allow NULL temporarily for existing cards)
ALTER TABLE Card 
ADD COLUMN LastUpdatedUser VARCHAR(50) NULL AFTER AvailableCashLimit;

-- Step 2: Update existing cards to set LastUpdatedUser to 'admin' (default active user)
-- This ensures we don't have NULL values after making it NOT NULL
UPDATE Card 
SET LastUpdatedUser = 'admin' 
WHERE LastUpdatedUser IS NULL;

-- Step 3: Make LastUpdatedUser NOT NULL now that all existing records have values
ALTER TABLE Card 
MODIFY COLUMN LastUpdatedUser VARCHAR(50) NOT NULL;

-- Step 4: Add foreign key constraint to ensure LastUpdatedUser references a valid user
ALTER TABLE Card 
ADD CONSTRAINT fk_card_last_updated_user 
FOREIGN KEY (LastUpdatedUser) REFERENCES User(UserName);

-- Verification: Show the updated table structure
DESCRIBE Card;

SELECT 'Migration completed successfully. LastUpdatedUser column added to Card table.' as status;
