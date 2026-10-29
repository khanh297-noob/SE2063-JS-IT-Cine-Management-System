package service.pos;

import dao.booking.BookingDAO;
import dao.booking.FnbDAO;
import dao.booking.PosShowtimeDAO;
import dao.pos.AuditLogDAO;
import dao.pos.StaffDAO;
import model.User;
import model.booking.Booking;
import model.booking.FnbItem;
import model.booking.Seat;
import model.booking.Showtime;
import model.payment.Promotion;
import model.pos.WorkShift;
import service.movie.ShowtimePriceCalculator;
import service.payment.PaymentService;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import util.PasswordUtil;

/**
 * Bán vé tại quầy (phần 7.1): suất chiếu, sơ đồ ghế, tạo đơn + thu tiền mặt hoặc VNPay QR, hủy & hoàn tiền.
 * Đơn tại quầy lưu Booking.UserID = UserID của thu ngân (khách vãng lai không có tài khoản).
 */
public class PosService {

    private final PosShowtimeDAO posShowtimeDAO = new PosShowtimeDAO();
    private final FnbDAO fnbDAO = new FnbDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final StaffDAO staffDAO = new StaffDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final PaymentService paymentService = new PaymentService();

    public static final int MAX_SEATS = 8;

    public BigDecimal getTicketPrice(BigDecimal basePrice, BigDecimal seatSurcharge) {
        return ShowtimePriceCalculator.ticketPrice(basePrice, seatSurcharge);
    }

    // Suất chiếu trong ngày của rạp, gom theo tên phim: { "Tên phim" -> [suất 1, suất 2...] }
    public Map<String, List<Showtime>> getShowtimesByMovie(int branchId, LocalDate date, String keyword) {
        Map<String, List<Showtime>> movies = new LinkedHashMap<>();
        for (Showtime s : posShowtimeDAO.getByBranchAndDate(branchId, Date.valueOf(date), keyword)) {
            if (!movies.containsKey(s.getMovieTitle())) {
                movies.put(s.getMovieTitle(), new ArrayList<>());
            }
            movies.get(s.getMovieTitle()).add(s);
        }
        return movies;
    }

    public Showtime getShowtime(int showtimeId) {
        return posShowtimeDAO.getById(showtimeId);
    }

    // Sơ đồ ghế kèm giá từng ghế
    public List<Seat> getSeats(Showtime st) {
        List<Seat> seats = posShowtimeDAO.getSeats(st.getShowtimeId(), st.getRoomId());
        for (Seat s : seats) {
            s.setPrice(getTicketPrice(st.getBasePrice(), s.getSurcharge()));
        }
        return seats;
    }

    // Thực đơn đồ ăn của chi nhánh (bảng Branch_F_B, giá riêng của chi nhánh nếu có)
    public List<FnbItem> getFnbMenu(int branchId) {
        return fnbDAO.getMenu(branchId);
    }

