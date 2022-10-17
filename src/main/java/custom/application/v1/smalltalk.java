package custom.application.v1;

import custom.application.talk;
import org.tinystruct.ApplicationException;
import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.handler.Reforward;
import org.tinystruct.http.Header;
import org.tinystruct.http.Request;
import org.tinystruct.http.Response;
import org.tinystruct.http.servlet.MultipartFormData;
import org.tinystruct.system.template.variable.Variable;
import org.tinystruct.system.util.Matrix;
import org.tinystruct.system.util.StringUtilities;
import org.tinystruct.transfer.http.upload.ContentDisposition;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class smalltalk extends talk implements HttpSessionListener {

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
        final Request request = (Request) this.context.getAttribute("HTTP_REQUEST");
        Object meetingCode = request.getSession().getAttribute("meeting_code");

        if (meetingCode == null) {
            meetingCode = java.util.UUID.randomUUID().toString();
            request.getSession().setAttribute("meeting_code", meetingCode);

            System.out.println("New meeting generated:" + meetingCode);
        }

        List<String> session_ids;
        final String sessionId = request.getSession().getId();
        if (this.meetings.get(meetingCode) == null) {
            this.meetings.put(meetingCode.toString(), new ArrayBlockingQueue<Builder>(DEFAULT_MESSAGE_POOL_SIZE));
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
        if ((topic = this.getVariable(meetingCode.toString())) != null) {
            this.setVariable("topic", topic.getValue().toString().replaceAll("[\r\n]", "<br />"), true);
        }
        else
        {
            this.setVariable("topic", "");
        }

        return this;
    }

    public String matrix() throws ApplicationException {
        final Request request = (Request) this.context.getAttribute("HTTP_REQUEST");
        System.out.println("this.getLink(\"talk/join\") = " + this.getLink("talk/join"));
        if (request.getParameter("meeting_code") != null) {
            BufferedImage qrImage = Matrix.toQRImage(this.getLink("talk/join") + "/" + request.getParameter("meeting_code"), 100, 100);
            return "data:image/png;base64," + Matrix.getBase64Image(qrImage);
        }

        return "";
    }

    public Object join(String meetingCode) throws ApplicationException {
        if (meetings.containsKey(meetingCode)) {
            final Request request = (Request) this.context.getAttribute("HTTP_REQUEST");
            final Response response = (Response) this.context.getAttribute("HTTP_RESPONSE");
            request.getSession().setAttribute("meeting_code", meetingCode);

            this.setVariable("meeting_code", meetingCode);

            Reforward reforward = new Reforward(request, response);
            reforward.setDefault("/?q=talk");
            return reforward.forward();
        } else {
            return "Invalid meeting code.";
        }
    }

    public Object start(String name) throws ApplicationException {
        final Request request = (Request) this.context.getAttribute("HTTP_REQUEST");
        final Response response = (Response) this.context.getAttribute("HTTP_RESPONSE");
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
        final Request request = (Request) this.context.getAttribute("HTTP_REQUEST");
        final Object meetingCode = request.getSession().getAttribute("meeting_code");
        final String sessionId = request.getSession().getId();
        if (meetingCode != null && sessions.get(meetingCode) != null && sessions.get(meetingCode).contains(sessionId)) {
            if (request.getSession().getAttribute("user") == null) {
                return "{ \"error\": \"missing user\" }";
            }

            Builder builder = new Builder();
            builder.put("user", request.getSession().getAttribute("user"));
            builder.put("cmd", request.getParameter("cmd"));

            return this.save(meetingCode, builder);
        }
//    response.setStatus(ResponseStatus.BAD_REQUEST);
        return "{ \"error\": \"expired\" }";
    }

    public String save() {
        final Request request = (Request) this.context.getAttribute("HTTP_REQUEST");
        final Object meetingCode = request.getSession().getAttribute("meeting_code");
        if (this.meetings.containsKey(meetingCode)) {
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

//    response.setStatus(ResponseStatus.BAD_REQUEST);
        return "{ \"error\": \"expired\" }";
    }

    public String update() throws ApplicationException, IOException {
        final Request request = (Request) this.context.getAttribute("HTTP_REQUEST");
        final Object meetingCode = request.getSession().getAttribute("meeting_code");
        final String sessionId = request.getSession().getId();
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

    public String upload() throws ApplicationException {
        final Request request = (Request) this.context.getAttribute("HTTP_REQUEST");

        // Create path components to save the file
        final String path = this.config.get("system.directory") != null ? this.config.get("system.directory").toString() + "/files" : "files";

        final Builders builders = new Builders();
        try {
            final MultipartFormData iter = new MultipartFormData(request);
            ContentDisposition e;
            int read = 0;
            while ((e = iter.getNextPart()) != null) {
                final String fileName = e.getFileName();
                final Builder builder = new Builder();
                builder.put("type", String.join(";", Arrays.asList(e.getContentType())));
                builder.put("file", new StringBuffer().append(this.context.getAttribute("HTTP_SCHEME")).append("://").append(request.headers().get(Header.HOST)).append("/files/").append(fileName));
                final File f = new File(path + File.separator + fileName);
                if (!f.exists()) {
                    if (!f.getParentFile().exists()) {
                        f.getParentFile().mkdirs();
                    }
                }

                final OutputStream out = new FileOutputStream(f);
                final BufferedOutputStream bout = new BufferedOutputStream(out);
                final ByteArrayInputStream is = new ByteArrayInputStream(e.getData());
                final BufferedInputStream bs = new BufferedInputStream(is);
                final byte[] bytes = new byte[8192];
                while ((read = bs.read(bytes)) != -1) {
                    bout.write(bytes, 0, read);
                }
                bout.close();
                bs.close();

                builders.add(builder);
                System.out.printf("File %s being uploaded to %s%n", fileName, path);
            }
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        } catch (ServletException e) {
            throw new ApplicationException(e.getMessage(), e);
        }

        return builders.toString();
    }

    public boolean topic() {
        final Request request = (Request) this.context.getAttribute("HTTP_REQUEST");
        final Object meeting_code = request.getSession().getAttribute("meeting_code");

        if (meeting_code != null && request.getParameter("topic") != null) {
            this.setVariable(meeting_code.toString(), filter(request.getParameter("topic")));
            return true;
        }

        return false;
    }

    protected talk exit() {
        final Request request = (Request) this.context.getAttribute("HTTP_REQUEST");
        request.getSession().removeAttribute("meeting_code");
        return this;
    }

    @Override
    protected String filter(String text) {
        text = text.replaceAll("<script(.*)>(.*)<\\/script>", "");
        return text;
    }

    @Override
    public void sessionCreated(HttpSessionEvent arg0) {
        Object meetingCode = arg0.getSession().getAttribute("meeting_code");
        if (meetingCode == null) {
            meetingCode = java.util.UUID.randomUUID().toString();
            arg0.getSession().setAttribute("meeting_code", meetingCode);

            System.out.println("New meeting generated by HttpSessionListener:" + meetingCode);
        }

        final String sessionId = arg0.getSession().getId();
        if (!this.list.containsKey(sessionId)) {
            this.list.put(sessionId, new ArrayDeque<Builder>());
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent arg0) {
        Object meetingCode = arg0.getSession().getAttribute("meeting_code");
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

            if ((messages = this.meetings.get(meetingCode)) != null) {
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
