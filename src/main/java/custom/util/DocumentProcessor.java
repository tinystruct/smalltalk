package custom.util;

import custom.objects.DocumentFragment;
import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for processing documents into fragments for embedding and search.
 */
public class DocumentProcessor {

    // Token limit constants
    private static final int MAX_TOKEN_LIMIT = 8192; // OpenAI's maximum context length
    private static final int SAFE_TOKEN_LIMIT = 6000; // Reduced safe limit to stay well under the maximum

    // Character to token ratio (approximate)
    private static final int CHARS_PER_TOKEN = 4; // Approximate ratio for English text

    // Fragment size constants (in characters)
    private static final int DEFAULT_MAX_FRAGMENT_SIZE = 1000; // Default maximum characters per fragment
    private static final int DEFAULT_MIN_FRAGMENT_SIZE = 500;  // Default minimum characters per fragment

    // Maximum tokens per fragment
    private static final int MAX_TOKENS_PER_FRAGMENT = 1500; // Maximum tokens per fragment

    // Configurable fragment sizes
    private int maxFragmentSize;
    private int minFragmentSize;

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

    public DocumentProcessor() {
        this.maxFragmentSize = DEFAULT_MAX_FRAGMENT_SIZE;
        this.minFragmentSize = DEFAULT_MIN_FRAGMENT_SIZE;
    }

    /**
     * Constructor with configurable fragment sizes
     *
     * @param maxFragmentSize Maximum characters per fragment
     * @param minFragmentSize Minimum characters per fragment to aim for
     */
    public DocumentProcessor(int maxFragmentSize, int minFragmentSize) {
        // Calculate the maximum safe fragment size in characters
        int maxSafeCharSize = SAFE_TOKEN_LIMIT * CHARS_PER_TOKEN;

        // Ensure the fragment size doesn't exceed the token limit
        this.maxFragmentSize = Math.min(maxFragmentSize, maxSafeCharSize);
        this.minFragmentSize = Math.min(minFragmentSize, this.maxFragmentSize / 2);

        System.out.println("Configured DocumentProcessor with maxFragmentSize=" + this.maxFragmentSize +
                          " chars (approx. " + (this.maxFragmentSize / CHARS_PER_TOKEN) + " tokens), " +
                          "minFragmentSize=" + this.minFragmentSize + " chars");
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
     * Check if a given file extension is supported for document processing
     * @param fileName The file name to check
     * @return True if the file extension is supported
     */
    public static boolean isSupportedFileExtension(String fileName) {
        if (fileName == null) return false;

        fileName = fileName.toLowerCase();
        return fileName.endsWith(".docx") ||
               fileName.endsWith(".doc") ||
               fileName.endsWith(".pdf") ||
               fileName.endsWith(".txt") ||
               fileName.endsWith(".md") ||
               fileName.endsWith(".csv");
    }

    /**
     * Process a document file into fragments with user information
     * @param filePath Path to the document file
     * @param mimeType MIME type of the document
     * @param userId ID of the user who uploaded the document
     * @param title Title of the document
     * @param description Description of the document
     * @param isPublic Whether the document is publicly accessible
     * @return List of document fragments
     * @throws ApplicationException if processing fails
     */
    public List<DocumentFragment> processDocument(String filePath, String mimeType, String userId, String title, String description, boolean isPublic) throws ApplicationException {
        // Check if the file extension is supported
        if (!isSupportedFileExtension(filePath)) {
            throw new ApplicationException("Unsupported file type: " + filePath);
        }

        // For backward compatibility, also check MIME type if provided
        if (mimeType != null && !mimeType.isEmpty() && !isSupportedMimeType(mimeType)) {
            throw new ApplicationException("Unsupported MIME type: " + mimeType);
        }

        List<DocumentFragment> fragments = new ArrayList<>();
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            throw new ApplicationException("File not found or is not a regular file: " + filePath);
        }

        try {
            // Extract content using Tika for rich documents or direct reading for text files
            String content = extractContent(filePath);

            // Check if we got any content
            if (content == null || content.trim().isEmpty()) {
                throw new ApplicationException("Failed to extract any content from the document");
            }

            String documentId = UUID.randomUUID().toString();

            // Split content into fragments
            List<String> contentFragments = splitContentIntoFragments(content);

            // Create document fragments
            for (int i = 0; i < contentFragments.size(); i++) {
                DocumentFragment fragment = new DocumentFragment();
                // Don't set ID - let the database generate it automatically
                fragment.setDocumentId(documentId);
                fragment.setContent(contentFragments.get(i));
                fragment.setFragmentIndex(i);
                fragment.setFilePath(filePath);
                fragment.setMimeType(mimeType);
                fragment.setCreatedAt(new Date());
                fragment.setUserId(userId);
                fragment.setTitle(title);
                fragment.setDescription(description);
                fragment.setIsPublic(isPublic);

                fragments.add(fragment);
            }

            return fragments;
        } catch (IOException e) {
            throw new ApplicationException("Failed to process document: " + e.getMessage(), e);
        }
    }

