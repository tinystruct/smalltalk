package custom.application.v1;

import custom.objects.DocumentFragment;
import custom.objects.User;
import custom.util.AuthenticationService;
import custom.util.DocumentProcessor;
import custom.util.EmbeddingManager;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.system.ApplicationManager;

import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.Table;
import org.tinystruct.handler.Reforward;
import org.tinystruct.data.FileEntity;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.http.ResponseStatus;
import org.tinystruct.system.annotation.Action;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Libraries application for managing document libraries
 */
public class libraries extends AbstractApplication {
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    public void init() {
        this.setTemplateRequired(true);
        // Actions are automatically mapped via @Action annotations
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
     * Get user's non-public documents
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
            // Get user's non-public documents
            DocumentFragment fragment = new DocumentFragment();
            Table documents = fragment.findWith("WHERE user_id = ? AND is_public = ?", new Object[]{userId.toString(), false});

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
            // Get all public documents (including those uploaded by the current user)
            DocumentFragment fragment = new DocumentFragment();
            Table documents = fragment.findWith("WHERE is_public = ?", new Object[]{true});

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

    /**
     * Upload a document to the library
     */
    @Action("libraries/upload")
    public String uploadDocument(Request request, Response response) throws ApplicationException {
        // Check if user is logged in
        Object userId = request.getSession().getAttribute("user_id");
        if (userId == null) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"User is not logged in\" }";
        }

        try {
            // Get form parameters
            String title = request.getParameter("title");
            String description = request.getParameter("description");
            String isPublicStr = request.getParameter("isPublic");
            boolean isPublic = isPublicStr != null && (isPublicStr.equals("true") || isPublicStr.equals("on"));

            // Auto-generate title from filename if empty
            if (title == null || title.trim().isEmpty()) {
                if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
                    FileEntity fileEntity = (FileEntity) request.getAttachments().get(0);
                    title = fileEntity.getFilename();
                    // Remove file extension from title
                    int lastDotIndex = title.lastIndexOf(".");
                    if (lastDotIndex > 0) {
                        title = title.substring(0, lastDotIndex);
                    }
                    // Replace underscores and hyphens with spaces
                    title = title.replace('_', ' ').replace('-', ' ');
                    System.out.println("Auto-generated title from filename: " + title);
                }
            }

            // Validate required fields
            if (title == null || title.trim().isEmpty()) {
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return "{ \"error\": \"missing_title\", \"message\": \"Title is required\" }";
            }

            // Get the uploaded files
            List<FileEntity> files = request.getAttachments();
            if (files == null || files.isEmpty()) {
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return "{ \"error\": \"missing_file\", \"message\": \"No file uploaded\" }";
            }

            // Get the first file
            FileEntity file = files.get(0);

            // Create upload directory if it doesn't exist
            String uploadPath = System.getProperty("user.dir") + File.separator + "files";
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // Save the file
            final File targetFile = new File(uploadPath + File.separator + file.getFilename());
            createDirectoryIfNeeded(targetFile.getParentFile());

            // Get file content
            byte[] fileContent = file.get();
            System.out.println("File content size: " + (fileContent != null ? fileContent.length : 0) + " bytes for " + file.getFilename());

            if (fileContent == null || fileContent.length == 0) {
                System.err.println("Warning: Empty file content for " + file.getFilename());
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return "{ \"error\": \"empty_file\", \"message\": \"File content is empty\" }";
            }

            try (final BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(targetFile))) {
                // Write file content directly
                bout.write(fileContent);
            }

            // Verify file was written correctly
            if (!targetFile.exists() || targetFile.length() == 0) {
                System.err.println("Warning: File appears to be empty after writing: " + targetFile.getPath());
                response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
                return "{ \"error\": \"file_write_error\", \"message\": \"Failed to write file to disk\" }";
            }

            System.out.println("File written successfully: " + targetFile.getPath() + " (" + targetFile.length() + " bytes)");

            // Process document if it's a supported type
            String mimeType = file.getContentType();
            System.out.println("Processing document with MIME type: " + mimeType);

