package custom.objects;

import org.tinystruct.data.component.AbstractData;
import org.tinystruct.data.component.Row;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a user in the system.
 * This class is used to store user information for authentication and authorization.
 */
public class User extends AbstractData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String passwordHash;
    private String email;
    private String fullName;
    private Date createdAt;
    private Date lastLogin;
    private boolean isActive;
    private boolean isAdmin;
    private String resetToken;
    private Date resetTokenExpiry;

    public String getId() {
        return String.valueOf(this.Id);
    }

    public void setUsername(String username) {
        this.username = this.setFieldAsString("username", username);
    }

    public String getUsername() {
        return this.username;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = this.setFieldAsString("passwordHash", passwordHash);
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public void setEmail(String email) {
        this.email = this.setFieldAsString("email", email);
    }

    public String getEmail() {
        return this.email;
    }

    public void setFullName(String fullName) {
        this.fullName = this.setFieldAsString("fullName", fullName);
    }

    public String getFullName() {
        return this.fullName;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = this.setFieldAsDate("createdAt", createdAt);
    }

    public Date getCreatedAt() {
        return this.createdAt;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = this.setFieldAsDate("lastLogin", lastLogin);
    }

    public Date getLastLogin() {
        return this.lastLogin;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = this.setFieldAsBoolean("isActive", isActive);
    }

    public boolean getIsActive() {
        return this.isActive;
    }

    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = this.setFieldAsBoolean("isAdmin", isAdmin);
    }

    public boolean getIsAdmin() {
        return this.isAdmin;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = this.setFieldAsString("resetToken", resetToken);
    }

    public String getResetToken() {
        return this.resetToken;
    }

    public void setResetTokenExpiry(Date resetTokenExpiry) {
        this.resetTokenExpiry = this.setFieldAsDate("resetTokenExpiry", resetTokenExpiry);
    }

    public Date getResetTokenExpiry() {
        return this.resetTokenExpiry;
    }

    @Override
    public void setData(Row row) {
        if (row.getFieldInfo("id") != null) this.setId(row.getFieldInfo("id").stringValue());
        if (row.getFieldInfo("username") != null) this.setUsername(row.getFieldInfo("username").stringValue());
        if (row.getFieldInfo("password_hash") != null) this.setPasswordHash(row.getFieldInfo("password_hash").stringValue());
        if (row.getFieldInfo("email") != null) this.setEmail(row.getFieldInfo("email").stringValue());
        if (row.getFieldInfo("full_name") != null) this.setFullName(row.getFieldInfo("full_name").stringValue());
        if (row.getFieldInfo("created_at") != null) this.setCreatedAt(row.getFieldInfo("created_at").dateValue());
        if (row.getFieldInfo("last_login") != null) this.setLastLogin(row.getFieldInfo("last_login").dateValue());
        if (row.getFieldInfo("is_active") != null) this.setIsActive(row.getFieldInfo("is_active").booleanValue());
        if (row.getFieldInfo("is_admin") != null) this.setIsAdmin(row.getFieldInfo("is_admin").booleanValue());
        if (row.getFieldInfo("reset_token") != null) this.setResetToken(row.getFieldInfo("reset_token").stringValue());
        if (row.getFieldInfo("reset_token_expiry") != null) this.setResetTokenExpiry(row.getFieldInfo("reset_token_expiry").dateValue());
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("{");
        buffer.append("\"id\":\"").append(this.getId()).append("\"");
        buffer.append(",\"username\":\"").append(this.getUsername()).append("\"");
        buffer.append(",\"email\":\"").append(this.getEmail() != null ? this.getEmail() : "").append("\"");
        buffer.append(",\"fullName\":\"").append(this.getFullName() != null ? this.getFullName() : "").append("\"");
        buffer.append(",\"createdAt\":\"").append(this.getCreatedAt()).append("\"");
        buffer.append(",\"lastLogin\":\"").append(this.getLastLogin() != null ? this.getLastLogin() : "").append("\"");
        buffer.append(",\"isActive\":").append(this.getIsActive());
        buffer.append(",\"isAdmin\":").append(this.getIsAdmin());
        buffer.append(",\"resetToken\":\"").append(this.getResetToken() != null ? this.getResetToken() : "").append("\"");
        buffer.append(",\"resetTokenExpiry\":\"").append(this.getResetTokenExpiry() != null ? this.getResetTokenExpiry() : "").append("\"");
        buffer.append("}");
        return buffer.toString();
    }
}