    // Tạm tính = tiền các ghế đã chọn + tiền đồ ăn
    public BigDecimal getSubtotal(List<Seat> seats, List<FnbItem> items) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Seat s : seats) {
            sum = sum.add(s.getPrice());
        }
        for (FnbItem f : items) {
            sum = sum.add(f.getLineTotal());
        }
        return sum;
    }

    /**
     * Tạo đơn tại quầy và thu tiền. Lỗi thì ném IllegalArgumentException(message).
     * @param seats ghế thu ngân đã chọn (lấy từ getSeats)
     * @param items đồ ăn có số lượng >= 1
     * @return BookingID vừa tạo
     */
    public int createCounterBooking(User cashier, WorkShift shift, Showtime st, List<Seat> seats,
            List<FnbItem> items, String method, BigDecimal cashReceived, String voucherCode, String ip) {
        if (shift == null) {
            throw new IllegalArgumentException("Bạn cần bắt đầu ca làm việc trước khi bán vé.");
        }
        if (st == null || st.getBranchId() != shift.getBranchId()) {
            throw new IllegalArgumentException("No search results.");
        }
        if (st.isStarted()) {
            throw new IllegalArgumentException("This screening is no longer available for booking.");
        }
        if (seats.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 ghế.");
        }
        if (seats.size() > MAX_SEATS) {
            throw new IllegalArgumentException("You cannot book more than " + MAX_SEATS + " seats per transaction.");
        }
        for (Seat s : seats) {
            if (!"AVAILABLE".equals(s.getState())) {
                throw new IllegalArgumentException("The selected seat is not available. (" + s.getLabel() + ")");
            }
        }
        if (!"Cash".equals(method) && !"VNPay".equals(method)) {
            throw new IllegalArgumentException("The Payment method is required.");
        }

        // Tính tiền
        BigDecimal subtotal = getSubtotal(seats, items);
        Integer promoId = null;
        BigDecimal discount = BigDecimal.ZERO;
        if (!voucherCode.isEmpty()) {
            Promotion p = paymentService.checkVoucher(voucherCode, subtotal, 0, shift.getBranchId());
            promoId = p.getPromoId();
            discount = paymentService.getDiscount(p, subtotal);
        }
        BigDecimal total = subtotal.subtract(discount);
        if ("Cash".equals(method) && (cashReceived == null || cashReceived.compareTo(total) < 0)) {
            throw new IllegalArgumentException("Tiền khách đưa không đủ (cần " + String.format("%,.0f ₫", total) + ").");
        }

        // Lưu DB (1 transaction). VNPay: mã tạm, sẽ được thay bằng txnRef khi tạo link thanh toán VNPay
        String txnCode = "Cash".equals(method) ? "CASH-" + System.currentTimeMillis() : "VNPAY-PENDING";
        int bookingId = bookingDAO.createCounterBooking(cashier.getUserId(), shift.getBranchId(), promoId, total,
                st.getShowtimeId(), seats, items, method, txnCode);
        if (bookingId < 0) {
            throw new IllegalArgumentException("The selected seat is not available.");
        }
        auditLogDAO.insert(cashier.getUserId(), "COUNTER_SALE|booking=" + bookingId
                + "|amount=" + total.toPlainString() + "|method=" + method, ip);
        return bookingId;
    }

    public Booking getBooking(int bookingId) {
        return bookingDAO.getBooking(bookingId);
    }

    // ===================== Thanh toán VNPay QR tại quầy =====================
    /**
     * Tạo link thanh toán VNPay sandbox cho đơn tại quầy vừa tạo (đơn đang Pending).
     * Lỗi thì hủy đơn để nhả ghế rồi báo lỗi.
     * @return link thanh toán - JSP vẽ link này thành mã QR
     */
    public String createVnpayQr(int bookingId, String returnUrl, String clientIp) {
        Booking b = bookingDAO.getBooking(bookingId);
        try {
            return paymentService.createVnpayUrl(b, returnUrl, clientIp);
        } catch (RuntimeException e) {
            bookingDAO.cancelUnpaid(bookingId);
            throw new IllegalArgumentException("Không tạo được thanh toán VNPay: " + e.getMessage());
        }
    }

    /**
     * Hỏi VNPay khách đã trả tiền chưa (màn hình QR tự tải lại mỗi 5 giây sẽ gọi hàm này).
     * @return "PAID" | "WAITING" | "FAILED"
     */
    public String checkVnpayPayment(Booking b) {
        return paymentService.checkVnpayPayment(b);
    }

    // Thu ngân bấm Hủy trên màn hình QR (khách đổi ý / muốn trả tiền mặt) -> hủy đơn, nhả ghế
    public void cancelVnpayPayment(User cashier, int bookingId, String ip) {
        Booking b = bookingDAO.getBooking(bookingId);
        if (b == null || b.getUserId() != cashier.getUserId()) {
            throw new IllegalArgumentException("No search results.");
        }
        if (bookingDAO.cancelUnpaid(bookingId)) {
            auditLogDAO.insert(cashier.getUserId(), "COUNTER_VNPAY_CANCEL|booking=" + bookingId, ip);
        }
    }

    // Giao dịch tại quầy của thu ngân trong 1 ngày
    public List<Booking> getTransactions(User cashier, LocalDate date, String status) {
        return bookingDAO.getCounterBookings(cashier.getUserId(), Date.valueOf(date), status);
    }

    /**
     * Hủy đơn tại quầy & hoàn tiền - cần quản lý chi nhánh nhập email + mật khẩu để duyệt.
     * Điều kiện: đơn do chính thu ngân tạo, đang Confirmed, chưa vé nào check-in, còn >= 60 phút tới giờ chiếu.
     */
    public void cancelCounterBooking(User cashier, int bookingId, String managerEmail, String managerPassword,
            String reason, String ip) {
        Booking b = bookingDAO.getBooking(bookingId);
        if (b == null) {
            throw new IllegalArgumentException("No search results.");
        }
        if (b.getUserId() != cashier.getUserId()) {
            throw new IllegalArgumentException("You do not have permission to perform this action.");
        }
        if (!"Confirmed".equals(b.getStatus()) || b.isAnyCheckedIn()
                || LocalDateTime.now().plusMinutes(60).isAfter(b.getStartDateTime())) {
            throw new IllegalArgumentException("This booking can no longer be cancelled or refunded.");
        }
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("The Reason is required.");
        }
        // Mật khẩu trong DB đã mã hóa BCrypt nên kiểm tra bằng PasswordUtil của nhóm
        User manager = staffDAO.getByEmail(managerEmail);
        if (manager == null || !PasswordUtil.verify(managerPassword, manager.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect username or password. Please check again.");
        }
        if (!"Branch Management".equals(manager.getRole()) && !"Administrator".equals(manager.getRole())) {
            throw new IllegalArgumentException("You do not have permission to perform this action.");
        }
        if (!bookingDAO.cancelCounterBooking(bookingId)) {
            throw new IllegalArgumentException("Refund failed. Please contact support.");
        }
        String safeReason = reason.replace("|", "/");
        if (safeReason.length() > 60) {
            safeReason = safeReason.substring(0, 60);
        }
        auditLogDAO.insert(cashier.getUserId(), "COUNTER_REFUND|booking=" + bookingId
                + "|amount=" + b.getTotalAmount().toPlainString()
                + "|method=" + b.getPaymentMethod()
                + "|approvedBy=" + manager.getUserId()
                + "|reason=" + safeReason, ip);
    }
}
