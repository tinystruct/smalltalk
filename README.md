# Document Question Answering System

This system provides a document-based question answering capability for the Smalltalk application. It allows users to upload documents, which are processed, split into fragments, and converted into vector embeddings for semantic search.

## Overview

The document QA system integrates with the existing Smalltalk application and enables:

1. Document upload and processing
2. Automatic splitting of documents into semantically meaningful fragments
3. Generation of vector embeddings for each fragment
4. Retrieval of relevant document fragments based on user queries
5. Enhanced responses that incorporate information from uploaded documents

## Key Components

### 1. DocumentFragment

This class represents a piece of text from a document, with metadata such as:
- Document ID
- Content
- Fragment index
- File path
- MIME type
- Creation timestamp

### 2. DocumentEmbedding

This class stores the vector representation of a document fragment:
- Fragment ID (references DocumentFragment)
- Embedding vector (stored as serialized byte array)
- Embedding dimension
- Creation timestamp

### 3. DocumentProcessor

Handles the processing of uploaded documents:
- Extracts textual content using Apache Tika for rich document formats
- Splits content into appropriately sized fragments
- Creates DocumentFragment objects for each fragment
- Supports PDF, Word, Excel, PowerPoint, and other document formats through Tika

### 4. EmbeddingManager

Manages the generation and retrieval of embeddings:
- Generates embeddings using OpenAI's embedding API
- Caches query embeddings to reduce API calls
- Implements the cosine similarity function for semantic search
- Provides methods to find similar documents based on query

### 5. DocumentQA

Implements the question answering functionality:
- Finds relevant document fragments for a given query
- Formats document context for inclusion in AI responses
- Enhances queries with document context

## Database Schema (SQLite)

The system uses SQLite for storage with the following schema:

```sql
CREATE TABLE document_fragments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    document_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    fragment_index INTEGER NOT NULL,
    file_path VARCHAR(255),
    mime_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE document_embeddings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fragment_id VARCHAR(100) NOT NULL,
    embedding BLOB NOT NULL,
    embedding_dimension INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chat_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    meeting_code VARCHAR(255) NOT NULL,
    user_name VARCHAR(255),
    message TEXT,
    session_id VARCHAR(255),
    message_type VARCHAR(50),
    image_url TEXT,
    created_at DATE DEFAULT CURRENT_DATE
);
```

## Integration with Smalltalk

The document QA functionality is integrated in the Smalltalk application:
- When a user uploads a document, it's automatically processed
- When a user asks a question, the system searches for relevant document fragments
- Retrieved fragments are included in the system prompt to provide context for the AI's response
- The AI can reference the specific document when answering questions

## Usage

### Document Upload

Documents can be uploaded through the Smalltalk interface. Supported file types include:
- Plain text files (text/plain)
- Markdown files (text/markdown)
- PDF documents (application/pdf)
- Word documents (docx, doc)
- Excel spreadsheets (xlsx, xls)
- PowerPoint presentations (pptx, ppt)
- Other text-based files (text/*)

### Asking Questions

Simply ask questions in the chat interface. The system will automatically:
1. Convert your question to an embedding
2. Find the most relevant document fragments
3. Include those fragments in the context sent to the AI
4. Return an answer that incorporates information from your documents

## Testing

The system includes several test classes to verify functionality:
- DocumentEmbedding.main() - Tests embedding storage and retrieval
- DocumentQATest - Tests the end-to-end document QA functionality
- DocumentProcessor.main() - Tests document processing and fragmentation

Run the tests using the provided batch file:
```
runtest.bat
```

This script will:
1. Initialize the SQLite database if needed
2. Run all test classes
3. Test document processing for various file formats

## Dependencies

The system relies on the following key libraries:
- SQLite - For database storage
- Apache Tika (3.1.0) - For extracting text from various document formats
- OpenAI API - For generating embeddings
- tinystruct - For application framework and database access

