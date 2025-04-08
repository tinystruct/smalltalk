package custom.util;

import custom.objects.DocumentEmbedding;
import custom.objects.DocumentFragment;
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.application.Context;
import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.data.component.Table;
import org.tinystruct.system.ApplicationManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static custom.application.v1.smalltalk.CONFIG_OPENAI_API_ENDPOINT;

public class EmbeddingManager extends AbstractApplication {
    private static final int EMBEDDING_DIMENSION = 1536; // OpenAI's text-embedding-ada-002 dimension
    private static final String EMBEDDING_MODEL = "text-embedding-ada-002";

    // In-memory cache for query embeddings to avoid redundant API calls
    private static final Map<String, Vector<Double>> queryEmbeddingCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000; // Maximum number of cached query embeddings

    /**
     * Check if an embedding exists for a given fragment
     * @param fragment The document fragment to check
     * @return true if an embedding exists, false otherwise
     */
    private static boolean hasEmbedding(DocumentFragment fragment) throws ApplicationException {
        DocumentEmbedding embedding = new DocumentEmbedding();
        Table results = embedding.findWith("WHERE fragment_id = ?", new Object[]{fragment.getId()});
        return results != null && !results.isEmpty();
    }

    /**
     * Retrieve an existing embedding from the database
     * @param fragment The document fragment
     * @return The embedding vector if found, null otherwise
     */
    private static Vector<Double> getExistingEmbedding(DocumentFragment fragment) throws ApplicationException {
        DocumentEmbedding embedding = new DocumentEmbedding();
        Table results = embedding.find("fragment_id = ?", new Object[]{fragment.getId()});

        if (results == null || results.isEmpty()) {
            return null;
        }

        DocumentEmbedding embeddingData = new DocumentEmbedding();
        embeddingData.setData(results.get(0));

        return embeddingData.getEmbeddingVector();
    }

