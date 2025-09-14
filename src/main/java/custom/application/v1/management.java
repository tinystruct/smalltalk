package custom.application.v1;

import custom.objects.User;
import custom.objects.DocumentFragment;
import custom.util.AuthenticationService;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.Table;
import org.tinystruct.data.component.Builder;
import org.tinystruct.AbstractApplication;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.http.ResponseStatus;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Management application for administrative functions
 */
public class management extends AbstractApplication {
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private AuthenticationService authService;

    @Override
    public void init() {
        this.setTemplateRequired(true);
        this.authService = AuthenticationService.getInstance();
    }

    @Override
    public String version() {
        return "1.0";
    }

    /**
     * Management dashboard page
     */
    @Action("management")
    public Object managementPage(Request request, Response response) throws ApplicationException {
        this.authService.setSession(request.getSession());

        if (!authService.isLoggedIn()) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"User is not logged in\" }";
        }

        if (!authService.isAdmin()) {
            response.setStatus(ResponseStatus.FORBIDDEN);
            return "{ \"error\": \"forbidden\", \"message\": \"Admin access required\" }";
        }

        this.setVariable("template", "management.view");
        return this;
    }

    /**
     * Get all users (admin only)
     */
    @Action("getAllUsers")
    public Object getAllUsers(Request request, Response response) throws ApplicationException {
        this.authService.setSession(request.getSession());

        if (!authService.isLoggedIn() || !authService.isAdmin()) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"Unauthorized access\" }";
        }

        User user = new User();
        Table results = user.findWith("WHERE 1=1", new Object[]{});
        List<Builder> users = new ArrayList<>();

        for (Row row : results) {
            Builder userData = new Builder();
            if (row.getFieldInfo("id") != null) userData.put("id", row.getFieldInfo("id").stringValue());
            if (row.getFieldInfo("username") != null)
                userData.put("username", row.getFieldInfo("username").stringValue());
            if (row.getFieldInfo("email") != null) userData.put("email", row.getFieldInfo("email").stringValue());
            if (row.getFieldInfo("full_name") != null)
                userData.put("fullName", row.getFieldInfo("full_name").stringValue());
            if (row.getFieldInfo("is_active") != null)
                userData.put("isActive", row.getFieldInfo("is_active").booleanValue());
            if (row.getFieldInfo("is_admin") != null)
                userData.put("isAdmin", row.getFieldInfo("is_admin").booleanValue());
            if (row.getFieldInfo("created_at") != null)
                userData.put("createdAt", row.getFieldInfo("created_at").dateValue());
            if (row.getFieldInfo("last_login") != null)
                userData.put("lastLogin", row.getFieldInfo("last_login").dateValue());
            users.add(userData);
        }

        return "{ \"users\": " + users.toString() + " }";
    }

    /**
     * Disable a user (admin only)
     */
    @Action("disableUser")
    public Object disableUser(Request request, Response response) throws ApplicationException {
        this.authService.setSession(request.getSession());

        if (!authService.isLoggedIn() || !authService.isAdmin()) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"Unauthorized access\" }";
        }

        String userId = request.getParameter("userId");
        if (userId == null || userId.isEmpty()) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"bad_request\", \"message\": \"User ID is required\" }";
        }

        User user = new User();
        user.setId(userId);
        user.setIsActive(false);
        user.update();

        return "{ \"success\": true, \"message\": \"User disabled successfully\" }";
    }

    /**
     * Enable a user (admin only)
     */
    @Action("enableUser")
    public Object enableUser(Request request, Response response) throws ApplicationException {
        this.authService.setSession(request.getSession());

        if (!authService.isLoggedIn() || !authService.isAdmin()) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"Unauthorized access\" }";
        }

        String userId = request.getParameter("userId");
        if (userId == null || userId.isEmpty()) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"bad_request\", \"message\": \"User ID is required\" }";
        }

        User user = new User();
        user.setId(userId);
        user.setIsActive(true);
        user.update();

        return "{ \"success\": true, \"message\": \"User enabled successfully\" }";
    }

    /**
     * Delete a user (admin only)
     */
    @Action("management/delete-user")
    public Object deleteUser(Request request, Response response) throws ApplicationException {
        this.authService.setSession(request.getSession());

        if (!authService.isLoggedIn() || !authService.isAdmin()) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"Unauthorized access\" }";
        }

        String userId = request.getParameter("userId");
        if (userId == null || userId.isEmpty()) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"bad_request\", \"message\": \"User ID is required\" }";
        }

        User user = new User();
        user.setId(userId);
        user.delete();

        return "{ \"success\": true, \"message\": \"User deleted successfully\" }";
    }

    /**
     * Get all documents (admin only)
     */
    @Action("management/documents")
    public String getAllDocuments(Request request, Response response) throws ApplicationException {
        // Check if user is logged in and is admin
        String userId = (String) request.getSession().getAttribute("user_id");
        User currentUser = AuthenticationService.getCurrentUser(userId);

        if (currentUser == null || !currentUser.getIsAdmin()) {
            response.setStatus(ResponseStatus.FORBIDDEN);
            return "{ \"error\": \"Access denied\" }";
        }

        DocumentFragment fragment = new DocumentFragment();
        Table documents = fragment.findAll();
        List<Map<String, Object>> docs = new ArrayList<>();

        for (Row row : documents) {
            Map<String, Object> docData = new HashMap<>();
            if (row.getFieldInfo("id") != null) docData.put("id", row.getFieldInfo("id").stringValue());
            if (row.getFieldInfo("document_id") != null)
                docData.put("documentId", row.getFieldInfo("document_id").stringValue());
            if (row.getFieldInfo("title") != null) docData.put("title", row.getFieldInfo("title").stringValue());
            if (row.getFieldInfo("description") != null)
                docData.put("description", row.getFieldInfo("description").stringValue());
            if (row.getFieldInfo("file_path") != null)
                docData.put("filePath", row.getFieldInfo("file_path").stringValue());
            if (row.getFieldInfo("mime_type") != null)
                docData.put("mimeType", row.getFieldInfo("mime_type").stringValue());
            if (row.getFieldInfo("created_at") != null)
                docData.put("createdAt", format.format(row.getFieldInfo("created_at").dateValue()));
            if (row.getFieldInfo("is_public") != null)
                docData.put("isPublic", row.getFieldInfo("is_public").booleanValue());
            if (row.getFieldInfo("user_id") != null) docData.put("userId", row.getFieldInfo("user_id").stringValue());
            docs.add(docData);
        }

        return "{ \"documents\": " + docs.toString() + " }";
    }

    /**
     * Delete a document (admin only)
     */
    @Action("management/documents/delete")
    public String deleteDocument(Request request, Response response) throws ApplicationException {
        // Check if user is logged in and is admin
        String userId = (String) request.getSession().getAttribute("user_id");
        User currentUser = AuthenticationService.getCurrentUser(userId);

        if (currentUser == null || !currentUser.getIsAdmin()) {
            response.setStatus(ResponseStatus.FORBIDDEN);
            return "{ \"error\": \"Access denied\" }";
        }

        String documentId = request.getParameter("documentId");
        if (documentId == null || documentId.isEmpty()) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"Document ID is required\" }";
        }

        DocumentFragment document = new DocumentFragment();
        if (document.findOneByKey("document_id", documentId) == null) {
            response.setStatus(ResponseStatus.NOT_FOUND);
            return "{ \"error\": \"Document not found\" }";
        }

        document.delete();

        return "{ \"success\": true, \"message\": \"Document deleted successfully\" }";
    }

    /**
     * Get active users
     */
    @Action("management/active-users")
    public Object getActiveUsers(Request request, Response response) throws ApplicationException {
        this.authService.setSession(request.getSession());

        if (!authService.isLoggedIn() || !authService.isAdmin()) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"Unauthorized access\" }";
        }

        User user = new User();
        Table results = user.findWith("WHERE is_active = ?", new Object[]{true});
        List<Builder> users = new ArrayList<>();

        for (Row row : results) {
            Builder userData = new Builder();
            if (row.getFieldInfo("id") != null) userData.put("id", row.getFieldInfo("id").stringValue());
            if (row.getFieldInfo("username") != null)
                userData.put("username", row.getFieldInfo("username").stringValue());
            if (row.getFieldInfo("email") != null) userData.put("email", row.getFieldInfo("email").stringValue());
            if (row.getFieldInfo("full_name") != null)
                userData.put("fullName", row.getFieldInfo("full_name").stringValue());
            if (row.getFieldInfo("is_active") != null)
                userData.put("isActive", row.getFieldInfo("is_active").booleanValue());
            if (row.getFieldInfo("is_admin") != null)
                userData.put("isAdmin", row.getFieldInfo("is_admin").booleanValue());
            if (row.getFieldInfo("created_at") != null)
                userData.put("createdAt", row.getFieldInfo("created_at").dateValue());
            if (row.getFieldInfo("last_login") != null)
                userData.put("lastLogin", row.getFieldInfo("last_login").dateValue());
            users.add(userData);
        }

        return "{ \"users\": " + users.toString() + " }";
    }

    /**
     * Get inactive users
     */
    @Action("management/inactive-users")
    public Object getInactiveUsers(Request request, Response response) throws ApplicationException {
        this.authService.setSession(request.getSession());

        if (!authService.isLoggedIn() || !authService.isAdmin()) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"Unauthorized access\" }";
        }

        User user = new User();
        Table results = user.findWith("WHERE is_active = ?", new Object[]{false});
        List<Builder> users = new ArrayList<>();

        for (Row row : results) {
            Builder userData = new Builder();
            if (row.getFieldInfo("id") != null) userData.put("id", row.getFieldInfo("id").stringValue());
            if (row.getFieldInfo("username") != null)
                userData.put("username", row.getFieldInfo("username").stringValue());
            if (row.getFieldInfo("email") != null) userData.put("email", row.getFieldInfo("email").stringValue());
            if (row.getFieldInfo("full_name") != null)
                userData.put("fullName", row.getFieldInfo("full_name").stringValue());
            if (row.getFieldInfo("is_active") != null)
                userData.put("isActive", row.getFieldInfo("is_active").booleanValue());
            if (row.getFieldInfo("is_admin") != null)
                userData.put("isAdmin", row.getFieldInfo("is_admin").booleanValue());
            if (row.getFieldInfo("created_at") != null)
                userData.put("createdAt", row.getFieldInfo("created_at").dateValue());
            if (row.getFieldInfo("last_login") != null)
                userData.put("lastLogin", row.getFieldInfo("last_login").dateValue());
            users.add(userData);
        }

        return "{ \"users\": " + users.toString() + " }";
    }

    /**
     * Get pending users
     */
    @Action("management/pending-users")
    public String getPendingUsers(Request request, Response response) throws ApplicationException {
        // Check if user is logged in and is admin
        String userId = (String) request.getSession().getAttribute("user_id");
        User currentUser = AuthenticationService.getCurrentUser(userId);

        if (currentUser == null || !currentUser.getIsAdmin()) {
            response.setStatus(ResponseStatus.FORBIDDEN);
            return "{ \"error\": \"Access denied\" }";
        }

        User user = new User();
        Table results = user.findWith("WHERE is_active = ?", new Object[]{true});
        List<Map<String, Object>> users = new ArrayList<>();

        for (Row row : results) {
            Map<String, Object> userData = new HashMap<>();
            if (row.getFieldInfo("id") != null) userData.put("id", row.getFieldInfo("id").stringValue());
            if (row.getFieldInfo("username") != null)
                userData.put("username", row.getFieldInfo("username").stringValue());
            if (row.getFieldInfo("email") != null) userData.put("email", row.getFieldInfo("email").stringValue());
            if (row.getFieldInfo("full_name") != null)
                userData.put("fullName", row.getFieldInfo("full_name").stringValue());
            if (row.getFieldInfo("is_admin") != null)
                userData.put("isAdmin", row.getFieldInfo("is_admin").booleanValue());
            if (row.getFieldInfo("created_at") != null)
                userData.put("createdAt", format.format(row.getFieldInfo("created_at").dateValue()));
            users.add(userData);
        }

        return "{ \"users\": " + users.toString() + " }";
    }

    /**
     * Add new user
     */
    @Action("addUser")
    public Object addUser(Request request, Response response) throws ApplicationException {
        this.authService.setSession(request.getSession());

        if (!authService.isLoggedIn() || !authService.isAdmin()) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"Unauthorized access\" }";
        }

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String email = request.getParameter("email");
        String fullName = request.getParameter("fullName");
        boolean isAdmin = Boolean.parseBoolean(request.getParameter("isAdmin"));

        if (username == null || password == null || email == null || fullName == null) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"bad_request\", \"message\": \"All fields are required\" }";
        }

        // Use AuthenticationService to register the user (handles hashing and saving)
        User user = authService.registerUser(username, password, email, fullName);
        user.setIsAdmin(isAdmin);
        user.setIsActive(true);
        user.update();

        return "{ \"success\": true, \"message\": \"User added successfully\" }";
    }

    /**
     * Reset a user's password (admin only)
     */
    @Action("admin/reset-user-password")
    public Object resetUserPassword(Request request, Response response) throws ApplicationException {
        this.authService.setSession(request.getSession());

        if (!authService.isLoggedIn() || !authService.isAdmin()) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"Unauthorized access\" }";
        }

        String userId = request.getParameter("userId");
        String newPassword = request.getParameter("password");

        if (userId == null || userId.trim().isEmpty()) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"missing_user_id\", \"message\": \"User ID is required\" }";
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"missing_password\", \"message\": \"New password is required\" }";
        }

        try {
            // Find user by ID
            User user = authService.findUserById(userId);
            if (user == null) {
                response.setStatus(ResponseStatus.NOT_FOUND);
                return "{ \"error\": \"user_not_found\", \"message\": \"User not found\" }";
            }

            // Update password
            user.setPasswordHash(authService.hashPassword(newPassword));
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            user.update();

            return "{ \"success\": true, \"message\": \"User password has been reset successfully\" }";
        } catch (Exception e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"An error occurred while resetting the password.\" }";
        }
    }

    /**
     * Update user status
     */
    @Action("management/update-status")
    public Object updateUserStatus(Request request, Response response) throws ApplicationException {
        this.authService.setSession(request.getSession());

        if (!authService.isLoggedIn() || !authService.isAdmin()) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"Unauthorized access\" }";
        }


        String userId = request.getParameter("userId");
        if (userId == null || userId.isEmpty()) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"bad_request\", \"message\": \"User ID is required\" }";
        }

        if (request.getParameter("status") == null) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"bad_request\", \"message\": \"Status is required\" }";
        }

        boolean isActive = request.getParameter("status").equalsIgnoreCase("active");

        User user = new User();
        user.setId(userId);
        user.setIsActive(isActive);
        user.update();

        return "{ \"success\": true, \"message\": \"User status updated successfully\" }";
    }

    /**
     * Update user role
     */
    @Action("management/update-role")
    public Object updateUserRole(Request request, Response response) throws ApplicationException {
        this.authService.setSession(request.getSession());

        if (!authService.isLoggedIn() || !authService.isAdmin()) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"Unauthorized access\" }";
        }

        String userId = request.getParameter("userId");
        boolean isAdmin = Boolean.parseBoolean(request.getParameter("isAdmin"));

        if (userId == null || userId.isEmpty()) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"bad_request\", \"message\": \"User ID is required\" }";
        }

        User user = new User();
        user.setId(userId);
        user.setIsAdmin(isAdmin);
        user.update();

        return "{ \"success\": true, \"message\": \"User role updated successfully\" }";
    }
} 