            if (DocumentProcessor.isSupportedMimeType(mimeType)) {
                try {
                    // Process the document with user information
                    DocumentProcessor processor = new DocumentProcessor();
                    List<DocumentFragment> fragments = processor.processDocument(
                        targetFile.getPath(),
                        mimeType,
                        userId.toString(),
                        title,
                        description,
                        isPublic
                    );

                    if (fragments.isEmpty()) {
                        System.err.println("Warning: No fragments were generated from the document");
                        response.setStatus(ResponseStatus.BAD_REQUEST);
                        return "{ \"error\": \"processing_failed\", \"message\": \"Failed to extract content from document\" }";
                    }

                    // Save fragments to database
                    for (DocumentFragment fragment : fragments) {
                        // Save the fragment and get the generated ID
                        fragment.appendAndGetId();

                        // Log the fragment ID for debugging
                        System.out.println("Saved fragment with ID: " + fragment.getId());
                    }

                    // Generate embeddings for the fragments
                    EmbeddingManager embeddingManager = (EmbeddingManager) ApplicationManager.get(EmbeddingManager.class.getName());
                    for (DocumentFragment fragment : fragments) {
                        embeddingManager.generateEmbedding(fragment);
                    }

                    // Return success response
                    Builder builder = new Builder();
                    builder.put("success", true);
                    builder.put("message", "Document uploaded and processed successfully");
                    builder.put("title", title);
                    builder.put("fragmentCount", fragments.size());

                    return builder.toString();
                } catch (Exception e) {
                    System.err.println("Error processing document: " + e.getMessage());
                    e.printStackTrace();
                    response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
                    return "{ \"error\": \"processing_error\", \"message\": \"" + e.getMessage() + "\" }";
                }
            } else {
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return "{ \"error\": \"unsupported_file_type\", \"message\": \"Unsupported file type: " + file.getContentType() + "\" }";
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"" + e.getMessage() + "\" }";
        }
    }

    /**
     * Create directory if it doesn't exist
     */
    private void createDirectoryIfNeeded(File directory) {
        if (directory != null && !directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * Delete a document and all its fragments
     */
    @Action("libraries/delete")
    public String deleteDocument(Request request, Response response) throws ApplicationException {
        // Check if user is logged in
        Object userId = request.getSession().getAttribute("user_id");
        if (userId == null) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"User is not logged in\" }";
        }

        try {
            // Get document ID from request
            String documentId = request.getParameter("documentId");
            if (documentId == null || documentId.trim().isEmpty()) {
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return "{ \"error\": \"missing_document_id\", \"message\": \"Document ID is required\" }";
            }

            // Check if document exists and user is the owner
            DocumentFragment fragment = new DocumentFragment();
            Table documents = fragment.findWith("WHERE document_id = ? AND user_id = ?", new Object[]{documentId, userId.toString()});

            if (documents == null || documents.isEmpty()) {
                // Check if user is admin
                AuthenticationService authService = AuthenticationService.getInstance();
                User user = authService.findUserById(userId.toString());

                if (user == null || !user.getIsAdmin()) {
                    response.setStatus(ResponseStatus.FORBIDDEN);
                    return "{ \"error\": \"forbidden\", \"message\": \"You don't have permission to delete this document\" }";
                }

                // Admin can delete any document, so check if document exists
                documents = fragment.findWith("WHERE document_id = ?", new Object[]{documentId});
                if (documents == null || documents.isEmpty()) {
                    response.setStatus(ResponseStatus.NOT_FOUND);
                    return "{ \"error\": \"not_found\", \"message\": \"Document not found\" }";
                }
            }

            // Get the file path from the first fragment to delete the file later
            DocumentFragment firstFragment = new DocumentFragment();
            firstFragment.setData(documents.get(0));
            String filePath = firstFragment.getFilePath();

            // Delete all embeddings for this document's fragments
            try (org.tinystruct.data.DatabaseOperator db = new org.tinystruct.data.DatabaseOperator()) {
                // First delete embeddings
                String deleteEmbeddingsSQL = "DELETE FROM document_embeddings WHERE fragment_id IN " +
                        "(SELECT id FROM document_fragments WHERE document_id = '" + documentId + "')";
                db.execute(deleteEmbeddingsSQL);

                // Then delete fragments
                String deleteFragmentsSQL = "DELETE FROM document_fragments WHERE document_id = '" + documentId + "'";
                db.execute(deleteFragmentsSQL);
            }

            // Try to delete the physical file if it exists
            try {
                if (filePath != null && !filePath.isEmpty()) {
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        file.delete();
                    }
                }
            } catch (Exception e) {
                // Log but continue - the database records are more important
                System.err.println("Warning: Could not delete file: " + filePath + ". Error: " + e.getMessage());
            }

            // Return success response
            Builder builder = new Builder();
            builder.put("success", true);
            builder.put("message", "Document deleted successfully");
            builder.put("documentId", documentId);

            return builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"" + e.getMessage() + "\" }";
        }
    }
}
