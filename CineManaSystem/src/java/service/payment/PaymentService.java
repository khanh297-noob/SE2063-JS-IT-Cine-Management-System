package service.payment;

import dao.booking.BookingDAO;
import dao.payment.PaymentDAO;
import dao.payment.PromotionDAO;
import dao.pos.AuditLogDAO;
import model.User;
import model.booking.Booking;
import model.payment.Payment;
import model.payment.Promotion;
import thirdparty.VNPayGateway;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Nghiệp vụ thanh toán online (phần 6): checkout, voucher, VNPay (sandbox), đối soát.
 * Lỗi nghiệp vụ được báo bằng IllegalArgumentException(message) - Servlet bắt và hiển thị message.
 */
public class PaymentService {

    private final BookingDAO bookingDAO = new BookingDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final PromotionDAO promotionDAO = new PromotionDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public Booking getBooking(int bookingId) {
        return bookingDAO.getBooking(bookingId);
    }

    public Payment getPayment(int paymentId) {
        return paymentDAO.getById(paymentId);
    }

    public List<Payment> getHistory(int userId, String status) {
        return paymentDAO.getHistory(userId, status);
    }

    // ===================== Giữ ghế =====================
    public static final int HOLD_MINUTES = 10;   // thời gian giữ ghế chờ thanh toán

    /**
     * Hủy các đơn Pending đã hết giờ giữ ghế. Được PaymentSyncJob (scheduler) gọi mỗi 1 phút.
     * @return số đơn đã hủy
     */
    public int releaseExpiredBookings() {
        List<Integer> ids = bookingDAO.getExpiredPendingIds(HOLD_MINUTES);
        for (int id : ids) {
            bookingDAO.cancelUnpaid(id);
        }
        return ids.size();
    }

    // Số giây giữ ghế còn lại (trang checkout dùng để chạy đồng hồ đếm ngược)
    public long getSecondsLeft(Booking b) {
        LocalDateTime expiry = b.getBookingDate().toLocalDateTime().plusMinutes(HOLD_MINUTES);
        long seconds = Duration.between(LocalDateTime.now(), expiry).getSeconds();
        return Math.max(0, seconds);
    }

    /**
     * Kiểm tra đơn còn thanh toán được không: đúng chủ đơn, còn Pending, còn giờ giữ ghế,
     * suất chiếu chưa bắt đầu.
     */
    public void checkCanPay(Booking b, User user) {
        if (b == null) {
            throw new IllegalArgumentException("No search results.");
        }
        if (b.getUserId() != user.getUserId()) {
            throw new IllegalArgumentException("You do not have permission to perform this action.");
        }
        if (!"Pending".equals(b.getStatus()) || getSecondsLeft(b) <= 0) {
            bookingDAO.cancelUnpaid(b.getBookingId());
            throw new IllegalArgumentException("Payment timed out. Your reserved seats have been released.");
        }
        if (b.getTickets().isEmpty()) {
            throw new IllegalArgumentException("The selected seat is not available.");
        }
        if (!b.getStartDateTime().isAfter(LocalDateTime.now())) {
            bookingDAO.cancelUnpaid(b.getBookingId());
            throw new IllegalArgumentException("This screening is no longer available for booking.");
        }
    }

    // ===================== Voucher =====================
    /**
     * Kiểm tra voucher, hợp lệ thì trả về Promotion.
     * @param bookingId đơn hiện tại (để không đếm lượt dùng của chính nó), đơn tại quầy truyền 0
     * @param branchId  chi nhánh của đơn (voucher có thể chỉ áp dụng cho 1 số chi nhánh - bảng PromotionBranch)
     */
    public Promotion checkVoucher(String code, BigDecimal subtotal, int bookingId, int branchId) {
        if (code.isEmpty()) {
            throw new IllegalArgumentException("The Voucher code is required.");
        }
        if (code.length() > 20) {
            throw new IllegalArgumentException("Exceeds the maximum length of 20 characters.");
        }
        Promotion p = promotionDAO.getByCode(code);
        LocalDate today = LocalDate.now();
        if (p == null || today.isBefore(p.getStartDate().toLocalDate()) || today.isAfter(p.getEndDate().toLocalDate())) {
            throw new IllegalArgumentException("This promotion is no longer available.");
        }
        if (!promotionDAO.isUsableAtBranch(p.getPromoId(), branchId)) {
            throw new IllegalArgumentException("This promotion is not available at this branch.");
        }
        if (subtotal.compareTo(p.getMinSpend()) < 0) {
            throw new IllegalArgumentException("The order does not meet the minimum spend required for this promotion.");
        }
        if (promotionDAO.countUsed(p.getPromoId(), bookingId) >= p.getUsageLimit()) {
            throw new IllegalArgumentException("This promotion has reached its usage limit.");
        }
        return p;
    }

