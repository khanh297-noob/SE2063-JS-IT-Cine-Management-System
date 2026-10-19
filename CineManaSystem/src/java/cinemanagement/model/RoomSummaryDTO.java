package cinemanagement.model;

/** Một dòng trong danh sách phòng: Room + tên chi nhánh, tên loại phòng, số ghế. */
public class RoomSummaryDTO {

    private final Room room;
    private final String branchName;
    private final String roomTypeName;
    private final int seatCount;

    public RoomSummaryDTO(Room room, String branchName, String roomTypeName, int seatCount) {
        this.room = room;
        this.branchName = branchName;
        this.roomTypeName = roomTypeName;
        this.seatCount = seatCount;
    }

    public Room getRoom() { return room; }
    public String getBranchName() { return branchName; }
    public String getRoomTypeName() { return roomTypeName; }
    public int getSeatCount() { return seatCount; }
}
