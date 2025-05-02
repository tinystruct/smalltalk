package custom.application.v1;

import custom.objects.DocumentFragment;
import custom.objects.User;
import custom.util.AuthenticationService;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;

import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.Table;
import org.tinystruct.handler.Reforward;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.http.ResponseStatus;
import org.tinystruct.system.annotation.Action;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Libraries application for managing document libraries
 */
public class libraries extends AbstractApplication {
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    public void init() {
        this.setTemplateRequired(true);
    }

    @Override
    public String version() {
        return "1.0";
    }

    /**
     * Libraries page
     */
    @Action("libraries")
    public Object librariesPage(Request request, Response response) throws ApplicationException {
        // Check if user is authenticated
        Object userId = request.getSession().getAttribute("user_id");
        if (userId == null) {
            // Redirect to login page
            try {
                Reforward reforward = new Reforward(request, response);
                reforward.setDefault("/?q=login");
                return reforward.forward();
            } catch (Exception e) {
                throw new ApplicationException("Failed to redirect to login page: " + e.getMessage(), e);
            }
        }

        return this;
    }

    /**
     * Get user's documents
     */
    @Action("libraries/my-documents")
    public String getMyDocuments(Request request, Response response) throws ApplicationException {
        // Check if user is logged in
        Object userId = request.getSession().getAttribute("user_id");
        if (userId == null) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"User is not logged in\" }";
        }

        try {
            // Get user's documents
            DocumentFragment fragment = new DocumentFragment();
            Table documents = fragment.findWith("WHERE user_id = ?", new Object[]{userId.toString()});

            Builders builders = new Builders();
            for (Row row : documents) {
                DocumentFragment doc = new DocumentFragment();
                doc.setData(row);
                Builder builder = new Builder();
                builder.put("id", doc.getId());
                builder.put("documentId", doc.getDocumentId());
                builder.put("title", doc.getTitle() != null ? doc.getTitle() : "");
                builder.put("description", doc.getDescription() != null ? doc.getDescription() : "");
                builder.put("filePath", doc.getFilePath());
                builder.put("mimeType", doc.getMimeType());
                builder.put("createdAt", format.format(doc.getCreatedAt()));
                builder.put("isPublic", doc.getIsPublic());
                builders.add(builder);
            }

            return builders.toString();
        } catch (Exception e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"" + e.getMessage() + "\" }";
        }
    }

    /**
     * Get public documents
     */
    @Action("libraries/public-documents")
    public String getPublicDocuments(Request request, Response response) throws ApplicationException {
        // Check if user is logged in
        Object userId = request.getSession().getAttribute("user_id");
        if (userId == null) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"User is not logged in\" }";
        }

        try {
            // Get public documents
            DocumentFragment fragment = new DocumentFragment();
            Table documents = fragment.findWith("WHERE is_public = ? AND user_id != ?", new Object[]{true, userId.toString()});

            Builders builders = new Builders();
            for (Row row : documents) {
                DocumentFragment doc = new DocumentFragment();
                doc.setData(row);
                Builder builder = new Builder();
                builder.put("id", doc.getId());
                builder.put("documentId", doc.getDocumentId());
                builder.put("title", doc.getTitle() != null ? doc.getTitle() : "");
                builder.put("description", doc.getDescription() != null ? doc.getDescription() : "");
                builder.put("filePath", doc.getFilePath());
                builder.put("mimeType", doc.getMimeType());
                builder.put("createdAt", format.format(doc.getCreatedAt()));
                builder.put("isPublic", doc.getIsPublic());
                builders.add(builder);
            }

            return builders.toString();
        } catch (Exception e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"" + e.getMessage() + "\" }";
        }
    }

    /**
     * Get all documents (admin only)
     */
    @Action("libraries/all-documents")
    public String getAllDocuments(Request request, Response response) throws ApplicationException {
        // Check if user is logged in
        Object userId = request.getSession().getAttribute("user_id");
        if (userId == null) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"User is not logged in\" }";
        }

        try {
            // Check if user is admin
            AuthenticationService authService = AuthenticationService.getInstance();
            User user = authService.findUserById(userId.toString());

            if (user == null || !user.getIsAdmin()) {
                response.setStatus(ResponseStatus.FORBIDDEN);
                return "{ \"error\": \"forbidden\", \"message\": \"Admin access required\" }";
            }

            // Get all documents
            DocumentFragment fragment = new DocumentFragment();
            Table documents = fragment.findWith("WHERE id > ?", new Object[]{0});

            Builders builders = new Builders();
            for (Row row : documents) {
                DocumentFragment doc = new DocumentFragment();
                doc.setData(row);
                Builder builder = new Builder();
                builder.put("id", doc.getId());
                builder.put("documentId", doc.getDocumentId());
                builder.put("title", doc.getTitle() != null ? doc.getTitle() : "");
                builder.put("description", doc.getDescription() != null ? doc.getDescription() : "");
                builder.put("filePath", doc.getFilePath());
                builder.put("mimeType", doc.getMimeType());
                builder.put("createdAt", format.format(doc.getCreatedAt()));
                builder.put("isPublic", doc.getIsPublic());
                builder.put("userId", doc.getUserId());
                builders.add(builder);
            }

            return builders.toString();
        } catch (Exception e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"" + e.getMessage() + "\" }";
        }
    }
}
