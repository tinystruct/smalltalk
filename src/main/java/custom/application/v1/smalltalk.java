package custom.application.v1;

import org.tinystruct.ApplicationException;
import org.tinystruct.application.Variables;
import org.tinystruct.data.FileEntity;
import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.handler.Reforward;
import org.tinystruct.http.*;
import org.tinystruct.system.cli.CommandOption;
import org.tinystruct.system.template.variable.Variable;
import org.tinystruct.system.util.Matrix;
import org.tinystruct.transfer.DistributedMessageQueue;

import javax.activation.MimetypesFileTypeMap;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import static org.tinystruct.http.Constants.*;

public class smalltalk extends DistributedMessageQueue implements SessionListener {

    private boolean cli_mode;

    public void init() {
        super.init();

        this.setAction("talk", "index");
        this.setAction("talk/join", "join");
        this.setAction("talk/start", "start");
        this.setAction("talk/save", "save");
        this.setAction("talk/update", "update");
        this.setAction("talk/upload", "upload");
        this.setAction("talk/command", "command");
        this.setAction("talk/topic", "topic");
        this.setAction("talk/matrix", "matrix");
        this.setAction("talk/chatbot", "chatGPT");
        this.setAction("files", "download");
        this.setAction("chat", "chat");
        this.commandLines.get("chat").setDescription("Chat with ChatGPT in command-line.");

        this.setVariable("message", "");
        this.setVariable("topic", "");

//      set env with LANG=en_US.UTF-8
        System.setProperty("LANG", "en_US.UTF-8");

        SessionManager.getInstance().addListener(this);
    }

    public smalltalk index() {
        final Request request = (Request) this.context.getAttribute(HTTP_REQUEST);
        Object meetingCode = request.getSession().getAttribute("meeting_code");

        if (meetingCode == null) {
            meetingCode = java.util.UUID.randomUUID().toString();
            request.getSession().setAttribute("meeting_code", meetingCode);

            System.out.println("New meeting generated:" + meetingCode);
        }

        List<String> session_ids;
        final String sessionId = request.getSession().getId();
        if (this.groups.get(meetingCode) == null) {
            this.groups.put(meetingCode.toString(), new ArrayBlockingQueue<Builder>(DEFAULT_MESSAGE_POOL_SIZE));
        }

        // If the current user is not in the list of the sessions, we create a default session list for the meeting generated.
        if ((session_ids = this.sessions.get(meetingCode)) == null) {
            this.sessions.put(meetingCode.toString(), session_ids = new ArrayList<String>());
        }

        if (!session_ids.contains(sessionId))
            session_ids.add(sessionId);

        if (!this.list.containsKey(sessionId)) {
            this.list.put(sessionId, new ArrayDeque<Builder>());
        }

        this.setVariable("meeting_code", meetingCode.toString());
        this.setVariable("session_id", request.getSession().getId());

        Variable<?> topic;
        if ((topic = Variables.getInstance().get("{%" + meetingCode + "%}")) != null) {
            this.setVariable("topic", topic.getValue().toString().replaceAll("[\r\n]", "<br />"), true);
        } else {
            this.setVariable("topic", "");
        }

        return this;
    }

    public String save(String groupId, String sessionid, String message) {
        return this.put(groupId, sessionid, message);
    }

    public String update(String sessionId) throws ApplicationException {
        return this.take(sessionId);
    }

    public String matrix() throws ApplicationException {
        final Request request = (Request) this.context.getAttribute(HTTP_REQUEST);
        System.out.println("this.getLink(\"talk/join\") = " + this.getLink("talk/join"));
        if (request.getParameter("meeting_code") != null) {
            BufferedImage qrImage = Matrix.toQRImage(this.getLink("talk/join") + "/" + request.getParameter("meeting_code"), 100, 100);
            return "data:image/png;base64," + Matrix.getBase64Image(qrImage);
        }

        return "";
    }

    public Object join(String meetingCode) throws ApplicationException {
        if (groups.containsKey(meetingCode)) {
            final Request request = (Request) this.context.getAttribute(HTTP_REQUEST);
            final Response response = (Response) this.context.getAttribute(HTTP_RESPONSE);
            request.getSession().setAttribute("meeting_code", meetingCode);

            this.setVariable("meeting_code", meetingCode);

            Reforward reforward = new Reforward(request, response);
            reforward.setDefault("/?q=talk");
            return reforward.forward();
        } else {
            final Response response = (Response) this.context.getAttribute(HTTP_RESPONSE);
            response.setStatus(ResponseStatus.NOT_FOUND);
            return "Invalid meeting code.";
        }
    }

