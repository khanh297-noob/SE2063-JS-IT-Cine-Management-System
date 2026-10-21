package model.movie;

import java.math.BigDecimal;

/**
 * Phòng chiếu kèm thông tin cần khi xếp suất chiếu: tên loại phòng, phụ thu loại phòng
 * (RoomType.Surcharge, dùng để tự tính giá suất chiếu) và số ghế dùng được.
 */
public class ShowtimeRoom {
    private int roomId;
    private int branchId;
    private String branchName;
    private String roomName;
    private String roomType;
    private BigDecimal roomSurcharge = BigDecimal.ZERO;
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
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public BigDecimal getRoomSurcharge() { return roomSurcharge; }
    public void setRoomSurcharge(BigDecimal roomSurcharge) { this.roomSurcharge = roomSurcharge == null ? BigDecimal.ZERO : roomSurcharge; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getSeatCount() { return seatCount; }
    public void setSeatCount(int seatCount) { this.seatCount = seatCount; }

    public boolean isActive() { return "Active".equals(status); }

    public String getStatusLabel() {
        return "Maintenance".equals(status) ? "Bảo trì" : "Inactive".equals(status) ? "Ngừng hoạt động" : "";
    }
}
