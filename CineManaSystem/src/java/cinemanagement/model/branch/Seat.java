package cinemanagement.model.branch;

/**
 * Bảng Seats.
 * rowNum / colNum: toạ độ 1-based của ghế trong ma trận (hàng 1 = A, cột 1 = cột đầu tiên).
 * rowLabel + seatNumber: nhãn in trên vé, vd "C7" (seatNumber hiện bằng colNum).
 */
public class Seat {

    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_BROKEN = "Broken";

    private int seatId;
    private int roomId;
    private int seatTypeId;
    private int rowNum;
    private int colNum;
    private String rowLabel;
    private int seatNumber;
    private String status = STATUS_ACTIVE;

    public boolean isBroken() { return STATUS_BROKEN.equals(status); }

    /** Nhãn vị trí, vd "C7". */
    public String getPosition() { return rowLabel + seatNumber; }

    /** Khoá duy nhất theo toạ độ trong ma trận, dùng để so sánh sơ đồ cũ và mới. */
    public static int coordKey(int rowNum, int colNum) { return rowNum * 100 + colNum; }
    public int getCoordKey() { return coordKey(rowNum, colNum); }

    public int getSeatId() { return seatId; }
    public void setSeatId(int seatId) { this.seatId = seatId; }
    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }
    public int getSeatTypeId() { return seatTypeId; }
    public void setSeatTypeId(int seatTypeId) { this.seatTypeId = seatTypeId; }
    public int getRowNum() { return rowNum; }
    public void setRowNum(int rowNum) { this.rowNum = rowNum; }
    public int getColNum() { return colNum; }
    public void setColNum(int colNum) { this.colNum = colNum; }
    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }
    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
