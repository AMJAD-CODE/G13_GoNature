package common;

import java.io.Serializable;
import java.sql.Date;

/**
 * Represents a promotional discount offered by a park in the
 * GoNature system.
 *
 * <p>A promotion defines a discount percentage that can be applied
 * to reservations within a specific date range. Promotions must be
 * approved before becoming active and may have statuses such as
 * pending approval, approved, rejected, or expired.</p>
 *
 * @author Rahaf Mreh
 * @version 1.0
 * @since 1.0
 */
public class Promotion implements Serializable {
	private static final long serialVersionUID = 1L;
    private int promotionId;
    private int parkId;
    private String promotionName;
    private double discountPercentage;
    private Date startDate;
    private Date endDate;
    private String status; // 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'EXPIRED'

    /**
     * Creates an empty promotion object.
     */
    public Promotion() {}

    /**
     * Creates a promotion with the specified details.
     *
     * @param promotionId the promotion identifier
     * @param parkId the identifier of the park offering the promotion
     * @param promotionName the name of the promotion
     * @param discountPercentage the discount percentage applied by the promotion
     * @param startDate the promotion start date
     * @param endDate the promotion end date
     * @param status the promotion status
     */
    public Promotion(int promotionId, int parkId, String promotionName, double discountPercentage, Date startDate, Date endDate, String status) {

        this.promotionId = promotionId;
        this.parkId = parkId;
        this.promotionName = promotionName;
        this.discountPercentage = discountPercentage;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;

    }

    /**
     * Returns the promotion identifier.
     *
     * @return the promotion identifier
     */
    public int getPromotionId() { return promotionId; }

    /**
     * Sets the promotion identifier.
     *
     * @param promotionId the promotion identifier to set
     */
    public void setPromotionId(int promotionId) { this.promotionId = promotionId; }

    /**
     * Returns the park identifier.
     *
     * @return the park identifier
     */
    public int getParkId() { return parkId; }

    /**
     * Sets the park identifier.
     *
     * @param parkId the park identifier to set
     */
    public void setParkId(int parkId) { this.parkId = parkId; }

    /**
     * Returns the promotion name.
     *
     * @return the promotion name
     */
    public String getPromotionName() { return promotionName; }

    /**
     * Sets the promotion name.
     *
     * @param promotionName the promotion name to set
     */
    public void setPromotionName(String promotionName) {
        this.promotionName = promotionName;
    }

    /**
     * Returns the discount percentage.
     *
     * @return the discount percentage
     */
    public double getDiscountPercentage() { return discountPercentage; }

    /**
     * Sets the discount percentage.
     *
     * @param discountPercentage the discount percentage to set
     */
    public void setDiscountPercentage(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    /**
     * Returns the promotion start date.
     *
     * @return the start date
     */
    public Date getStartDate() { return startDate; }

    /**
     * Sets the promotion start date.
     *
     * @param startDate the start date to set
     */
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    /**
     * Returns the promotion end date.
     *
     * @return the end date
     */
    public Date getEndDate() { return endDate; }

    /**
     * Sets the promotion end date.
     *
     * @param endDate the end date to set
     */
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    /**
     * Returns the promotion status.
     *
     * @return the promotion status
     */
    public String getStatus() { return status; }

    /**
     * Sets the promotion status.
     *
     * @param status the promotion status to set
     */
    public void setStatus(String status) { this.status = status; }

    /**
     * Returns a string representation of the promotion.
     *
     * @return a formatted promotion description
     */
    @Override
    public String toString() {
        return promotionName + " (" + (discountPercentage * 100) + "% off)";
    }
}
