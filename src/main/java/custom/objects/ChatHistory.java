package custom.objects;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.DatabaseOperator;
import org.tinystruct.data.component.AbstractData;
import org.tinystruct.data.component.Row;
import org.tinystruct.system.ApplicationManager;

public class ChatHistory extends AbstractData {
    private String meetingCode;
    private String userName;
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

    public void setUserName(String userName) {
        this.userName = this.setFieldAsString("userName", userName);
    }

    public String getUserName() {
        return this.userName;
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
        if(row.getFieldInfo("user_name") != null) this.setUserName(row.getFieldInfo("user_name").stringValue());
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
        buffer.append(",\"userName\":\"" + this.getUserName() + "\"");
        buffer.append(",\"message\":\"" + this.getMessage() + "\"");
        buffer.append(",\"sessionId\":\"" + this.getSessionId() + "\"");
        buffer.append(",\"messageType\":\"" + this.getMessageType() + "\"");
        buffer.append(",\"imageUrl\":\"" + (this.getImageUrl() != null ? this.getImageUrl() : "") + "\"");
        buffer.append(",\"createdAt\":\"" + this.getCreatedAt() + "\"");
        buffer.append("}");
        return buffer.toString();
    }

    public static void main(String[] args) throws ApplicationException {
        ApplicationManager.init();
        try(DatabaseOperator db = new DatabaseOperator();) {
            String sql = "CREATE TABLE chat_history (\n" +
                    "    id BIGINT PRIMARY KEY AUTO_INCREMENT,\n" +
                    "    meeting_code VARCHAR(255) NOT NULL,\n" +
                    "    user_name VARCHAR(255),\n" +
                    "    message TEXT,\n" +
                    "    session_id VARCHAR(255),\n" +
                    "    message_type VARCHAR(50),\n" +
                    "    image_url TEXT,\n" +
                    "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    INDEX idx_meeting_code (meeting_code),\n" +
                    "    INDEX idx_created_at (created_at)\n" +
                    ");";
            db.execute(sql);
/*            ChatHistory chatHistory = new ChatHistory();
            chatHistory.setMeetingCode("1234567890");
            chatHistory.setUserName("John Doe");
            chatHistory.setMessage("Hello, world!");
            System.out.println(chatHistory.toString());*/
        } catch (ApplicationException e) {
            throw new RuntimeException(e);
        }
    }
} 