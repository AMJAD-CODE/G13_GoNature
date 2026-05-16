package common;

import java.io.Serializable;
import java.sql.Date;

public class Order implements Serializable {

	private static final long serialVersionUID = 1L;

	private int orderNumber;
	private Date orderDate;
	private int numberOfVisitors;
	private int confirmationCode;
	private int subscriberId;
	private Date dateOfPlacingOrder;

	public Order(int orderNumber, Date orderDate, int numberOfVisitors, int confirmationCode, int subscriberId,
			Date dateOfPlacingOrder) {
		this.orderNumber = orderNumber;
		this.orderDate = orderDate;
		this.numberOfVisitors = numberOfVisitors;
		this.confirmationCode = confirmationCode;
		this.subscriberId = subscriberId;
		this.dateOfPlacingOrder = dateOfPlacingOrder;
	}

	public int getOrderNumber() {
		return orderNumber;
	}

	public Date getOrderDate() {
		return orderDate;
	}

	public int getNumberOfVisitors() {
		return numberOfVisitors;
	}

	public int getConfirmationCode() {
		return confirmationCode;
	}

	public int getSubscriberId() {
		return subscriberId;
	}

	public Date getDateOfPlacingOrder() {
		return dateOfPlacingOrder;
	}

	public void setOrderDate(Date orderDate) {
		this.orderDate = orderDate;
	}

	public void setNumberOfVisitors(int numberOfVisitors) {
		this.numberOfVisitors = numberOfVisitors;
	}

	@Override
	public String toString() {
		return String.format("Order #%d | date=%s | visitors=%d | code=%d | subscriber=%d | placed=%s", orderNumber,
				orderDate, numberOfVisitors, confirmationCode, subscriberId, dateOfPlacingOrder);
	}
}