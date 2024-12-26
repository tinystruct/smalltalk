package custom.application.v1;

import org.tinystruct.system.Event;

public class SessionCreated implements Event<String> {
    String meetingCode;
    public SessionCreated(String meetingCode){
        this.meetingCode = meetingCode;
    }
    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getPayload() {
        return "New meeting generated:" + meetingCode;
    }
}