    /**
     * Call OpenAI API to generate an embedding with retry logic
     *
     * @param text Text to generate embedding for
     * @return Vector of embedding values
     */
    private Vector<Double> callOpenAIEmbeddingAPI(String text) throws ApplicationException {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                System.out.println("Calling OpenAI API for embedding generation (attempt " + (retryCount + 1) + ")");

                // Create a payload for the OpenAI API call
                Builder payload = new Builder();
                payload.parse("{\n" +
                        "  \"model\": \"" + EMBEDDING_MODEL + "\",\n" +
                        "  \"input\": \"" + escapeJsonString(text) + "\"\n" +
                        "}");

                String API_URL = getConfiguration().get(CONFIG_OPENAI_API_ENDPOINT);
                if (API_URL == null || API_URL.trim().isEmpty()) {
                    throw new ApplicationException("OpenAI API endpoint not configured");
                }

                // Check if API key is configured
                String API_KEY = getConfiguration().get("openai.api_key");
                if (API_KEY == null || API_KEY.trim().isEmpty() ||
                    API_KEY.equals("your_openai_api_key_here") ||
                    API_KEY.equals("$_OPENAI_API_KEY")) {

                    // Try to get from environment variable
                    String envApiKey = System.getenv("OPENAI_API_KEY");
                    if (envApiKey != null && !envApiKey.trim().isEmpty()) {
                        // Use the environment variable
                        API_KEY = envApiKey;
                        System.out.println("Using OpenAI API key from environment variable");
                    } else {
                        throw new ApplicationException("OpenAI API key not configured. Please set a valid API key in application.properties or as an environment variable OPENAI_API_KEY");
                    }
                }

                API_URL = API_URL + "/v1/embeddings";
                System.out.println("Using API URL: " + API_URL);

                // Create context for the API call
                Context context = new ApplicationContext();
                context.setAttribute("payload", payload);
                context.setAttribute("api", API_URL);

                // Call the OpenAI API
                Builder response = (Builder) ApplicationManager.call("openai", context);
                if (response == null) {
                    throw new ApplicationException("No response received from OpenAI API");
                }

                // Check for errors
                if (response.get("error") != null) {
                    Builder error = (Builder) response.get("error");
                    String errorMessage = error.get("message") != null ?
                            error.get("message").toString() : "Unknown error from OpenAI API";
                    throw new ApplicationException(errorMessage);
                }

                Builders data = null;
                // Extract the embedding
                if (response.get("data") != null && response.get("data") instanceof Builders) {
                    data = (Builders) response.get("data");
                    if (data == null || data.get(0) == null) {
                        throw new ApplicationException("No embedding data in API response");
                    }
                } else {
                    throw new ApplicationException("Invalid response format: 'data' field missing or not a Builders object");
                }

                Object embeddingObj = data.get(0).get("embedding");
                if (embeddingObj == null) {
                    throw new ApplicationException("No embedding field in API response");
                }

                System.out.println("Embedding object class: " + embeddingObj.getClass().getName());

                List<Object> embeddingList;
                if (embeddingObj instanceof List) {
                    embeddingList = (List<Object>) embeddingObj;
                    if (!embeddingList.isEmpty()) {
                        Object firstValue = embeddingList.get(0);
                        System.out.println("First embedding value class: " +
                                (firstValue != null ? firstValue.getClass().getName() : "null") +
                                ", value: " + firstValue);
                    }
                } else {
                    throw new ApplicationException("Embedding is not a list: " + embeddingObj.getClass().getName());
                }

                if (embeddingList.isEmpty()) {
                    throw new ApplicationException("Empty embedding vector in API response");
                }

                // Convert to Vector<Double>
                Vector<Double> embedding = new Vector<>();
                for (Object value : embeddingList) {
                    try {
                        if (value instanceof Number) {
                            embedding.add(((Number) value).doubleValue());
                        } else {
                            // Remove any quotes from the string before parsing
                            String valueStr = value.toString().replaceAll("\"", "");
                            embedding.add(Double.parseDouble(valueStr));
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing embedding value: " + value +
                                " (" + (value != null ? value.getClass().getName() : "null") + ")");
                        throw new ApplicationException("Failed to parse embedding value: " + e.getMessage(), e);
                    }
                }

                if (embedding.size() != EMBEDDING_DIMENSION) {
                    System.out.println("Warning: Unexpected embedding dimension: " + embedding.size() +
                            " (expected " + EMBEDDING_DIMENSION + ")");
                }

                System.out.println("Successfully generated embedding with dimension: " + embedding.size());
                return embedding;

            } catch (Exception e) {
                retryCount++;
                System.err.println("Error calling OpenAI API (attempt " + retryCount + "): " + e.getMessage());
                e.printStackTrace();

                if (retryCount >= maxRetries) {
                    throw new ApplicationException("Failed to call OpenAI embedding API after " + maxRetries + " attempts: " + e.getMessage(), e);
                }

                try {
                    // Exponential backoff
                    long sleepTime = 1000 * (long)Math.pow(2, retryCount - 1);
                    System.out.println("Retrying in " + sleepTime + "ms...");
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ApplicationException("Interrupted while waiting to retry", ie);
                }
            }
        }

        // This should never be reached due to the exception in the loop
        throw new ApplicationException("Failed to call OpenAI embedding API");
    }

    /**
     * Escape special characters in a string for JSON
     */
    private static String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            switch (ch) {
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (ch < ' ') {
                        String hex = Integer.toHexString(ch);
                        sb.append("\\u");
                        for (int j = 0; j < 4 - hex.length(); j++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Generate embedding for a document fragment using OpenAI's API
     *
     * @param fragment The document fragment to generate embedding for
     * @return The embedding vector
     */
    public Vector<Double> generateEmbedding(DocumentFragment fragment) throws ApplicationException {
        try {
            // Check if embedding already exists
            if (hasEmbedding(fragment)) {
                return getExistingEmbedding(fragment);
            }

            // Call OpenAI API to generate embedding
            Vector<Double> embedding = callOpenAIEmbeddingAPI(fragment.getContent());

            // Save the new embedding
            saveEmbedding(fragment, embedding);

            return embedding;
        } catch (Exception e) {
            throw new ApplicationException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Save embedding to database
     * @param fragment The document fragment
     * @param embedding The embedding vector
     */
    public static void saveEmbedding(DocumentFragment fragment, Vector<Double> embedding) throws ApplicationException {
        try {
            DocumentEmbedding embeddingData = new DocumentEmbedding();
            embeddingData.setFragmentId(fragment.getId());
            embeddingData.setEmbeddingVector(embedding);
            embeddingData.setEmbeddingDimension(embedding.size());
            embeddingData.setCreatedAt(new Date());
            embeddingData.append();
        } catch (Exception e) {
            throw new ApplicationException("Failed to save embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Find similar document fragments using cosine similarity
     * @param queryEmbedding The query embedding vector
     * @param limit Maximum number of results to return
     * @return List of similar document fragments with their similarity scores
     */
    public static List<SimilarityResult> findSimilar(Vector<Double> queryEmbedding, int limit) throws ApplicationException {
        List<SimilarityResult> results = new ArrayList<>();
        try {
            if (queryEmbedding == null) {
                System.err.println("Error: Query embedding is null");
                return results;
            }

            System.out.println("Finding similar documents for query embedding with dimension: " + queryEmbedding.size());

            // Get all embeddings from database
            DocumentEmbedding embeddingModel = new DocumentEmbedding();
            // Use a valid SQL condition to find all records
            Table embeddingTable = embeddingModel.findAll();  // Find all records

            if (embeddingTable == null || embeddingTable.isEmpty()) {
                System.out.println("No embeddings found in database");
                return results;
            }

            System.out.println("Found " + embeddingTable.size() + " embeddings in database");

            // Calculate similarity for each embedding
            int processedCount = 0;
            int errorCount = 0;

            for (int i = 0; i < embeddingTable.size(); i++) {
                try {
                    DocumentEmbedding embeddingData = new DocumentEmbedding();
                    embeddingData.setData(embeddingTable.get(i));
                    String fragmentId = embeddingData.getFragmentId();

                    Vector<Double> storedEmbedding = embeddingData.getEmbeddingVector();
                    if (storedEmbedding == null) {
                        System.err.println("Warning: Null embedding vector for fragment ID: " + fragmentId);
                        errorCount++;
                        continue;
                    }

                    // Verify vector dimensions match
                    if (storedEmbedding.size() != queryEmbedding.size()) {
                        System.err.println("Warning: Vector dimension mismatch. Query: " + queryEmbedding.size() +
                                ", Stored: " + storedEmbedding.size() + " for fragment ID: " + fragmentId);
                        errorCount++;
                        continue;
                    }

                    // Calculate cosine similarity
                    double similarity = cosineSimilarity(queryEmbedding, storedEmbedding);

                    // Get corresponding document fragment
                    DocumentFragment fragment = new DocumentFragment();
                    Table fragmentTable = fragment.findWith("WHERE id = ?", new Object[]{fragmentId});

                    if (fragmentTable == null || fragmentTable.isEmpty()) {
                        System.err.println("Warning: No document fragment found for fragment ID: " + fragmentId);
                        errorCount++;
                        continue;
                    }

                    DocumentFragment fragmentData = new DocumentFragment();
                    fragmentData.setData(fragmentTable.get(0));

                    results.add(new SimilarityResult(fragmentData, similarity));
                    processedCount++;
                } catch (Exception e) {
                    System.err.println("Error processing embedding at index " + i + ": " + e.getMessage());
                    e.printStackTrace();
                    errorCount++;
                }
            }

            System.out.println("Processed " + processedCount + " embeddings successfully, " +
                    errorCount + " errors, found " + results.size() + " results");

            // Sort by similarity score and limit results
            results.sort((a, b) -> Double.compare(b.similarity, a.similarity));
            if (results.size() > limit) {
                results = results.subList(0, limit);
            }

            System.out.println("Returning " + results.size() + " most similar documents");
        } catch (Exception e) {
            System.err.println("Failed to find similar documents: " + e.getMessage());
            e.printStackTrace();
            throw new ApplicationException("Failed to find similar documents: " + e.getMessage(), e);
        }

        return results;
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    private static double cosineSimilarity(Vector<Double> v1, Vector<Double> v2) {
        if (v1.size() != v2.size()) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            norm1 += v1.get(i) * v1.get(i);
            norm2 += v2.get(i) * v2.get(i);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Initialize for an application once it's loaded.
     */
    @Override
    public void init() {
        this.setTemplateRequired(false);
    }

    /**
     * Return the version of the application.
     *
     * @return version
     */
    @Override
    public String version() {
        return "";
    }

    /**
     * Helper class to hold similarity search results
     */
    public static class SimilarityResult {
        public final DocumentFragment fragment;
        public final double similarity;

        public SimilarityResult(DocumentFragment fragment, double similarity) {
            this.fragment = fragment;
            this.similarity = similarity;
        }
    }

    /**
     * Generate embedding for a query string
     *
     * @param query The query string
     * @return The embedding vector
     */
    public Vector<Double> generateQueryEmbedding(String query) throws ApplicationException {
        try {
            // Check if the query embedding is already in the cache
            if (queryEmbeddingCache.containsKey(query)) {
                return queryEmbeddingCache.get(query);
            }

            // Generate new embedding
            Vector<Double> embedding = callOpenAIEmbeddingAPI(query);

            // Cache the result for future use
            addToCache(query, embedding);

            return embedding;
        } catch (Exception e) {
            throw new ApplicationException("Failed to generate query embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Add a query and its embedding to the cache
     * @param query The query string
     * @param embedding The embedding vector
     */
    private static void addToCache(String query, Vector<Double> embedding) {
        // Manage cache size
        if (queryEmbeddingCache.size() >= MAX_CACHE_SIZE) {
            // Simple LRU-like strategy: remove a random entry (first one we encounter)
            String keyToRemove = queryEmbeddingCache.keySet().iterator().next();
            queryEmbeddingCache.remove(keyToRemove);
        }

        // Add new entry to cache
        queryEmbeddingCache.put(query, embedding);
    }

    /**
     * Clear the query embedding cache
     */
    public static void clearCache() {
        queryEmbeddingCache.clear();
    }
}