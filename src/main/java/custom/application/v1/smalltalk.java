package custom.application.v1;

import custom.ai.ImageProcessorType;
import custom.ai.OpenAI;
import custom.ai.StabilityAI;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.application.Context;
import org.tinystruct.application.Variables;
import org.tinystruct.data.FileEntity;
import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.handler.Reforward;
import org.tinystruct.http.*;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.template.variable.Variable;
import org.tinystruct.system.util.Matrix;
import org.tinystruct.transfer.DistributedMessageQueue;

import javax.activation.MimetypesFileTypeMap;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

import static custom.ai.OpenAI.*;
import static org.tinystruct.http.Constants.*;

public class smalltalk extends DistributedMessageQueue implements SessionListener {

    public static final String CHAT_GPT = "ChatGPT";

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-M-d h:m:s");
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
        this.setAction("files", "download");
        this.setAction("chat", "chat");
        this.commandLines.get("chat").setDescription("Chat with ChatGPT in command-line.");

        this.setVariable("message", "");
        this.setVariable("topic", "");

//      set env with LANG=en_US.UTF-8
        System.setProperty("LANG", "en_US.UTF-8");

        SessionManager.getInstance().addListener(this);

        ApplicationManager.install(new OpenAI());
        ApplicationManager.install(new StabilityAI());
    }

    public smalltalk index() {
        final Request request = (Request) this.context.getAttribute(HTTP_REQUEST);
        final Response response = (Response) this.context.getAttribute(HTTP_RESPONSE);

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

        request.headers().add(Header.CACHE_CONTROL.set("no-cache, no-store, max-age=0, must-revalidate"));
        response.headers().add(Header.CACHE_CONTROL.set("no-cache, no-store, max-age=0, must-revalidate"));
        return this;
    }

    public String update(String sessionId) throws ApplicationException {
        return this.take(sessionId);
    }

    public String matrix(String meetingCode) throws ApplicationException {
        final Request request = (Request) this.context.getAttribute(HTTP_REQUEST);
        final Response response = (Response) this.context.getAttribute(HTTP_RESPONSE);

        request.headers().add(Header.CACHE_CONTROL.set("no-cache, no-store, max-age=0, must-revalidate"));
        response.headers().add(Header.CACHE_CONTROL.set("no-cache, no-store, max-age=0, must-revalidate"));
        if (meetingCode != null && meetingCode.length() > 32) {
            BufferedImage qrImage = Matrix.toQRImage(this.getLink("talk/join") + "/" + meetingCode, 100, 100);
            return "data:image/png;base64," + Matrix.getBase64Image(qrImage);
        }

        return "";
    }

    public Object join(String meetingCode) throws ApplicationException {
        if (groups.containsKey(meetingCode)) {
            final Request request = (Request) this.context.getAttribute(HTTP_REQUEST);
            final Response response = (Response) this.context.getAttribute(HTTP_RESPONSE);
            request.getSession().setAttribute("meeting_code", meetingCode);

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
                    if (request.headers().get(Header.USER_AGENT) != null) {
                        String[] agent = request.headers().get(Header.USER_AGENT).toString().split(" ");
                        this.setVariable("browser", agent[agent.length - 1]);
                    }

                    final Builder builder = new Builder();
                    builder.put("user", request.getSession().getAttribute("user"));
                    builder.put("time", format.format(new Date()));
                    builder.put("message", filter(message));
                    builder.put("session_id", sessionId);

                    if (message.contains("@" + CHAT_GPT)) {
                        final String finalMessage = message.replaceAll("@" + CHAT_GPT, "");
                        return this.save(meetingCode, builder, new Runnable() {
                            /**
                             * When an object implementing interface {@code Runnable} is used
                             * to create a thread, starting the thread causes the object's
                             * {@code run} method to be called in that separately executing
                             * thread.
                             * <p>
                             * The general contract of the method {@code run} is that it may
                             * take any action whatsoever.
                             *
                             * @see Thread#run()
                             */
                            @Override
                            public void run() {
                                final SimpleDateFormat format = new SimpleDateFormat("yyyy-M-d h:m:s");
                                final Builder data = new Builder();
                                data.put("user", CHAT_GPT);
                                data.put("session_id", request.getSession().getId());
                                try {
                                    String filterMessage = filter(chat(sessionId, finalMessage));

                                    data.put("time", format.format(new Date()));
                                    data.put("message", filterMessage);
                                    save(meetingCode, data);
                                } catch (ApplicationException e) {
                                    data.put("time", format.format(new Date()));
                                    data.put("message", e.getMessage());
                                    save(meetingCode, data);
                                }
                            }
                        });
                    }

                    return this.save(meetingCode, builder);
                }
            }
        }

        final Response response = (Response) this.context.getAttribute(HTTP_RESPONSE);
        response.setStatus(ResponseStatus.REQUEST_TIMEOUT);
        return "{ \"error\": \"expired\" }";
    }

    public void chat() {
        this.cli_mode = true;
        if (this.config.get("chatGPT.api_key") == null || this.config.get("chatGPT.api_key").isEmpty()) {
            String url = "https://platform.openai.com/account/api-keys";

            Context ctx = new ApplicationContext();
            ctx.setAttribute("--url", url);
            try {
                ApplicationManager.call("open", ctx);
            } catch (ApplicationException e) {
                e.printStackTrace();
            }

            Console console = System.console();
            String prompt = "Enter your ChatGPT Secret Key: ";

            if (console != null) {
                char[] chars;
                while ((chars = console.readPassword(prompt)) == null || chars.length == 0) ;
                this.config.set("chatGPT.api_key", new String(chars));
            } else {
                throw new ApplicationRuntimeException("chatGPT.api_key is required.");
            }
        }
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to use smalltalk, you can type your questions or quit by type `exit`.");

        String sessionId = UUID.randomUUID().toString();
        while (true) {
            System.out.printf("%s >: ", format.format(new Date()));
            String input = scanner.nextLine();

            if (input.equals("exit")) {
                System.out.println("Exiting...");
                System.exit(-1);
                break;
            } else {
                try {
                    if (input.trim().length() > 0) {
                        String message = this.chat(sessionId, input.replaceAll("\n", " ") + "\n");
                        System.out.print(String.format("%s %s >: ", format.format(new Date()), CHAT_GPT));
                        message = message.replaceAll("\\\\n", "\n").replaceAll("\\\\\"", "\"");
                        if (!message.startsWith("data:image/png;base64,")) {
                            for (int i = 0; i < message.length(); i++) {
                                System.out.print(message.charAt(i));
                                if (message.charAt(i) == ',')
                                    Thread.sleep(777);
                                else
                                    Thread.sleep(ThreadLocalRandom.current().nextInt(7, 77));
                            }
                        } else {
                            System.out.print(message);
                        }

                        System.out.println();
                    }
                } catch (ApplicationException e) {
                    System.out.println(e.getMessage());
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        scanner.close();
    }

    /**
     * Call chat GPT API
     *
     * @return message from API
     * @throws ApplicationException while the failure occurred due to an exception
     */
    private String chat(String sessionId, String message) throws ApplicationException {
        // Replace YOUR_API_KEY with your actual API key
        String API_URL = this.config.get("chatGPT.api_endpoint");

        if (!cli_mode)
            message = message.replaceAll("<br>|<br />", "");

        String payload = "{\n" +
                "  \"model\": \"text-davinci-003\"," +
                "  \"prompt\": \"\"," +
                "  \"max_tokens\": 2500," +
                "  \"temperature\": 0.8," +
                "  \"n\":1" +
                "}";

        Builder _message = new Builder();
        _message.parse(payload);
        _message.put("prompt", "I want you to be a highly intelligent AI assistant，especially in IT. If you get any linux command, please execute it for me and output the result should be show in terminal. Otherwise, you can treat it as a normal conversation, but you should consider the conversation context to answer questions. If some questions you are not good at, please forward the question to the right engine and back with the answer quickly. but if you got any request about image creation, then you just need to return the OpenAI api: https://api.openai.com/v1/images/generations. If it's about image edit, then return: https://api.openai.com/v1/images/edits. If it's about image variations, then return: https://api.openai.com/v1/images/variations\n" +
                "\n" + message + "\n");
        _message.put("user", sessionId);

        Context context = new ApplicationContext();
        context.setAttribute("payload", _message);
        context.setAttribute("api", API_URL);

        Builder apiResponse = (Builder) ApplicationManager.call("openai", context);
        assert apiResponse != null;
        Builders builders;
        if (apiResponse.get("choices") != null) {
            builders = (Builders) apiResponse.get("choices");

            if (builders.get(0).size() > 0) {
                Builder choice = builders.get(0);

                if (choice.get("text") != null) {
                    String choiceText = choice.get("text").toString();
                    if (choiceText.contains(IMAGES_GENERATIONS)) {
                        return this.imageProcessorStability(ImageProcessorType.GENERATIONS, null, sessionId + ":" + message);
                    } else if (choiceText.contains(IMAGES_EDITS)) {
                        return this.imageProcessor(ImageProcessorType.EDITS, "", sessionId + ":" + message);
                    } else if (choiceText.contains(IMAGES_VARIATIONS)) {
                        return this.imageProcessor(ImageProcessorType.VARIATIONS, "", sessionId + ":" + message);
                    }

                    return choiceText;
                }
            }
        } else if (apiResponse.get("error") != null) {
            Builder error = (Builder) apiResponse.get("error");
            if (error.get("message") != null) {
                throw new ApplicationException(error.get("message").toString());
            }
        }

        return "";
    }

    /**
     * Process image requests with the given image processor from stability AI.
     *
     * @param imageProcessorType
     * @param image
     * @param message
     * @return image base64 encoded string
     */
    private String imageProcessorStability(ImageProcessorType imageProcessorType, String image, String message) throws ApplicationException {
        Builder _message = new Builder();
        Builders builders;
        Builder apiResponse = null;
        String[] prompt = message.trim().split(":");
        String payload;
        Context context = new ApplicationContext();
        switch (imageProcessorType) {
            case GENERATIONS:
                payload = "{\"text_prompts\": [\n" +
                        "      {\n" +
                        "        \"text\": \"A lighthouse on a cliff\"\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"cfg_scale\": 7,\n" +
                        "    \"clip_guidance_preset\": \"FAST_BLUE\",\n" +
                        "    \"height\": 512,\n" +
                        "    \"width\": 512,\n" +
                        "    \"samples\": 1,\n" +
                        "    \"steps\": 50" +
                        "}";

                try {
                    _message.parse(payload);
                } catch (ApplicationException e) {
                    e.printStackTrace();
                }

                Builders textPrompts = new Builders();
                Builder textBuilder = new Builder();
                textBuilder.put("text", prompt[1]);
                textPrompts.add(textBuilder);

                _message.put("text_prompts", textPrompts);
                context.setAttribute("payload", _message);
                context.setAttribute("api", "v1beta/generation/stable-diffusion-512-v2-1/text-to-image");

                apiResponse = (Builder) ApplicationManager.call("stability", context);
                if (apiResponse.size() > 0) {
                    Builders artifacts = (Builders) apiResponse.get("artifacts");
                    if (artifacts != null && artifacts.size() > 0 && artifacts.get(0).get("base64") != null) {
                        return "data:image/png;base64," + artifacts.get(0).get("base64").toString();
                    }
                }

                return "";
            case EDITS:
                payload = "{\n" +
                        " \"image\": \"\"," +
                        "  \"prompt\": \"\"," +
                        "  \"n\":1," +
                        "  \"response_format\":\"b64_json\"" +
                        "}";
                try {
                    _message.parse(payload);
                } catch (ApplicationException e) {
                    e.printStackTrace();
                }
                _message.put("prompt", prompt[1]);
                _message.put("user", prompt[0]);

                context.setAttribute("payload", _message);
                context.setAttribute("api", IMAGES_GENERATIONS);

                apiResponse = (Builder) ApplicationManager.call("openai", context);
                break;
            case VARIATIONS:
                payload = "{\n" +
                        " \"image\": \"\"," +
                        "  \"prompt\": \"\"," +
                        "  \"n\":1," +
                        "  \"response_format\":\"b64_json\"" +
                        "}";

                try {
                    _message.parse(payload);
                } catch (ApplicationException e) {
                    e.printStackTrace();
                }
                _message.put("prompt", prompt[1]);
                _message.put("user", prompt[0]);

                context.setAttribute("payload", _message);
                context.setAttribute("api", IMAGES_VARIATIONS);

                apiResponse = (Builder) ApplicationManager.call("openai", context);
            default:
                break;
        }

        if (apiResponse != null) {
            if (apiResponse.get("data") != null) {
                builders = (Builders) apiResponse.get("data");
                if (builders.size() > 0 && builders.get(0) != null) {
                    return "data:image/png;base64," + builders.get(0).get("b64_json").toString();
                }
            } else if (apiResponse.get("error") != null) {
                Builder error = (Builder) apiResponse.get("error");
                if (error.get("message") != null) {
                    return error.get("message").toString();
                }
            }
        }

        return "";
    }

    private String imageProcessor(ImageProcessorType imageProcessorType, String image, String message) throws ApplicationException {
        Builder _message = new Builder();
        Builders builders;
        Builder apiResponse = null;
        String[] prompt = message.trim().split(":");
        String payload;
        switch (imageProcessorType) {
            case GENERATIONS:
                payload = "{\n" +
                        "  \"prompt\": \"\"," +
                        "  \"n\":1," +
                        "  \"response_format\":\"b64_json\"" +
                        "}";

                try {
                    _message.parse(payload);
                } catch (ApplicationException e) {
                    e.printStackTrace();
                }

                _message.put("prompt", prompt[1]);
                _message.put("user", prompt[0]);

                context.setAttribute("payload", _message);
                context.setAttribute("api", IMAGES_GENERATIONS);

                apiResponse = (Builder) ApplicationManager.call("openai", context);
                break;
            case EDITS:
                payload = "{\n" +
                        " \"image\": \"\"," +
                        "  \"prompt\": \"\"," +
                        "  \"n\":1," +
                        "  \"response_format\":\"b64_json\"" +
                        "}";
                try {
                    _message.parse(payload);
                } catch (ApplicationException e) {
                    e.printStackTrace();
                }
                _message.put("prompt", prompt[1]);
                _message.put("user", prompt[0]);

                context.setAttribute("payload", _message);
                context.setAttribute("api", IMAGES_GENERATIONS);

                apiResponse = (Builder) ApplicationManager.call("openai", context);
                break;
            case VARIATIONS:
                payload = "{\n" +
                        " \"image\": \"\"," +
                        "  \"prompt\": \"\"," +
                        "  \"n\":1," +
                        "  \"response_format\":\"b64_json\"" +
                        "}";

                try {
                    _message.parse(payload);
                } catch (ApplicationException e) {
                    e.printStackTrace();
                }
                _message.put("prompt", prompt[1]);
                _message.put("user", prompt[0]);

                context.setAttribute("payload", _message);
                context.setAttribute("api", IMAGES_VARIATIONS);

                apiResponse = (Builder) ApplicationManager.call("openai", context);
            default:
                break;
        }

        if (apiResponse != null) {
            if (apiResponse.get("data") != null) {
                builders = (Builders) apiResponse.get("data");
                if (builders.size() > 0 && builders.get(0) != null) {
                    return "data:image/png;base64," + builders.get(0).get("b64_json").toString();
                }
            } else if (apiResponse.get("error") != null) {
                Builder error = (Builder) apiResponse.get("error");
                if (error.get("message") != null) {
                    return error.get("message").toString();
                }
            }
        }
        return "";
    }

    public String update() throws ApplicationException {
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

    public String update(String meetingCode, String sessionId) throws ApplicationException {
        final Request request = (Request) this.context.getAttribute(HTTP_REQUEST);
        if (request.getSession().getId().equalsIgnoreCase(sessionId)) {
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

        return "{}";
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
                response.addHeader(Header.CONTENT_TYPE.name(), mimeType);
            else
                response.addHeader(Header.CONTENT_DISPOSITION.name(), "application/octet-stream;filename=\"" + fileName + "\"");

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