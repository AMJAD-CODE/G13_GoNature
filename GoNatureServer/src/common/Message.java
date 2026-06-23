package common;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    // Actions
    public static final String LOGIN = "LOGIN";
    public static final String LOGOUT = "LOGOUT";
    
    public static final String GET_PARK = "GET_PARK";
    public static final String GET_ALL_PARKS = "GET_ALL_PARKS";
    public static final String UPDATE_PARK_PARAMETERS = "UPDATE_PARK_PARAMETERS";
    public static final String GET_PENDING_PARKS = "GET_PENDING_PARKS";
    public static final String APPROVE_PARK_PARAMETERS = "APPROVE_PARK_PARAMETERS";
    
    public static final String CREATE_RESERVATION = "CREATE_RESERVATION";
    public static final String GET_OCCUPANCY = "GET_OCCUPANCY";
    public static final String GET_RESERVATION = "GET_RESERVATION";
    public static final String GET_RESERVATIONS_BY_ID = "GET_RESERVATIONS_BY_ID";
    public static final String CANCEL_RESERVATION = "CANCEL_RESERVATION";
    public static final String CONFIRM_RESERVATION = "CONFIRM_RESERVATION";
    public static final String ENTER_WAITING_LIST = "ENTER_WAITING_LIST";
    public static final String GET_OCCUPANCY_TABLE = "GET_OCCUPANCY_TABLE";
    
    public static final String REGISTER_SUBSCRIBER = "REGISTER_SUBSCRIBER";
    public static final String REGISTER_GUIDE = "REGISTER_GUIDE";
    
    public static final String REGISTER_ENTRY = "REGISTER_ENTRY";
    public static final String REGISTER_EXIT = "REGISTER_EXIT";
    
    public static final String GET_MONTHLY_VISITOR_REPORT = "GET_MONTHLY_VISITOR_REPORT";
    public static final String GET_MONTHLY_USAGE_REPORT = "GET_MONTHLY_USAGE_REPORT";
    public static final String GET_MONTHLY_VISITS_REPORT = "GET_MONTHLY_VISITS_REPORT";
    public static final String GET_MONTHLY_CANCELLATIONS_REPORT = "GET_MONTHLY_CANCELLATIONS_REPORT";
    
    public static final String CREATE_PROMOTION = "CREATE_PROMOTION";
    public static final String GET_PENDING_PROMOTIONS = "GET_PENDING_PROMOTIONS";
    public static final String APPROVE_PROMOTION = "APPROVE_PROMOTION";
    
    public static final String SERVER_ALERT = "SERVER_ALERT";
    public static final String GET_SIMULATION_TIME = "GET_SIMULATION_TIME";
    
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";

    private String action;//action
    private Object payload;//data for it

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
