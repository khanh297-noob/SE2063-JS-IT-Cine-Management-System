package model;

public class Booking {
    private int bookingId;
    private int userId;
    private Integer branchId;
    private Integer promoId;
    private java.math.BigDecimal totalAmount;
    private java.math.BigDecimal discountApplied;
    private String status;
    private String customerName;
    private String paymentStatus;
    private String channel;
    private java.sql.Timestamp bookingDate;

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Integer getBranchId() {
        return branchId;
    }

    public void setBranchId(Integer branchId) {
        this.branchId = branchId;
    }

    public Integer getPromoId() {
        return promoId;
    }

    public void setPromoId(Integer promoId) {
        this.promoId = promoId;
    }

    public java.math.BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(java.math.BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public java.math.BigDecimal getDiscountApplied() {
        return discountApplied;
    }

    public void setDiscountApplied(java.math.BigDecimal discountApplied) {
        this.discountApplied = discountApplied;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public java.sql.Timestamp getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(java.sql.Timestamp bookingDate) {
        this.bookingDate = bookingDate;
    }

}
