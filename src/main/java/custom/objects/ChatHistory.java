package custom.objects;

import org.tinystruct.data.component.AbstractData;
import org.tinystruct.data.component.Row;

public class ChatHistory extends AbstractData {
    private String meetingCode;
    private Integer userId;
    private String message;
    private String sessionId;
    private String messageType;
    private String imageUrl;
    private String createdAt;

    public String getId() {
        return String.valueOf(this.Id);
    }

    public void setMeetingCode(String meetingCode) {
        this.meetingCode = this.setFieldAsString("meetingCode", meetingCode);
    }

    public String getMeetingCode() {
        return this.meetingCode;
    }

    public void setUserId(Integer userId) {
        this.userId = this.setFieldAsInt("userId", userId);
    }

    public Integer getUserId() {
        return this.userId;
    }

    public void setMessage(String message) {
        this.message = this.setFieldAsString("message", message);
    }

    public String getMessage() {
        return this.message;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = this.setFieldAsString("sessionId", sessionId);
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public void setMessageType(String messageType) {
        this.messageType = this.setFieldAsString("messageType", messageType);
    }

    public String getMessageType() {
        return this.messageType;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = this.setFieldAsString("imageUrl", imageUrl);
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = this.setFieldAsString("createdAt", createdAt);
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public void setData(Row row) {
        if(row.getFieldInfo("id") != null) this.setId(row.getFieldInfo("id").stringValue());
        if(row.getFieldInfo("meeting_code") != null) this.setMeetingCode(row.getFieldInfo("meeting_code").stringValue());
        if(row.getFieldInfo("user_id") != null) this.setUserId(row.getFieldInfo("user_id").intValue());
        if(row.getFieldInfo("message") != null) this.setMessage(row.getFieldInfo("message").stringValue());
        if(row.getFieldInfo("session_id") != null) this.setSessionId(row.getFieldInfo("session_id").stringValue());
        if(row.getFieldInfo("message_type") != null) this.setMessageType(row.getFieldInfo("message_type").stringValue());
        if(row.getFieldInfo("image_url") != null) this.setImageUrl(row.getFieldInfo("image_url").stringValue());
        if(row.getFieldInfo("created_at") != null) this.setCreatedAt(row.getFieldInfo("created_at").stringValue());
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("{");
        buffer.append("\"Id\":\"" + this.getId() + "\"");
        buffer.append(",\"meetingCode\":\"" + this.getMeetingCode() + "\"");
        buffer.append(",\"userId\":" + this.getUserId());
        buffer.append(",\"message\":\"" + this.getMessage() + "\"");
        buffer.append(",\"sessionId\":\"" + this.getSessionId() + "\"");
        buffer.append(",\"messageType\":\"" + this.getMessageType() + "\"");
        buffer.append(",\"imageUrl\":\"" + (this.getImageUrl() != null ? this.getImageUrl() : "") + "\"");
        buffer.append(",\"createdAt\":\"" + this.getCreatedAt() + "\"");
        buffer.append("}");
        return buffer.toString();
    }
} 