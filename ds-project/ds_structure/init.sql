-- Create the 'test_results' table

CREATE TABLE users (
    uid SERIAL PRIMARY KEY,         -- Unique user ID (Auto-incremented)
    name VARCHAR(50) NOT NULL      -- User's name
);

CREATE TABLE assignment_results (
    id SERIAL PRIMARY KEY,          -- Unique identifier for test entries
    assignment INT NOT NULL,        -- Assignment number
    passed BOOLEAN NOT NULL,        -- Whether the user passed or not
    uid INT NOT NULL,               -- Foreign key linking to users table
    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE,
    UNIQUE (uid, assignment)        -- Prevents duplicate test results per user for the same assignment
);


INSERT INTO users (uid, name) VALUES 
('7211', 'ole.roessler'),
('8888', 'test.user');
