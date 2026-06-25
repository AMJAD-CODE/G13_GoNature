package common;

import java.io.Serializable;

/**
 * Represents a communication message exchanged between
 * clients and the GoNature server.
 *
 * <p>A message consists of an action identifier and an
 * optional payload object containing the data associated
 * with that action. Messages are serialized and transmitted
 * between the client and server to perform system operations
 * such as login, reservations, park management, reports,
 * promotions, and heartbeat monitoring.</p>
 *
 * @author Rahaf Mreh
 * @version 1.0
 * @since 1.0
 */
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
	public static final String PING = "PING";
	public static final String PONG = "PONG";

	public static final String UPDATE_SUBSCRIBER = "UPDATE_SUBSCRIBER";
	public static final String GET_USER_PROFILE = "GET_USER_PROFILE";
	public static final String VALIDATE_VISITOR_LOGIN = "VALIDATE_VISITOR_LOGIN";
	public static final String GET_PARK_RESERVATIONS = "GET_PARK_RESERVATIONS";

	public static final String OK = "OK";
	public static final String ERROR = "ERROR";

	private String action;//action
	private Object payload;//data for it

	/**
	 * Creates a new message with the specified action and payload.
	 *
	 * @param action the action or command of the message
	 * @param payload the data associated with the action
	 */
	public Message(String action, Object payload) {
		this.action = action;
		this.payload = payload;
	}

	/**
	 * Returns the action associated with this message.
	 *
	 * @return the message action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * Sets the action associated with this message.
	 *
	 * @param action the message action to set
	 */
	public void setAction(String action) { 
		this.action = action; 
	}

	/**
	 * Returns the payload associated with this message.
	 *
	 * @return the message payload
	 */
	public Object getPayload() { return payload; }
	/**
	 * Sets the payload associated with this message.
	 *
	 * @param payload the message payload to set
	 */
	public void setPayload(Object payload) {
		this.payload = payload; 
	}

	/**
	 * Returns a string representation of this message.
	 *
	 * @return a string containing the action and payload values
	 */
	@Override
	public String toString(){
		return "Message[action=" + action + ", payload=" + payload + "]";
	}
}
