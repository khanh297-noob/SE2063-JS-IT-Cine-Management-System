package cinemanagement.model.branch;

import java.math.BigDecimal;

/** Bảng RoomTypes. */
public class RoomType {

    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_INACTIVE = "Inactive";

    private int roomTypeId;
    private String name;
    private BigDecimal surcharge = BigDecimal.ZERO;
    private String description;
    private String status = STATUS_ACTIVE;

    public boolean isActive() { return STATUS_ACTIVE.equals(status); }

    public int getRoomTypeId() { return roomTypeId; }
    public void setRoomTypeId(int roomTypeId) { this.roomTypeId = roomTypeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getSurcharge() { return surcharge; }
    public void setSurcharge(BigDecimal surcharge) { this.surcharge = surcharge; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
