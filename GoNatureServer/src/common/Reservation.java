
package common;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Represents a reservation in the GoNature system.
 *
 * <p>A reservation stores information about a visitor's planned
 * visit to a park, including visitor details, reservation type,
 * visit date and time, payment information, status, and activity
 * timestamps. Reservations can belong to subscribers, guides,
 * or guest visitors and may progress through several statuses
 * such as confirmed, waiting list, active, completed, or cancelled.</p>
 *
 * @author Rahaf Mreh
 * @version 1.0
 * @since 1.0
 */
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
    private String reservationType;
    private String status;
    private String paymentStatus;
    private double price;
    private Timestamp createdAt;
    private Timestamp actualEntryTime;
    private Timestamp actualExitTime;
    private Timestamp reminderSentTime;
    private Timestamp spotPromotedTime;
    private Timestamp cancelledAt;
    private boolean isNoShow;

    /**
     * Creates an empty reservation object.
     */
    public Reservation() {}

    /**
     * Creates a reservation with the specified details.
     *
     * @param reservationId the reservation identifier
     * @param visitorId the visitor identifier
     * @param parkId the park identifier
     * @param parkName the park name
     * @param visitDateTime the scheduled visit date and time
     * @param numberOfVisitors the number of visitors in the reservation
     * @param email the visitor's email address
     * @param phoneNumber the visitor's phone number
     * @param reservationType the reservation type
     * @param status the reservation status
     * @param paymentStatus the payment status
     * @param price the reservation price
     * @param createdAt the reservation creation time
     */
    public Reservation(int reservationId, String visitorId, int parkId, String parkName,
                       Timestamp visitDateTime, int numberOfVisitors, String email,
                       String phoneNumber, String reservationType, String status,
                       String paymentStatus, double price, Timestamp createdAt) {
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
    }

    /** @return the reservation identifier */
    public int getReservationId() { return reservationId; }

    /** @param reservationId the reservation identifier to set */
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }

    /** @return the visitor identifier */
    public String getVisitorId() { return visitorId; }

    /** @param visitorId the visitor identifier to set */
    public void setVisitorId(String visitorId) { this.visitorId = visitorId; }

    /** @return the park identifier */
    public int getParkId() { return parkId; }

    /** @param parkId the park identifier to set */
    public void setParkId(int parkId) { this.parkId = parkId; }

    /** @return the park name */
    public String getParkName() { return parkName; }

    /** @param parkName the park name to set */
    public void setParkName(String parkName) { this.parkName = parkName; }

    /** @return the scheduled visit date and time */
    public Timestamp getVisitDateTime() { return visitDateTime; }

    /** @param visitDateTime the visit date and time to set */
    public void setVisitDateTime(Timestamp visitDateTime) { this.visitDateTime = visitDateTime; }

    /** @return the number of visitors */
    public int getNumberOfVisitors() { return numberOfVisitors; }

    /** @param numberOfVisitors the number of visitors to set */
    public void setNumberOfVisitors(int numberOfVisitors) { this.numberOfVisitors = numberOfVisitors; }

    /** @return the visitor email address */
    public String getEmail() { return email; }

    /** @param email the email address to set */
    public void setEmail(String email) { this.email = email; }

    /** @return the visitor phone number */
    public String getPhoneNumber() { return phoneNumber; }

    /** @param phoneNumber the phone number to set */
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    /** @return the reservation type */
    public String getReservationType() { return reservationType; }

    /** @param reservationType the reservation type to set */
    public void setReservationType(String reservationType) { this.reservationType = reservationType; }

    /** @return the reservation status */
    public String getStatus() { return status; }

    /** @param status the reservation status to set */
    public void setStatus(String status) { this.status = status; }

    /** @return the payment status */
    public String getPaymentStatus() { return paymentStatus; }

    /** @param paymentStatus the payment status to set */
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    /** @return the reservation price */
    public double getPrice() { return price; }

    /** @param price the reservation price to set */
    public void setPrice(double price) { this.price = price; }

    /** @return the reservation creation timestamp */
    public Timestamp getCreatedAt() { return createdAt; }

    /** @param createdAt the creation timestamp to set */
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    /** @return the actual entry timestamp */
    public Timestamp getActualEntryTime() { return actualEntryTime; }

    /** @param actualEntryTime the entry timestamp to set */
    public void setActualEntryTime(Timestamp actualEntryTime) { this.actualEntryTime = actualEntryTime; }

    /** @return the actual exit timestamp */
    public Timestamp getActualExitTime() { return actualExitTime; }

    /** @param actualExitTime the exit timestamp to set */
    public void setActualExitTime(Timestamp actualExitTime) { this.actualExitTime = actualExitTime; }

    /** @return the reminder sent timestamp */
    public Timestamp getReminderSentTime() { return reminderSentTime; }

    /** @param reminderSentTime the reminder timestamp to set */
    public void setReminderSentTime(Timestamp reminderSentTime) { this.reminderSentTime = reminderSentTime; }

    /** @return the waiting-list promotion timestamp */
    public Timestamp getSpotPromotedTime() { return spotPromotedTime; }

    /** @param spotPromotedTime the promotion timestamp to set */
    public void setSpotPromotedTime(Timestamp spotPromotedTime) { this.spotPromotedTime = spotPromotedTime; }

    /** @return the cancellation timestamp */
    public Timestamp getCancelledAt() { return cancelledAt; }

    /** @param cancelledAt the cancellation timestamp to set */
    public void setCancelledAt(Timestamp cancelledAt) { this.cancelledAt = cancelledAt; }

    /** @return true if the visitor was marked as a no-show */
    public boolean isNoShow() { return isNoShow; }

    /** @param noShow true if the reservation should be marked as a no-show */
    public void setNoShow(boolean noShow) { isNoShow = noShow; }

    /**
     * Returns a string representation of the reservation.
     *
     * @return a formatted reservation description
     */
    @Override
    public String toString() {
        return "Reservation #" + reservationId + " to " + parkName
                + " (visitors: " + numberOfVisitors
                + ", status: " + status + ")";
    }
}

