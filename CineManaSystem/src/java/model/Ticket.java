package model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vé (bảng Ticket) kèm thông tin suất chiếu để in vé / soát vé.
 */
public class Ticket {

    private int ticketId;
    private int bookingId;
    private String ticketCode;
    private int seatId;
    private String rowLabel;
    private int seatNumber;
    private String seatType;
    private BigDecimal finalPrice;
    private String status;          // Valid | Checked-In | Cancelled
    private int showtimeId;
    private String movieTitle;
    private String ageRating;
    private String roomName;
    private String roomType;
    private int branchId;
    private String branchName;
    private Date showDate;
    private Time startTime;
    private Time endTime;
    private String bookingStatus;

    public Ticket() {
    }

    public String getSeatLabel() {
        return rowLabel + seatNumber;
    }

    public String getBookingCode() {
        return String.format("CG-%08d", bookingId);
    }

    public LocalDateTime getStartDateTime() {
        return LocalDateTime.of(showDate.toLocalDate(), startTime.toLocalTime());
    }

    // Nếu giờ kết thúc nhỏ hơn giờ bắt đầu => suất chiếu kết thúc sau nửa đêm
    public LocalDateTime getEndDateTime() {
        LocalDate d = showDate.toLocalDate();
        if (endTime.toLocalTime().isBefore(startTime.toLocalTime())) {
            d = d.plusDays(1);
        }
        return LocalDateTime.of(d, endTime.toLocalTime());
    }

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public int getBranchId() {
        return branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = branchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public Date getShowDate() {
        return showDate;
    }

    public void setShowDate(Date showDate) {
        this.showDate = showDate;
    }

    public Time getStartTime() {
        return startTime;
    }

    public void setStartTime(Time startTime) {
        this.startTime = startTime;
    }

    public Time getEndTime() {
        return endTime;
    }

    public void setEndTime(Time endTime) {
        this.endTime = endTime;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }
}