    public List<DocumentFragment> processDocument(String filePath, String mimeType) throws ApplicationException {
        // Call the more detailed method with default values
        return processDocument(filePath, mimeType, null, getDefaultTitle(filePath), "", true);
    }

    /**
     * Generate a default title from the file path
     * @param filePath Path to the file
     * @return A title based on the file name
     */
    private String getDefaultTitle(String filePath) {
        File file = new File(filePath);
        String fileName = file.getName();
        // Remove extension if present
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileName = fileName.substring(0, dotIndex);
        }
        // Replace underscores and hyphens with spaces
        fileName = fileName.replace('_', ' ').replace('-', ' ');
        // Capitalize first letter of each word
        String[] words = fileName.split("\\s+");
        StringBuilder titleBuilder = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                titleBuilder.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    titleBuilder.append(word.substring(1));
                }
                titleBuilder.append(" ");
            }
        }
        return titleBuilder.toString().trim();
    }

    /**
     * Extract content from a document based on its MIME type
     *
     * @param filePath Path to the document file
     * @return Extracted text content
     * @throws IOException if file reading fails
     */
    private String extractContent(String filePath) throws IOException, ApplicationException {
        File file = new File(filePath);
        return extractContent(file);
    }

    /**
     * Extract content from a DOCX file using Apache POI
     * @param file The DOCX file
     * @return The extracted text content
     * @throws IOException if reading fails
     */
    private String extractContent(File file) throws ApplicationException {
        try {
            // Detect file type based on extension
            String fileName = file.getName().toLowerCase();
            System.out.println("Processing file: " + fileName);

            // Process based on file extension
            if (fileName.endsWith(".docx")) {
                return extractDocx(file);
            } else if (fileName.endsWith(".doc")) {
                return extractDoc(file);
            } else if (fileName.endsWith(".pdf")) {
                return extractPdf(file);
            } else if (fileName.endsWith(".txt") || fileName.endsWith(".md") || fileName.endsWith(".csv")) {
                return new String(Files.readAllBytes(file.toPath()));
            } else {
                // For unknown types, try to extract as DOCX first, then as plain text
                try {
                    return extractDocx(file);
                } catch (Exception e) {
                    System.out.println("Failed to extract as DOCX, trying as plain text");
                    return new String(Files.readAllBytes(file.toPath()));
                }
            }
        } catch (IOException e) {
            throw new ApplicationException("Failed to extract content from file: " + e.getMessage(), e);
        }
    }

    /**
     * Extract text from a DOCX file (Office Open XML)
     * @param file The DOCX file
     * @return The extracted text
     * @throws IOException If extraction fails
     */
    private String extractDocx(File file) throws IOException {
        System.out.println("Extracting DOCX file...");

        try {
            // Try using Apache POI first
            Class.forName("org.apache.poi.xwpf.usermodel.XWPFDocument");
            try (FileInputStream fis = new FileInputStream(file)) {
                // Use reflection to avoid direct dependency that might not be available
                Object document = Class.forName("org.apache.poi.xwpf.usermodel.XWPFDocument")
                        .getConstructor(FileInputStream.class)
                        .newInstance(fis);

                Object extractor = Class.forName("org.apache.poi.xwpf.extractor.XWPFWordExtractor")
                        .getConstructor(Class.forName("org.apache.poi.xwpf.usermodel.XWPFDocument"))
                        .newInstance(document);

                String content = (String) extractor.getClass().getMethod("getText").invoke(extractor);
                System.out.println("POI extracted " + (content != null ? content.length() : 0) + " characters");

                // Close resources
                extractor.getClass().getMethod("close").invoke(extractor);

                if (content != null && !content.trim().isEmpty()) {
                    return content;
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("POI classes not available: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error extracting DOCX content with POI: " + e.getMessage());
        }

        // Fallback: Try to read the file as a ZIP and extract text from document.xml
        try {
            System.out.println("Trying fallback method to extract DOCX content...");
            return extractTextFromDocxAsZip(file);
        } catch (IOException e) {
            System.err.println("Error extracting DOCX content as ZIP: " + e.getMessage());
            throw new IOException("Failed to extract DOCX content: " + e.getMessage(), e);
        }
    }

    /**
     * Extract text from a legacy DOC file (Microsoft Word 97-2003)
     * @param file The DOC file
     * @return The extracted text
     * @throws IOException If extraction fails
     */
    private String extractDoc(File file) throws IOException {
        System.out.println("Extracting DOC file...");

        try {
            // Try using Apache POI HWPF
            Class.forName("org.apache.poi.hwpf.HWPFDocument");

            try (FileInputStream fis = new FileInputStream(file)) {
                // Use reflection to avoid direct dependency that might not be available
                Object document = Class.forName("org.apache.poi.hwpf.HWPFDocument")
                        .getConstructor(InputStream.class)
                        .newInstance(fis);

                Object extractor = Class.forName("org.apache.poi.hwpf.extractor.WordExtractor")
                        .getConstructor(Class.forName("org.apache.poi.hwpf.HWPFDocument"))
                        .newInstance(document);

                String content = (String) extractor.getClass().getMethod("getText").invoke(extractor);
                System.out.println("POI extracted " + (content != null ? content.length() : 0) + " characters from DOC");

                // Close resources
                extractor.getClass().getMethod("close").invoke(extractor);

                if (content != null && !content.trim().isEmpty()) {
                    return content;
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("POI HWPF classes not available: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error extracting DOC content with POI: " + e.getMessage());
        }

        // No fallback available for DOC files
        throw new IOException("Failed to extract DOC content: No suitable extractor available");
    }

    /**
     * Extract text from a PDF file
     * @param file The PDF file
     * @return The extracted text
     * @throws IOException If extraction fails
     */
    private String extractPdf(File file) throws IOException {
        System.out.println("Extracting PDF file...");

        try {
            // Try using PDFBox if available
            Class.forName("org.apache.pdfbox.pdmodel.PDDocument");

            Object document = null;
            try {
                document = Class.forName("org.apache.pdfbox.pdmodel.PDDocument")
                        .getMethod("load", File.class)
                        .invoke(null, file);

                Object extractor = Class.forName("org.apache.pdfbox.text.PDFTextStripper")
                        .getConstructor()
                        .newInstance();

                String content = (String) extractor.getClass().getMethod("getText",
                        Class.forName("org.apache.pdfbox.pdmodel.PDDocument"))
                        .invoke(extractor, document);

                System.out.println("PDFBox extracted " + (content != null ? content.length() : 0) + " characters");

                if (content != null && !content.trim().isEmpty()) {
                    return content;
                }
            } finally {
                // Close the document
                if (document != null) {
                    document.getClass().getMethod("close").invoke(document);
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("PDFBox classes not available: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error extracting PDF content with PDFBox: " + e.getMessage());
        }

        // No fallback available for PDF files
        throw new IOException("Failed to extract PDF content: No suitable extractor available");
    }

    /**
     * Extract text from a DOCX file by treating it as a ZIP archive and reading document.xml
     * @param file The DOCX file
     * @return The extracted text content
     * @throws IOException if reading fails
     */
    private String extractTextFromDocxAsZip(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(file)) {
            // Look for document.xml in the word directory
            java.util.zip.ZipEntry documentEntry = zipFile.getEntry("word/document.xml");
            if (documentEntry != null) {
                try (java.io.InputStream is = zipFile.getInputStream(documentEntry)) {
                    // Read the XML content
                    byte[] buffer = new byte[1024];
                    int read;
                    StringBuilder xmlContent = new StringBuilder();
                    while ((read = is.read(buffer)) != -1) {
                        xmlContent.append(new String(buffer, 0, read, "UTF-8"));
                    }

                    // Extract text from XML - only get the actual text content between <w:t> tags
                    String xml = xmlContent.toString();
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<w:t[^>]*>(.*?)</w:t>", java.util.regex.Pattern.DOTALL);
                    java.util.regex.Matcher matcher = pattern.matcher(xml);

                    while (matcher.find()) {
                        // Only add the actual text content (group 1), not the XML tags
                        String textContent = matcher.group(1);
                        if (textContent != null && !textContent.trim().isEmpty()) {
                            content.append(textContent).append(" ");
                        }
                    }

                    System.out.println("Extracted " + content.length() + " characters from document.xml");
                }
            } else {
                System.err.println("Could not find word/document.xml in the DOCX file");
            }
        }

        return content.toString().trim();
    }

    /**
     * Split content into reasonably sized fragments for embedding
     * @param content The document content
     * @return List of content fragments
     */
    private List<String> splitContentIntoFragments(String content) {
        List<String> fragments = new ArrayList<>();

        // Calculate maximum characters per fragment based on token limit
        int maxCharsPerFragment = MAX_TOKENS_PER_FRAGMENT * CHARS_PER_TOKEN;
        System.out.println("Using max chars per fragment: " + maxCharsPerFragment +
                          " (approx. " + MAX_TOKENS_PER_FRAGMENT + " tokens)");

        // Split by paragraphs first (one or more newlines)
        String[] paragraphs = content.split("\\n\\s*\\n");

        StringBuilder currentFragment = new StringBuilder();
        int estimatedTokens = 0;

        for (String paragraph : paragraphs) {
            // Skip empty paragraphs
            if (paragraph.trim().isEmpty()) {
                continue;
            }

            // Estimate tokens in this paragraph
            int paragraphTokens = estimateTokens(paragraph);
            System.out.println("Paragraph length: " + paragraph.length() + " chars, estimated tokens: " + paragraphTokens);

            // If adding this paragraph would exceed the token limit, start a new fragment
            if (estimatedTokens + paragraphTokens > MAX_TOKENS_PER_FRAGMENT) {
                // Only save if we have something significant
                if (currentFragment.length() > 0) {
                    String fragment = currentFragment.toString().trim();
                    fragments.add(fragment);
                    System.out.println("Created fragment with " + fragment.length() + " chars, approx. " +
                                      estimateTokens(fragment) + " tokens");

                    currentFragment = new StringBuilder();
                    estimatedTokens = 0;
                }

                // If the paragraph itself is too large, split it further
                if (paragraphTokens > MAX_TOKENS_PER_FRAGMENT) {
                    System.out.println("Paragraph exceeds token limit, splitting further");
                    splitLargeParagraph(paragraph, fragments);
                    continue;
                }
            }

            // Add the paragraph to the current fragment
            currentFragment.append(paragraph).append("\n\n");
            estimatedTokens += paragraphTokens + 2; // Add 2 tokens for the newlines

            // If we've accumulated enough text for a decent-sized fragment, save it
            if (estimatedTokens >= MAX_TOKENS_PER_FRAGMENT / 2) {
                String fragment = currentFragment.toString().trim();
                fragments.add(fragment);
                System.out.println("Created fragment with " + fragment.length() + " chars, approx. " +
                                  estimateTokens(fragment) + " tokens");

                currentFragment = new StringBuilder();
                estimatedTokens = 0;
            }
        }

        // Add any remaining content
        if (currentFragment.length() > 0) {
            String fragment = currentFragment.toString().trim();
            fragments.add(fragment);
            System.out.println("Created final fragment with " + fragment.length() + " chars, approx. " +
                              estimateTokens(fragment) + " tokens");
        }

        return fragments;
    }

    /**
     * Estimate the number of tokens in a text string
     * This is a simple approximation based on character count
     * @param text The text to estimate tokens for
     * @return Estimated token count
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Simple approximation: 1 token ≈ 4 characters for English text
        return (int) Math.ceil((double) text.length() / CHARS_PER_TOKEN);
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
        int estimatedTokens = 0;

        for (String sentence : sentences) {
            // Add period back since it was removed by the split
            if (!sentence.endsWith(".")) {
                sentence = sentence + ". ";
            }

            // Estimate tokens in this sentence
            int sentenceTokens = estimateTokens(sentence);

            // If adding this sentence would exceed the token limit, start a new fragment
            if (estimatedTokens + sentenceTokens > MAX_TOKENS_PER_FRAGMENT) {
                if (currentFragment.length() > 0) {
                    String fragment = currentFragment.toString().trim();
                    fragments.add(fragment);
                    System.out.println("Created sentence fragment with " + fragment.length() + " chars, approx. " +
                                      estimateTokens(fragment) + " tokens");

                    currentFragment = new StringBuilder();
                    estimatedTokens = 0;
                }

                // If the sentence itself is extremely large, we might need to split it further
                // This is a rare case, but we handle it by truncating
                if (sentenceTokens > MAX_TOKENS_PER_FRAGMENT) {
                    System.out.println("Warning: Very long sentence detected (" + sentenceTokens + " tokens), truncating");
                    // Split the sentence into chunks that fit within token limit
                    int maxChars = MAX_TOKENS_PER_FRAGMENT * CHARS_PER_TOKEN;
                    for (int i = 0; i < sentence.length(); i += maxChars) {
                        int endIndex = Math.min(i + maxChars, sentence.length());
                        String chunk = sentence.substring(i, endIndex);
                        fragments.add(chunk);
                        System.out.println("Created truncated fragment with " + chunk.length() + " chars");
                    }
                    continue;
                }
            }

            // Add the sentence to the current fragment
            currentFragment.append(sentence);
            estimatedTokens += sentenceTokens;
        }

        // Add any remaining content
        if (currentFragment.length() > 0) {
            String fragment = currentFragment.toString().trim();
            fragments.add(fragment);
            System.out.println("Created final sentence fragment with " + fragment.length() + " chars, approx. " +
                              estimateTokens(fragment) + " tokens");
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