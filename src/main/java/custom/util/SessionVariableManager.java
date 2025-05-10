package custom.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SessionVariableManager - A dedicated class to manage session-specific variables
 * Uses an in-memory storage indexed by sessionId
 */
public class SessionVariableManager {
    // In-memory storage for session variables
    private static final Map<String, Map<String, Object>> sessionVariables = new ConcurrentHashMap<>();
    
    // Maximum number of conversation pairs to store per session
    private static final int MAX_HISTORY_SIZE = 5;
    
    /**
     * Set a session variable
     * 
     * @param sessionId The session ID
     * @param name The variable name
     * @param value The variable value
     */
    public static void setVariable(String sessionId, String name, Object value) {
        if (sessionId == null || sessionId.isEmpty()) {
            System.err.println("Cannot set variable: sessionId is null or empty");
            return;
        }
        
        // Get or create the session variables map
        Map<String, Object> variables = sessionVariables.computeIfAbsent(sessionId, k -> new HashMap<>());
        
        // Set the variable
        variables.put(name, value);
    }
    
    /**
     * Get a session variable
     * 
     * @param sessionId The session ID
     * @param name The variable name
     * @return The variable value, or null if not found
     */
    public static Object getVariable(String sessionId, String name) {
        if (sessionId == null || sessionId.isEmpty()) {
            System.err.println("Cannot get variable: sessionId is null or empty");
            return null;
        }
        
        // Get the session variables map
        Map<String, Object> variables = sessionVariables.get(sessionId);
        if (variables == null) {
            return null;
        }
        
        // Get the variable
        return variables.get(name);
    }
    
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
        
        try {
            // Shift existing conversation history
            for (int i = MAX_HISTORY_SIZE - 1; i > 0; i--) {
                String prevUserVarName = "previous_user_message_" + i;
                String prevAssistantVarName = "previous_assistant_message_" + i;
                String userVarName = "previous_user_message_" + (i + 1);
                String assistantVarName = "previous_assistant_message_" + (i + 1);
                
                Object prevUserMessage = getVariable(sessionId, prevUserVarName);
                Object prevAssistantMessage = getVariable(sessionId, prevAssistantVarName);
                
                if (prevUserMessage != null) {
                    setVariable(sessionId, userVarName, prevUserMessage);
                    
                    if (prevAssistantMessage != null) {
                        setVariable(sessionId, assistantVarName, prevAssistantMessage);
                    }
                }
            }
            
            // Add current message pair to history
            setVariable(sessionId, "previous_user_message_1", userMessage);
            setVariable(sessionId, "previous_assistant_message_1", assistantMessage);
            
            // For backward compatibility
            setVariable(sessionId, "previous_user_message", userMessage);
            setVariable(sessionId, "previous_system_message", assistantMessage);
            
            System.out.println("Stored conversation history in session variables for session " + sessionId);
        } catch (Exception e) {
            System.err.println("Error storing conversation in session variables: " + e.getMessage());
        }
    }
    
    /**
     * Get a specific message pair from the conversation history
     * 
     * @param sessionId The session ID
     * @param index The index of the message pair (1-based)
     * @return A string array with [userMessage, assistantMessage] or null if not found
     */
    public static String[] getMessagePair(String sessionId, int index) {
        if (sessionId == null || sessionId.isEmpty() || index < 1 || index > MAX_HISTORY_SIZE) {
            return null;
        }
        
        String userVarName = "previous_user_message_" + index;
        String assistantVarName = "previous_assistant_message_" + index;
        
        Object userMessage = getVariable(sessionId, userVarName);
        Object assistantMessage = getVariable(sessionId, assistantVarName);
        
        if (userMessage != null && assistantMessage != null) {
            return new String[] { userMessage.toString(), assistantMessage.toString() };
        }
        
        return null;
    }
    
    /**
     * Get all message pairs from the conversation history
     * 
     * @param sessionId The session ID
     * @return A list of message pairs, each containing [userMessage, assistantMessage]
     */
    public static List<String[]> getAllMessagePairs(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String[]> messagePairs = new ArrayList<>();
        
        for (int i = 1; i <= MAX_HISTORY_SIZE; i++) {
            String[] pair = getMessagePair(sessionId, i);
            if (pair != null) {
                messagePairs.add(pair);
            }
        }
        
        return messagePairs;
    }
    
    /**
     * Clear all variables for a session
     * 
     * @param sessionId The session ID
     */
    public static void clearSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return;
        }
        
        sessionVariables.remove(sessionId);
        System.out.println("Cleared session variables for session " + sessionId);
    }
    
    /**
     * Get the number of sessions with variables
     * 
     * @return The number of sessions
     */
    public static int getSessionCount() {
        return sessionVariables.size();
    }
}
