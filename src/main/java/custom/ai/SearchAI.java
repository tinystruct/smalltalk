package custom.ai;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.http.*;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchAI extends AbstractApplication implements Provider {
    private static final String SEARCH_URL = "https://lite.duckduckgo.com/lite/";
    private static final String REGEX_PATTERN = "(?i)\\b((?:https?:\\/\\/|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}\\/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))*(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s\\W`!()\\[\\]{};:'\\\".,<>?«»“”‘’]))";

    @Override
    public Builder call() throws ApplicationException {
        if (this.context.getAttribute("--query") == null) {
            throw new ApplicationException("query is required");
        }
        String query = this.context.getAttribute("--query").toString().trim();

        HttpRequestBuilder builder = new HttpRequestBuilder();
        Headers headers = new Headers();
        headers.add(Header.USER_AGENT.set("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.13; rv:109.0) Gecko/20100101 Firefox/111.0"));
        headers.add(Header.ACCEPT.set("text/html,application/xhtml+xml,application/xmlq=0.9,image/avif,image/webp,image/apng,*/*q=0.8,application/signed-exchangev=b3q=0.7"));
        headers.add(Header.ACCEPT_ENCODING.set("gzip, deflate, br"));

        String url = SEARCH_URL;
        List<String> urls;
        if ((urls = this.extractUrls(query)) != null && urls.size() > 0) {
            builder.setHeaders(headers).setMethod(Method.GET);
            url = urls.get(0);
        } else {
            String contentType = "application/x-www-form-urlencoded";
            headers.add(Header.CONTENT_TYPE.set(contentType));
            headers.add(Header.REFERER.set("https://lite.duckduckgo.com/"));
            headers.add(Header.HOST.set("lite.duckduckgo.com"));
            headers.add(Header.ORIGIN.set("https://lite.duckduckgo.com"));
            headers.add(Header.CONNECTION.set("keep-alive"));

            builder.setHeaders(headers).setMethod(Method.POST);
            builder.setParameter("q", query + "" + LocalDateTime.now().getYear() + "-" + LocalDateTime.now().getMonthValue());
            builder.setParameter("kl", "");
        }

        try {
            URLRequest request = new URLRequest(new URL(url));
            byte[] bytes = request.send(builder);

            // Create a parser delegator
            ParserDelegator parserDelegator = new ParserDelegator();

            // Collect the Top 3 results.
            ArrayList<String> list = new ArrayList<String>();

            HTMLEditorKit.ParserCallback cb = new HTMLEditorKit.ParserCallback() {
                final StringBuffer buffer = new StringBuffer();
                boolean ready = false;
                boolean withDuckDuckGo = false;
                int i = 0;

                @Override
                public void handleComment(char[] data, int pos) {
                    if (list.size() < 3 && new String(data).contains("Web results are present")) {
                        withDuckDuckGo = true;
                    }
                }

                @Override
                public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
                    if ((withDuckDuckGo && list.size() < 3 && t == HTML.Tag.TD)) {
                        ready = true;
                    } else {
                        if (t == HTML.Tag.HTML || t == HTML.Tag.TITLE || t == HTML.Tag.HEAD || t == HTML.Tag.META || t == HTML.Tag.BASE || t == HTML.Tag.LINK || t == HTML.Tag.SCRIPT || t == HTML.Tag.STYLE || t == HTML.Tag.MAP || t == HTML.Tag.FRAMESET) {
                            ready = false;
                        } else if (t == HTML.Tag.BODY || t == HTML.Tag.H1 || t == HTML.Tag.H2 || t == HTML.Tag.H3 || t == HTML.Tag.H4 || t == HTML.Tag.H5 || t == HTML.Tag.H6 || t == HTML.Tag.DIV || t == HTML.Tag.SPAN || t == HTML.Tag.P || t == HTML.Tag.A || t == HTML.Tag.B || t == HTML.Tag.I || t == HTML.Tag.STRONG || t == HTML.Tag.TD || t == HTML.Tag.LI) {
                            ready = true;
                        }
                    }
                }

                @Override
                public void handleEndTag(HTML.Tag t, int pos) {
                    if (withDuckDuckGo && list.size() < 3 && t == HTML.Tag.TD) {
                        if (buffer.length() > 0 && buffer.indexOf("No results.") == -1){
                            if (i == 7) {
                                i = 0;
                                list.add(buffer.toString().replaceAll("\"", "\\\""));
                                buffer.setLength(0);
                            } else {
                                i++;
                            }
                        }
                    }

                    if (t == HTML.Tag.BODY) {
                        list.add(buffer.toString().replaceAll("\"", "\\\""));
                    }

                    ready = false;
                }

                @Override
                public void handleText(char[] data, int pos) {
                    if (withDuckDuckGo && ready && list.size() < 3) {
                        buffer.append(data);
                        buffer.append(" \n");
                    } else if (ready) {
                        buffer.append(data);
                        buffer.append(" \n");
                    }
                }
            };

            // Remove all style tags manually as the HTMLEditorKit not working properly
            String pattern = "<style[^>]*>[\\s\\S]*?<\\/style>";
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(new String(bytes));

            String html = matcher.replaceAll("");

            // Parse the HTML document
            parserDelegator.parse(new StringReader(html), cb, true);

            Builder apiResponse = new Builder();
            if (list.size() > 0)
                apiResponse.put("data", list);

            return apiResponse;
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        } catch (ApplicationException e) {
            throw e;
        } catch (URISyntaxException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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

    public List<String> extractUrls(String text) {
        Matcher matcher = Pattern.compile(REGEX_PATTERN).matcher(text);
        List<String> urls = new ArrayList<>();
        while (matcher.find()) {
            urls.add(matcher.group());
        }
        return urls;
    }
}

class Result {
    String id;
    String title;
    String description;
    String url;
}
