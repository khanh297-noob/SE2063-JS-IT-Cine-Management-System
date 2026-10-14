package cinemanagement.model;

import java.math.BigDecimal;

public class Ticket {
    private int ticketId;
    private int bookingId;
    private int showtimeId;
    private int seatId;
    private String seatCode;
    private String seatType;
    private String ticketCode;
    private BigDecimal finalPrice;
    private String status;
    private int ownerUserId;
    private String bookingStatus;
    private int branchId;

    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }
    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public int getShowtimeId() { return showtimeId; }
    public void setShowtimeId(int showtimeId) { this.showtimeId = showtimeId; }
    public int getSeatId() { return seatId; }
    public void setSeatId(int seatId) { this.seatId = seatId; }
    public String getSeatCode() { return seatCode; }
    public void setSeatCode(String seatCode) { this.seatCode = seatCode; }
    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }
    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }
    public BigDecimal getFinalPrice() { return finalPrice; }
    public void setFinalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(int ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public boolean isValid() { return "Valid".equals(status); }
    public boolean isCheckedIn() { return "Checked-In".equals(status); }
}
