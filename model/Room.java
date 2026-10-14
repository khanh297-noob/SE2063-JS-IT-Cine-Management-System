package cinemanagement.model;

public class Room {
    private int roomId;
    private int branchId;
    private String branchName;
    private String roomName;
    private int capacity;
    private String roomType;
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getSeatCount() { return seatCount; }
    public void setSeatCount(int seatCount) { this.seatCount = seatCount; }

    public boolean isActive() { return "Active".equals(status); }
}
