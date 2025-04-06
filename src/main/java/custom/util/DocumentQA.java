package custom.util;

import custom.objects.DocumentFragment;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.system.ApplicationManager;

import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Utility class for document-based question answering.
 * Uses semantic search to find relevant document fragments for user queries.
 */
public class DocumentQA {
    
    private static final int DEFAULT_MAX_RESULTS = 3;
    private static final double SIMILARITY_THRESHOLD = 0.7;
    
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
            EmbeddingManager manager = (EmbeddingManager) ApplicationManager.get(EmbeddingManager.class.getName());
                    // Generate embedding for the query
            Vector<Double> queryEmbedding = manager.generateQueryEmbedding(query);
            
            // Find similar documents
            List<EmbeddingManager.SimilarityResult> results = EmbeddingManager.findSimilar(queryEmbedding, maxResults);
            
            // Filter by similarity threshold
            return results.stream()
                    .filter(result -> result.similarity >= SIMILARITY_THRESHOLD)
                    .collect(Collectors.toList());
        } catch (Exception e) {
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
        
        for (int i = 0; i < results.size(); i++) {
            EmbeddingManager.SimilarityResult result = results.get(i);
            DocumentFragment fragment = result.fragment;
            
            context.append("Document ").append(i + 1).append(" (")
                   .append(new java.io.File(fragment.getFilePath()).getName())
                   .append("):\n");
            context.append(fragment.getContent()).append("\n\n");
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
            List<EmbeddingManager.SimilarityResult> relevantDocs = findRelevantDocuments(query);
            
            if (relevantDocs.isEmpty()) {
                return false;
            }
            
            // Filter documents by meeting code if needed in the future
            // For now, we're using all available document fragments
            
            String context = formatDocumentsAsContext(relevantDocs);
            
            // Add system message with document context
            Builder contextMessage = new Builder();
            contextMessage.put("role", "system");
            contextMessage.put("content", "I am providing you with some relevant document fragments to help answer the user's question. " +
                    "Use this information to provide an accurate and helpful response. " +
                    "If the documents contain the answer, cite the specific document in your response.\n\n" + context);
            messages.add(contextMessage);
            
            return true;
        } catch (Exception e) {
            System.err.println("Warning: Failed to add document context to messages: " + e.getMessage());
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