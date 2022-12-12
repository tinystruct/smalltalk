package custom.application;

import org.tinystruct.ApplicationException;
import org.tinystruct.transfer.DistributedMessageQueue;

public class talk extends DistributedMessageQueue {

    @Override
    public void init() {
        this.setAction("talk/update", "update");
        this.setAction("talk/save", "save");
    }

    public String save(String groupId, String sessionid, String message) {
        return this.put(groupId, sessionid, message);
    }

    public String update(String sessionId) throws ApplicationException {
        return this.take(sessionId);
    }

    /**
     * This function can be override.
     *
     * @param text
     * @return
     */
    protected String filter(String text) {
        return text;
    }

    @Override
    public String version() {
        return "Talk core version:1.0 stable; Released on 2017-07-24";
    }

}
