package common;

import java.io.Serializable;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    // General responses
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";

    // Login
    public static final String LOGIN = "LOGIN";
    public static final String LOGOUT = "LOGOUT";

    // Parks
    public static final String GET_PARK = "GET_PARK";
    public static final String GET_ALL_PARKS = "GET_ALL_PARKS";
    public static final String UPDATE_PARK_PARAMETERS = "UPDATE_PARK_PARAMETERS";
    public static final String GET_PENDING_PARKS = "GET_PENDING_PARKS";
    public static final String APPROVE_PARK_PARAMETERS = "APPROVE_PARK_PARAMETERS";

    // Reservations
    public static final String CREATE_RESERVATION = "CREATE_RESERVATION";
    public static final String ENTER_WAITING_LIST = "ENTER_WAITING_LIST";
    public static final String GET_RESERVATION = "GET_RESERVATION";
    public static final String GET_RESERVATIONS_BY_ID = "GET_RESERVATIONS_BY_ID";
    public static final String CANCEL_RESERVATION = "CANCEL_RESERVATION";
    public static final String CONFIRM_RESERVATION = "CONFIRM_RESERVATION";

    // Entry / Exit
    public static final String REGISTER_ENTRY = "REGISTER_ENTRY";
    public static final String REGISTER_EXIT = "REGISTER_EXIT";

    // Occupancy
    public static final String GET_OCCUPANCY = "GET_OCCUPANCY";
    public static final String GET_OCCUPANCY_TABLE = "GET_OCCUPANCY_TABLE";

    // Reports
    public static final String GET_MONTHLY_VISITOR_REPORT =
            "GET_MONTHLY_VISITOR_REPORT";

    // Promotions
    public static final String CREATE_PROMOTION =
            "CREATE_PROMOTION";

    public static final String GET_PENDING_PROMOTIONS =
            "GET_PENDING_PROMOTIONS";

    public static final String APPROVE_PROMOTION =
            "APPROVE_PROMOTION";

    // Simulation
    public static final String GET_SIMULATION_TIME =
            "GET_SIMULATION_TIME";

    // Existing Order messages (if still needed)
    public static final String GET_ORDERS = "GET_ORDERS";
    public static final String ORDERS_LIST = "ORDERS_LIST";
    public static final String UPDATE_ORDER = "UPDATE_ORDER";
    public static final String UPDATE_OK = "UPDATE_OK";

    private String action;
    private Object payload;

    public Message(String action, Object payload) {
        this.action = action;
        this.payload = payload;
    }

    public String getAction() {
        return action;
    }

    public Object getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Message[action=" + action +
               ", payload=" + payload + "]";
    }
}