package custom.application.v1;

import custom.ai.ImageProcessorType;
import custom.ai.OpenAI;
import custom.ai.SearchAI;
import custom.ai.StabilityAI;
import custom.objects.ChatHistory;
import custom.objects.DocumentFragment;
import custom.objects.User;
import custom.util.AuthenticationService;
import custom.util.ConversationHistoryManager;
import custom.util.DocumentProcessor;
import custom.util.DocumentQA;
import custom.util.EmbeddingManager;
import custom.util.SessionVariableManager;
import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.application.Context;
import org.tinystruct.application.SharedVariables;
import org.tinystruct.data.FileEntity;
import org.tinystruct.data.component.*;
import org.tinystruct.handler.Reforward;
import org.tinystruct.http.*;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.EventDispatcher;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.template.variable.StringVariable;
import org.tinystruct.system.template.variable.Variable;
import org.tinystruct.system.util.Matrix;
import org.tinystruct.transfer.DistributedMessageQueue;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static custom.ai.OpenAI.*;
import static org.tinystruct.http.Constants.HTTP_HOST;

public class smalltalk extends DistributedMessageQueue implements SessionListener {

    // Constants
    public static final String CHAT_GPT = "ChatGPT";
    // private static final String DEFAULT_MODEL = "deepseek/deepseek-r1:free";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final int DEFAULT_MESSAGE_POOL_SIZE = 100;
    private static final int DEFAULT_MAX_TOKENS = 3000;
    private static final double DEFAULT_TEMPERATURE = 0.8;
    private static final String DATE_FORMAT_PATTERN = "yyyy-M-d h:m:s";
    private static final String FILE_UPLOAD_DIR = "files";
    private static final int MAX_CONVERSATION_HISTORY = 5; // Store up to 5 message pairs for context

    // Configuration keys
    public static final String CONFIG_OPENAI_API_KEY = "openai.api_key";
    public static final String CONFIG_OPENAI_API_ENDPOINT = "openai.api_endpoint";
    private static final String CONFIG_DEFAULT_CHAT_ENGINE = "default.chat.engine";
    private static final String CONFIG_SYSTEM_DIRECTORY = "system.directory";

