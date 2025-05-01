
DROP TABLE IF EXISTS users;
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    full_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    is_admin BOOLEAN DEFAULT FALSE
);

-- Create indexes for users
CREATE INDEX idx_username ON users(username);
CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_user_created_at ON users(created_at);

-- Add this table to the init.sql file as well


DROP TABLE IF EXISTS document_fragments;
-- Create document_fragments table
CREATE TABLE IF NOT EXISTS document_fragments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    document_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    fragment_index INTEGER NOT NULL,
    file_path VARCHAR(255),
    mime_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id INTEGER,
    title VARCHAR(255),
    description TEXT,
    is_public BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
-- Create indexes for document_fragments
CREATE INDEX idx_document_id ON document_fragments(document_id);
CREATE INDEX idx_fragment_index ON document_fragments(fragment_index);
CREATE INDEX idx_created_at ON document_fragments(created_at);
CREATE INDEX idx_user_id ON document_fragments(user_id);
CREATE INDEX idx_is_public ON document_fragments(is_public);

DROP TABLE IF EXISTS document_embeddings;
CREATE TABLE IF NOT EXISTS document_embeddings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fragment_id VARCHAR(100) NOT NULL,
    embedding BLOB NOT NULL,
    embedding_dimension INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Create indexes for document_embeddings
CREATE INDEX idx_fragment_id ON document_embeddings(fragment_id);
CREATE INDEX idx_embedding_created_at ON document_embeddings(created_at);

DROP TABLE IF EXISTS chat_history;
CREATE TABLE IF NOT EXISTS chat_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    meeting_code VARCHAR(255) NOT NULL,
    user_name VARCHAR(255),
    message TEXT,
    session_id VARCHAR(255),
    message_type VARCHAR(50),
    image_url TEXT,
    created_at DATE DEFAULT CURRENT_DATE
);
-- Create indexes for chat_history
CREATE INDEX idx_meeting_code ON chat_history(meeting_code);
CREATE INDEX idx_chat_created_at ON chat_history(created_at);