package custom.util;

import custom.objects.UserPrompt;
import org.tinystruct.ApplicationException;

/**
 * Service for managing user-specific system prompts
 */
public class UserPromptService {

    /**
     * Get the system prompt for a specific user
     *
     * @param userId The user ID as a String
     * @return The user's custom system prompt, or null if not found
     * @throws ApplicationException If an error occurs
     */
    public static UserPrompt getUserPrompt(String userId) throws ApplicationException {
        try {
            UserPrompt prompt = new UserPrompt();
            // Use findWith to search by user_id
            if (prompt.findWith("WHERE user_id = ?", new Object[]{userId}).isEmpty()) {
                return null;
            }
            return prompt;
        } catch (Exception e) {
            throw new ApplicationException("Error retrieving user prompt: " + e.getMessage(), e);
        }
    }

    /**
     * Save or update a user's system prompt
     *
     * @param prompt The user prompt to save
     * @throws ApplicationException If an error occurs
     */
    public static void saveUserPrompt(UserPrompt prompt) throws ApplicationException {
        if (prompt == null || prompt.getUserId() != null || prompt.getSystemPrompt() == null) {
            throw new ApplicationException("Invalid user prompt data");
        }

        // Check if the user already has a prompt
        UserPrompt existingPrompt = getUserPrompt(prompt.getUserId());

        if (existingPrompt != null) {
            // Update existing prompt
            prompt.setId(existingPrompt.getId());
            if (!prompt.update()) {
                throw new ApplicationException("Failed to update user prompt");
            }
        } else {
            // Insert new prompt
            if (!prompt.append()) {
                throw new ApplicationException("Failed to insert user prompt");
            }
        }
    }

    /**
     * Delete a user's system prompt
     *
     * @param userId The user ID
     * @throws ApplicationException If an error occurs
     */
    public static void deleteUserPrompt(String userId) throws ApplicationException {
        UserPrompt prompt = getUserPrompt(userId);
        if (prompt != null) {
            if (!prompt.delete()) {
                throw new ApplicationException("Failed to delete user prompt");
            }
        }
    }
}