    private static final SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT_PATTERN);
    protected static final String MODEL = DEFAULT_MODEL;
    private boolean cliMode;
    private boolean chatGPT;
    private static final EventDispatcher dispatcher = EventDispatcher.getInstance();

    public void init() {
        try {
            super.init();
            initializeBasicSettings();
            configureSecurity();
            initializeAIServices();
            setupEventHandling();
        } catch (Exception e) {
            throw new ApplicationRuntimeException("Failed to initialize smalltalk: " + e.getMessage(), e);
        }
    }

    private void initializeBasicSettings() {
        this.setVariable("message", "");
        this.setVariable("topic", "");
        this.setVariable("meeting_update_url", "");

        System.setProperty("LANG", "en_US.UTF-8");
    }

    private void configureSecurity() {
        // Configure TLS settings
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2,TLSv1.3");
        System.setProperty("https.cipherSuites",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
    }

    private void initializeAIServices() {
        ApplicationManager.install(new OpenAI());
        ApplicationManager.install(new StabilityAI());
        ApplicationManager.install(new SearchAI());
        ApplicationManager.install(new EmbeddingManager());

        this.chatGPT = getConfiguration().get(CONFIG_DEFAULT_CHAT_ENGINE) != null
                ? !getConfiguration().get(CONFIG_DEFAULT_CHAT_ENGINE).equals("gpt-3")
                : false;
    }

    private void setupEventHandling() {
        SessionManager.getInstance().addListener(this);
        dispatcher.registerHandler(SessionCreated.class, event ->
                System.out.println(event.getPayload()));
    }

    /**
     * Login page
     */
    @Action("login")
    public smalltalk loginPage(Request request, Response response) {
        // If user is already logged in, redirect to chat
        Object userId = request.getSession().getAttribute("user_id");
        if (userId != null) {
            try {
                Reforward reforward = new Reforward(request, response);
                reforward.setDefault("/?q=talk");
                return (smalltalk) reforward.forward();
            } catch (Exception e) {
                // Continue to login page if redirect fails
            }
        }

        // Set variable to show login form
        this.setVariable("show_login", "true");
        return this;
    }

    @Action("talk")
    public Object index(Request request, Response response) throws ApplicationException {
        // Check if user is authenticated
        Object userId = request.getSession().getAttribute("user_id");
        if (userId == null) {
            // User is not authenticated, redirect to login page
            System.out.println("User is not authenticated, redirecting to login page");
            try {
                Reforward reforward = new Reforward(request, response);
                reforward.setDefault("/?q=login");
                return reforward.forward();
            } catch (Exception e) {
                throw new ApplicationException("Failed to redirect to login page: " + e.getMessage(), e);
            }
        }

        // User is authenticated, set username variable
        String username = (String) request.getSession().getAttribute("username");
        this.setVariable("username", username != null ? username : "User");
        this.setVariable("show_login", "false");
        System.out.println("User is authenticated as: " + username);

        Object meetingCode = request.getSession().getAttribute("meeting_code");

        if (meetingCode == null) {
            meetingCode = java.util.UUID.randomUUID().toString();
            request.getSession().setAttribute("meeting_code", meetingCode);

            dispatcher.dispatch(new SessionCreated(String.valueOf(meetingCode)));
        }

        Set<String> session_ids;
        final String sessionId = request.getSession().getId();
        if (this.groups.get(meetingCode) == null) {
            this.groups.put(meetingCode.toString(), new ArrayBlockingQueue<Builder>(DEFAULT_MESSAGE_POOL_SIZE));
        }

        // If the current user is not in the list of the sessions, we create a default session list for the meeting generated.
        if ((session_ids = this.sessions.get(meetingCode)) == null) {
            this.sessions.put(meetingCode.toString(), session_ids = new HashSet<String>());
        }

        if (!session_ids.contains(sessionId)) session_ids.add(sessionId);

        if (!this.list.containsKey(sessionId)) {
            this.list.put(sessionId, new ArrayDeque<Builder>());
        }

        this.setVariable("meeting_code", meetingCode.toString());
        this.setVariable("meeting_url", this.getLink("talk/join", null) + "/" + meetingCode + "&lang=" + this.getLocale().toLanguageTag());
        this.setVariable("session_id", request.getSession().getId());
        this.setVariable("start_url", this.getLink("talk/start", null));
        this.setVariable("meeting_update_url", this.getLink("talk/update", null) + "/" + meetingCode + "/" + request.getSession().getId());
        this.setVariable("meeting_qr_code_url", this.getLink("talk/matrix", null) + "/" + meetingCode);

        Variable<?> topic;
        SharedVariables sharedVariables = SharedVariables.getInstance(meetingCode.toString());
        if ((topic = sharedVariables.getVariable(meetingCode.toString())) != null) {
            this.setVariable("topic", topic.getValue().toString().replaceAll("[\r\n]", "<br />"), true);
        } else {
            this.setVariable("topic", "");
        }

        request.headers().add(Header.CACHE_CONTROL.set("no-cache, no-store, max-age=0, must-revalidate"));
        response.headers().add(Header.CACHE_CONTROL.set("no-cache, no-store, max-age=0, must-revalidate"));
        return this;
    }

    @Action("talk/update")
    public String update(String sessionId) throws ApplicationException {
        return this.take(sessionId);
    }

    @Action("talk/matrix")
    public String matrix(String meetingCode, Request request, Response response) throws ApplicationException {
        request.headers().add(Header.CACHE_CONTROL.set("no-cache, no-store, max-age=0, must-revalidate"));
        response.headers().add(Header.CACHE_CONTROL.set("no-cache, no-store, max-age=0, must-revalidate"));
        if (meetingCode != null && meetingCode.length() > 32) {
            BufferedImage qrImage = Matrix.toQRImage(this.getLink("talk/join") + "/" + meetingCode, 100, 100);
            return "data:image/png;base64," + Matrix.getBase64Image(qrImage);
        }

        return "";
    }

    @Action("talk/join")
    public Object join(String meetingCode, Request request, Response response) throws ApplicationException {
        // Check if user is authenticated
        Object userId = request.getSession().getAttribute("user_id");
        if (userId == null) {
            // Redirect to login page
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "Please login to join a meeting.";
        }

        if (!isValidMeetingCode(meetingCode)) {
            response.setStatus(ResponseStatus.NOT_FOUND);
            return "Invalid meeting code.";
        }

        request.getSession().setAttribute("meeting_code", meetingCode);
        try {
            return redirectToTalk(request, response);
        } catch (ApplicationException e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "Error redirecting to talk page: " + e.getMessage();
        }
    }

    private boolean isValidMeetingCode(String meetingCode) {
        return groups.containsKey(meetingCode);
    }

    private Object redirectToTalk(Request request, Response response) throws ApplicationException {
        try {
            Reforward reforward = new Reforward(request, response);
            reforward.setDefault("/?q=talk");
            return reforward.forward();
        } catch (Exception e) {
            throw new ApplicationException("Failed to redirect: " + e.getMessage(), e);
        }
    }

    @Action("talk/start")
    public Object start(String name, Request request, Response response) throws ApplicationException {
        request.getSession().setAttribute("user", name);

        Object meetingCode = request.getSession().getAttribute("meeting_code");
        if (meetingCode == null) {
            Reforward reforward = new Reforward(request, response);
            reforward.setDefault("/?q=talk");
            return reforward.forward();
        } else {
            this.setVariable("meeting_code", meetingCode.toString());
            this.setVariable("meeting_url", this.getLink("talk/join", null) + "/" + meetingCode + "&lang=" + this.getLocale().toLanguageTag());
        }

        return name;
    }

    @Action("talk/command")
    public String command(Request request, Response response) {
        final Object meetingCode = request.getSession().getAttribute("meeting_code");
        final String sessionId = request.getSession().getId();

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

    @Action("talk/save")
    public String save(Request request, Response response) {
        try {
            final Object meetingCode = request.getSession().getAttribute("meeting_code");
            if (meetingCode == null) {
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return "{ \"error\": \"missing_meeting_code\" }";
            }

            if (!this.groups.containsKey(meetingCode)) {
                response.setStatus(ResponseStatus.NOT_FOUND);
                return "{ \"error\": \"invalid_meeting_code\" }";
            }

            final String sessionId = request.getSession().getId();
            Set<String> sessions = this.sessions.get(meetingCode);

            if (sessions == null || !sessions.contains(sessionId)) {
                response.setStatus(ResponseStatus.UNAUTHORIZED);
                return "{ \"error\": \"invalid_session\" }";
            }

            String message = request.getParameter("text");
            if (message == null || message.trim().isEmpty()) {
                response.setStatus(ResponseStatus.BAD_REQUEST);
                return "{ \"error\": \"empty_message\" }";
            }

            final Builder builder = new Builder();
            builder.put("user", request.getSession().getAttribute("user"));
            builder.put("time", format.format(new Date()));
            builder.put("session_id", sessionId);

            String image = request.getParameter("image");
            if (image != null && !image.isEmpty()) {
                builder.put("message", filter(message) + "<img src=\"" + image + "\" />");
            } else {
                builder.put("message", filter(message));
            }

            if (message.contains("@" + CHAT_GPT)) {
                final String finalMessage = message.replaceAll("@" + CHAT_GPT, "");
                return this.save(meetingCode, builder, () -> {
                    try {
                        processChatGPTResponse(request, meetingCode, sessionId, finalMessage, image);
                    } catch (Exception e) {
                        System.err.println("Error processing ChatGPT response: " + e.getMessage());
                        sendErrorMessage(meetingCode, sessionId, e.getMessage());
                    }
                });
            }

            return this.save(meetingCode, builder);
        } catch (Exception e) {
            System.err.println("Error saving message: " + e.getMessage());
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"" + e.getMessage() + "\" }";
        }
    }

    private void processChatGPTResponse(Request request, Object meetingCode, String sessionId,
                                        String message, String image) throws ApplicationException {
        final Builder data = new Builder();
        data.put("user", CHAT_GPT);
        data.put("session_id", sessionId);
        data.put("time", format.format(new Date()));

        try {
            String response = chatGPT ? chatGPT(sessionId, message, image) : chat(sessionId, message, image);
            if (response == null || response.trim().isEmpty()) {
                throw new ApplicationException("Empty response from chat service");
            }
            data.put("message", filter(response));
        } catch (Exception e) {
            data.put("message", "Error: " + e.getMessage());
        }

        save(meetingCode, data);
    }

    private void sendErrorMessage(Object meetingCode, String sessionId, String errorMessage) {
        try {
            final Builder data = new Builder();
            data.put("user", CHAT_GPT);
            data.put("session_id", sessionId);
            data.put("time", format.format(new Date()));
            data.put("message", "Error: " + errorMessage);
            save(meetingCode, data);
        } catch (Exception e) {
            System.err.println("Failed to send error message: " + e.getMessage());
        }
    }

    @Action("chat")
    public void chat() {
        this.cliMode = true;
        if (getConfiguration().get("openai.api_key") == null || getConfiguration().get("openai.api_key").isEmpty()) {
            String url = "https://platform.openai.com/account/api-keys";

            Context ctx = new ApplicationContext();
            ctx.setAttribute("--url", url);
            try {
                ApplicationManager.call("open", ctx);
            } catch (ApplicationException e) {
                e.printStackTrace();
            }

            Console console = System.console();
            String prompt = "Enter your OpenAI Secret Key: ";

            if (console != null) {
                char[] chars;
                while ((chars = console.readPassword(prompt)) == null || chars.length == 0) ;
                getConfiguration().set("openai.api_key", new String(chars));
            } else {
                throw new ApplicationRuntimeException("openai.api_key is required.");
            }
        }
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to use smalltalk, you can type your questions or quit by type `exit`.");

        String sessionId = UUID.randomUUID().toString();
        while (true) {
            System.out.printf("%s >: ", format.format(new Date()));
            String input = scanner.nextLine();

            if (input.equals("exit")) {
                System.out.println("Bye!");
                break;
            } else {
                try {
                    if (!input.trim().isEmpty()) {
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
        System.exit(-1);
    }

    private String processAIResponse(String response) throws ApplicationException {
        if (response == null || response.isEmpty()) {
            return "";
        }

        // Regex to detect PlantUML code block - either in markdown format or raw format
        Pattern pattern = Pattern.compile("```plantuml\\s*@startuml(.*?)@enduml\\s*```|@startuml(.*?)@enduml",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);

        StringBuilder processedResponse = new StringBuilder(response);
        int offset = 0;

        while (matcher.find()) {
            try {
                String match = matcher.group(0);
                plantuml plantUML = new plantuml();
                List<String> umlImages = plantUML.generateUML(match);

                if (!umlImages.isEmpty()) {
                    String umlImage = umlImages.get(0);
                    String replacement = "\n<placeholder-image>data:image/png;base64," +
                            umlImage + "</placeholder-image>";

                    int start = matcher.end() + offset;
                    processedResponse.insert(start, replacement);
                    offset += replacement.length();
                }
            } catch (IOException e) {
                System.err.println("Error generating UML: " + e.getMessage());
                // Continue processing other UML diagrams even if one fails
            }
        }

        return processedResponse.toString();
    }

    private String chat(String sessionId, String message) throws ApplicationException {
        return chat(sessionId, message, null);
    }

    private String chat(String sessionId, String message, String image) throws ApplicationException {
        if (!cliMode) {
            message = sanitizeMessage(message);
        }

        if (message == null || message.trim().isEmpty()) {
            throw new ApplicationException("Message cannot be empty");
        }

        try {
            String response = chatGPT ? chatGPT(sessionId, message, image) : chat(sessionId, message, image);

            if (response == null || response.trim().isEmpty()) {
                System.err.println("Warning: Received empty response from chat API for message: " + message);
                throw new ApplicationException("No response received from chat service");
            }

            return processAIResponse(response);
        } catch (ApplicationException e) {
            System.err.println("Error in chat processing: " + e.getMessage() + " for message: " + message);
            throw new ApplicationException("Chat processing failed: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error in chat: " + e.getMessage() + " for message: " + message);
            throw new ApplicationException("Unexpected error in chat processing", e);
        }
    }

    private String sanitizeMessage(String message) {
        if (message == null) return "";
        return message.replaceAll("<br>|<br />", "");
    }

    /**
     * Creates a more contextual query by combining the current message with recent conversation history
     * This helps improve document retrieval relevance by providing more context
     *
     * @param currentMessage The current user message
     * @return A contextual query that includes relevant parts of the conversation history
     */
    private String createContextualQuery(String sessionId, String currentMessage) {
        StringBuilder contextualQuery = new StringBuilder(currentMessage);
        int totalContextAdded = 0;

        System.out.println("Building contextual query starting with: '" + currentMessage + "'");

        // Get conversation history from our manager
        List<Map<String, String>> conversationHistory = ConversationHistoryManager.getConversationHistory(sessionId);

        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            // Add up to MAX_CONVERSATION_HISTORY previous message pairs
            int count = Math.min(conversationHistory.size(), MAX_CONVERSATION_HISTORY);

            for (int i = 0; i < count; i++) {
                Map<String, String> messagePair = conversationHistory.get(i);
                String userMessage = messagePair.get("user");
                String assistantMessage = messagePair.get("assistant");

                if (userMessage != null && assistantMessage != null) {
                    // Add all messages regardless of length, but truncate very long messages
                    String truncatedUserMessage = userMessage;
                    String truncatedAssistantMessage = assistantMessage;

                    // Truncate very long messages to avoid excessive token usage
                    if (userMessage.length() > 1000) {
                        truncatedUserMessage = userMessage.substring(0, 1000) + "... (truncated)";
                    }

                    if (assistantMessage.length() > 1000) {
                        truncatedAssistantMessage = assistantMessage.substring(0, 1000) + "... (truncated)";
                    }

                    contextualQuery.append("\nPreviously I asked: ").append(truncatedUserMessage);
                    contextualQuery.append("\nAnd you answered: ").append(truncatedAssistantMessage);

                    totalContextAdded++;
                    System.out.println("Added context pair #" + (i+1) + " from conversation history to query");
                }
            }
        }

        // Fallback to session variables if no conversation history found
        if (totalContextAdded == 0) {
            System.out.println("No conversation history found, falling back to session variables");

            // Get conversation history from session variable manager
            java.util.List<String[]> messagePairs = SessionVariableManager.getAllMessagePairs(sessionId);
            System.out.println("Retrieved " + messagePairs.size() + " message pairs from session variable history");

            // Add message pairs to the contextual query
            for (int i = 0; i < messagePairs.size(); i++) {
                String[] pair = messagePairs.get(i);
                String userMessage = pair[0];
                String assistantMessage = pair[1];

                if (userMessage != null && !userMessage.isEmpty() &&
                    assistantMessage != null && !assistantMessage.isEmpty()) {

                    // Add all messages regardless of length, but truncate very long messages
                    String truncatedUserMessage = userMessage;
                    String truncatedAssistantMessage = assistantMessage;

                    // Truncate very long messages to avoid excessive token usage
                    if (userMessage.length() > 1000) {
                        truncatedUserMessage = userMessage.substring(0, 1000) + "... (truncated)";
                    }

                    if (assistantMessage.length() > 1000) {
                        truncatedAssistantMessage = assistantMessage.substring(0, 1000) + "... (truncated)";
                    }

                    contextualQuery.append("\nPreviously I asked: ").append(truncatedUserMessage);
                    contextualQuery.append("\nAnd you answered: ").append(truncatedAssistantMessage);

                    totalContextAdded++;
                    System.out.println("Added context pair #" + (i+1) + " from session variable history to query");
                }
            }
        }

        System.out.println("Final contextual query length: " + contextualQuery.length() + " characters");
        return contextualQuery.toString();
    }

    private Builder createChatPayload(String sessionId, String message, String model) {
        Builder payload = new Builder();
        payload.put("model", model);
        payload.put("max_tokens", DEFAULT_MAX_TOKENS);
        payload.put("temperature", DEFAULT_TEMPERATURE);
        payload.put("n", 1);
        payload.put("user", sessionId);

        Builders messages = new Builders();
        Builder systemMessage = new Builder();
        systemMessage.put("role", "system");
        systemMessage.put("content", getSystemPrompt());
        messages.add(systemMessage);

        // Add previous context if available
        addPreviousContext(messages, sessionId);

        // Add current message
        Builder userMessage = new Builder();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        messages.add(userMessage);

        payload.put("messages", messages);
        return payload;
    }

    private String getSystemPrompt() {
        return "I am an AI assistant specialized in IT. If you enter any Linux command, " +
                "I will execute it and display the result as you would see in a terminal. " +
                "I always consider the full context of our conversation to provide the most relevant answers. " +
                "This includes both our conversation history and any relevant documents that have been uploaded. " +
                "When I find information in uploaded documents that's relevant to your question, I will: " +
                "1. Use that information as my primary source for answering " +
                "2. Cite the specific document I'm referencing " +
                "3. Synthesize information from multiple documents if needed " +
                "4. Maintain continuity with our previous conversation " +
                "If you ask me a question that I am not knowledgeable enough to answer, I will ask if you have any " +
                "reference content you can provide or a URL I can reference. " +
                "I will always prioritize context from our conversation and uploaded documents over my general knowledge.";
    }

    private void addPreviousContext(Builders messages, String sessionId) {
        // Try to get conversation history from our manager first
        List<Map<String, String>> conversationHistory = ConversationHistoryManager.getConversationHistory(sessionId);

        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            System.out.println("Adding previous context from conversation history manager");

            // Add up to MAX_CONVERSATION_HISTORY previous message pairs
            int count = Math.min(conversationHistory.size(), MAX_CONVERSATION_HISTORY);

            for (int i = 0; i < count; i++) {
                Map<String, String> messagePair = conversationHistory.get(i);
                String userMessage = messagePair.get("user");
                String assistantMessage = messagePair.get("assistant");

                if (userMessage != null) {
                    Builder previousUser = new Builder();
                    previousUser.put("role", "user");
                    previousUser.put("content", userMessage);
                    messages.add(previousUser);

                    if (assistantMessage != null) {
                        Builder previousAssistant = new Builder();
                        previousAssistant.put("role", "assistant");
                        previousAssistant.put("content", assistantMessage);
                        messages.add(previousAssistant);
                    }
                }
            }

            System.out.println("Added " + count + " message pairs from conversation history manager");
            return; // Return early if we successfully added context
        }

        // Fall back to session variables if no conversation history is available
        System.out.println("No conversation history found, falling back to session variables");

        // Get conversation history from session variable manager
        java.util.List<String[]> messagePairs = SessionVariableManager.getAllMessagePairs(sessionId);
        System.out.println("Retrieved " + messagePairs.size() + " message pairs from session variable history");

        // Add message pairs to the context
        for (String[] pair : messagePairs) {
            String userMessage = pair[0];
            String assistantMessage = pair[1];

            if (userMessage != null && !userMessage.isEmpty()) {
                Builder previousUser = new Builder();
                previousUser.put("role", "user");
                previousUser.put("content", userMessage);
                messages.add(previousUser);

                if (assistantMessage != null && !assistantMessage.isEmpty()) {
                    Builder previousAssistant = new Builder();
                    previousAssistant.put("role", "assistant");
                    previousAssistant.put("content", assistantMessage);
                    messages.add(previousAssistant);
                }
            }
        }
    }

    /**
     * Call chat GPT API
     *
     * @return message from API
     * @throws ApplicationException while the failure occurred due to an exception
     */
    private String chatGPT(String sessionId, String message, String image) throws ApplicationException {
        validateChatGPTRequest(sessionId, message);

        String API_URL = getConfiguration().get(CONFIG_OPENAI_API_ENDPOINT);
        if (API_URL == null || API_URL.trim().isEmpty()) {
            throw new ApplicationException("OpenAI API endpoint not configured");
        }

        // Check if API key is configured
        String API_KEY = getConfiguration().get(CONFIG_OPENAI_API_KEY);
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

        API_URL = API_URL + "/v1/chat/completions";

        message = sanitizeMessage(message);
        Builder payloadBuilder = createChatGPTPayload(sessionId, message);
        Builder apiResponse = callOpenAIAPI(API_URL, payloadBuilder);
        return processGPTResponse(apiResponse, sessionId, message, image);
    }

    private void validateChatGPTRequest(String sessionId, String message) throws ApplicationException {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new ApplicationException("Session ID is required");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new ApplicationException("Message cannot be empty");
        }
    }

    private Builder createChatGPTPayload(String sessionId, String message) throws ApplicationException {
        Builder payloadBuilder = new Builder();
        try {
            payloadBuilder.parse("{\n" + "  \"model\": \"" + MODEL + "\"}");

            Builders messages = new Builders();
            Builder systemMessage = new Builder();
            systemMessage.put("role", "system");
            systemMessage.put("content", getSystemPrompt());
            messages.add(systemMessage);

            addPreviousContext(messages, sessionId);

            // Try to add relevant document context for the query
            try {
                System.out.println("Attempting to add document context for message: '" + message + "'");

                // Get meeting code from session
                String meetingCode = null;
                for (Map.Entry<?, Set<String>> entry : this.sessions.entrySet()) {
                    if (entry.getValue().contains(sessionId)) {
                        meetingCode = entry.getKey().toString();
                        break;
                    }
                }

                if (meetingCode != null) {
                    System.out.println("Found meeting code: " + meetingCode + " for session ID: " + sessionId);
                } else {
                    System.out.println("No meeting code found for session ID: " + sessionId + ", using default context");
                }

                // Create a more contextual query by combining the current message with recent history
                String contextualQuery = createContextualQuery(sessionId, message);
                System.out.println("Created contextual query: '" + contextualQuery + "'");

                // Add document context with meeting code and contextual query
                boolean contextAdded = DocumentQA.addDocumentContextToMessages(contextualQuery, meetingCode, messages);

                if (contextAdded) {
                    System.out.println("Successfully added document context to messages");
                } else {
                    System.out.println("No relevant document context found for message");
                }
            } catch (Exception e) {
                System.err.println("Warning: Error adding document context: " + e.getMessage());
                e.printStackTrace();
                System.out.println("Continuing without document context due to error");
                // Continue without document context if there's an error
            }

            Builder userMessage = new Builder();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messages.add(userMessage);

            payloadBuilder.put("messages", messages);
            payloadBuilder.put("user", sessionId);

            return payloadBuilder;
        } catch (Exception e) {
            throw new ApplicationException("Failed to create chat payload: " + e.getMessage(), e);
        }
    }

    private Builder callOpenAIAPI(String API_URL, Builder payload) throws ApplicationException {
        try {
            Context context = new ApplicationContext();
            context.setAttribute("payload", payload);
            context.setAttribute("api", API_URL);

            Builder response = (Builder) ApplicationManager.call("openai", context);
            if (response == null) {
                throw new ApplicationException("No response received from OpenAI API");
            }
            return response;
        } catch (Exception e) {
            throw new ApplicationException("Failed to call OpenAI API: " + e.getMessage(), e);
        }
    }

    private String processGPTResponse(Builder apiResponse, String sessionId, String message, String image)
            throws ApplicationException {
        if (apiResponse.get("error") != null) {
            Builder error = (Builder) apiResponse.get("error");
            String errorMessage = error.get("message") != null ?
                    error.get("message").toString() : "Unknown error from OpenAI API";
            System.err.println("OpenAI API error: " + errorMessage);
            throw new ApplicationException(errorMessage);
        }

        Builders choices = (Builders) apiResponse.get("choices");
        if (choices == null || choices.isEmpty()) {
            System.err.println("No choices returned from OpenAI API");
            throw new ApplicationException("No response choices available");
        }

        Builder choice = choices.get(0);
        if (choice == null || choice.get("message") == null) {
            System.err.println("Invalid choice structure in API response");
            throw new ApplicationException("Invalid response format");
        }

        String choiceText = ((Builder) choice.get("message")).get("content").toString();
        if (choiceText == null || choiceText.trim().isEmpty()) {
            System.err.println("Empty response content from OpenAI API");
            throw new ApplicationException("Empty response content");
        }

        // Store conversation history using our manager
        ConversationHistoryManager.addMessagePair(sessionId, message, choiceText);
        System.out.println("Stored conversation history for session " + sessionId);

        // Also store in session variables using the dedicated manager
        try {
            SessionVariableManager.addMessagePair(sessionId, message, choiceText);
        } catch (Exception e) {
            System.err.println("Error storing conversation in session variables: " + e.getMessage());
            // Continue execution even if variable storage fails
        }

        // Handle special response types
        if (choiceText.contains(IMAGES_GENERATIONS)) {
            return this.imageProcessorStability(ImageProcessorType.GENERATIONS, null, sessionId + ":" + message);
        } else if (choiceText.contains(IMAGES_EDITS)) {
            return this.imageProcessorStability(ImageProcessorType.EDITS, image, sessionId + ":" + message);
        } else if (choiceText.contains(IMAGES_VARIATIONS)) {
            return this.imageProcessor(ImageProcessorType.VARIATIONS, image, sessionId + ":" + message);
        } else if (choiceText.contains("@startuml")) {
            return processAIResponse(choiceText);
        }

        return choiceText;
    }

    private Builder preprocess(String message) throws ApplicationException {
        Context context = new ApplicationContext();
        context.setAttribute("--query", message);
        return (Builder) ApplicationManager.call("search", context);
    }

    /**
     * Process image requests with the given image processor from stability AI.
     *
     * @param imageProcessorType type of image processor
     * @param image              image base64 encoded string
     * @param message            message to process
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
                payload = "{\"text_prompts\": [\n" + "      {\n" + "        \"text\": \"A lighthouse on a cliff\"\n" + "      }\n" + "    ],\n" + "    \"cfg_scale\": 7,\n" + "    \"clip_guidance_preset\": \"FAST_BLUE\",\n" + "    \"height\": 512,\n" + "    \"width\": 512,\n" + "    \"samples\": 1,\n" + "    \"steps\": 50" + "}";

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
                if (!apiResponse.isEmpty()) {
                    Builders artifacts = (Builders) apiResponse.get("artifacts");
                    if (artifacts != null && !artifacts.isEmpty() && artifacts.get(0).get("base64") != null) {
                        return "data:image/png;base64," + artifacts.get(0).get("base64").toString();
                    }
                }

                return "";
            case EDITS:
                payload = "{\n" + "\"image_strength\": 0.35,\n" + "\"init_image_mode\": \"IMAGE_STRENGTH\",\n" + "\"init_image\": \"<image binary>\",\n" + "\"text_prompts[0][text]\": \"A dog space commander\",\n" + "\"text_prompts[0][weight]\": 1,\n" + "\"cfg_scale\": 7,\n" + "\"clip_guidance_preset\": \"FAST_BLUE\",\n" + "\"height\": 512,\n" + "\"width\": 512,\n" + "\"sampler\": \"K_DPM_2_ANCESTRAL\",\n" + "\"samples\": 3,\n" + "\"steps\": 20\n" + "}";

                try {
                    _message.parse(payload);
                } catch (ApplicationException e) {
                    e.printStackTrace();
                }
                _message.put("prompt", prompt[1]);
                _message.put("user", prompt[0]);

                context.setAttribute("content-type", "multipart/form-data");
                context.setAttribute("image", image);
                context.setAttribute("payload", _message);
                context.setAttribute("api", "v1beta/generation/stable-diffusion-512-v2-1/image-to-image");

                apiResponse = (Builder) ApplicationManager.call("stability", context);
                if (!apiResponse.isEmpty()) {
                    Builders artifacts = (Builders) apiResponse.get("artifacts");
                    if (artifacts != null && !artifacts.isEmpty() && artifacts.get(0).get("base64") != null) {
                        return "data:image/png;base64," + artifacts.get(0).get("base64").toString();
                    } else if (apiResponse.get("message") != null) {
                        return apiResponse.get("message").toString();
                    }
                }

                return "";
            case VARIATIONS:
                // TODO
                payload = "{\n" + "  \"prompt\": \"\"," + "  \"n\":1," + "  \"response_format\":\"b64_json\"" + "}";
                try {
                    _message.parse(payload);
                } catch (ApplicationException e) {
                    e.printStackTrace();
                }
                _message.put("prompt", prompt[1]);
                _message.put("user", prompt[0]);

                context.setAttribute("content-type", "multipart/form-data");
                context.setAttribute("image", image);
                context.setAttribute("payload", _message);
                context.setAttribute("api", IMAGES_VARIATIONS);

                apiResponse = (Builder) ApplicationManager.call("openai", context);
            default:
                break;
        }

        if (apiResponse != null) {
            if (apiResponse.get("data") != null) {
                builders = (Builders) apiResponse.get("data");
                if (!builders.isEmpty() && builders.get(0) != null) {
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
                payload = "{\n" + "  \"prompt\": \"\"," + "  \"n\":1," + "  \"response_format\":\"b64_json\"" + "}";

                try {
                    _message.parse(payload);
                } catch (ApplicationException e) {
                    e.printStackTrace();
                }

                _message.put("prompt", prompt[1]);
                _message.put("user", prompt[0]);

                getContext().setAttribute("payload", _message);
                getContext().setAttribute("api", getConfiguration().get("openai.api_endpoint") + IMAGES_GENERATIONS);

                apiResponse = (Builder) ApplicationManager.call("openai", getContext());
                break;
            case EDITS:
                // TODO
                payload = "{\n" + "  \"prompt\": \"\"," + "  \"n\":1," + "  \"response_format\":\"b64_json\"" + "}";
                try {
                    _message.parse(payload);
                } catch (ApplicationException e) {
                    e.printStackTrace();
                }
                _message.put("prompt", prompt[1]);
                _message.put("user", prompt[0]);

                getContext().setAttribute("content-type", "multipart/form-data");
                getContext().setAttribute("image", image);
                getContext().setAttribute("payload", _message);
                getContext().setAttribute("api", IMAGES_EDITS);

                apiResponse = (Builder) ApplicationManager.call("openai", getContext());
                break;
            case VARIATIONS:
                // TODO
                payload = "{\n" + "  \"prompt\": \"\"," + "  \"n\":1," + "  \"response_format\":\"b64_json\"" + "}";
                try {
                    _message.parse(payload);
                } catch (ApplicationException e) {
                    e.printStackTrace();
                }
                _message.put("prompt", prompt[1]);
                _message.put("user", prompt[0]);

                getContext().setAttribute("image", image);
                getContext().setAttribute("payload", _message);
                getContext().setAttribute("api", IMAGES_VARIATIONS);

                apiResponse = (Builder) ApplicationManager.call("openai", getContext());
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

    @Action("talk/update")
    public String update(Request request, Response response) throws ApplicationException {
        final Object meetingCode = request.getSession().getAttribute("meeting_code");
        final String sessionId = request.getSession().getId();
        if (meetingCode != null) {
            return this.update(meetingCode.toString(), sessionId, request, response);
        }

        response.setStatus(ResponseStatus.REQUEST_TIMEOUT);

        return "{ \"error\": \"expired\" }";
    }

    @Action("talk/update")
    public String update(String meetingCode, String sessionId, Request request, Response response) throws ApplicationException {
        if (request.getSession().getId().equalsIgnoreCase(sessionId)) {
            String error = "{ \"error\": \"expired\" }";
            if (this.groups.containsKey(meetingCode)) {
                Set<String> list;
                if ((list = sessions.get(meetingCode)) != null && list.contains(sessionId)) {
                    return this.take(sessionId);
                }

                error = "{ \"error\": \"session-timeout\" }";
            }

            response.setStatus(ResponseStatus.REQUEST_TIMEOUT);
            return error;
        }

        return "{}";
    }

    private String getUploadDirectory() {
        return getConfiguration().get(CONFIG_SYSTEM_DIRECTORY) != null
                ? getConfiguration().get(CONFIG_SYSTEM_DIRECTORY) + "/" + FILE_UPLOAD_DIR
                : FILE_UPLOAD_DIR;
    }

    private void validateFileUpload(String meetingCode) throws ApplicationException {
        if (meetingCode == null) {
            throw new ApplicationException("Not allowed to upload any files.");
        }
    }

    private void validateFileDownload(String meetingCode, boolean encoded) throws ApplicationException {
        if (encoded && meetingCode == null) {
            throw new ApplicationException("Not allowed to download any files.");
        }
    }

    private void encryptFile(byte[] data, String meetingCode) {
        if (data == null || meetingCode == null) return;

        byte[] keys = meetingCode.getBytes(StandardCharsets.UTF_8);
        int blocks = (data.length - data.length % 1024) / 1024;
        int i = 0;
        do {
            int min = Math.min(keys.length, data.length);
            for (int j = 0; j < min; j++) {
                data[i * 1024 + j] = (byte) (data[i * 1024 + j] ^ keys[j]);
            }
        } while (i++ < blocks);
    }

    @Action("talk/upload")
    public String upload(Request request) throws ApplicationException {
        final Object meetingCode = request.getSession().getAttribute("meeting_code");
        validateFileUpload(meetingCode.toString());

        final String uploadPath = getUploadDirectory();
        final Builders builders = new Builders();

        List<FileEntity> attachments = request.getAttachments();
        for (FileEntity file : attachments) {
            try {
                processUploadedFile(file, uploadPath, meetingCode.toString(), builders);
            } catch (IOException e) {
                throw new ApplicationException("Error processing file: " + e.getMessage(), e);
            }
        }

        return builders.toString();
    }

    private void processUploadedFile(FileEntity file, String uploadPath, String meetingCode, Builders builders)
            throws IOException, ApplicationException {
        final Builder builder = new Builder();
        builder.put("type", file.getContentType());
        builder.put("file", new StringBuilder()
                .append(getContext().getAttribute(HTTP_HOST))
                .append("files/")
                .append(file.getFilename()));
        builder.put("originalName", file.getFilename());

        final File targetFile = new File(uploadPath + File.separator + file.getFilename());
        createDirectoryIfNeeded(targetFile.getParentFile());

        // Get file content
        byte[] fileContent = file.get();

        if (fileContent == null || fileContent.length == 0) {
            System.err.println("Warning: Empty file content for " + file.getFilename());
        }

        try (final BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(targetFile))) {
            // Write file content
            if (fileContent != null && fileContent.length > 0) {
                // Check if encryption is enabled
                String encryptionEnabledStr = getConfiguration().get("file.upload.encryption.enabled");
                boolean encryptionEnabled = Boolean.parseBoolean(encryptionEnabledStr);

                if (encryptionEnabled) {
                    // Write encrypted content
                    try (final BufferedInputStream bs = new BufferedInputStream(new ByteArrayInputStream(fileContent))) {
                        writeEncryptedFile(bout, bs, meetingCode);
                    }
                } else {
                    // Write content directly
                    bout.write(fileContent);
                }
            }
        }

        builders.add(builder);
        System.out.printf("File %s being uploaded to %s%n", file.getFilename(), uploadPath);

        // Verify file was written correctly
        if (targetFile.exists() && targetFile.length() > 0) {
            System.out.println("File written successfully: " + targetFile.getPath() + " (" + targetFile.length() + " bytes)");

            // Process document if it's a supported type
            if (DocumentProcessor.isSupportedMimeType(file.getContentType())) {
                processDocumentContent(targetFile.getPath(), file.getContentType(), meetingCode);
            }
        } else {
            System.err.println("Warning: File appears to be empty after writing: " + targetFile.getPath());
        }
    }

    private void createDirectoryIfNeeded(File directory) throws ApplicationException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new ApplicationException("Failed to create directory: " + directory.getPath());
        }
    }

    private void writeEncryptedFile(BufferedOutputStream bout, BufferedInputStream bs, String meetingCode)
            throws IOException {
        final byte[] buffer = new byte[1024];
        final byte[] keys = meetingCode.getBytes(StandardCharsets.UTF_8);
        int read;

        while ((read = bs.read(buffer)) != -1) {
            int min = Math.min(read, keys.length);
            for (int i = 0; i < min; i++) {
                buffer[i] = (byte) (buffer[i] ^ keys[i]);
            }
            bout.write(buffer, 0, read);
        }
    }

    private void processDocumentContent(String filePath, String mimeType, String meetingCode) {
        try {
            System.out.println("Starting document processing for: " + filePath);
            System.out.println("MIME type: " + mimeType);
            System.out.println("Meeting code: " + meetingCode);

            // Get user ID from session
            String userId = null;
            String username = null;
            for (Map.Entry<?, Set<String>> entry : this.sessions.entrySet()) {
                if (entry.getKey().toString().equals(meetingCode)) {
                    // Get the first session ID for this meeting
                    if (!entry.getValue().isEmpty()) {
                        String sessionId = entry.getValue().iterator().next();
                        // Get the session object
                        Session session = SessionManager.getInstance().getSession(sessionId);
                        if (session != null) {
                            userId = (String) session.getAttribute("user_id");
                            username = (String) session.getAttribute("username");
                            break;
                        }
                    }
                }
            }

            System.out.println("User ID: " + (userId != null ? userId : "none (anonymous)"));

            // Get file name for title
            String fileName = new File(filePath).getName();
            String title = fileName;
            String description = "Uploaded by " + (username != null ? username : "anonymous user") + " on " +
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            DocumentProcessor processor = new DocumentProcessor();
            List<DocumentFragment> fragments = processor.processDocument(
                filePath,
                mimeType.trim(),
                userId,
                title,
                description,
                true  // Public by default
            );

            System.out.println("Successfully processed document into " + fragments.size() + " fragments");

            EmbeddingManager manager = (EmbeddingManager) ApplicationManager.get(EmbeddingManager.class.getName());
            if (manager == null) {
                System.err.println("ERROR: EmbeddingManager not found. Make sure it's properly initialized.");
                throw new ApplicationException("EmbeddingManager not found");
            }

            // Save fragments to database
            int successCount = 0;
            for (DocumentFragment fragment : fragments) {
                try {
                    fragment.appendAndGetId();
                    System.out.println("Saved fragment " + fragment.getId() + " to database");

                    // Generate embedding for the fragment
                    try {
                        manager.generateEmbedding(fragment);
                        successCount++;
                        System.out.println("Generated embedding for fragment " + fragment.getId());
                    } catch (Exception e) {
                        System.err.println("Failed to generate embedding for document fragment: " + e.getMessage());
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    System.err.println("Failed to save document fragment: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            System.out.println("Document processing complete. Successfully processed " +
                    successCount + " out of " + fragments.size() + " fragments.");

            // Add a message to the chat about the document processing
            final Builder messageBuilder = new Builder();
            messageBuilder.put("user", "System");
            messageBuilder.put("time", format.format(new Date()));
            messageBuilder.put("message", String.format("Document '%s' has been processed into %d fragments and is now searchable.",
                    new File(filePath).getName(), fragments.size()));
            save(meetingCode, messageBuilder);
        } catch (ApplicationException e) {
            System.err.println("Error processing document: " + e.getMessage());
            e.printStackTrace();
            // Add error message to chat
            final Builder errorBuilder = new Builder();
            errorBuilder.put("user", "System");
            errorBuilder.put("time", format.format(new Date()));
            errorBuilder.put("message", "Error processing document: " + e.getMessage());
            save(meetingCode, errorBuilder);
        } catch (Exception e) {
            System.err.println("Unexpected error processing document: " + e.getMessage());
            e.printStackTrace();
            // Add error message to chat
            final Builder errorBuilder = new Builder();
            errorBuilder.put("user", "System");
            errorBuilder.put("time", format.format(new Date()));
            errorBuilder.put("message", "Unexpected error processing document: " + e.getMessage());
            save(meetingCode, errorBuilder);
        }
    }

    private byte[] download(String fileName, boolean encoded, Request request, Response response) throws ApplicationException {
        final Object meetingCode = request.getSession().getAttribute("meeting_code");
        validateFileDownload(meetingCode.toString(), encoded);

        // Create path to download the file
        final String fileDir = getConfiguration().get("system.directory") != null ? getConfiguration().get("system.directory") + "/files" : "files";

        try {
            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        // Creating an object of Path class and
        // assigning local directory path of file to it
        Path path = Paths.get(fileDir, fileName);

        // Converting the file into a byte array
        // using Files.readAllBytes() method
        byte[] arr;
        try {
            String mimeType = Files.probeContentType(path);
            if (mimeType != null) {
                response.addHeader(Header.CONTENT_TYPE.name(), mimeType);
            }

            response.addHeader(Header.CONTENT_DISPOSITION.name(), "attachment; filename*=UTF-8''" +
                    URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20"));

            arr = Files.readAllBytes(path);
            if (encoded) {
                encryptFile(arr, meetingCode.toString());
            }
        } catch (IOException e) {
            throw new ApplicationException("Error reading the file: " + e.getMessage(), e);
        }

        return arr;
    }

    @Action("files")
    public byte[] download(String fileName, Request request, Response response) throws ApplicationException {
        return this.download(fileName, true, request, response);
    }

    @Action("talk/topic")
    public boolean topic(Request request) {
        final Object meeting_code = request.getSession().getAttribute("meeting_code");
        if (meeting_code != null && request.getParameter("topic") != null) {
            SharedVariables sharedVariables = SharedVariables.getInstance(meeting_code.toString());
            StringVariable variable = new StringVariable(meeting_code.toString(), filter(request.getParameter("topic")));
            sharedVariables.setVariable(variable, true);
            return true;
        }

        return false;
    }

    protected smalltalk exit(Request request) {
        request.getSession().removeAttribute("meeting_code");
        return this;
    }

    @Override
    protected String filter(String text) {
        text = text.replaceAll("<script(.*)>(.*)<\\/script>", "");
        text = text.replaceAll("\\\\\"", "\"");
        text = text.replaceAll("\\\\n\\\\n", "<br />");
        text = text.replaceAll("\\\\n", "<br />");
        return text;
    }

    @Override
    public void onSessionEvent(SessionEvent event) {
        switch (event.getType()) {
            case CREATED:
                handleSessionCreated(event);
                break;
            case DESTROYED:
                handleSessionDestroyed(event);
                break;
            default:
                // Log unexpected event type
                System.err.println("Unexpected session event type: " + event.getType());
        }
    }

    private void handleSessionCreated(SessionEvent event) {
        Object meetingCode = event.getSession().getAttribute("meeting_code");
        if (meetingCode == null) {
            meetingCode = generateMeetingCode();
            event.getSession().setAttribute("meeting_code", meetingCode);
            dispatcher.dispatch(new SessionCreated(String.valueOf(meetingCode)));
        }

        final String sessionId = event.getSession().getId();
        initializeSessionQueue(sessionId);
    }

    private String generateMeetingCode() {
        return UUID.randomUUID().toString();
    }

    private void initializeSessionQueue(String sessionId) {
        if (!this.list.containsKey(sessionId)) {
            this.list.put(sessionId, new ArrayDeque<Builder>());
        }
    }

    private void handleSessionDestroyed(SessionEvent event) {
        String sessionId = event.getSession().getId();
        Object meetingCode = event.getSession().getAttribute("meeting_code");

        // Clean up conversation history
        ConversationHistoryManager.clearConversationHistory(sessionId);
        SessionVariableManager.clearSession(sessionId);
        System.out.println("Cleared conversation history for session: " + sessionId);

        if (meetingCode != null) {
            cleanupSession(event.getSession(), meetingCode.toString());
        }
    }

    private void cleanupSession(Session session, String meetingCode) {
        notifySessionExpired(meetingCode);
        removeSessionFromGroups(session.getId(), meetingCode);
        removeSessionQueue(session.getId());
    }

    private void notifySessionExpired(String meetingCode) {
        final Builder builder = new Builder();
        builder.put("user", "System");
        builder.put("time", format.format(new Date()));
        builder.put("cmd", "expired");
        builder.put("message", "Session expired");
        this.save(meetingCode, builder);
    }

    private void removeSessionFromGroups(String sessionId, String meetingCode) {
        Set<String> sessionIds = this.sessions.get(meetingCode);
        if (sessionIds != null) {
            sessionIds.remove(sessionId);
        }

        Queue<Builder> messages = this.groups.get(meetingCode);
        if (messages != null) {
            messages.remove(meetingCode);
        }
    }

    private void removeSessionQueue(String sessionId) {
        if (this.list.containsKey(sessionId)) {
            this.list.remove(sessionId);
            wakeup();
        }
    }

    private void saveChatHistory(Builder builder, String meetingCode) throws ApplicationException {
        ChatHistory history = new ChatHistory();
        history.setMeetingCode(meetingCode);

        // Safely handle user field which might be null
        Object user = builder.get("user");
        history.setUserName(user != null ? user.toString() : "System");

        // Safely handle message field which might be null
        Object message = builder.get("message");
        if (message != null) {
            history.setMessage(message.toString());
        } else {
            // If message is null, check if there's a cmd field
            Object cmd = builder.get("cmd");
            history.setMessage(cmd != null ? "Command: " + cmd.toString() : "No message");
        }

        history.setSessionId(builder.get("session_id") != null ? builder.get("session_id").toString() : "");
        history.setMessageType(builder.get("image") != null ? "IMAGE" : "TEXT");
        history.setImageUrl(builder.get("image") != null ? builder.get("image").toString() : null);
        history.setCreatedAt(format.format(new Date()));

        try {
            history.append();
        } catch (Exception e) {
            throw new ApplicationException("Failed to save chat history: " + e.getMessage(), e);
        }
    }

    private String save(String meetingCode, Builder data) {
        Queue<Builder> queue = this.groups.get(meetingCode);
        if (queue != null) {
            queue.add(data);

            // Automatically save to chat history database
            try {
                saveChatHistory(data, meetingCode);
            } catch (ApplicationException e) {
                System.err.println("Failed to save chat history: " + e.getMessage());
                // Continue execution even if saving to history fails
            }

            wakeup();
            return "{ \"status\": \"ok\" }";
        }

        return "{ \"error\": \"invalid_meeting_code\" }";
    }

    @Action("talk/history")
    public String getChatHistory(Request request, Response response) throws ApplicationException {
        final Object meetingCode = request.getSession().getAttribute("meeting_code");
        if (meetingCode == null) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"missing_meeting_code\" }";
        }

        try {
            ChatHistory history = new ChatHistory();
            Table messages = history.find("meeting_code = ?", new Object[]{meetingCode.toString()});

            Builders builders = new Builders();
            for (Row row : messages) {
                ChatHistory msg = new ChatHistory();
                msg.setData(row);
                Builder builder = new Builder();
                builder.put("user", msg.getUserName());
                builder.put("time", msg.getCreatedAt());
                builder.put("message", msg.getMessage());
                builder.put("type", msg.getMessageType());
                if (msg.getImageUrl() != null && !msg.getImageUrl().isEmpty()) {
                    builder.put("image", msg.getImageUrl());
                }
                builders.add(builder);
            }

            return builders.toString();
        } catch (Exception e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"" + e.getMessage() + "\" }";
        }
    }

    /**
     * Register a new user
     */
    @Action("auth/register")
    public String register(Request request, Response response) throws ApplicationException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String email = request.getParameter("email");
        String fullName = request.getParameter("fullName");

        // Validate required fields
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"missing_required_fields\", \"message\": \"Username and password are required\" }";
        }

        try {
            // Register the user
            AuthenticationService authService = AuthenticationService.getInstance();
            User user = authService.registerUser(username, password, email, fullName);

            // Set user in session
            request.getSession().setAttribute("user_id", user.getId());
            request.getSession().setAttribute("username", user.getUsername());

            // Return success response
            Builder builder = new Builder();
            builder.put("id", user.getId());
            builder.put("username", user.getUsername());
            builder.put("email", user.getEmail() != null ? user.getEmail() : "");
            builder.put("fullName", user.getFullName() != null ? user.getFullName() : "");
            builder.put("createdAt", format.format(user.getCreatedAt()));

            return builder.toString();
        } catch (ApplicationException e) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"registration_failed\", \"message\": \"" + e.getMessage() + "\" }";
        } catch (Exception e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"" + e.getMessage() + "\" }";
        }
    }

    /**
     * Login a user
     */
    @Action("auth/login")
    public String login(Request request, Response response) throws ApplicationException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // Validate required fields
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            response.setStatus(ResponseStatus.BAD_REQUEST);
            return "{ \"error\": \"missing_required_fields\", \"message\": \"Username and password are required\" }";
        }

        try {
            // Authenticate the user
            AuthenticationService authService = AuthenticationService.getInstance();
            User user = authService.authenticateUser(username, password);

            // Set user in session
            request.getSession().setAttribute("user_id", user.getId());
            request.getSession().setAttribute("username", user.getUsername());

            // Return success response
            Builder builder = new Builder();
            builder.put("id", user.getId());
            builder.put("username", user.getUsername());
            builder.put("email", user.getEmail() != null ? user.getEmail() : "");
            builder.put("fullName", user.getFullName() != null ? user.getFullName() : "");
            builder.put("lastLogin", user.getLastLogin() != null ? format.format(user.getLastLogin()) : "");
            builder.put("isActive",user.getIsActive());

            return builder.toString();
        } catch (ApplicationException e) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"authentication_failed\", \"message\": \"" + e.getMessage() + "\" }";
        } catch (Exception e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"" + e.getMessage() + "\" }";
        }
    }

    /**
     * Logout the current user
     */
    @Action("auth/logout")
    public String logout(Request request, Response response) {
        // Clear user session
        request.getSession().removeAttribute("user_id");
        request.getSession().removeAttribute("username");

        // Return success response
        return "{ \"status\": \"ok\" }";
    }

    /**
     * Get the current user's profile
     */
    @Action("auth/profile")
    public String getProfile(Request request, Response response) throws ApplicationException {
        // Check if user is logged in
        Object userId = request.getSession().getAttribute("user_id");
        if (userId == null) {
            response.setStatus(ResponseStatus.UNAUTHORIZED);
            return "{ \"error\": \"not_authenticated\", \"message\": \"User is not logged in\" }";
        }

        try {
            // Get user profile
            AuthenticationService authService = AuthenticationService.getInstance();
            User user = authService.findUserById(userId.toString());

            if (user == null) {
                response.setStatus(ResponseStatus.NOT_FOUND);
                return "{ \"error\": \"user_not_found\", \"message\": \"User not found\" }";
            }

            // Return user profile
            Builder builder = new Builder();
            builder.put("id", user.getId());
            builder.put("username", user.getUsername());
            builder.put("email", user.getEmail() != null ? user.getEmail() : "");
            builder.put("fullName", user.getFullName() != null ? user.getFullName() : "");
            builder.put("createdAt", format.format(user.getCreatedAt()));
            builder.put("lastLogin", user.getLastLogin() != null ? format.format(user.getLastLogin()) : "");
            builder.put("isAdmin", user.getIsAdmin());

            return builder.toString();
        } catch (Exception e) {
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR);
            return "{ \"error\": \"internal_error\", \"message\": \"" + e.getMessage() + "\" }";
        }
    }


}