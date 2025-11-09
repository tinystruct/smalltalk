package custom.objects;

import org.tinystruct.data.component.AbstractData;
import org.tinystruct.data.component.Row;
import java.io.Serializable;
import java.util.Date;

public class Feedback extends AbstractData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String content;
    private Date createdAt;
    private Integer userId;

    public String getId() {
        return String.valueOf(this.Id);
    }

    public void setContent(String content) {
        this.content = this.setFieldAsString("content", content);
    }

    public String getContent() {
        return this.content;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = this.setFieldAsDate("createdAt", createdAt);
    }

    public Date getCreatedAt() {
        return this.createdAt;
    }

    public void setUserId(Integer userId) {
        this.userId = this.setFieldAsInt("userId", userId);
    }

    public Integer getUserId() {
        return this.userId;
    }

    @Override
    public void setData(Row row) {
        if (row.getFieldInfo("id") != null) this.setId(row.getFieldInfo("id").stringValue());
        if (row.getFieldInfo("content") != null) this.setContent(row.getFieldInfo("content").stringValue());
        if (row.getFieldInfo("created_at") != null) this.setCreatedAt(row.getFieldInfo("created_at").dateValue());
        if (row.getFieldInfo("user_id") != null) this.setUserId(row.getFieldInfo("user_id").intValue());
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("{");
        buffer.append("\"id\":\"").append(this.getId()).append("\"");
        buffer.append(",\"content\":\"").append(this.getContent() != null ? this.getContent().replace("\"", "\\\"") : "").append("\"");
        buffer.append(",\"createdAt\":\"").append(this.getCreatedAt()).append("\"");
        buffer.append(",\"userId\":").append(this.getUserId());
        buffer.append("}");
        return buffer.toString();
    }
} 