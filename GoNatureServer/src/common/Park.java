package common;

import java.io.Serializable;

public class Park implements Serializable {
    private static final long serialVersionUID = 1L;

    private int parkId;
    private String parkName;
    private int maxQuota;
    private int currentQuota;
    private int reservedGap;
    private int stayDuration; // in hours
    private Integer pendingMaxQuota;// Nullable => meaning this field is optinal 
    private Integer pendingReservedGap;
    private Integer pendingStayDuration;
    private String pendingChangesStatus; // 'NONE', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED'

    //we use this when we build an object piece by piece with setters 
    public Park() {}

    public Park(int parkId, String parkName, int maxQuota, int currentQuota, int reservedGap, int stayDuration) {
        this.parkId = parkId;
        this.parkName = parkName;
        this.maxQuota = maxQuota;
        this.currentQuota = currentQuota;
        this.reservedGap = reservedGap;
        this.stayDuration = stayDuration;
        this.pendingChangesStatus = "NONE";
    }

    public int getParkId() { return parkId; }
    public void setParkId(int parkId) { this.parkId = parkId; }

    public String getParkName() { return parkName; }
    public void setParkName(String parkName) { this.parkName = parkName; }

    public int getMaxQuota() { return maxQuota; }
    public void setMaxQuota(int maxQuota) { this.maxQuota = maxQuota; }

    public int getCurrentQuota() { return currentQuota; }
    public void setCurrentQuota(int currentQuota) { this.currentQuota = currentQuota; }

    public int getReservedGap() { return reservedGap; }
    public void setReservedGap(int reservedGap) { this.reservedGap = reservedGap; }

    public int getStayDuration() { return stayDuration; }
    public void setStayDuration(int stayDuration) { this.stayDuration = stayDuration; }

    public Integer getPendingMaxQuota() { return pendingMaxQuota; }
    public void setPendingMaxQuota(Integer pendingMaxQuota) { this.pendingMaxQuota = pendingMaxQuota; }

    public Integer getPendingReservedGap() { return pendingReservedGap; }
    public void setPendingReservedGap(Integer pendingReservedGap) { this.pendingReservedGap = pendingReservedGap; }

    public Integer getPendingStayDuration() { return pendingStayDuration; }
    public void setPendingStayDuration(Integer pendingStayDuration) { this.pendingStayDuration = pendingStayDuration; }

    public String getPendingChangesStatus() { return pendingChangesStatus; }
    public void setPendingChangesStatus(String pendingChangesStatus) { this.pendingChangesStatus = pendingChangesStatus; }

    @Override
    public String toString() {
        return parkName + " (ID: " + parkId + ")";
    }
}
