-- Create User table
DROP TABLE IF EXISTS User;

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

CREATE INDEX idx_user_status ON User(Status);
CREATE INDEX idx_user_update_time ON User(LastUpdateTime);

INSERT INTO User (UserName, Status, Name, Description) VALUES
('admin', 'ACT', 'Supun', 'I am the main admin'),
('admin1', 'DACT', 'Vidura', 'I am the second admin');

SELECT * FROM User;
