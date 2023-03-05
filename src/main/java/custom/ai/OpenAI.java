package custom.ai;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.http.*;
import org.tinystruct.transfer.http.upload.ContentDisposition;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;

public class OpenAI extends AbstractApplication implements Provider {
    public static final String IMAGES_GENERATIONS = "https://api.openai.com/v1/images/generations";
    public static final String IMAGES_EDITS = "https://api.openai.com/v1/images/edits";
    public static final String IMAGES_VARIATIONS = "https://api.openai.com/v1/images/variations";

    public Builder call() throws ApplicationException {
        if (this.context.getAttribute("api") == null) {
            throw new ApplicationException("API is required");
        }
        Builder payload = null;
        String contentType = "application/json";
        Object image = null, mask = null;
        if (this.context.getAttribute("content-type") != null && this.context.getAttribute("content-type").toString().equalsIgnoreCase("multipart/form-data")) {
            contentType = "multipart/form-data";

            if ((image = this.context.getAttribute("image")) != null && image.toString().startsWith("data:image/png;base64,")) {
                image = image.toString().substring("data:image/png;base64,".length());
            }

            if ((mask = this.context.getAttribute("mask")) != null && mask.toString().startsWith("data:image/png;base64,")) {
                mask = mask.toString().substring("data:image/png;base64,".length());
            }
        } else if (this.context.getAttribute("payload") == null) {
            throw new ApplicationException("Payload is required");
        }

        if (this.context.getAttribute("payload") != null) {
            payload = (Builder) this.context.getAttribute("payload");
        }

        String api = this.context.getAttribute("api").toString();

        // Replace YOUR_API_KEY with your actual API key
        String API_KEY = this.config.get("openai.api_key");

        Headers headers = new Headers();
        headers.add(Header.AUTHORIZATION.set("Bearer " + API_KEY));
        headers.add(Header.CONTENT_TYPE.set(contentType));

        HttpRequestBuilder builder = new HttpRequestBuilder();
        builder.setHeaders(headers).setMethod(Method.POST);
        if (!contentType.equalsIgnoreCase("multipart/form-data")) {
            assert payload != null;
            builder.setRequestBody(payload.toString());
        }

        builder.setParameter("prompt", payload.get("prompt").toString());
        builder.setParameter("user", payload.get("user").toString());
        builder.setParameter("n", Integer.parseInt(payload.get("n").toString()));
        builder.setParameter("response_format", "b64_json");

        if (image != null) {
            ContentDisposition imageContent = new ContentDisposition("image", "image.png", "image/png", Base64.getDecoder().decode(image.toString()));
            builder.setFormData(new ContentDisposition[]{imageContent});
            if (mask != null) {
                ContentDisposition maskContent = new ContentDisposition("mask", "mask.png", "image/png", Base64.getDecoder().decode(mask.toString()));
                builder.setFormData(new ContentDisposition[]{maskContent});
            }
        }

        try {
            URLRequest request = new URLRequest(new URL(api));
            byte[] bytes = request.send(builder);
            String response = new String(bytes);
            Builder apiResponse = new Builder();
            apiResponse.parse(response);

            return apiResponse;
        } catch (MalformedURLException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        } catch (ApplicationException e) {
            throw e;
        } catch (URISyntaxException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        }

    }

    /**
     * Initialize for an application once it's loaded.
     */
    @Override
    public void init() {
        this.setAction("openai", "call");
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
