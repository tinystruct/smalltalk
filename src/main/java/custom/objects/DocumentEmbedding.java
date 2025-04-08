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
            if (embeddingVector == null) {
                throw new ApplicationException("Cannot serialize null embedding vector");
            }

            System.out.println("Serializing embedding vector with dimension: " + embeddingVector.size());

            // Print first few values for debugging
            if (!embeddingVector.isEmpty()) {
                StringBuilder sb = new StringBuilder("First 5 values: ");
                for (int i = 0; i < Math.min(5, embeddingVector.size()); i++) {
                    sb.append(embeddingVector.get(i));
                    if (i < Math.min(4, embeddingVector.size() - 1)) {
                        sb.append(", ");
                    }
                }
                System.out.println(sb.toString());
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(embeddingVector);
            oos.close();

            byte[] serializedData = baos.toByteArray();
            System.out.println("Serialized embedding to " + serializedData.length + " bytes");

            this.setEmbedding(serializedData);
            this.setEmbeddingDimension(embeddingVector.size());
        } catch (Exception e) {
            System.err.println("Error serializing embedding vector: " + e.getMessage());
            e.printStackTrace();
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
                System.err.println("Warning: Embedding is null for fragment ID: " + this.getFragmentId());
                return null;
            }

            byte[] embeddingData = this.getEmbedding();
            System.out.println("Deserializing embedding data of size: " + embeddingData.length + " bytes for fragment ID: " + this.getFragmentId());

            ByteArrayInputStream bais = new ByteArrayInputStream(embeddingData);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Object obj = ois.readObject();
            ois.close();

            System.out.println("Deserialized object class: " + (obj != null ? obj.getClass().getName() : "null"));

            if (!(obj instanceof Vector)) {
                System.err.println("Error: Deserialized object is not a Vector: " +
                        (obj != null ? obj.getClass().getName() : "null") +
                        " for fragment ID: " + this.getFragmentId());
                return null;
            }

            @SuppressWarnings("unchecked")
            Vector<Double> embeddingVector = (Vector<Double>) obj;

            // Validate the vector
            if (embeddingVector.size() != this.getEmbeddingDimension()) {
                System.err.println("Warning: Vector dimension (" + embeddingVector.size() +
                        ") does not match stored dimension (" + this.getEmbeddingDimension() + ") " +
                        "for fragment ID: " + this.getFragmentId());
            }

            return embeddingVector;
        } catch (Exception e) {
            System.err.println("Error deserializing embedding vector for fragment ID: " +
                    this.getFragmentId() + ": " + e.getMessage());
            e.printStackTrace();
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
                // Get BLOB data directly
                Object embeddingObj = row.getFieldInfo("embedding").value();
                // If getValue() returns null, try stringValue() as fallback
                if (embeddingObj == null) {
                    try {
                        embeddingObj = row.getFieldInfo("embedding").stringValue();
                        System.out.println("Using stringValue() as fallback for embedding");
                    } catch (Exception e) {
                        System.err.println("Failed to get embedding using stringValue(): " + e.getMessage());
                    }
                }
                String fragmentId = row.getFieldInfo("fragment_id") != null ?
                        row.getFieldInfo("fragment_id").stringValue() : "unknown";

                System.out.println("Retrieved embedding object for fragment ID: " + fragmentId +
                        ", class: " + (embeddingObj != null ? embeddingObj.getClass().getName() : "null"));

                if (embeddingObj != null) {
                    if (embeddingObj instanceof byte[]) {
                        byte[] embeddingBytes = (byte[]) embeddingObj;
                        System.out.println("Setting embedding from byte array of size: " + embeddingBytes.length);
                        this.setEmbedding(embeddingBytes);
                    } else if (embeddingObj instanceof String) {
                        String embeddingStr = (String) embeddingObj;
                        System.out.println("Setting embedding from string of length: " + embeddingStr.length());
                        this.setEmbedding(embeddingStr.getBytes());
                    } else {
                        System.err.println("Unexpected embedding data type: " + embeddingObj.getClass().getName());
                    }
                } else {
                    System.err.println("Embedding object is null for fragment ID: " + fragmentId);
                }
            } catch (Exception e) {
                System.err.println("Failed to convert embedding data: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("No embedding field found in row");
        }
        if (row.getFieldInfo("embedding_dimension") != null) this.setEmbeddingDimension(row.getFieldInfo("embedding_dimension").intValue());
        if (row.getFieldInfo("created_at") != null) this.setCreatedAt(row.getFieldInfo("created_at").dateValue());
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