package common;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String LOGIN = "LOGIN";
    public static final String LOGOUT = "LOGOUT";
    public static final String CREATE_RESERVATION = "CREATE_RESERVATION";
    
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";

    private String action;
    private Object payload;

    public Message(String action, Object payload) {
        this.action = action;
        this.payload = payload;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }

    @Override
    public String toString() {
        return "Message[action=" + action + ", payload=" + payload + "]";
    }
}