    // Voucher khách dùng được cho đơn này (lọc bằng chính các điều kiện của checkVoucher)
    public List<Promotion> getUsableVouchers(Booking b) {
        List<Promotion> usable = new java.util.ArrayList<>();
        for (Promotion p : promotionDAO.getActive()) {
            try {
                checkVoucher(p.getPromoCode(), b.getSubtotal(), b.getBookingId(), b.getBranchId());
                usable.add(p);
            } catch (IllegalArgumentException e) {
                // không đủ điều kiện -> không hiện trong danh sách
            }
        }
        return usable;
    }

    // Số tiền được giảm (không giảm quá giá trị đơn)
    public BigDecimal getDiscount(Promotion p, BigDecimal subtotal) {
        return p.getDiscountAmount().min(subtotal);
    }

    public void applyVoucher(int bookingId, User user, String code) {
        Booking b = bookingDAO.getBooking(bookingId);
        checkCanPay(b, user);
        Promotion p = checkVoucher(code, b.getSubtotal(), bookingId, b.getBranchId());
        BigDecimal total = b.getSubtotal().subtract(getDiscount(p, b.getSubtotal()));
        bookingDAO.updateVoucher(bookingId, p.getPromoId(), total);
    }

    public void removeVoucher(int bookingId, User user) {
        Booking b = bookingDAO.getBooking(bookingId);
        checkCanPay(b, user);
        bookingDAO.updateVoucher(bookingId, null, b.getSubtotal());
    }

    // ===================== Tạo thanh toán =====================
    // Đơn 0đ (voucher giảm hết) thì xác nhận luôn, không cần thanh toán
    public void payFreeBooking(Booking b) {
        // Cột PaymentMethod chỉ cho phép VNPay / MoMo / Cash / Credit Card (CHECK trong DB) -> ghi Cash, mã FREE-... để phân biệt
        paymentDAO.savePending(b.getBookingId(), BigDecimal.ZERO, "Cash", "FREE-" + b.getBookingCode());
        bookingDAO.confirmPaid(b.getBookingId(), null);
    }

    // VNPay: lưu Payment Pending rồi trả về link chuyển sang VNPay
    public String createVnpayUrl(Booking b, String returnUrl, String clientIp) {
        String txnRef = b.getBookingId() + "_" + System.currentTimeMillis();
        paymentDAO.savePending(b.getBookingId(), b.getTotalAmount(), "VNPay", txnRef);
        return VNPayGateway.createPaymentUrl(txnRef, b.getTotalAmount(),
                "Thanh toan ve xem phim don " + b.getBookingCode(), returnUrl, clientIp);
    }

    // ===================== Kết quả VNPay =====================
    /**
     * Xử lý kết quả VNPay gửi về (dùng cho cả Return URL và IPN).
     * @return mã theo chuẩn VNPay: 00 ok, 01 không thấy đơn, 02 đã xử lý, 04 sai số tiền, 97 sai chữ ký
     */
    public String processVnpayResult(Map<String, String> params) {
        if (!VNPayGateway.verifySignature(params)) {
            return "97";
        }
        String txnRef = params.get("vnp_TxnRef");
        int bookingId = getBookingIdFromTxnRef(txnRef);
        Payment p = paymentDAO.getByBookingId(bookingId);
        if (p == null || p.getTransactionCode() == null || !p.getTransactionCode().startsWith(txnRef)) {
            return "01";
        }
        BigDecimal paid = new BigDecimal(params.get("vnp_Amount")).divide(BigDecimal.valueOf(100));
        if (paid.compareTo(p.getAmount()) != 0) {
            return "04";
        }
        if (!"Pending".equals(p.getGatewayStatus())) {
            return "02";
        }
        boolean success = "00".equals(params.get("vnp_ResponseCode"))
                && "00".equals(params.get("vnp_TransactionStatus"));
        if (success) {
            confirm(bookingId, txnRef + "-" + params.get("vnp_TransactionNo"));
        } else {
            bookingDAO.cancelUnpaid(bookingId);
        }
        return "00";
    }

