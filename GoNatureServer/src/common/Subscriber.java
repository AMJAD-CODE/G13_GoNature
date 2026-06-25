package common;

import java.io.Serializable;

/**
 * Represents a subscriber in the GoNature system.
 *
 * <p>A subscriber is a registered customer who can make reservations
 * and receive subscriber benefits. The class stores personal details,
 * contact information, family size, and optional payment information.</p>
 *
 * @author Rahaf Mreh
 * @version 1.0
 * @since 1.0
 */
public class Subscriber implements Serializable {
    private static final long serialVersionUID = 1L;

    private int subscriberId;
    private String idNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private int familySize;
    private String creditCardNumber;

    /**
     * Creates an empty subscriber object.
     */
    public Subscriber() {}

    /**
     * Creates a subscriber with the specified details.
     *
     * @param subscriberId the subscriber identifier
     * @param idNumber the subscriber's identification number
     * @param firstName the subscriber's first name
     * @param lastName the subscriber's last name
     * @param email the subscriber's email address
     * @param phoneNumber the subscriber's phone number
     * @param familySize the number of family members included in the subscription
     * @param creditCardNumber the subscriber's credit card number
     */
    public Subscriber(int subscriberId, String idNumber, String firstName,
                      String lastName, String email, String phoneNumber,
                      int familySize, String creditCardNumber) {
        this.subscriberId = subscriberId;
        this.idNumber = idNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.familySize = familySize;
        this.creditCardNumber = creditCardNumber;
    }

    /** @return the subscriber identifier */
    public int getSubscriberId() { return subscriberId; }

    /** @param subscriberId the subscriber identifier to set */
    public void setSubscriberId(int subscriberId) { this.subscriberId = subscriberId; }

    /** @return the subscriber identification number */
    public String getIdNumber() { return idNumber; }

    /** @param idNumber the identification number to set */
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    /** @return the subscriber's first name */
    public String getFirstName() { return firstName; }

    /** @param firstName the first name to set */
    public void setFirstName(String firstName) { this.firstName = firstName; }

    /** @return the subscriber's last name */
    public String getLastName() { return lastName; }

    /** @param lastName the last name to set */
    public void setLastName(String lastName) { this.lastName = lastName; }

    /** @return the subscriber's email address */
    public String getEmail() { return email; }

    /** @param email the email address to set */
    public void setEmail(String email) { this.email = email; }

    /** @return the subscriber's phone number */
    public String getPhoneNumber() { return phoneNumber; }

    /** @param phoneNumber the phone number to set */
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    /** @return the family size associated with the subscription */
    public int getFamilySize() { return familySize; }

    /** @param familySize the family size to set */
    public void setFamilySize(int familySize) { this.familySize = familySize; }

    /** @return the subscriber's credit card number */
    public String getCreditCardNumber() { return creditCardNumber; }

    /** @param creditCardNumber the credit card number to set */
    public void setCreditCardNumber(String creditCardNumber) { this.creditCardNumber = creditCardNumber; }

    /**
     * Returns a string representation of the subscriber.
     *
     * @return a formatted subscriber description
     */
    @Override
    public String toString() {
        return "Subscriber " + subscriberId + ": " + firstName + " " + lastName;
    }
}