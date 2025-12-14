package custom.application.v1;

import custom.objects.Feedback;
import custom.objects.User;
import custom.util.AuthenticationService;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Table;
import org.tinystruct.data.component.Builder;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.http.ResponseStatus;
import org.tinystruct.system.annotation.Action;

import java.util.Date;

public class feedback extends AbstractApplication {
    @Override
    public void init() {
    }

    @Override
    public String version() {
        return "1.0";
    }

    @Action("feedback/submit")
    public String submit(Request request, Response response) {
        String content = request.getParameter("content");
        if (content == null || content.trim().isEmpty()) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{\"success\":false,\"message\":\"Feedback content is required.\"}";
        }
        Feedback feedback = new Feedback();
        feedback.setContent(content.trim());
        feedback.setCreatedAt(new Date());
        Object userId = request.getSession().getAttribute("user_id");
        if (userId != null) {
            try {
                feedback.setUserId(Integer.parseInt(userId.toString()));
            } catch (Exception ignored) {
            }
        }
        try {
            boolean status = feedback.append();
            return "{\"success\":" + status + "}";
        } catch (Exception e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{\"success\":false,\"message\":\"Failed to save feedback.\"}";
        }
    }

    @Action("feedback/list")
    public String list(Request request, Response response) throws ApplicationException {
        // Only admin can view
        Object userId = request.getSession().getAttribute("user_id");
        if (userId == null) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{\"error\":\"not_authenticated\"}";
        }
        User user = AuthenticationService.getCurrentUser(userId.toString());
        if (user == null || !user.getIsAdmin()) {
            response.setStatus(ResponseStatus.FORBIDDEN);
            return "{\"error\":\"forbidden\"}";
        }
        Feedback feedback = new Feedback();
        Table all = feedback.orderBy(new String[]{"created_at"}).findAll();
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        for (int i = 0; i < all.size(); i++) {
            Feedback f = new Feedback();
            f.setData(all.get(i));
            json.append(f);
            if (i < all.size() - 1) json.append(",\n");
        }
        json.append("\n]");
        return json.toString();
    }

    private String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
} 