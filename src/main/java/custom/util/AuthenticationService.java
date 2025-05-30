package custom.util;

import custom.objects.User;
import org.mindrot.jbcrypt.BCrypt;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Table;
import org.tinystruct.http.Session;

import java.util.Date;

/**
 * Service for handling user authentication, registration, and password management.
 */
public class AuthenticationService {
    private static final int BCRYPT_WORKLOAD = 12; // Recommended workload factor for BCrypt
    private Session session;

    public void setSession(Session session) {
        this.session = session;
    }

    /**
     * Check if the current user is logged in
     *
     * @return true if the user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return session != null && session.getAttribute("user_id") != null;
    }

    /**
     * Check if the current user is an admin
     *
     * @return true if the user is an admin, false otherwise
     * @throws ApplicationException if an error occurs
     */
    public boolean isAdmin() throws ApplicationException {
        if (!isLoggedIn()) {
            return false;
        }

        String userId = session.getAttribute("user_id").toString();
        User user = findUserById(userId);
        return user != null && user.getIsAdmin();
    }

    /**
     * Register a new user
     *
     * @param username Username
     * @param password Plain text password
     * @param email    Email address
     * @param fullName Full name
     * @return The newly created user
     * @throws ApplicationException if registration fails
     */
    public User registerUser(String username, String password, String email, String fullName) throws ApplicationException {
        // Check if username already exists
        User existingUser = findUserByUsername(username);
        if (existingUser != null) {
            throw new ApplicationException("Username already exists");
        }

        // Check if email already exists
        if (email != null && !email.isEmpty()) {
            existingUser = findUserByEmail(email);
            if (existingUser != null) {
                throw new ApplicationException("Email already exists");
            }
        }

        // Create new user
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hashPassword(password));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setCreatedAt(new Date());
        user.setIsActive(true);

        // Save user to database
        user.append();

        return user;
    }

    /**
     * Authenticate a user
     *
     * @param username Username
     * @param password Plain text password
     * @return The authenticated user, or null if authentication fails
     * @throws ApplicationException if authentication fails
     */
    public User authenticateUser(String username, String password) throws ApplicationException {
        User user = findUserByUsername(username);
        if (user == null) {
            throw new ApplicationException("Invalid username or password");
        }

        if (!verifyPassword(password, user.getPasswordHash())) {
            throw new ApplicationException("Invalid username or password");
        }

        if (!user.getIsActive()) {
            throw new ApplicationException("Account is inactive");
        }

        // Update last login time
        user.setLastLogin(new Date());
        user.update();

        return user;
    }

    /**
     * Find a user by username
     *
     * @param username Username to search for
     * @return The user, or null if not found
     * @throws ApplicationException if database operation fails
     */
    public User findUserByUsername(String username) throws ApplicationException {
        User user = new User();
        Table results = user.findWith("WHERE username = ?", new Object[]{username});

        if (results == null || results.isEmpty()) {
            return null;
        }

        user.setData(results.get(0));
        return user;
    }

    /**
     * Find a user by email
     *
     * @param email Email to search for
     * @return The user, or null if not found
     * @throws ApplicationException if database operation fails
     */
    public User findUserByEmail(String email) throws ApplicationException {
        User user = new User();
        Table results = user.findWith("WHERE email = ?", new Object[]{email});

        if (results == null || results.isEmpty()) {
            return null;
        }

        user.setData(results.get(0));
        return user;
    }

    /**
     * Find a user by ID
     *
     * @param id User ID to search for
     * @return The user, or null if not found
     * @throws ApplicationException if database operation fails
     */
    public User findUserById(String id) throws ApplicationException {
        User user = new User();
        user.setId(id);
        try {
            user.findOneById();
        } catch (ApplicationException e) {
            throw new RuntimeException(e);
        }

        return user;
    }

    /**
     * Hash a password using BCrypt
     *
     * @param password Plain text password
     * @return BCrypt hashed password
     */
    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_WORKLOAD));
    }

    /**
     * Verify a password against a BCrypt hash
     *
     * @param password       Plain text password
     * @param hashedPassword BCrypt hashed password
     * @return True if the password matches, false otherwise
     */
    private boolean verifyPassword(String password, String hashedPassword) {
        try {
            return BCrypt.checkpw(password, hashedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Get the singleton instance of the AuthenticationService
     *
     * @return The AuthenticationService instance
     */
    public static AuthenticationService getInstance() {
        AuthenticationService service = null;
        if (service == null) {
            service = new AuthenticationService();
        }
        return service;
    }

    /**
     * Get the current authenticated user from the request
     *
     * @param userId User ID
     * @return The authenticated user, or null if not authenticated
     * @throws ApplicationException if an error occurs
     */
    public static User getCurrentUser(String userId) throws ApplicationException {
        try {
            if (userId == null) {
                System.err.println("Cannot get current user: user_id attribute is null");
                return null;
            }

            // Get user by ID
            String userIdStr = userId.toString();
            User user = getInstance().findUserById(userIdStr);

            if (user == null) {
                System.err.println("Cannot get current user: user not found with ID " + userIdStr);
                return null;
            }

            return user;
        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
            throw new ApplicationException("Error getting current user: " + e.getMessage(), e);
        }
    }
}
