package custom.application.v1;

import custom.objects.ChatHistory;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.Table;
import org.tinystruct.handler.Reforward;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.system.annotation.Action;

import java.util.List;

public class history extends AbstractApplication {
    public void init() {
        // Initialize any required resources
    }

    @Override
    public String version() {
        return "";
    }

    @Action("api/history")
    public String getChatHistory(Request request, Response response) throws ApplicationException {
        try {
            String meetingCode = request.getParameter("meetingCode");
            if (meetingCode == null || meetingCode.isEmpty()) {
                throw new ApplicationException("Meeting code is required");
            }

            List<ChatHistory> history = getChatHistoryByMeetingCode(meetingCode);
            Builders builders = new Builders();
            for (ChatHistory entry : history) {
                Builder builder = new Builder();
                builder.put("id", entry.getId());
                builder.put("meetingCode", entry.getMeetingCode());
                builder.put("userId", entry.getUserId());
                builder.put("message", entry.getMessage());
                builder.put("sessionId", entry.getSessionId());
                builder.put("messageType", entry.getMessageType());
                builder.put("imageUrl", entry.getImageUrl());
                builder.put("createdAt", entry.getCreatedAt());

                // Get username from session
                if (entry.getUserId() == 0) {
                    builder.put("username", "Assistant");
                } else {
                    // Try to get username from session
                    Object username = request.getSession().getAttribute("username");
                    if (username != null) {
                        builder.put("username", username.toString());
                    } else {
                        builder.put("username", "User " + entry.getUserId());
                    }
                }
                builders.add(builder);
            }
            return builders.toString();
        } catch (Exception e) {
            throw new ApplicationException("Failed to get chat history: " + e.getMessage());
        }
    }

    @Action("api/meetings")
    public String getMeetings(Request request, Response response) throws ApplicationException {
        try {
            // Check if user is logged in
            Object userId = request.getSession().getAttribute("user_id");
            if (userId == null) {
                throw new ApplicationException("User not logged in");
            }

            ChatHistory history = new ChatHistory();
            Table meetings = history.find(
                "SELECT DISTINCT meeting_code, MIN(created_at) as first_message, " +
                "COUNT(*) as message_count, COUNT(DISTINCT user_id) as participant_count " +
                "FROM chat_history " +
                "WHERE user_id = ? " +  // Only show meetings for the current user
                "GROUP BY meeting_code " +
                "ORDER BY first_message DESC", 
                new Object[]{userId}
            );

            Builders builders = new Builders();
            for (Row row : meetings) {
                Builder builder = new Builder();
                builder.put("meetingCode", row.getFieldInfo("meeting_code").stringValue());
                builder.put("createdAt", row.getFieldInfo("first_message").stringValue());
                builder.put("messageCount", row.getFieldInfo("message_count").intValue());
                builder.put("participantCount", row.getFieldInfo("participant_count").intValue());
                builders.add(builder);
            }
            return builders.toString();
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException("Failed to get meetings: " + e.getMessage());
        }
    }

    @Action("talk/history")
    public Object listChatHistory(Request request, Response response) throws ApplicationException {
        try {
            // Check if user is logged in
            Object userId = request.getSession().getAttribute("user_id");
            if (userId == null) {
                // Store the current URL as pending
                request.getSession().setAttribute("pending_url", "/?q=talk/history");
                // Redirect to login page
                Reforward reforward = new Reforward(request, response);
                reforward.setDefault("/?q=login");
                return reforward.forward();
            }
            return this;
        } catch (Exception e) {
            throw new ApplicationException("Failed to list chat history: " + e.getMessage());
        }
    }

    private List<ChatHistory> getChatHistoryByMeetingCode(String meetingCode) throws ApplicationException {
        try {
            ChatHistory history = new ChatHistory();
            Table messages = history.findWith(
                "WHERE meeting_code = ? ORDER BY created_at ASC",
                new Object[]{meetingCode}
            );

            List<ChatHistory> historyList = new java.util.ArrayList<>();
            for (Row row : messages) {
                ChatHistory entry = new ChatHistory();
                entry.setData(row);
                historyList.add(entry);
            }
            return historyList;
        } catch (Exception e) {
            throw new ApplicationException("Failed to get chat history: " + e.getMessage());
        }
    }
} 