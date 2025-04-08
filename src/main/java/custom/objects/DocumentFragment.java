package custom.objects;

import org.tinystruct.data.component.AbstractData;
import org.tinystruct.data.component.Row;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a fragment of a document after processing.
 * Used to store parts of larger documents that have been split for easier processing.
 */
public class DocumentFragment extends AbstractData implements Serializable {
    /**
     * Auto Generated Serial Version UID
     */
    private static final long serialVersionUID = 0L;
    private String documentId;
    private String content;
    private int fragmentIndex;
    private String filePath;
    private String mimeType;
    private Date createdAt;

    public String getId() {
        return String.valueOf(this.Id);
    }

    public void setDocumentId(String documentId) {
        this.documentId = this.setFieldAsString("documentId", documentId);
    }

    public String getDocumentId() {
        return this.documentId;
    }

    public void setContent(String content) {
        this.content = this.setFieldAsString("content", content);
    }

    public String getContent() {
        return this.content;
    }

    public void setFragmentIndex(int fragmentIndex) {
        this.fragmentIndex = this.setFieldAsInt("fragmentIndex", fragmentIndex);
    }

    public int getFragmentIndex() {
        return this.fragmentIndex;
    }

    public void setFilePath(String filePath) {
        this.filePath = this.setFieldAsString("filePath", filePath);
    }

    public String getFilePath() {
        return this.filePath;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = this.setFieldAsString("mimeType", mimeType);
    }

    public String getMimeType() {
        return this.mimeType;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = this.setFieldAsDate("createdAt", createdAt);
    }

    public Date getCreatedAt() {
        return this.createdAt;
    }


    @Override
    public void setData(Row row) {
        if (row.getFieldInfo("id") != null) this.setId(row.getFieldInfo("id").stringValue());
        if (row.getFieldInfo("document_id") != null) this.setDocumentId(row.getFieldInfo("document_id").stringValue());
        if (row.getFieldInfo("content") != null) this.setContent(row.getFieldInfo("content").stringValue());
        if (row.getFieldInfo("fragment_index") != null)
            this.setFragmentIndex(row.getFieldInfo("fragment_index").intValue());
        if (row.getFieldInfo("file_path") != null) this.setFilePath(row.getFieldInfo("file_path").stringValue());
        if (row.getFieldInfo("mime_type") != null) this.setMimeType(row.getFieldInfo("mime_type").stringValue());
        if (row.getFieldInfo("created_at") != null) this.setCreatedAt(row.getFieldInfo("created_at").dateValue());
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("{");
        buffer.append("\"Id\":\"" + this.getId() + "\"");
        buffer.append(",\"documentId\":\"" + this.getDocumentId() + "\"");
        buffer.append(",\"content\":\"" + this.getContent() + "\"");
        buffer.append(",\"fragmentIndex\":" + this.getFragmentIndex());
        buffer.append(",\"filePath\":\"" + this.getFilePath() + "\"");
        buffer.append(",\"mimeType\":\"" + this.getMimeType() + "\"");
        buffer.append(",\"createdAt\":" + this.getCreatedAt());
        buffer.append("}");
        return buffer.toString();
    }
} 