    public Object start(String name) throws ApplicationException {
        final Request request = (Request) this.context.getAttribute(HTTP_REQUEST);
        final Response response = (Response) this.context.getAttribute(HTTP_RESPONSE);
        request.getSession().setAttribute("user", name);

        Object meetingCode = request.getSession().getAttribute("meeting_code");
        if (meetingCode == null) {
            Reforward reforward = new Reforward(request, response);
            reforward.setDefault("/?q=talk");
            return reforward.forward();
        } else {
            this.setVariable("meeting_code", meetingCode.toString());
        }

        return name;
    }

    public String command() {
        final Request request = (Request) this.context.getAttribute(HTTP_REQUEST);
        final Object meetingCode = request.getSession().getAttribute("meeting_code");
        final String sessionId = request.getSession().getId();
        final Response response = (Response) this.context.getAttribute(HTTP_RESPONSE);

        if (meetingCode != null && sessions.get(meetingCode) != null && sessions.get(meetingCode).contains(sessionId)) {
            if (request.getSession().getAttribute("user") == null) {
                response.setStatus(ResponseStatus.UNAUTHORIZED);
                return "{ \"error\": \"missing user\" }";
            }

            Builder builder = new Builder();
            builder.put("user", request.getSession().getAttribute("user"));
            builder.put("cmd", request.getParameter("cmd"));

            return this.save(meetingCode, builder);
        }
        response.setStatus(ResponseStatus.UNAUTHORIZED);
        return "{ \"error\": \"expired\" }";
    }

    public String save() {
        final Request request = (Request) this.context.getAttribute(HTTP_REQUEST);
        final Object meetingCode = request.getSession().getAttribute("meeting_code");
        if (this.groups.containsKey(meetingCode)) {
            final String sessionId = request.getSession().getId();
            if (meetingCode != null && sessions.get(meetingCode) != null && sessions.get(meetingCode).contains(sessionId)) {
                String message;
                if ((message = request.getParameter("text")) != null && !message.isEmpty()) {

                    String[] agent = request.headers().get(Header.USER_AGENT).toString().split(" ");
                    this.setVariable("browser", agent[agent.length - 1]);

                    final SimpleDateFormat format = new SimpleDateFormat("yyyy-M-d h:m:s");
                    final Builder builder = new Builder();
                    builder.put("user", request.getSession().getAttribute("user"));
                    builder.put("time", format.format(new Date()));
                    builder.put("message", filter(message));
                    builder.put("session_id", sessionId);

                    return this.save(meetingCode, builder);
                }
            }
        }

        final Response response = (Response) this.context.getAttribute(HTTP_RESPONSE);
        response.setStatus(ResponseStatus.REQUEST_TIMEOUT);
        return "{ \"error\": \"expired\" }";
    }

    /**
     * Call chat GPT API
     *
     * @return message from API
     * @throws ApplicationException
     */
    public String chatGPT() throws ApplicationException {
        final Request request = (Request) this.context.getAttribute(HTTP_REQUEST);
        final Object meetingCode = request.getSession().getAttribute("meeting_code");
        if (this.groups.containsKey(meetingCode)) {
            final String sessionId = request.getSession().getId();
            if (meetingCode != null && sessions.get(meetingCode) != null && sessions.get(meetingCode).contains(sessionId)) {
                String message;
                if ((message = request.getParameter("text")) != null && !message.isEmpty()) {
                    return chat(sessionId, message);
                }
            }
        }
        return "{}";
    }

    public void chat() {
        this.cli_mode = true;
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to user smalltalk, you can type your questions or quite by type `exit`.");

        String sessionId = UUID.randomUUID().toString();
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-M-d h:m:s");

        while (true) {
            System.out.print(String.format("%s >: ", format.format(new Date())));
            String input = scanner.nextLine();

            if (input.equals("exit")) {
                System.out.println("Exiting...");
                System.exit(-1);
                break;
            } else {
                try {
                    String message = this.chat(sessionId, "\n\n" + input.replaceAll("\n", "") + "\n");
                    message = message.replaceAll("\\\\n", "\n").replaceAll("\\\\\"", "\"");

                    System.out.println(message);
                } catch (ApplicationException e) {
                    e.printStackTrace();
                }
            }
        }

        scanner.close();
    }

