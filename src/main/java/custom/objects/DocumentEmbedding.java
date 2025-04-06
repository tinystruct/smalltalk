package custom.objects;

import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.AbstractData;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.Table;
import org.tinystruct.system.ApplicationManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Vector;

/**
 * Represents an embedding for a document fragment.
 * This class is used to store embedding vectors in the database.
 */
public class DocumentEmbedding extends AbstractData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String fragmentId;
    private byte[] embedding;
    private int embeddingDimension;
    private Date createdAt;
    
    public String getId() {
        return String.valueOf(this.Id);
    }
    
    public void setFragmentId(String fragmentId) {
        this.fragmentId = this.setFieldAsString("fragmentId", fragmentId);
    }
    
    public String getFragmentId() {
        return this.fragmentId;
    }
    
    public void setEmbedding(byte[] embedding) {
        this.embedding = embedding;
        this.setField("embedding", embedding);
    }
    
    public byte[] getEmbedding() {
        return this.embedding;
    }
    
    public void setEmbeddingDimension(int embeddingDimension) {
        this.embeddingDimension = this.setFieldAsInt("embeddingDimension", embeddingDimension);
    }
    
    public int getEmbeddingDimension() {
        return this.embeddingDimension;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = this.setFieldAsDate("createdAt", createdAt);
    }
    
    public Date getCreatedAt() {
        return this.createdAt;
    }
    
    /**
     * Store a Vector<Double> as a byte array in the embedding field
     * @param embeddingVector The embedding vector
     * @throws ApplicationException if serialization fails
     */
    public void setEmbeddingVector(Vector<Double> embeddingVector) throws ApplicationException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(embeddingVector);
            oos.close();
            this.setEmbedding(baos.toByteArray());
            this.setEmbeddingDimension(embeddingVector.size());
        } catch (Exception e) {
            throw new ApplicationException("Failed to serialize embedding vector: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get the embedding vector from the byte array
     * @return The deserialized embedding vector
     * @throws ApplicationException if deserialization fails
     */
    public Vector<Double> getEmbeddingVector() throws ApplicationException {
        try {
            if (this.getEmbedding() == null) {
                return null;
            }
            
            ByteArrayInputStream bais = new ByteArrayInputStream(this.getEmbedding());
            ObjectInputStream ois = new ObjectInputStream(bais);
            Vector<Double> embeddingVector = (Vector<Double>) ois.readObject();
            ois.close();
            return embeddingVector;
        } catch (Exception e) {
            throw new ApplicationException("Failed to deserialize embedding vector: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void setData(Row row) {
        if (row.getFieldInfo("id") != null) this.setId(row.getFieldInfo("id").stringValue());
        if (row.getFieldInfo("fragment_id") != null) this.setFragmentId(row.getFieldInfo("fragment_id").stringValue());
        if (row.getFieldInfo("embedding") != null) {
            // Get the raw object from the field info
            try {
                // Use stringValue() to get BLOB data
                String embeddingString = row.getFieldInfo("embedding").stringValue();
                if (embeddingString != null) {
                    this.setEmbedding(embeddingString.getBytes());
                }
            } catch (Exception e) {
                // Handle exception silently
                System.err.println("Failed to convert embedding data: " + e.getMessage());
            }
        }
        if (row.getFieldInfo("embedding_dimension") != null) this.setEmbeddingDimension(row.getFieldInfo("embedding_dimension").intValue());
        if (row.getFieldInfo("created_at") != null) this.setCreatedAt(row.getFieldInfo("created_at").timestampValue());
    }
    
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("{");
        buffer.append("\"Id\":\"" + this.getId() + "\"");
        buffer.append(",\"fragmentId\":\"" + this.getFragmentId() + "\"");
        buffer.append(",\"embeddingDimension\":" + this.getEmbeddingDimension());
        buffer.append(",\"createdAt\":\"" + this.getCreatedAt() + "\"");
        buffer.append("}");
        return buffer.toString();
    }
    
    /**
     * Test method for the DocumentEmbedding class
     */
    public static void main(String[] args) {
        try {
            ApplicationManager.init();
            
            // Test creating and saving an embedding
            DocumentEmbedding embedding = new DocumentEmbedding();
            embedding.setFragmentId("1");
            
            // Create a test vector
            Vector<Double> testVector = new Vector<>();
            for (int i = 0; i < 10; i++) {
                testVector.add((double) i);
            }
            
            embedding.setEmbeddingVector(testVector);
            embedding.setCreatedAt(new Date());
            
            // Save to database
            try {
                embedding.append();
                System.out.println("Successfully saved embedding: " + embedding.toString());
            } catch (Exception e) {
                System.err.println("Failed to save embedding: " + e.getMessage());
            }
            
            // Test retrieving embeddings
            DocumentEmbedding retrievedEmbedding = new DocumentEmbedding();
            Table results = retrievedEmbedding.find("fragment_id = ?", new Object[]{"1"});
            
            if (results != null && !results.isEmpty()) {
                DocumentEmbedding resultEmbedding = new DocumentEmbedding();
                resultEmbedding.setData(results.get(0));
                
                Vector<Double> retrievedVector = resultEmbedding.getEmbeddingVector();
                System.out.println("Retrieved embedding vector: " + retrievedVector);
                System.out.println("Original embedding vector: " + testVector);
                
                // Verify vectors match
                if (retrievedVector != null && retrievedVector.equals(testVector)) {
                    System.out.println("Vectors match! Test passed.");
                } else {
                    System.err.println("Vectors do not match. Test failed.");
                }
            } else {
                System.err.println("No embedding found for fragment_id = 1");
            }
            
        } catch (Exception e) {
            System.err.println("Error during test: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 