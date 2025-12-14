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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;

public class StabilityAI extends AbstractApplication implements Provider {

    @Action("stability")
    public Builder call() throws ApplicationException {
        if (getContext().getAttribute("api") == null) {
            throw new ApplicationException("API is required");
        }
        Builder payload;
        String contentType = "application/json";
        Object image = null;
        if (getContext().getAttribute("content-type") != null && getContext().getAttribute("content-type").toString().equalsIgnoreCase("multipart/form-data")) {
            contentType = "multipart/form-data";
            if ((image = getContext().getAttribute("image")) != null && image.toString().startsWith("data:image/png;base64,")) {
                image = image.toString().substring("data:image/png;base64,".length());
            }
        } else if (getContext().getAttribute("payload") == null) {
            throw new ApplicationException("Payload is required");
        }

        String api = getContext().getAttribute("api").toString();

        // Replace YOUR_API_KEY with your actual API key
        String API_KEY = getConfiguration().get("stability.api_key");
        try {
            URLRequest request = new URLRequest(new URL(getConfiguration().get("stability.host") + "/" + api));
            request.setHeader("Authorization", "Bearer " + API_KEY);
            request.setHeader("Content-Type", contentType);
            request.setHeader("Accept", "application/json");
            request.setMethod("POST");

            if (getContext().getAttribute("payload") != null) {
                payload = (Builder) getContext().getAttribute("payload");
                if (contentType.equalsIgnoreCase("multipart/form-data")) {
                    request.setParameter("text_prompts[0][text]", payload.get("text_prompts[0][text]").toString());
                    request.setParameter("cfg_scale", Float.parseFloat(payload.get("cfg_scale").toString()));
                    request.setParameter("clip_guidance_preset", payload.get("clip_guidance_preset").toString());
                    request.setParameter("height", Integer.parseInt(payload.get("height").toString()));
                    request.setParameter("width", Integer.parseInt(payload.get("width").toString()));
                    request.setParameter("samples", Integer.parseInt(payload.get("samples").toString()));
                    request.setParameter("steps", Integer.parseInt(payload.get("steps").toString()));
                } else {
                    request.setBody(payload.toString());
                }
            }

            if (image != null) {
                ContentDisposition imageContent = new ContentDisposition("init_image", "image.png", "image/png", Base64.getDecoder().decode(image.toString()));
                request.setFormData(new ContentDisposition[]{imageContent});
            }

            URLHandler handler = URLHandlerFactory.getHandler(request.getURL());
            URLResponse response = handler.handleRequest(request);
            Builder apiResponse = new Builder();
            apiResponse.parse(response.getBody());
            return apiResponse;
        } catch (MalformedURLException e) {
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
