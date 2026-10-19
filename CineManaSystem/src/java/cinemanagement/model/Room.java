package cinemanagement.model;

/** Bảng Rooms. */
public class Room {

    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_MAINTENANCE = "Maintenance";
    public static final String STATUS_INACTIVE = "Inactive";

    private int roomId;
    private int branchId;
    private Integer roomTypeId;
    private String roomName;
    private int totalRows;
    private int totalColumns;
    private String status = STATUS_ACTIVE;

    public boolean isActive() { return STATUS_ACTIVE.equals(status); }

    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }
    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }
    public Integer getRoomTypeId() { return roomTypeId; }
    public void setRoomTypeId(Integer roomTypeId) { this.roomTypeId = roomTypeId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getTotalColumns() { return totalColumns; }
    public void setTotalColumns(int totalColumns) { this.totalColumns = totalColumns; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
