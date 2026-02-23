-- ============================================================
-- Migration: Fix CardNumber column size for encrypted data
-- Date: 2026-02-23
-- Issue: CardNumber VARCHAR(16) is too small for encrypted data
-- Solution: Change to VARCHAR(255) to accommodate Base64-encoded encrypted card numbers
-- ============================================================

USE cms_db;

-- Step 1: Drop foreign key constraints that reference Card.CardNumber
ALTER TABLE CardRequest DROP FOREIGN KEY fk_request_card;

-- Step 2: Modify the CardNumber column in Card table
ALTER TABLE Card MODIFY COLUMN CardNumber VARCHAR(255) NOT NULL;

-- Step 3: Modify the CardNumber column in CardRequest table (foreign key reference)
ALTER TABLE CardRequest MODIFY COLUMN CardNumber VARCHAR(255) NOT NULL;

-- Step 4: Remove the old check constraint on CardNumber length
ALTER TABLE Card DROP CHECK chk_card_number;

-- Step 5: Recreate the foreign key constraint
ALTER TABLE CardRequest 
ADD CONSTRAINT fk_request_card 
FOREIGN KEY (CardNumber) REFERENCES Card(CardNumber) ON DELETE CASCADE;

-- Verification: Show the updated column definitions
DESCRIBE Card;
DESCRIBE CardRequest;

SELECT 'Migration completed successfully. CardNumber column is now VARCHAR(255).' as status;
