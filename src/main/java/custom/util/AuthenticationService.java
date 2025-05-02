package custom.util;

import custom.objects.User;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Table;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

/**
 * Service for handling user authentication, registration, and password management.
 */
public class AuthenticationService {
    private static final int SALT_LENGTH = 16;
    private static final String HASH_ALGORITHM = "SHA-256";

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
     * Hash a password with a random salt
     *
     * @param password Plain text password
     * @return Hashed password with salt
     */
    private String hashPassword(String password) {
        try {
            // Generate a random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // Hash the password with the salt
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));

            // Combine salt and hashed password
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);

            // Encode as Base64 string
            return Base64.getEncoder().encodeToString(combined);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash password: " + e.getMessage(), e);
        }
    }

    /**
     * Verify a password against a hashed password
     *
     * @param password       Plain text password
     * @param hashedPassword Hashed password with salt
     * @return True if the password matches, false otherwise
     */
    private boolean verifyPassword(String password, String hashedPassword) {
        try {
            // Decode the combined salt and hash
            byte[] combined = Base64.getDecoder().decode(hashedPassword);

            // Extract the salt
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, salt.length);

            // Hash the password with the extracted salt
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashedInput = md.digest(password.getBytes(StandardCharsets.UTF_8));

            // Extract the original hash
            byte[] originalHash = new byte[combined.length - salt.length];
            System.arraycopy(combined, salt.length, originalHash, 0, originalHash.length);

            // Compare the hashes
            return MessageDigest.isEqual(hashedInput, originalHash);
        } catch (NoSuchAlgorithmException | IllegalArgumentException e) {
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
}