    private String chat(String sessionId, String message) throws ApplicationException {
        // Replace YOUR_API_KEY with your actual API key
        String API_KEY = this.config.get("chatGPT.api_key");
        String API_URL = this.config.get("chatGPT.api_endpoint");
        Headers headers = new Headers();
        headers.add(Header.AUTHORIZATION.set("Bearer " + API_KEY));
        headers.add(Header.CONTENT_TYPE.set("application/json"));

        if (!cli_mode)
            message = message.replaceAll("<br>|<br />", "");

        String payload = "{\n" +
                "  \"model\": \"text-davinci-003\"," +
                "  \"prompt\": \"\"," +
                "  \"max_tokens\": 2500," +
                "  \"temperature\": 0," +
                "  \"n\":1" +
                "}";

        Builder _message = new Builder();
        _message.parse(payload);
        if (this.getVariable("previous") != null) {
            _message.put("prompt", this.getVariable("previous").getValue() + "\\\\n" + message);
            _message.put("stop", "\\\\n");
        } else {
            _message.put("prompt", message);
        }
        _message.put("user", sessionId);

        this.setVariable("previous", message);

        HttpRequestBuilder builder = new HttpRequestBuilder();
        builder.setHeaders(headers)
                .setMethod(Method.POST).setRequestBody(_message +"\n");

        URLRequest _request;
        byte[] bytes;
        try {
            _request = new URLRequest(new URL(API_URL));
            bytes = _request.send(builder);
            String response = new String(bytes);
            Builder tokens = new Builder();
            tokens.parse(response);

            Builders builders;
            if (tokens.get("choices") != null) {
                builders = (Builders) tokens.get("choices");

                if (builders.get(0).size() > 0) {
                    Builder choice = builders.get(0);

                    final SimpleDateFormat format = new SimpleDateFormat("yyyy-M-d h:m:s");
                    final Builder data = new Builder();
                    data.put("user", "ChatGPT");
                    data.put("time", format.format(new Date()));
                    data.put("message", filter(choice.get("text").toString()));

                    if (cli_mode)
                        return String.format("%s %s >: %s", data.get("time"), data.get("user"), choice.get("text"));
                    else
                        return data.toString();
                }
            }
        } catch (URISyntaxException | MalformedURLException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        }
        return "";
    }

    public String update() throws ApplicationException, IOException {
        final Request request = (Request) this.context.getAttribute(HTTP_REQUEST);
        final Object meetingCode = request.getSession().getAttribute("meeting_code");
        final String sessionId = request.getSession().getId();
        if (meetingCode != null) {
            return this.update(meetingCode.toString(), sessionId);
        }

        final Response response = (Response) this.context.getAttribute(HTTP_RESPONSE);
        response.setStatus(ResponseStatus.REQUEST_TIMEOUT);

        return "{ \"error\": \"expired\" }";
    }

    public String update(String meetingCode, String sessionId) throws ApplicationException, IOException {
        String error = "{ \"error\": \"expired\" }";
        if (this.groups.containsKey(meetingCode)) {
            List<String> list;
            if ((list = sessions.get(meetingCode)) != null && list.contains(sessionId)) {
                return this.take(sessionId);
            }

            error = "{ \"error\": \"session-timeout\" }";
        }

        final Response response = (Response) this.context.getAttribute(HTTP_RESPONSE);
        response.setStatus(ResponseStatus.REQUEST_TIMEOUT);
        return error;
    }

