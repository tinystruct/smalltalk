package custom.util;

import custom.objects.DocumentFragment;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.system.ApplicationManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for document-based question answering.
 * Uses semantic search to find relevant document fragments for user queries.
 */
public class DocumentQA {

    private static final int DEFAULT_MAX_RESULTS = 5; // Increased from 3 to provide more context
    private static final double SIMILARITY_THRESHOLD = 0.65; // Slightly lowered to capture more relevant documents
    private static final int MAX_CONTEXT_LENGTH = 4000; // Maximum length of context to avoid token limits

    // Token limit constants
    private static final int MAX_TOKEN_LIMIT = 8192; // OpenAI's maximum context length
    private static final int SAFE_TOKEN_LIMIT = 7000; // Safe limit to stay under the maximum
    private static final int CHARS_PER_TOKEN = 4; // Approximate ratio for English text

    /**
     * Find relevant document fragments for a given query
     *
     * @param query      The user's query
     * @param maxResults Maximum number of results to return
     * @return List of relevant document fragments with their similarity scores
     */
    public static List<EmbeddingManager.SimilarityResult> findRelevantDocuments(String query, int maxResults)
            throws ApplicationException {
        try {
            System.out.println("Finding relevant documents for query: '" + query + "' with max results: " + maxResults);

            EmbeddingManager manager = (EmbeddingManager) ApplicationManager.get(EmbeddingManager.class.getName());
            if (manager == null) {
                System.err.println("Error: EmbeddingManager not found");
                throw new ApplicationException("EmbeddingManager not found");
            }

            // Generate embedding for the query
            System.out.println("Generating embedding for query");
            Vector<Double> queryEmbedding = manager.generateQueryEmbedding(query);
            if (queryEmbedding == null) {
                System.err.println("Error: Failed to generate query embedding");
                throw new ApplicationException("Failed to generate query embedding");
            }
            System.out.println("Generated query embedding with dimension: " + queryEmbedding.size());

            // Find similar documents - get more results than needed to ensure diversity
            System.out.println("Finding similar documents");
            int initialResultsCount = maxResults * 3; // Get 3x more results initially
            List<EmbeddingManager.SimilarityResult> allResults = EmbeddingManager.findSimilar(queryEmbedding, initialResultsCount);
            System.out.println("Found " + allResults.size() + " similar documents");

            // Filter by similarity threshold
            List<EmbeddingManager.SimilarityResult> filteredResults = allResults.stream()
                    .filter(result -> result.similarity >= SIMILARITY_THRESHOLD)
                    .collect(Collectors.toList());

            System.out.println("Filtered to " + filteredResults.size() + " documents with similarity >= " + SIMILARITY_THRESHOLD);
            return filteredResults;
        } catch (Exception e) {
            System.err.println("Error finding relevant documents: " + e.getMessage());
            e.printStackTrace();
            throw new ApplicationException("Failed to find relevant documents: " + e.getMessage(), e);
        }
    }

    /**
     * Find relevant document fragments for a given query (with default max results)
     *
     * @param query The user's query
     * @return List of relevant document fragments with their similarity scores
     */
    public static List<EmbeddingManager.SimilarityResult> findRelevantDocuments(String query)
            throws ApplicationException {
        return findRelevantDocuments(query, DEFAULT_MAX_RESULTS);
    }

    /**
     * Format document fragments as context for a language model
     * @param results List of document fragments with similarity scores
     * @return Formatted context string
     */
    public static String formatDocumentsAsContext(List<EmbeddingManager.SimilarityResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("I found the following information that might help answer your question:\n\n");

        int totalLength = context.length();
        int documentsAdded = 0;

        // Sort results by similarity score (highest first)
        results.sort((a, b) -> Double.compare(b.similarity, a.similarity));

        for (int i = 0; i < results.size(); i++) {
            EmbeddingManager.SimilarityResult result = results.get(i);
            DocumentFragment fragment = result.fragment;

            // Get document metadata
            String fileName = new java.io.File(fragment.getFilePath()).getName();
            String title = fragment.getTitle() != null ? fragment.getTitle() : fileName;
            String content = fragment.getContent();

            // No special cleaning needed as document fragments should already be clean text

            // Create document entry
            StringBuilder documentEntry = new StringBuilder();
            documentEntry.append("Document ").append(i + 1)
                   .append(" (Title: ").append(title)
                   .append(", File: ").append(fileName)
                   .append(", Relevance: ").append(String.format("%.2f", result.similarity))
                   .append("):\n");
            documentEntry.append(content).append("\n\n");

            // Check if adding this document would exceed the maximum context length
            if (totalLength + documentEntry.length() > MAX_CONTEXT_LENGTH && documentsAdded > 0) {
                // If we already have at least one document, stop adding more
                System.out.println("Reached maximum context length after " + documentsAdded + " documents");
                break;
            }

            // Add the document to the context
            context.append(documentEntry);
            totalLength += documentEntry.length();
            documentsAdded++;
        }

        // Add a note about how many documents were found vs. included
        if (documentsAdded < results.size()) {
            context.append("Note: Found " + results.size() + " relevant documents, but only included " +
                          documentsAdded + " to stay within context limits.\n\n");
        }

        return context.toString();
    }

