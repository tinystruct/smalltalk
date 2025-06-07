package custom.application.v1;

import custom.objects.User;
import custom.util.AuthenticationService;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.handler.Reforward;
import org.tinystruct.http.*;
import org.tinystruct.mail.SimpleMail;
import org.tinystruct.system.annotation.Action;

import java.text.SimpleDateFormat;
import java.util.Date;

public class authentication extends AbstractApplication {
    private static final String DATE_FORMAT_PATTERN = "yyyy-M-d h:m:s";
    private static final SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT_PATTERN);

    @Action("auth/register")
    public String register(Request request, Response response) throws ApplicationException {
        try {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            String email = request.getParameter("email");
            String fullName = request.getParameter("fullName");

            // Validate required fields
            if (username == null || username.trim().isEmpty()) {
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return "{ \"error\": \"missing_username\", \"message\": \"Username is required\" }";
            }

            if (password == null || password.trim().isEmpty()) {
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return "{ \"error\": \"missing_password\", \"message\": \"Password is required\" }";
            }

            if (email == null || email.trim().isEmpty()) {
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return "{ \"error\": \"missing_email\", \"message\": \"Email is required\" }";
            }

            // Validate email format
            if (!isValidEmail(email)) {
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return "{ \"error\": \"invalid_email\", \"message\": \"Invalid email format\" }";
            }

            // Check if username already exists
            if (userExists(username)) {
                response.setStatus(ResponseStatus.CONFLICT);
                return "{ \"error\": \"username_exists\", \"message\": \"Username already exists\" }";
            }

            // Check if email already exists
            if (emailExists(email)) {
                response.setStatus(ResponseStatus.CONFLICT);
                return "{ \"error\": \"email_exists\", \"message\": \"Email already registered\" }";
            }

            // Create user
            User user = createUser(username, password, email, fullName);

            // Set session attributes
            request.getSession().setAttribute("user_id", user.getId());
            request.getSession().setAttribute("username", user.getUsername());

            return "{ \"success\": true, \"username\": \"" + username + "\" }";
        } catch (Exception e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"registration_failed\", \"message\": \"Registration failed: " + e.getMessage() + "\" }";
        }
    }

    private boolean isValidEmail(String email) {
        // Basic email validation regex
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email != null && email.matches(emailRegex);
    }

    private boolean userExists(String username) {
        try {
            User user = AuthenticationService.getInstance().findUserByUsername(username);
            return user != null;
        } catch (ApplicationException e) {
            System.err.println("Error checking username existence: " + e.getMessage());
            return false;
        }
    }

    private boolean emailExists(String email) {
        try {
            User user = AuthenticationService.getInstance().findUserByEmail(email);
            return user != null;
        } catch (ApplicationException e) {
            System.err.println("Error checking email existence: " + e.getMessage());
            return false;
        }
    }

    private User createUser(String username, String password, String email, String fullName) throws ApplicationException {
        return AuthenticationService.getInstance().registerUser(username, password, email, fullName);
    }

    @Action("auth/login")
    public String login(Request request, Response response) throws ApplicationException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // Validate required fields
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"missing_required_fields\", \"message\": \"Username and password are required\" }";
        }

        try {
            // Authenticate the user
            AuthenticationService authService = AuthenticationService.getInstance();
            User user = authService.authenticateUser(username, password);

            // Set user in session
            request.getSession().setAttribute("user_id", user.getId());
            request.getSession().setAttribute("username", user.getUsername());

            // Return success response
            Builder builder = new Builder();
            builder.put("id", user.getId());
            builder.put("username", user.getUsername());
            builder.put("email", user.getEmail() != null ? user.getEmail() : "");
            builder.put("fullName", user.getFullName() != null ? user.getFullName() : "");
            builder.put("lastLogin", user.getLastLogin() != null ? format.format(user.getLastLogin()) : "");
            builder.put("isActive", user.getIsActive());

            return builder.toString();
        } catch (ApplicationException e) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"authentication_failed\", \"message\": \"" + e.getMessage() + "\" }";
        } catch (Exception e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"" + e.getMessage() + "\" }";
        }
    }

    /**
     * Login page
     */
    @Action("login")
    public Object loginPage(Request request, Response response) {
        // If user is already logged in, check for pending URL or meeting code
        Object userId = request.getSession().getAttribute("user_id");
        if (userId != null) {
            Object pendingUrl = request.getSession().getAttribute("pending_url");
            if (pendingUrl != null) {
                try {
                    // Clear the pending URL
                    request.getSession().removeAttribute("pending_url");
                    // Redirect to the pending URL
                    Reforward reforward = new Reforward(request, response);
                    reforward.setDefault(pendingUrl.toString());
                    return reforward.forward();
                } catch (Exception e) {
                    // Continue to default redirect if pending URL redirect fails
                }
            }
            
            Object pendingMeetingCode = request.getSession().getAttribute("pending_meeting_code");
            if (pendingMeetingCode != null) {
                try {
                    // Clear the pending meeting code
                    request.getSession().removeAttribute("pending_meeting_code");
                    // Set the actual meeting code
                    request.getSession().setAttribute("meeting_code", pendingMeetingCode);
                    // Redirect to talk page
                    Reforward reforward = new Reforward(request, response);
                    reforward.setDefault("/?q=talk");
                    this.setVariable("show_login", "false");
                    return (smalltalk) reforward.forward();
                } catch (Exception e) {
                    // Continue to login page if redirect fails
                }
            } else {
                // No pending meeting, redirect to talk
                try {
                    Reforward reforward = new Reforward(request, response);
                    reforward.setDefault("/?q=talk");
                    this.setVariable("show_login", "false");
                    return (smalltalk) reforward.forward();
                } catch (Exception e) {
                    // Continue to login page if redirect fails
                }
            }
        }

        // Set variable to show login form
        this.setVariable("show_login", "true");
        return this;
    }

    @Action("auth/logout")
    public String logout(Request request, Response response) {
        // Clear user session
        request.getSession().removeAttribute("user_id");
        request.getSession().removeAttribute("username");

        // Return success response
        return "{ \"status\": \"ok\" }";
    }

    @Action("auth/profile")
    public String getProfile(Request request, Response response) throws ApplicationException {
        // Check if user is logged in
        Object userId = request.getSession().getAttribute("user_id");
        if (userId == null) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"User is not logged in\" }";
        }

        try {
            // Get user profile
            AuthenticationService authService = AuthenticationService.getInstance();
            User user = authService.findUserById(userId.toString());

            if (user == null) {
                response.setStatus(ResponseStatus.NOT_FOUND);
                return "{ \"error\": \"user_not_found\", \"message\": \"User not found\" }";
            }

            // Return user profile
            Builder builder = new Builder();
            builder.put("id", user.getId());
            builder.put("username", user.getUsername());
            builder.put("email", user.getEmail() != null ? user.getEmail() : "");
            builder.put("fullName", user.getFullName() != null ? user.getFullName() : "");
            builder.put("createdAt", format.format(user.getCreatedAt()));
            builder.put("lastLogin", user.getLastLogin() != null ? format.format(user.getLastLogin()) : "");
            builder.put("isAdmin", user.getIsAdmin());

            return builder.toString();
        } catch (Exception e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"" + e.getMessage() + "\" }";
        }
    }

    @Action("auth/forgot-password")
    public String forgotPassword(Request request, Response response) throws ApplicationException {
        String email = request.getParameter("email");

        // Validate email
        if (email == null || email.trim().isEmpty()) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"missing_email\", \"message\": \"Email is required\" }";
        }

        try {
            // Find user by email
            User user = AuthenticationService.getInstance().findUserByEmail(email);
            
            // Always return success even if user not found (security best practice)
            if (user != null) {
                // Generate reset token
                String resetToken = generateResetToken();
                Date expiryDate = new Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000)); // 24 hours from now
                
                // Save reset token to user
                user.setResetToken(resetToken);
                user.setResetTokenExpiry(expiryDate);
                user.update();
                
                // Send reset email
                sendPasswordResetEmail(user.getEmail(), resetToken, request);
            }
            
            return "{ \"success\": true, \"message\": \"If an account exists with this email, you will receive password reset instructions.\" }";
        } catch (Exception e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"An error occurred while processing your request.\" }";
        }
    }

    private String generateResetToken() {
        // Generate a secure random token
        byte[] randomBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(randomBytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private void sendPasswordResetEmail(String email, String resetToken, Request request) throws ApplicationException {
        try {
            // Get base URL from configuration or fallback
            String baseUrl = getConfiguration().get("app.base_url");
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = "http://localhost";
            }
            // Create reset link with absolute URL
            String resetLink = baseUrl + "/?q=reset-password&token=" + resetToken;

            // Create email content
            String emailContent = String.format(
                "Hello,\n\n" +
                "You have requested to reset your password for SmallTalk. " +
                "Click the link below to reset your password:\n\n" +
                "%s\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you did not request this password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "SmallTalk Team",
                resetLink
            );

            SimpleMail mail = new SimpleMail();
            mail.setFrom(this.getProperty("mail.from.address"));
            mail.setSubject("Password Reset Request - SmallTalk");
            mail.setBody(emailContent);
            mail.addTo(email);
            mail.send();
        } catch (Exception e) {
            throw new ApplicationException("Failed to send password reset email: " + e.getMessage());
        }
    }

    @Action("reset-password")
    public String resetPassword(Request request, Response response) throws ApplicationException {
        String token = request.getParameter("token");
        String newPassword = request.getParameter("password");

        // Validate required fields
        if (token == null || token.trim().isEmpty()) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"missing_token\", \"message\": \"Reset token is required\" }";
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"missing_password\", \"message\": \"New password is required\" }";
        }

        try {
            // Find user by reset token
            User user = findUserByResetToken(token);
            if (user == null) {
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return "{ \"error\": \"invalid_token\", \"message\": \"Invalid or expired reset token\" }";
            }

            // Check if token is expired
            if (user.getResetTokenExpiry().before(new Date())) {
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return "{ \"error\": \"expired_token\", \"message\": \"Reset token has expired\" }";
            }

            // Update password
            user.setPasswordHash(AuthenticationService.getInstance().hashPassword(newPassword));
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            user.update();

            return "{ \"success\": true, \"message\": \"Password has been reset successfully\" }";
        } catch (Exception e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"An error occurred while resetting your password.\" }";
        }
    }

    private User findUserByResetToken(String token) throws ApplicationException {
        return AuthenticationService.getInstance().findUserByResetToken(token);
    }

    @Override
    public void init() {

    }

    @Override
    public String version() {
        return "";
    }
}