    public String upload() throws ApplicationException {
        final Request request = (Request) this.context.getAttribute(HTTP_REQUEST);
        final Object meetingCode = request.getSession().getAttribute("meeting_code");
        if (meetingCode == null) throw new ApplicationException("Not allowed to upload any files.");

        // Create path components to save the file
        final String path = this.config.get("system.directory") != null ? this.config.get("system.directory").toString() + "/files" : "files";

        final Builders builders = new Builders();
        List<FileEntity> list = request.getAttachments();
        for (FileEntity file : list) {
            final Builder builder = new Builder();
            builder.put("type", file.getContentType());
            builder.put("file", new StringBuilder().append(this.context.getAttribute(HTTP_HOST)).append("files/").append(file.getFilename()));
            final File f = new File(path + File.separator + file.getFilename());
            if (!f.exists()) {
                if (!f.getParentFile().exists()) {
                    f.getParentFile().mkdirs();
                }
            }

            try (final OutputStream out = new FileOutputStream(f);
                 final BufferedOutputStream bout = new BufferedOutputStream(out);
                 final BufferedInputStream bs = new BufferedInputStream(new ByteArrayInputStream(file.get()));
            ) {
                final byte[] bytes = new byte[1024];
                byte[] keys = meetingCode.toString().getBytes(StandardCharsets.UTF_8);
                int read;
                while ((read = bs.read(bytes)) != -1) {
                    for (int i = 0; i < keys.length; i++) {
                        bytes[i] = (byte) (bytes[i] ^ keys[i]);
                    }
                    bout.write(bytes, 0, read);
                }

                bout.close();
                bs.close();

                builders.add(builder);
                System.out.printf("File %s being uploaded to %s%n", file.getFilename(), path);
            } catch (FileNotFoundException e) {
                throw new ApplicationException(e.getMessage(), e);
            } catch (IOException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
        }

        return builders.toString();
    }

    public byte[] download(String fileName, boolean encoded) throws ApplicationException {
        final Request request = (Request) this.context.getAttribute(HTTP_REQUEST);
        final Response response = (Response) this.context.getAttribute(HTTP_RESPONSE);

        final Object meetingCode = request.getSession().getAttribute("meeting_code");
        if (encoded && meetingCode == null) throw new ApplicationException("Not allowed to download any files.");

        // Create path to download the file
        final String fileDir = this.config.get("system.directory") != null ? this.config.get("system.directory") + "/files" : "files";

        // Creating an object of Path class and
        // assigning local directory path of file to it
        Path path = Paths.get(fileDir, new String(fileName.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8));

        // Converting the file into a byte array
        // using Files.readAllBytes() method
        byte[] arr = new byte[0];
        try {
            String mimeType = new MimetypesFileTypeMap().getContentType(path.toFile());
            if (mimeType != null)
                response.addHeader(Header.CONTENT_TYPE.toString(), mimeType);
            else
                response.addHeader(Header.CONTENT_DISPOSITION.toString(), "application/octet-stream;filename=\"" + fileName + "\"");

            arr = Files.readAllBytes(path);
            if (encoded) {
                byte[] keys = meetingCode.toString().getBytes(StandardCharsets.UTF_8);
                for (int i = 0; i < arr.length; i = i + 1024) {
                    for (int j = 0; j < keys.length; j++) {
                        arr[i + j] = (byte) (arr[i + j] ^ keys[j]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return arr;
    }

    public byte[] download(String fileName) throws ApplicationException {
        return this.download(fileName, true);
    }

    public boolean topic() {
        final Request request = (Request) this.context.getAttribute(HTTP_REQUEST);
        final Object meeting_code = request.getSession().getAttribute("meeting_code");

        if (meeting_code != null && request.getParameter("topic") != null) {
            this.setSharedVariable(meeting_code.toString(), filter(request.getParameter("topic")));
            return true;
        }

        return false;
    }

    protected smalltalk exit() {
        final Request request = (Request) this.context.getAttribute(HTTP_REQUEST);
        request.getSession().removeAttribute("meeting_code");
        return this;
    }

    @Override
    protected String filter(String text) {
        text = text.replaceAll("<script(.*)>(.*)<\\/script>", "");
        text = text.replaceAll("\\\\n\\\\n", "<br />");
        text = text.replaceAll("\\\\n", "<br />");
        return text;
    }

    @Override
    public void onSessionEvent(SessionEvent arg0) {
        Object meetingCode = arg0.getSession().getAttribute("meeting_code");
        if (arg0.getType() == SessionEvent.Type.CREATED) {
            if (meetingCode == null) {
                meetingCode = java.util.UUID.randomUUID().toString();
                arg0.getSession().setAttribute("meeting_code", meetingCode);

                System.out.println("New meeting generated by HttpSessionListener:" + meetingCode);
            }

            final String sessionId = arg0.getSession().getId();
            if (!this.list.containsKey(sessionId)) {
                this.list.put(sessionId, new ArrayDeque<Builder>());
            }
        } else if (arg0.getType() == SessionEvent.Type.DESTROYED) {
            if (meetingCode != null) {
                final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                final Builder builder = new Builder();
                builder.put("user", null);
                builder.put("time", format.format(new Date()));
                builder.put("cmd", "expired");
                this.save(meetingCode, builder);

                Queue<Builder> messages;
                List<String> session_ids;
                if ((session_ids = this.sessions.get(meetingCode)) != null) {
                    session_ids.remove(arg0.getSession().getId());
                }

                if ((messages = this.groups.get(meetingCode)) != null) {
                    messages.remove(meetingCode);
                }

                final String sessionId = arg0.getSession().getId();
                if (this.list.containsKey(sessionId)) {
                    this.list.remove(sessionId);
                    wakeup();
                }
            }
        }
    }
}
