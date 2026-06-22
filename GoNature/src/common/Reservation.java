package common;

import java.io.Serializable;
import java.sql.Timestamp;

public class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;

    private int reservationId;
    private String visitorId;
    private int parkId;
    private String parkName;
    private Timestamp visitDateTime;
    private int numberOfVisitors;
    private String email;
    private String phoneNumber;
    private String reservationType; // 'INDIVIDUAL', 'FAMILY_SUBSCRIBER', 'ORGANIZED_GROUP'
    private String status; // 'PENDING_CONFIRMATION', 'CONFIRMED', 'CANCELLED', 'WAITING_LIST', etc.
  
    private String paymentStatus; // 'UNPAID', 'PAID_IN_ADVANCE', 'PAID_AT_ENTRANCE'
    private double price;
    private Timestamp createdAt;
    
    // Added in Commit 3 for Exit/Entry validation & automated timers
    private Timestamp actualEntryTime;
    private Timestamp actualExitTime;
    private Timestamp reminderSentTime;
    private Timestamp spotPromotedTime;
    private Timestamp cancelledAt;
    private boolean isNoShow;
    private int stayDuration;

    public Reservation() {}

    public Reservation(int reservationId, String visitorId, int parkId, String parkName, Timestamp visitDateTime,
                       int numberOfVisitors, String email, String phoneNumber, String reservationType,
                       String status, String paymentStatus, double price, Timestamp createdAt,int stayDuration) {
        this.reservationId = reservationId;
        this.visitorId = visitorId;
        this.parkId = parkId;
        this.parkName = parkName;
        this.visitDateTime = visitDateTime;
        this.numberOfVisitors = numberOfVisitors;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.reservationType = reservationType;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.price = price;
        this.createdAt = createdAt;
        this.stayDuration=stayDuration;
    }

    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }

    public String getVisitorId() { return visitorId; }
    public void setVisitorId(String visitorId) { this.visitorId = visitorId; }

    public int getParkId() { return parkId; }
    public void setParkId(int parkId) { this.parkId = parkId; }

    public String getParkName() { return parkName; }
    public void setParkName(String parkName) { this.parkName = parkName; }

    public Timestamp getVisitDateTime() { return visitDateTime; }
    public void setVisitDateTime(Timestamp visitDateTime) { this.visitDateTime = visitDateTime; }

    public int getNumberOfVisitors() { return numberOfVisitors; }
    public void setNumberOfVisitors(int numberOfVisitors) { this.numberOfVisitors = numberOfVisitors; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getReservationType() { return reservationType; }
    public void setReservationType(String reservationType) { this.reservationType = reservationType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getActualEntryTime() { return actualEntryTime; }
    public void setActualEntryTime(Timestamp actualEntryTime) { this.actualEntryTime = actualEntryTime; }

    public Timestamp getActualExitTime() { return actualExitTime; }
    public void setActualExitTime(Timestamp actualExitTime) { this.actualExitTime = actualExitTime; }

    public Timestamp getReminderSentTime() { return reminderSentTime; }
    public void setReminderSentTime(Timestamp reminderSentTime) { this.reminderSentTime = reminderSentTime; }

    public Timestamp getSpotPromotedTime() { return spotPromotedTime; }
    public void setSpotPromotedTime(Timestamp spotPromotedTime) { this.spotPromotedTime = spotPromotedTime; }

    public Timestamp getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Timestamp cancelledAt) { this.cancelledAt = cancelledAt; }

    public boolean isNoShow() { return isNoShow; }
    public void setNoShow(boolean noShow) { isNoShow = noShow; }

    @Override
    public String toString() {
        return "Reservation #" + reservationId + " to " + parkName + " (visitors: " + numberOfVisitors + ", status: " + status + ")";
    }

	public int getStayDuration() {
		return stayDuration;
	}

	public void setStayDuration(int stayDuration) {
		this.stayDuration = stayDuration;
	}
}