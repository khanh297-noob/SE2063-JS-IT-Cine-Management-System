package cinemanagement.model;

import java.math.BigDecimal;

public class Room {
    private int roomId;
    private int branchId;
    private String branchName;
    private String roomName;
    private int capacity;
    private String roomType;
    private BigDecimal roomSurcharge;
    private int totalRows;
    private int totalColumns;
    private String status;
    private int seatCount;

    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }
    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public BigDecimal getRoomSurcharge() { return roomSurcharge; }
    public void setRoomSurcharge(BigDecimal roomSurcharge) { this.roomSurcharge = roomSurcharge; }
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getTotalColumns() { return totalColumns; }
    public void setTotalColumns(int totalColumns) { this.totalColumns = totalColumns; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getSeatCount() { return seatCount; }
    public void setSeatCount(int seatCount) { this.seatCount = seatCount; }

    public boolean isActive() { return "Active".equals(status); }
    public String getStatusLabel() {
        return "Maintenance".equals(status) ? "BẢO TRÌ" : "Inactive".equals(status) ? "NGỪNG HOẠT ĐỘNG" : "";
    }
}
