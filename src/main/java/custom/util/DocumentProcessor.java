package custom.util;

import custom.objects.DocumentFragment;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.tinystruct.ApplicationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for processing documents into fragments for embedding and search.
 */
public class DocumentProcessor {
    
    private static final int MAX_FRAGMENT_SIZE = 1000; // Maximum characters per fragment
    private static final int MIN_FRAGMENT_SIZE = 500;  // Minimum characters per fragment to aim for
    
    private static final String[] SUPPORTED_MIME_TYPES = {
        "text/plain",
        "text/markdown",
        "text/csv",
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/msword",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    };
    
    private final Tika tika;
    
    public DocumentProcessor() {
        this.tika = new Tika();
    }
    
    /**
     * Check if a given MIME type is supported for document processing
     * @param mimeType The MIME type to check
     * @return True if the MIME type is supported
     */
    public static boolean isSupportedMimeType(String mimeType) {
        if (mimeType == null) return false;
        
        mimeType = mimeType.toLowerCase();
        for (String supported : SUPPORTED_MIME_TYPES) {
            if (supported.equals(mimeType)) {
                return true;
            }
        }
        
        // Also support all text types
        return mimeType.startsWith("text/");
    }
    
    /**
     * Process a document file into fragments
     * @param filePath Path to the document file
     * @param mimeType MIME type of the document
     * @return List of document fragments
     * @throws ApplicationException if processing fails
     */
    public List<DocumentFragment> processDocument(String filePath, String mimeType) throws ApplicationException {
        if (!isSupportedMimeType(mimeType)) {
            throw new ApplicationException("Unsupported MIME type: " + mimeType);
        }
        
        List<DocumentFragment> fragments = new ArrayList<>();
        File file = new File(filePath);
        
        if (!file.exists() || !file.isFile()) {
            throw new ApplicationException("File not found or is not a regular file: " + filePath);
        }
        
        try {
            // Extract content using Tika for rich documents or direct reading for text files
            String content = extractContent(filePath, mimeType);
            String documentId = UUID.randomUUID().toString();
            
            // Split content into fragments
            List<String> contentFragments = splitContentIntoFragments(content);
            
            // Create document fragments
            for (int i = 0; i < contentFragments.size(); i++) {
                DocumentFragment fragment = new DocumentFragment();
                fragment.setId(UUID.randomUUID().toString());
                fragment.setDocumentId(documentId);
                fragment.setContent(contentFragments.get(i));
                fragment.setFragmentIndex(i);
                fragment.setFilePath(filePath);
                fragment.setMimeType(mimeType);
                fragment.setCreatedAt(new Date());
                
                fragments.add(fragment);
            }
            
            return fragments;
        } catch (IOException e) {
            throw new ApplicationException("Failed to process document: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract content from a document based on its MIME type
     * @param filePath Path to the document file
     * @param mimeType MIME type of the document
     * @return Extracted text content
     * @throws IOException if file reading fails
     */
    private String extractContent(String filePath, String mimeType) throws IOException {
        File file = new File(filePath);
        
        // Detect MIME type if not provided
        String detectedMimeType = mimeType != null ? mimeType : tika.detect(file);
        
        // For simple text files, use direct file reading for efficiency
        if (detectedMimeType.equals("text/plain") || 
            detectedMimeType.equals("text/markdown") || 
            detectedMimeType.equals("text/csv")) {
            return readFileContent(file);
        }
        
        try {
            // Use Tika to extract text content from rich documents
            return tika.parseToString(file);
        } catch (TikaException e) {
            throw new IOException("Failed to parse document with Tika: " + e.getMessage(), e);
        }
    }
    
    /**
     * Read the content of a plain text file
     * @param file The file to read
     * @return The file content as a string
     * @throws IOException if reading fails
     */
    private String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    /**
     * Split content into reasonably sized fragments for embedding
     * @param content The document content
     * @return List of content fragments
     */
    private List<String> splitContentIntoFragments(String content) {
        List<String> fragments = new ArrayList<>();
        
        // Split by paragraphs first (one or more newlines)
        String[] paragraphs = content.split("\\n\\s*\\n");
        
        StringBuilder currentFragment = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            // Skip empty paragraphs
            if (paragraph.trim().isEmpty()) {
                continue;
            }
            
            // If adding this paragraph would exceed the max size, start a new fragment
            if (currentFragment.length() + paragraph.length() > MAX_FRAGMENT_SIZE) {
                // Only save if we have something significant
                if (currentFragment.length() > 0) {
                    fragments.add(currentFragment.toString().trim());
                    currentFragment = new StringBuilder();
                }
                
                // If the paragraph itself is too large, split it further
                if (paragraph.length() > MAX_FRAGMENT_SIZE) {
                    splitLargeParagraph(paragraph, fragments);
                    continue;
                }
            }
            
            // Add the paragraph to the current fragment
            currentFragment.append(paragraph).append("\n\n");
            
            // If we've accumulated enough text for a decent-sized fragment, save it
            if (currentFragment.length() >= MIN_FRAGMENT_SIZE) {
                fragments.add(currentFragment.toString().trim());
                currentFragment = new StringBuilder();
            }
        }
        
        // Add any remaining content
        if (currentFragment.length() > 0) {
            fragments.add(currentFragment.toString().trim());
        }
        
        return fragments;
    }
    
    /**
     * Split a large paragraph into smaller fragments
     * @param paragraph The paragraph to split
     * @param fragments The list to add fragments to
     */
    private void splitLargeParagraph(String paragraph, List<String> fragments) {
        // Split by sentences (period followed by space or end)
        String[] sentences = paragraph.split("\\. ");
        
        StringBuilder currentFragment = new StringBuilder();
        
        for (String sentence : sentences) {
            // Add period back since it was removed by the split
            if (!sentence.endsWith(".")) {
                sentence = sentence + ". ";
            }
            
            // If adding this sentence would exceed the max size, start a new fragment
            if (currentFragment.length() + sentence.length() > MAX_FRAGMENT_SIZE) {
                fragments.add(currentFragment.toString().trim());
                currentFragment = new StringBuilder();
            }
            
            // Add the sentence to the current fragment
            currentFragment.append(sentence);
        }
        
        // Add any remaining content
        if (currentFragment.length() > 0) {
            fragments.add(currentFragment.toString().trim());
        }
    }
    
    /**
     * Main method for testing the document processor
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: DocumentProcessor <file_path> [mime_type]");
            return;
        }
        
        String filePath = args[0];
        String mimeType = args.length > 1 ? args[1] : "text/plain";
        
        try {
            DocumentProcessor processor = new DocumentProcessor();
            List<DocumentFragment> fragments = processor.processDocument(filePath, mimeType);
            
            System.out.println("Processed document into " + fragments.size() + " fragments:");
            for (int i = 0; i < fragments.size(); i++) {
                DocumentFragment fragment = fragments.get(i);
                System.out.println("Fragment " + (i + 1) + " (" + fragment.getContent().length() + " chars):");
                System.out.println("--------------------------------------------------");
                System.out.println(fragment.getContent().substring(0, Math.min(100, fragment.getContent().length())) + "...");
                System.out.println("--------------------------------------------------");
            }
        } catch (ApplicationException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
} 