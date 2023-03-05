package custom.ai;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.http.*;
import org.tinystruct.transfer.http.upload.ContentDisposition;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;

public class StabilityAI extends AbstractApplication implements Provider {
    public Builder call() throws ApplicationException {
        if (this.context.getAttribute("api") == null) {
            throw new ApplicationException("API is required");
        }
        Builder payload = null;
        String contentType = "application/json";
        Object image = null;
        if (this.context.getAttribute("content-type") != null && this.context.getAttribute("content-type").toString().equalsIgnoreCase("multipart/form-data")) {
            contentType = "multipart/form-data";

            if ((image = this.context.getAttribute("image")) != null && image.toString().startsWith("data:image/png;base64,")) {
                image = image.toString().substring("data:image/png;base64,".length());
            }
        } else if (this.context.getAttribute("payload") == null) {
            throw new ApplicationException("Payload is required");
        }

        if (this.context.getAttribute("payload") != null) {
            payload = (Builder) this.context.getAttribute("payload");
        }

        String api = this.context.getAttribute("api").toString();

        // Replace YOUR_API_KEY with your actual API key
        String API_KEY = this.config.get("stability.api_key");

        Headers headers = new Headers();
        headers.add(Header.AUTHORIZATION.set("Bearer " + API_KEY));
        headers.add(Header.CONTENT_TYPE.set(contentType));
        headers.add(Header.ACCEPT.set("application/json"));

        HttpRequestBuilder builder = new HttpRequestBuilder();
        builder.setVersion(Version.HTTP1_1);
        builder.setHeaders(headers).setMethod(Method.POST);

        if (!contentType.equalsIgnoreCase("multipart/form-data")) {
            assert payload != null;
            builder.setRequestBody(payload.toString());
        }

        builder.setParameter("text_prompts[0][text]", payload.get("text_prompts[0][text]").toString());
        builder.setParameter("cfg_scale", Float.parseFloat(payload.get("cfg_scale").toString()));
        builder.setParameter("clip_guidance_preset", payload.get("clip_guidance_preset").toString());
        builder.setParameter("height", Integer.parseInt(payload.get("height").toString()));
        builder.setParameter("width", Integer.parseInt(payload.get("width").toString()));
        builder.setParameter("samples", Integer.parseInt(payload.get("samples").toString()));
        builder.setParameter("steps", Integer.parseInt(payload.get("steps").toString()));

        if (image != null) {
            ContentDisposition imageContent = new ContentDisposition("init_image", "image.png", "image/png", Base64.getDecoder().decode(image.toString()));
            builder.setFormData(new ContentDisposition[]{imageContent});
        }

        try {
            URLRequest request = new URLRequest(new URL(this.config.get("stability.host") + "/" + api));
            byte[] bytes = request.send(builder);
            String response = new String(bytes);
            Builder apiResponse = new Builder();
            apiResponse.parse(response);
            return apiResponse;
        } catch (MalformedURLException | URISyntaxException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        } catch (ApplicationException e) {
            throw e;
        }
    }

    /**
     * Initialize for an application once it's loaded.
     */
    @Override
    public void init() {
        this.setAction("stability", "call");
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
