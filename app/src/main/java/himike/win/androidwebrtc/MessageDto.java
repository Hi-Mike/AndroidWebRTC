package himike.win.androidwebrtc;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

/**
 * Created by HiMike on 2017/3/17.
 */

public class MessageDto {
    private IceCandidate candidate;
    private SessionDescription description;
    private String type;

    public MessageDto() {
    }

    public MessageDto(IceCandidate candidate, String type) {
        this.candidate = candidate;
        this.type = type;
    }

    public MessageDto(SessionDescription description, String type) {
        this.description = description;
        this.type = type;
    }

    public IceCandidate getCandidate() {
        return candidate;
    }

    public void setCandidate(IceCandidate candidate) {
        this.candidate = candidate;
    }

    public SessionDescription getDescription() {
        return description;
    }

    public void setDescription(SessionDescription description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
