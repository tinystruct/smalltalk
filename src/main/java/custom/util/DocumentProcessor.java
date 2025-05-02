package custom.util;

import custom.objects.DocumentFragment;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.tinystruct.ApplicationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
     * @param filePath Path to the document file
     * @param mimeType MIME type of the document
     * @return Extracted text content
     * @throws IOException if file reading fails
     */
    private String extractContent(String filePath, String mimeType) throws IOException {
        File file = new File(filePath);

        // Detect MIME type if not provided
        String detectedMimeType = mimeType != null ? mimeType : tika.detect(file);
        System.out.println("Processing file: " + filePath + " with MIME type: " + detectedMimeType);

        // For simple text files, use direct file reading for efficiency
        if (detectedMimeType.equals("text/plain") ||
            detectedMimeType.equals("text/markdown") ||
            detectedMimeType.equals("text/csv")) {
            return readFileContent(file);
        }

        try {
            // Use Tika to extract text content from rich documents
            String content = tika.parseToString(file);

            // Check if content was extracted successfully
            if (content == null || content.trim().isEmpty()) {
                System.err.println("Warning: Tika extracted empty content from file: " + filePath);

                // For DOCX files, try an alternative approach if the content is empty
                if (detectedMimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                    System.out.println("Attempting alternative DOCX parsing using Apache POI...");
                    try {
                        content = extractDocxContent(file);

                        if (content == null || content.trim().isEmpty()) {
                            System.err.println("Alternative parsing also produced empty content");
                        } else {
                            System.out.println("Alternative parsing successful, extracted " + content.length() + " characters");
                        }
                    } catch (Exception ex) {
                        System.err.println("Alternative parsing failed: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            } else {
                System.out.println("Successfully extracted " + content.length() + " characters from document");
            }

            return content;
        } catch (TikaException e) {
            System.err.println("Tika exception: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to parse document with Tika: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected exception during content extraction: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Unexpected error during content extraction: " + e.getMessage(), e);
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
     * Extract content from a DOCX file using Apache POI
     * @param file The DOCX file
     * @return The extracted text content
     * @throws IOException if reading fails
     */
    private String extractDocxContent(File file) throws IOException {
        // First try using Tika with explicit configuration
        try {
            System.out.println("Trying to extract DOCX content with explicit Tika configuration...");
            Tika configuredTika = new Tika();
            String content = configuredTika.parseToString(file);
            if (content != null && !content.trim().isEmpty()) {
                System.out.println("Successfully extracted " + content.length() + " characters with configured Tika");
                return content;
            }
        } catch (Exception e) {
            System.err.println("Error with configured Tika: " + e.getMessage());
        }

        // If POI classes are available, try using them directly
        try {
            // Check if POI classes are available
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

                return content;
            }
        } catch (ClassNotFoundException e) {
            System.err.println("POI classes not available: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error extracting DOCX content with POI: " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback: Try to read the file as a ZIP and extract text from document.xml
        try {
            System.out.println("Trying fallback method to extract DOCX content...");
            return extractTextFromDocxAsZip(file);
        } catch (Exception e) {
            System.err.println("Fallback extraction failed: " + e.getMessage());
            e.printStackTrace();
        }

        // If all methods fail, throw an exception
        throw new IOException("Failed to extract content from DOCX file using all available methods");
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

                    // Extract text from XML (simple approach - extract content between <w:t> tags)
                    String xml = xmlContent.toString();
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<w:t[^>]*>(.*?)</w:t>", java.util.regex.Pattern.DOTALL);
                    java.util.regex.Matcher matcher = pattern.matcher(xml);

                    while (matcher.find()) {
                        content.append(matcher.group(1)).append(" ");
                    }

                    System.out.println("Extracted " + content.length() + " characters from document.xml");
                }
            } else {
                System.err.println("Could not find word/document.xml in the DOCX file");
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