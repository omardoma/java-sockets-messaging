package message;

import java.io.Serializable;

public class JoinRoomResponse implements Serializable {
    private String response;
    private boolean error;

    public JoinRoomResponse(String response) {
        this.response = response;
        this.error = false;
    }

    public JoinRoomResponse(String response, boolean error) {
        this.response = response;
        this.error = error;
    }

    public String getResponse() {
        return response;
    }

    public boolean isError() {
        return error;
    }
}
