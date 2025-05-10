package custom.util;

import java.util.*;

/**
 * ConversationHistoryManager - A dedicated class to manage conversation history
 * Uses an in-memory storage indexed by sessionId
 */
public class ConversationHistoryManager {
    // In-memory storage for conversation history
    private static final Map<String, List<Map<String, String>>> conversationHistoryStore = new HashMap<>();
    
    // Maximum number of conversation pairs to store per session
    private static final int MAX_HISTORY_SIZE = 5;
    
    /**
     * Add a new message pair to the conversation history
     * 
     * @param sessionId The session ID
     * @param userMessage The user message
     * @param assistantMessage The assistant message
     */
    public static void addMessagePair(String sessionId, String userMessage, String assistantMessage) {
        if (sessionId == null || sessionId.isEmpty()) {
            System.err.println("Cannot add message pair: sessionId is null or empty");
            return;
        }
        
        // Create a new message pair
        Map<String, String> messagePair = new HashMap<>();
        messagePair.put("user", userMessage);
        messagePair.put("assistant", assistantMessage);
        messagePair.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        // Get or create the conversation history for this session
        List<Map<String, String>> history = conversationHistoryStore.get(sessionId);
        if (history == null) {
            history = new ArrayList<>();
            conversationHistoryStore.put(sessionId, history);
        }
        
        // Add the new message pair at the beginning
        history.add(0, messagePair);
        
        // Limit the history size
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(history.size() - 1);
        }
        
        System.out.println("Added message pair to conversation history for session " + sessionId + 
                          " (total: " + history.size() + ")");
    }
    
    /**
     * Get the conversation history for a session
     * 
     * @param sessionId The session ID
     * @return The conversation history, or null if none exists
     */
    public static List<Map<String, String>> getConversationHistory(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            System.err.println("Cannot get conversation history: sessionId is null or empty");
            return null;
        }
        
        return conversationHistoryStore.get(sessionId);
    }
    
    /**
     * Clear the conversation history for a session
     * 
     * @param sessionId The session ID
     */
    public static void clearConversationHistory(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            System.err.println("Cannot clear conversation history: sessionId is null or empty");
            return;
        }
        
        conversationHistoryStore.remove(sessionId);
        System.out.println("Cleared conversation history for session " + sessionId);
    }
    
    /**
     * Get the number of sessions with conversation history
     * 
     * @return The number of sessions
     */
    public static int getSessionCount() {
        return conversationHistoryStore.size();
    }
}
