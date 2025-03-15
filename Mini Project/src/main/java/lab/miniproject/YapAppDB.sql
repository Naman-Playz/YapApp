-- Table 1: Users
CREATE TABLE users (
    email TEXT PRIMARY KEY,
    username TEXT NOT NULL,
    password TEXT NOT NULL,
);
-- CREATE TABLE users (
--     email VARCHAR(255) PRIMARY KEY,
--     username VARCHAR(100) NOT NULL,
--     hashed_password VARCHAR(255) NOT NULL
-- );

-- Table 2: Channels
CREATE TABLE channels(
     id SERIAL PRIMARY KEY,
     type BOOLEAN NOT NULL DEFAULT FALSE,
     name TEXT NOT NULL
);

-- CREATE TABLE channels (
--     channelId SERIAL PRIMARY KEY,
--     type VARCHAR(10) NOT NULL,
--     channelName VARCHAR(100) NOT NULL
-- );

-- Table 3: User-Channel Relationship
CREATE TABLE user_channels(
    email TEXT REFERENCES users(email) ON DELETE CASCADE,
    id INT REFERENCES channels(id) ON DELETE CASCADE,
    PRIMARY KEY (email, id)
);

-- CREATE TABLE user_channels (
--     user_email VARCHAR(255),
--     channelId INT,
--     PRIMARY KEY (user_email, channelId),
--     FOREIGN KEY (user_email) REFERENCES users(email) ON DELETE CASCADE,
--     FOREIGN KEY (channelId) REFERENCES channels(channelId) ON DELETE CASCADE
-- );

-- Table 4: Messages
CREATE TABLE messages (
    messageId SERIAL PRIMARY KEY,
    channelID INT,
    sender_email VARCHAR(255),
    content TEXT NOT NULL,
    time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (channelID) REFERENCES channels(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_email) REFERENCES users(email) ON DELETE CASCADE
);