    /**
     * Enhance a user query with relevant document fragments
     *
     * @param query The original user query
     * @return Enhanced query with document context, or the original query if no relevant documents found
     */
    public static String enhanceQueryWithDocuments(String query) throws ApplicationException {
        try {
            List<EmbeddingManager.SimilarityResult> relevantDocs = findRelevantDocuments(query);

            if (relevantDocs.isEmpty()) {
                return query;
            }

            String context = formatDocumentsAsContext(relevantDocs);
            return context + "\nBased on the information above, please answer the following question:\n" + query;
        } catch (Exception e) {
            System.err.println("Warning: Failed to enhance query with documents: " + e.getMessage());
            // Return original query if enhancement fails
            return query;
        }
    }

    /**
     * Get document fragments for a specific meeting code
     * This allows contextualizing document search by meeting
     *
     * @param query       The user query
     * @param meetingCode The meeting code to filter documents by
     * @param messages    The existing messages array
     * @return True if document context was added, false otherwise
     */
    public static boolean addDocumentContextToMessages(String query, String meetingCode,
                                                       Builders messages) throws ApplicationException {
        try {
            System.out.println("Finding relevant documents for query: " + query);
            List<EmbeddingManager.SimilarityResult> relevantDocs = findRelevantDocuments(query);

            if (relevantDocs.isEmpty()) {
                System.out.println("No relevant documents found for query");
                return false;
            }

            System.out.println("Found " + relevantDocs.size() + " relevant documents");

            // Print document details for debugging
            for (int i = 0; i < relevantDocs.size(); i++) {
                EmbeddingManager.SimilarityResult result = relevantDocs.get(i);
                DocumentFragment fragment = result.fragment;
                System.out.println("Document " + (i + 1) + ": " +
                        new java.io.File(fragment.getFilePath()).getName() +
                        " (similarity: " + result.similarity + ")");
            }

            // Filter documents by meeting code if needed in the future
            // For now, we're using all available document fragments

            String context = formatDocumentsAsContext(relevantDocs);
            System.out.println("Generated document context with " + context.length() + " characters");

            // Ensure context doesn't exceed token limits
            String formattedContext = context;
            int maxSafeCharLimit = SAFE_TOKEN_LIMIT * CHARS_PER_TOKEN;

            if (context.length() > maxSafeCharLimit) {
                System.out.println("Warning: Context exceeds safe token limit. Truncating from " +
                                  context.length() + " to " + maxSafeCharLimit + " characters");
                formattedContext = context.substring(0, maxSafeCharLimit);
            }

            // Add system message with document context
            Builder contextMessage = new Builder();
            contextMessage.put("role", "system");
            contextMessage.put("content", "I am providing you with some relevant document fragments to help answer the user's question. " +
                    "These documents are highly relevant to the current conversation context. " +
                    "Instructions for using this information:\n" +
                    "1. Prioritize this information over your general knowledge when answering\n" +
                    "2. Cite the specific document title/number when using information from it\n" +
                    "3. If multiple documents contain relevant information, synthesize it\n" +
                    "4. If the documents don't contain the answer, clearly state that and use your general knowledge\n" +
                    "5. Maintain continuity with the previous conversation\n\n" + formattedContext);
            messages.add(contextMessage);

            System.out.println("Added document context to messages");
            return true;
        } catch (Exception e) {
            System.err.println("Warning: Failed to add document context to messages: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Add document context to a chat system messages array if relevant
     *
     * @param query    The user query
     * @param messages The existing messages array
     * @return True if document context was added, false otherwise
     */
    public static boolean addDocumentContextToMessages(String query, Builders messages)
            throws ApplicationException {
        // Call the overloaded method with null meeting code
        return addDocumentContextToMessages(query, null, messages);
    }
}