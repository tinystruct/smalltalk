package custom.objects;

import org.tinystruct.data.component.AbstractData;
import org.tinystruct.data.component.Row;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Represents a user-specific system prompt
 */
public class UserPrompt extends AbstractData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String systemPrompt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public UserPrompt() {
        super();
        this.setTableName("user_prompts");
    }

    public UserPrompt(String userId, String systemPrompt) {
        this();
        this.setUserId(userId);
        this.setSystemPrompt(systemPrompt);
    }

    public String getId() {
        return String.valueOf(this.Id);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        this.setFieldAsString("userId", userId);
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = this.setFieldAsString("systemPrompt", systemPrompt);
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
        this.setFieldAsTimestamp("createdAt", createdAt);
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
        this.setFieldAsTimestamp("updatedAt", updatedAt);
    }

    @Override
    public void setData(Row row) {
        if (row.getFieldInfo("id") != null) this.setId(row.getFieldInfo("id").stringValue());
        if (row.getFieldInfo("user_id") != null) this.setUserId(row.getFieldInfo("user_id").stringValue());
        if (row.getFieldInfo("system_prompt") != null) this.setSystemPrompt(row.getFieldInfo("system_prompt").stringValue());
        if (row.getFieldInfo("created_at") != null) this.setCreatedAt(row.getFieldInfo("created_at").timestampValue());
        if (row.getFieldInfo("updated_at") != null) this.setUpdatedAt(row.getFieldInfo("updated_at").timestampValue());
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("{");
        buffer.append("\"id\":\"").append(this.getId()).append("\"");
        buffer.append(",\"userId\":\"").append(this.getUserId()).append("\"");
        buffer.append(",\"systemPrompt\":\"").append(this.getSystemPrompt()).append("\"");
        buffer.append(",\"createdAt\":\"").append(this.getCreatedAt()).append("\"");
        buffer.append(",\"updatedAt\":\"").append(this.getUpdatedAt()).append("\"");
        buffer.append("}");
        return buffer.toString();
    }
}
