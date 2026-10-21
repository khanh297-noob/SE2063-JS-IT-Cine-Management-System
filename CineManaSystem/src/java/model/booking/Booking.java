package model.booking;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Đơn đặt vé (bảng Booking) kèm khách hàng, voucher, thanh toán, vé và đồ ăn.
 * Status: Pending (đang giữ ghế chờ trả tiền) -> Confirmed (đã trả) | Cancelled
 */
public class Booking {

    private int bookingId;
    private int userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerRole;
    private Integer promoId;
    private String promoCode;
    private Timestamp bookingDate;
    private BigDecimal totalAmount;   // số tiền cuối cùng phải trả (đã trừ voucher)
    private String status;

    // Thông tin suất chiếu (lấy từ vé đầu tiên)
    private String movieTitle;
    private String ageRating;
    private int branchId;
    private String branchName;
    private String branchAddress;
    private String roomName;
    private String roomType;
    private Date showDate;
    private Time startTime;

    // Thanh toán
    private String paymentMethod;
    private String paymentStatus;     // Pending | Success | Failed
    private String transactionCode;
    private Timestamp paymentTime;

    private List<Ticket> tickets = new ArrayList<>();
    private List<FnbItem> fnbItems = new ArrayList<>();

    public Booking() {
    }

    // ===== Các giá trị tính từ dữ liệu =====
    public String getBookingCode() {
        return String.format("CG-%08d", bookingId);
    }

    public BigDecimal getTicketTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Ticket t : tickets) {
            sum = sum.add(t.getFinalPrice());
        }
        return sum;
    }

    public BigDecimal getFnbTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (FnbItem f : fnbItems) {
            sum = sum.add(f.getLineTotal());
        }
        return sum;
    }

    // Tạm tính = tiền vé + tiền đồ ăn (chưa trừ voucher)
    public BigDecimal getSubtotal() {
        return getTicketTotal().add(getFnbTotal());
    }

    // Giảm giá = Tạm tính - Tổng tiền phải trả
    public BigDecimal getDiscount() {
        BigDecimal d = getSubtotal().subtract(totalAmount);
        return d.signum() < 0 ? BigDecimal.ZERO : d;
    }

    public String getSeatLabels() {
        String s = "";
        for (Ticket t : tickets) {
            s += (s.isEmpty() ? "" : ", ") + t.getSeatLabel();
        }
        return s;
    }

    public int getTicketCount() {
        return tickets.size();
    }

    public LocalDateTime getStartDateTime() {
        if (showDate == null) {
            return null;
        }
        return LocalDateTime.of(showDate.toLocalDate(), startTime.toLocalTime());
    }

    public boolean isAnyCheckedIn() {
        for (Ticket t : tickets) {
            if ("Checked-In".equals(t.getStatus())) {
                return true;
            }
        }
        return false;
    }

    public boolean isAllCheckedIn() {
        if (tickets.isEmpty()) {
            return false;
        }
        for (Ticket t : tickets) {
            if (!"Checked-In".equals(t.getStatus())) {
                return false;
            }
        }
        return true;
    }

    // ===== Getter / Setter =====
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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerRole() {
        return customerRole;
    }

    public void setCustomerRole(String customerRole) {
        this.customerRole = customerRole;
    }

    public Integer getPromoId() {
        return promoId;
    }

    public void setPromoId(Integer promoId) {
        this.promoId = promoId;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public Timestamp getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(Timestamp bookingDate) {
        this.bookingDate = bookingDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getBranchAddress() {
        return branchAddress;
    }

    public void setBranchAddress(String branchAddress) {
        this.branchAddress = branchAddress;
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public Timestamp getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(Timestamp paymentTime) {
        this.paymentTime = paymentTime;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public List<FnbItem> getFnbItems() {
        return fnbItems;
    }

    public void setFnbItems(List<FnbItem> fnbItems) {
        this.fnbItems = fnbItems;
    }
}
