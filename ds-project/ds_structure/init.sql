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
('4328', 'student.name1'),
('5231', 'student.name2'),
('6220', 'student.name3'),
('6430', 'student.name4'),
('7014', 'student.name5'),
('7211', 'ole.roessler'),
('8888', 'test.user');

INSERT INTO assignment_results (assignment, passed, uid) VALUES 
('1', 'false', '4328'),
('1', 'false', '5231'),
('1', 'false', '6220'),
('1', 'false', '6430'),
('1', 'false', '7014'),
('1', 'false', '7211');