    // txnRef dạng "{bookingId}_{millis}" -> bookingId
    public int getBookingIdFromTxnRef(String txnRef) {
        try {
            return Integer.parseInt(txnRef.split("_")[0]);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Hỏi VNPay xem đơn đã được thanh toán chưa (dùng cho VNPay QR tại quầy).
     * @return "PAID" (đã trả, đơn Confirmed) | "WAITING" (khách chưa trả xong) | "FAILED" (thất bại / hết giờ, đơn đã hủy)
     */
    public String checkVnpayPayment(Booking b) {
        if ("Confirmed".equals(b.getStatus())) {
            return "PAID";      // Return URL đã xác nhận trước rồi
        }
        if (!"Pending".equals(b.getStatus()) || !"Pending".equals(b.getPaymentStatus())) {
            return "FAILED";
        }
        Map<String, String> r;
        try {
            r = VNPayGateway.queryPayment(b.getTransactionCode(), "127.0.0.1");
        } catch (IllegalArgumentException e) {
            return "WAITING";   // mất mạng tạm thời -> lần tải lại sau hỏi tiếp
        }
        if ("00".equals(r.get("responseCode")) && "00".equals(r.get("transactionStatus"))) {
            // Đã trả: kiểm tra đúng số tiền rồi xác nhận đơn
            if (r.get("amount") != null && new BigDecimal(r.get("amount")).compareTo(b.getTotalAmount()) != 0) {
                return "WAITING";
            }
            confirm(b.getBookingId(), b.getTransactionCode() + "-" + r.get("transactionNo"));
            return "PAID";
        }
        if ("00".equals(r.get("responseCode")) && "02".equals(r.get("transactionStatus"))) {
            bookingDAO.cancelUnpaid(b.getBookingId());     // giao dịch lỗi
            return "FAILED";
        }
        if (getSecondsLeft(b) <= 0) {
            bookingDAO.cancelUnpaid(b.getBookingId());     // hết 10 phút giữ ghế
            return "FAILED";
        }
        return "WAITING";       // "91" chưa có giao dịch / "01" khách đang thanh toán
    }

    // Xác nhận đã nhận tiền. Nếu tiền về khi đơn đã bị hủy (hết giờ giữ ghế) thì ghi log để hoàn tiền thủ công.
    private void confirm(int bookingId, String txnCode) {
        String result = bookingDAO.confirmPaid(bookingId, txnCode);
        if ("LATE".equals(result)) {
            Booking b = bookingDAO.getBooking(bookingId);
            auditLogDAO.insert(b.getUserId(), "PAYMENT_LATE|booking=" + bookingId
                    + "|amount=" + b.getTotalAmount().toPlainString() + "|needRefund=true", "system");
        }
    }

    // ===================== Admin đối soát =====================
    /**
     * Admin / Quản lý cập nhật thủ công giao dịch đang Pending (đối chiếu với trang quản trị VNPay).
     * Success -> xác nhận đơn; Failed -> hủy đơn và nhả ghế.
     */
    public void reconcile(int paymentId, String newStatus, User admin, String ip) {
        Payment p = paymentDAO.getById(paymentId);
        if (p == null) {
            throw new IllegalArgumentException("No search results.");
        }
        if (!"Pending".equals(p.getGatewayStatus())) {
            throw new IllegalArgumentException("Only Pending payments can be reconciled.");
        }
        if ("Success".equals(newStatus)) {
            confirm(p.getBookingId(), null);
        } else if ("Failed".equals(newStatus)) {
            bookingDAO.cancelUnpaid(p.getBookingId());
        } else {
            throw new IllegalArgumentException("Invalid status.");
        }
        auditLogDAO.insert(admin.getUserId(), "PAYMENT_RECONCILE|payment=" + paymentId + "|status=" + newStatus, ip);
    }
}
