package custom.ai;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.http.*;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

public class SearchAI extends AbstractApplication implements Provider {
    private static final String SEARCH_URL = "https://lite.duckduckgo.com/lite/";

    @Override
    public Builder call() throws ApplicationException {
        if (this.context.getAttribute("query") == null) {
            throw new ApplicationException("query is required");
        }
        String contentType = "application/x-www-form-urlencoded";
        String query = this.context.getAttribute("query").toString().trim();

        Headers headers = new Headers();
        headers.add(Header.CONTENT_TYPE.set(contentType));
        headers.add(Header.USER_AGENT.set("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.13; rv:109.0) Gecko/20100101 Firefox/111.0"));
        headers.add(Header.ACCEPT.set("text/html,application/xhtml+xml,application/xmlq=0.9,image/avif,image/webp,image/apng,*/*q=0.8,application/signed-exchangev=b3q=0.7"));
        headers.add(Header.ACCEPT_ENCODING.set("gzip, deflate, br"));
        headers.add(Header.REFERER.set("https://lite.duckduckgo.com/"));
        headers.add(Header.HOST.set("lite.duckgo.com"));
        headers.add(Header.ORIGIN.set("https://lite.duckgo.com"));
        headers.add(Header.CONNECTION.set("keep-alive"));

        HttpRequestBuilder builder = new HttpRequestBuilder();
        builder.setHeaders(headers).setMethod(Method.POST);

        try {
            builder.setParameter("q", URLEncoder.encode(query, "UTF-8"));
            builder.setParameter("kl", "us-en");
            URLRequest request = new URLRequest(new URL(SEARCH_URL));
            byte[] bytes = request.send(builder);
            String response = new String(bytes);
            Builder apiResponse = new Builder();
            apiResponse.parse(response);

            return apiResponse;
        } catch (MalformedURLException | UnsupportedEncodingException e) {
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
        this.setAction("search", "call");
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
