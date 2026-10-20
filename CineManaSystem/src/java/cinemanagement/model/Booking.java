package cinemanagement.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import cinemanagement.util.DateTimeHelper;

public class Booking {
    private int bookingId;
    private int userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private LocalDateTime bookingDate;
    private BigDecimal totalAmount;
    private String status;
    private String channel;
    private Integer staffId;
    private LocalDateTime cancelledAt;
    private BigDecimal refundAmount;
    private String paymentMethod;
    private String paymentStatus;
    private int ticketCount;
    private String seatCodes;
    private Showtime showtime;
    private List<Ticket> tickets = new ArrayList<>();
    private String cancelBlockReason;

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public LocalDateTime getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDateTime bookingDate) { this.bookingDate = bookingDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public Integer getStaffId() { return staffId; }
    public void setStaffId(Integer staffId) { this.staffId = staffId; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public int getTicketCount() { return ticketCount; }
    public void setTicketCount(int ticketCount) { this.ticketCount = ticketCount; }
    public String getSeatCodes() { return seatCodes; }
    public void setSeatCodes(String seatCodes) { this.seatCodes = seatCodes; }
    public Showtime getShowtime() { return showtime; }
    public void setShowtime(Showtime showtime) { this.showtime = showtime; }
    public List<Ticket> getTickets() { return tickets; }
    public void setTickets(List<Ticket> tickets) { this.tickets = tickets; }
    public String getCancelBlockReason() { return cancelBlockReason; }
    public void setCancelBlockReason(String cancelBlockReason) { this.cancelBlockReason = cancelBlockReason; }

    public String getBookingCode() { return String.format("BK%06d", bookingId); }
    public String getBookingDateStr() { return DateTimeHelper.format(bookingDate); }
    public String getCancelledAtStr() { return DateTimeHelper.format(cancelledAt); }
    public boolean isCancelled() { return "Cancelled".equals(status); }
    public boolean isConfirmed() { return "Confirmed".equals(status); }
    public boolean isCancellable() { return cancelBlockReason == null; }
    public boolean isUpcoming() { return isConfirmed() && showtime != null && !showtime.isFinished(); }

    public String getDisplayStatus() {
        if (isCancelled()) return "Đã hủy";
        if ("Pending".equals(status)) return "Chờ thanh toán";
        boolean anyChecked = false;
        for (Ticket t : tickets) {
            if (t.isCheckedIn()) anyChecked = true;
        }
        if (anyChecked) return "Đã check-in";
        if (showtime != null && showtime.isFinished()) return "Đã chiếu";
        return "Đã xác nhận";
    }

    public String getStatusClass() {
        if (isCancelled()) return "cancelled";
        if ("Pending".equals(status)) return "scheduled";
        if (showtime != null && showtime.isFinished()) return "finished";
        return "open";
    }
}
