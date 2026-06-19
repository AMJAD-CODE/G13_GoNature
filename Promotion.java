package common;

import java.io.Serializable;
import java.sql.Date;

public class Promotion implements Serializable {
    private static final long serialVersionUID = 1L;

    private int promotionId;
    private int parkId;
    private String promotionName;
    private double discountPercentage;
    private Date startDate;
    private Date endDate;
    private String status; // 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'EXPIRED'

    public Promotion() {}

    public Promotion(int promotionId, int parkId, String promotionName, double discountPercentage, Date startDate, Date endDate, String status) {
        this.promotionId = promotionId;
        this.parkId = parkId;
        this.promotionName = promotionName;
        this.discountPercentage = discountPercentage;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public int getPromotionId() { return promotionId; }
    public void setPromotionId(int promotionId) { this.promotionId = promotionId; }

    public int getParkId() { return parkId; }
    public void setParkId(int parkId) { this.parkId = parkId; }

    public String getPromotionName() { return promotionName; }
    public void setPromotionName(String promotionName) { this.promotionName = promotionName; }

    public double getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(double discountPercentage) { this.discountPercentage = discountPercentage; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return promotionName + " (" + (discountPercentage * 100) + "% off)";
    }
}
