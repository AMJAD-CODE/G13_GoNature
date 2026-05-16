package common;

import java.io.Serializable;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    
    //Client to Server: Send all rows from the Order table
    public static final String GET_ORDERS = "GET_ORDERS";

    //Server to Client: Here are the orders." Payload = ArrayList<Order>
    public static final String ORDERS_LIST = "ORDERS_LIST";

    //Client to Server: update this order in the DB, Payload = Order
    public static final String UPDATE_ORDER = "UPDATE_ORDER";

    //Server to Client: update succeeded." Payload = null
    public static final String UPDATE_OK = "UPDATE_OK";

    //Either direction: something went wrong. Payload = String (error description)
    public static final String ERROR = "ERROR";

    //Fields
    private String action;
    private Object payload;

    //Constructor
    public Message(String action, Object payload) {
        this.action = action;
        this.payload = payload;
    }

    //Getters
    public String getAction() {
        return action;
    }

    public Object getPayload() {
        return payload;
    }

    //toString
    @Override
    public String toString() {
        return "Message[action=" + action + ", payload=" + payload + "]";
    }
}