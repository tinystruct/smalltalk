package custom.ai;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.net.URLHandler;
import org.tinystruct.net.URLHandlerFactory;
import org.tinystruct.net.URLRequest;
import org.tinystruct.net.URLResponse;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.transfer.http.upload.ContentDisposition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Consumer;

public class OpenAI extends AbstractApplication implements Provider {
    public static final String API_ENDPOINT = "https://openrouter.ai/api";
    public static final String CHAT_COMPLETIONS = "/v1/chat/completions";
    public static final String IMAGES_GENERATIONS = "/v1/images/generations";
    public static final String IMAGES_EDITS = "/v1/images/edits";
    public static final String IMAGES_VARIATIONS = "/v1/images/variations";

    @Action("openai")
    public Builder call() throws ApplicationException {
        if (getContext().getAttribute("api") == null) {
            throw new ApplicationException("API is required");
        }
        Builder payload = null;
        String contentType = "application/json";
        Object image = null, mask = null;
        if (getContext().getAttribute("content-type") != null && getContext().getAttribute("content-type").toString().equalsIgnoreCase("multipart/form-data")) {
            contentType = "multipart/form-data";

            if ((image = getContext().getAttribute("image")) != null && image.toString().startsWith("data:image/png;base64,")) {
                image = image.toString().substring("data:image/png;base64,".length());
            }

            if ((mask = getContext().getAttribute("mask")) != null && mask.toString().startsWith("data:image/png;base64,")) {
                mask = mask.toString().substring("data:image/png;base64,".length());
            }
        } else if (getContext().getAttribute("payload") == null) {
            throw new ApplicationException("Payload is required");
        }

        String api = getContext().getAttribute("api").toString();

        // Get API key from configuration or environment variable
        String API_KEY = getConfiguration().get("openai.api_key");

        // Check if API key is configured or using placeholder
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

        try {
            URLRequest request = new URLRequest(new URL(api));
            request.setHeader("Authorization", "Bearer " + API_KEY);
            request.setHeader("Content-Type", contentType);
            request.setHeader("Accept", "application/json");

            // Add OpenRouter specific headers
            request.setHeader("Referer", "https://github.com/tinystruct/smalltalk");
            request.setHeader("User-Agent", "Smalltalk/1.0.0");

            if (getContext().getAttribute("payload") != null) {
                payload = (Builder) getContext().getAttribute("payload");
                if (contentType.equalsIgnoreCase("multipart/form-data")) {
                    request.setParameter("prompt", payload.get("prompt").toString());
                    request.setParameter("user", payload.get("user").toString());
                    request.setParameter("n", Integer.parseInt(payload.get("n").toString()));
                    request.setParameter("response_format", "b64_json");
                } else {
                    request.setBody(payload.toString());
                }
            }

            if (image != null) {
                ContentDisposition imageContent = new ContentDisposition("image", "image.png", "image/png", Base64.getDecoder().decode(image.toString()));
                request.setFormData(new ContentDisposition[]{imageContent});
                if (mask != null) {
                    ContentDisposition maskContent = new ContentDisposition("mask", "mask.png", "image/png", Base64.getDecoder().decode(mask.toString()));
                    request.setFormData(new ContentDisposition[]{maskContent});
                }
            }

            URLHandler handler = URLHandlerFactory.getHandler(new URL(api));
            URLResponse urlResponse = handler.handleRequest(request);
            String response = urlResponse.getBody();

            // Check if response looks like HTML
            if (response.trim().startsWith("<!DOCTYPE html>") || response.trim().startsWith("<html>")) {
                throw new ApplicationException("Received HTML response instead of JSON. API endpoint may be incorrect or returning an error page.");
            }

            Builder apiResponse = new Builder();
            apiResponse.parse(response);
            return apiResponse;
        } catch (MalformedURLException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        } catch (ApplicationException e) {
            throw e;
        }
    }

    /**
     * Stream chat completion responses from OpenAI API
     *
     * @param payload The request payload
     * @param chunkHandler A consumer function to handle each chunk of the response
     * @throws ApplicationException If an error occurs during the API call
     */
    public void streamChatCompletion(Builder payload, Consumer<Builder> chunkHandler) throws ApplicationException {
        if (payload == null) {
            throw new ApplicationException("Payload is required for streaming");
        }

        // Ensure stream parameter is set to true
        if (payload.get("stream") == null || !payload.get("stream").toString().equals("true")) {
            payload.put("stream", true);
        }

        String API_URL = getConfiguration().get("openai.api_endpoint");
        if (API_URL == null || API_URL.trim().isEmpty()) {
            API_URL = API_ENDPOINT;
        }

        API_URL = API_URL + CHAT_COMPLETIONS;

        // Get API key from configuration or environment variable
        String API_KEY = getConfiguration().get("openai.api_key");

        // Check if API key is configured or using placeholder
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

        HttpURLConnection connection = null;
        try {
            // Create connection
            URL url = new URL(API_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "text/event-stream");
            connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
            connection.setRequestProperty("Referer", "https://github.com/tinystruct/smalltalk");
            connection.setRequestProperty("User-Agent", "Smalltalk/1.0.0");
            connection.setDoOutput(true);
            connection.setChunkedStreamingMode(0);

            // Send request
            connection.getOutputStream().write(payload.toString().getBytes(StandardCharsets.UTF_8));

            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new ApplicationException("Failed to stream from OpenAI API: HTTP error code " + responseCode);
            }

            // Process streaming response
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder eventData = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    // Skip empty lines
                    if (line.trim().isEmpty()) {
                        // End of event, process the data if we have any
                        if (eventData.length() > 0) {
                            String data = eventData.toString().trim();
                            if (data.startsWith("data: ")) {
                                data = data.substring(6).trim();

                                // Skip [DONE] message
                                if (data.equals("[DONE]")) {
                                    continue;
                                }

                                try {
                                    // Parse the JSON data
                                    Builder chunkBuilder = new Builder();
                                    chunkBuilder.parse(data);

                                    // Pass the chunk to the handler
                                    chunkHandler.accept(chunkBuilder);
                                } catch (Exception e) {
                                    System.err.println("Error parsing chunk: " + e.getMessage());
                                    System.err.println("Raw chunk data: " + data);
                                }
                            }

                            // Reset for next event
                            eventData.setLength(0);
                        }
                    } else if (line.startsWith("data: ")) {
                        // Accumulate data lines
                        eventData.append(line).append("\n");
                    }
                }
            }
        } catch (IOException e) {
            throw new ApplicationException("Error streaming from OpenAI API: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Initialize for an application once it's loaded.
     */
    @Override
    public void init() {

    }

    /**
     * Return the version of the application.
     *
     * @return version
     */
    @Override
    public String version() {
        return null;
    }
}
