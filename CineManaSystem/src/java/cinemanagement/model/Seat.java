package cinemanagement.model;

import java.math.BigDecimal;

public class Seat {
    private int seatId;
    private int roomId;
    private String rowLabel;
    private int seatNumber;
    private int rowNum;
    private int colNum;
    private String seatType;
    private BigDecimal surcharge;
    private String color;
    private String status;

    public int getSeatId() { return seatId; }
    public void setSeatId(int seatId) { this.seatId = seatId; }
    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }
    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }
    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }
    public int getRowNum() { return rowNum; }
    public void setRowNum(int rowNum) { this.rowNum = rowNum; }
    public int getColNum() { return colNum; }
    public void setColNum(int colNum) { this.colNum = colNum; }
    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }
    public BigDecimal getSurcharge() { return surcharge; }
    public void setSurcharge(BigDecimal surcharge) { this.surcharge = surcharge; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCode() { return rowLabel + seatNumber; }
    public boolean isBroken() { return "Broken".equals(status); }
}
