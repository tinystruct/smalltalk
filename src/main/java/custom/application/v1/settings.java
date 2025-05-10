package custom.application.v1;

import custom.objects.User;
import custom.objects.UserPrompt;
import custom.util.AuthenticationService;
import custom.util.UserPromptService;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.http.ResponseStatus;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.annotation.Action;

/**
 * Controller for user settings
 */
public class settings extends AbstractApplication {

    @Override
    public void init() {
        // Actions are registered using annotations
    }

    /**
     * Display the settings page
     */
    @Action("settings")
    public Object index(Request request, Response response) {
        try {
            if(request.getSession().getAttribute("user_id") == null){
                response.setStatus(ResponseStatus.UNAUTHORIZED);
                return "{ \"error\": \"authentication_required\" }";
            }
            String userId = request.getSession().getAttribute("user_id").toString();
            // Check if user is authenticated
            User user = AuthenticationService.getCurrentUser(userId);
            if (user == null) {
                response.setStatus(ResponseStatus.UNAUTHORIZED);
                return "{ \"error\": \"authentication_required\" }";
            }

            // Set variables for the template
            this.setVariable("username", user.getUsername());
            this.setVariable("show_login", "false");
            this.setVariable("default", "settings.view");

            // Get the user's custom prompt if it exists
            UserPrompt userPrompt = UserPromptService.getUserPrompt(user.getId());
            if (userPrompt != null) {
                this.setVariable("custom_prompt", userPrompt.getSystemPrompt());
            } else {
                // Use the default system prompt
                smalltalk smalltalkApp = (smalltalk) ApplicationManager.get(smalltalk.class.getName());
                this.setVariable("custom_prompt", smalltalkApp.getSystemPrompt());
            }

            return this;
        } catch (ApplicationException e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"" + e.getMessage() + "\" }";
        }
    }

    /**
     * Update the user's custom prompt
     */
    @Action("settings/prompt")
    public String updatePrompt(Request request, Response response) {
        try {
            if(request.getSession().getAttribute("user_id") == null){
                response.setStatus(ResponseStatus.UNAUTHORIZED);
                return "{ \"error\": \"authentication_required\" }";
            }
            String userId = request.getSession().getAttribute("user_id").toString();
            // Check if user is authenticated
            User user = AuthenticationService.getCurrentUser(userId);
            if (user == null) {
                response.setStatus(ResponseStatus.UNAUTHORIZED);
                return "{ \"error\": \"authentication_required\" }";
            }

            // Get the new prompt from the request
            String systemPrompt = request.getParameter("system_prompt");
            if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return "{ \"error\": \"missing_prompt\" }";
            }

            // Save the user's custom prompt
            UserPrompt userPrompt = new UserPrompt();
            userPrompt.setUserId(user.getId());
            userPrompt.setSystemPrompt(systemPrompt);
            UserPromptService.saveUserPrompt(userPrompt);

            // Return success response
            Builder builder = new Builder();
            builder.put("success", true);
            builder.put("message", "System prompt updated successfully");

            return builder.toString();
        } catch (ApplicationException e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"" + e.getMessage() + "\" }";
        }
    }

    /**
     * Get the user's custom prompt
     */
    @Action("settings/prompt/get")
    public String getPrompt(Request request, Response response) {
        try {
            if(request.getSession().getAttribute("user_id") == null){
                response.setStatus(ResponseStatus.UNAUTHORIZED);
                return "{ \"error\": \"authentication_required\" }";
            }

            String userId = request.getSession().getAttribute("user_id").toString();
            // Check if user is authenticated
            User user = AuthenticationService.getCurrentUser(userId);
            if (user == null) {
                response.setStatus(ResponseStatus.UNAUTHORIZED);
                return "{ \"error\": \"authentication_required\" }";
            }

            // Get the user's custom prompt if it exists
            UserPrompt userPrompt = UserPromptService.getUserPrompt(user.getId());

            // Build response
            Builder builder = new Builder();

            if (userPrompt != null) {
                builder.put("custom_prompt", userPrompt.getSystemPrompt());
                builder.put("is_default", false);
            } else {
                // Use the default system prompt
                smalltalk smalltalkApp = (smalltalk) ApplicationManager.get("smalltalk");
                builder.put("custom_prompt", smalltalkApp.getSystemPrompt());
                builder.put("is_default", true);
            }

            return builder.toString();
        } catch (ApplicationException e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"" + e.getMessage() + "\" }";
        }
    }

    @Override
    public String version() {
        return "1.0";
    }
}
