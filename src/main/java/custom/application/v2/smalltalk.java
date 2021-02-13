package custom.application;

import org.tinystruct.ApplicationContext;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.system.ApplicationManager;
import org.tinystruct.system.Dispatcher;
import org.tinystruct.system.NettyHttpServer;
import org.tinystruct.system.template.variable.Variable;
import org.tinystruct.system.util.Matrix;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class smalltalk extends talk {

    public void init() {
        super.init();

        this.setAction("talk", "index");
        this.setAction("talk/join", "join");
        this.setAction("talk/start", "start");
        this.setAction("talk/upload", "upload");
        this.setAction("talk/command", "command");
        this.setAction("talk/topic", "topic");
        this.setAction("talk/matrix", "matrix");

        this.setVariable("message", "");
        this.setVariable("topic", "");
    }

    public talk index() {
        Object meetingCode = this.context.getParameter("meeting_code");

        if (meetingCode == null) {
            meetingCode = java.util.UUID.randomUUID().toString();
            this.context.setAttribute("meeting_code", meetingCode);

            System.out.println("New meeting generated:" + meetingCode);
        }

        if (this.meetings.get(meetingCode) == null) {
            this.meetings.put(meetingCode.toString(), new ArrayBlockingQueue<Builder>(DEFAULT_MESSAGE_POOL_SIZE));
        }

        List<String> session_ids;
        final String sessionId = this.context.getAttribute("session_id")!=null?this.context.getAttribute("session_id").toString():"";
        // If the current user is not in the list of the sessions, we create RequestMerger default session list for the meeting generated.
        if ((session_ids = this.sessions.get(meetingCode)) == null) {
            this.sessions.put(meetingCode.toString(), session_ids = new ArrayList<String>());
        }

        if (!session_ids.contains(sessionId))
            session_ids.add(sessionId);

        if (!this.list.containsKey(sessionId)) {
            this.list.put(sessionId, new ArrayDeque<Builder>());
        }

        this.setVariable("meeting_code", meetingCode.toString());
        this.setVariable("session_id", sessionId);

        Variable<?> topic;
        if ((topic = this.getVariable(meetingCode.toString())) != null) {
            this.setVariable("topic", topic.getValue().toString().replaceAll("[\r\n]", "<br />"), true);
        }

        return this;
    }

    public String matrix() throws ApplicationException {
        if (this.context.getAttribute("meeting_code") != null) {
            BufferedImage qrImage = Matrix.toQRImage(this.getLink("talk/join") + "/" + this.context.getAttribute("meeting_code"), 100, 100);
            return "data:image/png;base64," + Matrix.getBase64Image(qrImage);
        }

        return "";
    }

    public String join(String meetingCode) throws ApplicationException {
        if (meetings.containsKey(meetingCode)) {
            this.context.setAttribute("meeting_code", meetingCode);

            this.setVariable("meeting_code", meetingCode);
        } else {
            return "Invalid meeting code.";
        }

        return "Please start the conversation with your name: " + this.config.get("default.base_url") + "talk/start/YOUR NAME";
    }

    public String start(String name) throws ApplicationException {
        this.context.setAttribute("user", name);

        Object meetingCode = this.context.getAttribute("meeting_code");
        if (meetingCode != null) {
            this.setVariable("meeting_code", meetingCode.toString());
        }

        return name;
    }

    public String command() {
        final Object meetingCode = this.context.getParameter("meeting_code");
        final String sessionId = this.context.getAttribute("session_id").toString();
        if (meetingCode != null && sessions.get(meetingCode) != null && sessions.get(meetingCode).contains(sessionId)) {
            if (this.context.getAttribute("user") == null) {
                return "{ \"error\": \"missing user\" }";
            }

            Builder builder = new Builder();
            builder.put("user", this.context.getAttribute("user"));
            builder.put("cmd", this.context.getAttribute("cmd"));

            return this.save(meetingCode, builder);
        }

        return "{ \"error\": \"expired\" }";
    }

    public String save() {
        final Object meetingCode = this.context.getAttribute("meeting_code");
        if (this.meetings.containsKey(meetingCode)) {
            final String sessionId = this.context.getAttribute("session_id").toString();
            if (meetingCode != null && sessions.get(meetingCode) != null && sessions.get(meetingCode).contains(sessionId)) {
                String message;
                if ((message = this.context.getParameter("text")) != null && !message.isEmpty()) {

                    final SimpleDateFormat format = new SimpleDateFormat("yyyy-M-d h:m:s");
                    final Builder builder = new Builder();
                    builder.put("user", this.context.getAttribute("user"));
                    builder.put("time", format.format(new Date()));
                    builder.put("message", filter(message));
                    builder.put("session_id", sessionId);

                    return this.save(meetingCode, builder);
                }
            }
        }

        return "{ \"error\": \"expired\" }";
    }

    public String update() throws ApplicationException, IOException {
        final Object meetingCode = this.context.getAttribute("meeting_code");
        final String sessionId = this.context.getAttribute("session_id").toString();
        if (meetingCode != null) {
            return this.update(meetingCode.toString(), sessionId);
        }
        return "{ \"error\": \"expired\" }";
    }

    public String update(String meetingCode, String sessionId) throws ApplicationException, IOException {
        String error = "{ \"error\": \"expired\" }";
        if (this.meetings.containsKey(meetingCode)) {
            List<String> list;
            if ((list = sessions.get(meetingCode)) != null && list.contains(sessionId)) {
                return this.update(sessionId);
            }
            error = "{ \"error\": \"session-timeout\" }";
        }

        return error;
    }

    public boolean topic() {
        final Object meeting_code = this.context.getAttribute("meeting_code");

        if (meeting_code != null) {
            this.setVariable(meeting_code.toString(), filter(this.context.getParameter("topic").toString()));
            return true;
        }

        return false;
    }

    protected talk exit() {
        this.context.removeAttribute("meeting_code");
        return this;
    }

    @Override
    protected String filter(String text) {
        text = text.replaceAll("<script(.*)>(.*)<\\/script>", "");
        return text;
    }

    public static void main(String[] args) throws ApplicationException {
        ApplicationManager.init();
        ApplicationContext context = new ApplicationContext();
        context.setAttribute("session_id", UUID.randomUUID());
        context.setAttribute("meeting_code", UUID.randomUUID());
        context.setAttribute("topic", "This is an example base on Netty technology.");

        ApplicationManager.install(new NettyHttpServer());
//        ApplicationManager.install(new Dispatcher());
//        ApplicationManager.install(new smalltalk());
        ApplicationManager.install(new hello());
        ApplicationManager.call("--start-server", context);

        HashSet<PosixFilePermission> set = new HashSet<>();
        set.add(PosixFilePermission.OTHERS_EXECUTE);
        set.add(PosixFilePermission.GROUP_EXECUTE);
        set.add(PosixFilePermission.OWNER_EXECUTE);
        set.add(PosixFilePermission.OWNER_READ);

        try {
            Files.setPosixFilePermissions(Paths.get("bin/dispatcher"), set);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
