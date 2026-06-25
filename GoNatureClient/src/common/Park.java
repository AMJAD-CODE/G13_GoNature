package common;

import java.io.Serializable;

/**
 * Represents a national park in the GoNature system.
 *
 * <p>A park contains information about visitor capacity,
 * reservation limits, stay duration, and pending parameter
 * changes awaiting approval. Park objects are transferred
 * between the server and clients as part of park management
 * and reservation operations.</p>
 *
 * @author Rahaf Mreh
 * @version 1.0
 * @since 1.0
 */
public class Park implements Serializable  {
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

    /**
     * Creates an empty park object.
     *
     * <p>This constructor is useful when a park object is
     * created and populated later using setter methods.</p>
     */
    public Park() {}

    /**
     * Creates a park with the specified details.
     *
     * @param parkId the unique identifier of the park
     * @param parkName the name of the park
     * @param maxQuota the maximum number of visitors allowed
     * @param currentQuota the current number of visitors in the park
     * @param reservedGap the number of places reserved for advance reservations
     * @param stayDuration the allowed stay duration in hours
     */
    public Park(int parkId, String parkName, int maxQuota,
                int currentQuota, int reservedGap, int stayDuration) {
        this.parkId = parkId;
        this.parkName = parkName;
        this.maxQuota = maxQuota;
        this.currentQuota = currentQuota;
        this.reservedGap = reservedGap;
        this.stayDuration = stayDuration;
        this.pendingChangesStatus = "NONE";
    }

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
     * Returns the park name.
     *
     * @return the park name
     */
    public String getParkName() { return parkName; }
    /**
     * Sets the park name.
     *
     * @param parkName the park name to set
     */
    public void setParkName(String parkName) { this.parkName = parkName; }

    /**
     * Returns the maximum visitor capacity of the park.
     *
     * @return the maximum visitor quota
     */
    public int getMaxQuota() { return maxQuota; }
    /**
     * Sets the maximum visitor capacity of the park.
     *
     * @param maxQuota the maximum visitor quota
     */
    public void setMaxQuota(int maxQuota) { this.maxQuota = maxQuota; }

    /**
     * Returns the current visitor count.
     *
     * @return the current visitor quota
     */
    public int getCurrentQuota() { return currentQuota; }
    /**
     * Sets the current visitor count.
     *
     * @param currentQuota the current visitor quota
     */
    public void setCurrentQuota(int currentQuota) { this.currentQuota = currentQuota; }

    /**
     * Returns the reserved capacity gap.
     *
     * @return the reserved gap value
     */
    public int getReservedGap() { return reservedGap; }
    /**
     * Sets the reserved capacity gap.
     *
     * @param reservedGap the reserved gap value
     */
    public void setReservedGap(int reservedGap) { this.reservedGap = reservedGap; }

    /**
     * Returns the allowed stay duration.
     *
     * @return the stay duration in hours
     */
    public int getStayDuration() { return stayDuration; }
    /**
     * Sets the allowed stay duration.
     *
     * @param stayDuration the stay duration in hours
     */
    public void setStayDuration(int stayDuration){ this.stayDuration = stayDuration; }

    /**
     * Returns the pending maximum quota awaiting approval.
     *
     * @return the pending maximum quota, or null if none exists
     */
    public Integer getPendingMaxQuota() { return pendingMaxQuota; }
    /**
     * Sets the pending maximum quota awaiting approval.
     *
     * @param pendingMaxQuota the pending maximum quota
     */
    public void setPendingMaxQuota(Integer pendingMaxQuota) { this.pendingMaxQuota = pendingMaxQuota; }

    /**
     * Returns the pending reserved gap awaiting approval.
     *
     * @return the pending reserved gap, or null if none exists
     */
    public Integer getPendingReservedGap() { return pendingReservedGap; }
    /**
     * Sets the pending reserved gap awaiting approval.
     *
     * @param pendingReservedGap the pending reserved gap
     */
    public void setPendingReservedGap(Integer pendingReservedGap) { this.pendingReservedGap = pendingReservedGap; }

    /**
     * Returns the pending stay duration awaiting approval.
     *
     * @return the pending stay duration, or null if none exists
     */
    public Integer getPendingStayDuration() { return pendingStayDuration; }
    /**
     * Sets the pending stay duration awaiting approval.
     *
     * @param pendingStayDuration the pending stay duration
     */
    public void setPendingStayDuration(Integer pendingStayDuration) { this.pendingStayDuration = pendingStayDuration; }

    /**
     * Returns the status of pending parameter changes.
     *
     * @return the pending changes status
     */
    public String getPendingChangesStatus() { return pendingChangesStatus; }
    /**
     * Sets the status of pending parameter changes.
     *
     * @param pendingChangesStatus the pending changes status
     */
    public void setPendingChangesStatus(String pendingChangesStatus) { this.pendingChangesStatus = pendingChangesStatus; }

    /**
     * Returns a string representation of the park.
     *
     * @return the park name and identifier
     */
    @Override
    public String toString() {
        return parkName + " (ID: " + parkId + ")";
    }
}
