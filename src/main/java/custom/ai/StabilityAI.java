package custom.ai;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.http.*;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class StabilityAI extends AbstractApplication implements Provider {
    public Builder call() throws ApplicationException {
        if(this.context.getAttribute("payload") == null) {
            throw new ApplicationException("Payload is required");
        }

        if(this.context.getAttribute("api") == null) {
            throw new ApplicationException("API is required");
        }
        Builder payload = (Builder) this.context.getAttribute("payload");
        String api = this.context.getAttribute("api").toString();
        // Replace YOUR_API_KEY with your actual API key
        String API_KEY = this.config.get("stability.api_key");

        Headers headers = new Headers();
        headers.add(Header.AUTHORIZATION.set("Bearer " + API_KEY));
        headers.add(Header.CONTENT_TYPE.set("application/json"));
        headers.add(Header.ACCEPT.set("application/json"));
        HttpRequestBuilder builder = new HttpRequestBuilder();
        builder.setVersion(Version.HTTP1_1);

        builder.setHeaders(headers)
                .setMethod(Method.POST).setRequestBody(payload.toString());

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
