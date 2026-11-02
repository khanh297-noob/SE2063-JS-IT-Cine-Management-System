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
    private final service.f_b.F_BCheckoutService fnbCheckout =
            new service.f_b.F_BCheckoutService();

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
            cancelUnpaidBooking(id);
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
        if (b.getSubtotal() == null) {
            throw new IllegalArgumentException(
                    "Đơn cũ chưa lưu đơn giá F&B. " + "Cần đối chiếu trước khi thanh toán."
            );
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

        // Giữ kiểm tra chủ đơn, trạng thái và thời hạn của Payment.
        checkCanPay(b, user);

        try {
            // Gọi đúng Service Promotion của Khánh.
            // Service kiểm tra điều kiện/lượt dùng và lưu:
            // PromoID + DiscountApplied + TotalAmount trong cùng transaction.
            service.promotion.PromotionService promotionService =
                    new service.promotion.PromotionService();

            promotionService.check(user, bookingId, code, true);

        } catch (java.sql.SQLException e) {
            System.getLogger(PaymentService.class.getName()).log(
                    System.Logger.Level.ERROR,
                    "Không áp dụng được voucher cho đơn " + bookingId,
                    e
            );

            throw new IllegalArgumentException(
                    "Không lưu được voucher. Hãy tải lại đơn và thử lại.",
                    e
            );

        } catch (SecurityException e) {
            throw new IllegalArgumentException(
                    "Bạn không có quyền áp voucher cho đơn này.",
                    e
            );
        }
    }

    public void removeVoucher(int bookingId, User user) {
        Booking b = bookingDAO.getBooking(bookingId);

        checkCanPay(b, user);

        try {
            // Service Khánh gỡ mã và tính lại tổng từ giá đã lưu.
            service.promotion.PromotionService promotionService =
                    new service.promotion.PromotionService();

            promotionService.remove(user, bookingId);

        } catch (java.sql.SQLException e) {
            System.getLogger(PaymentService.class.getName()).log(
                    System.Logger.Level.ERROR,
                    "Không gỡ được voucher của đơn " + bookingId,
                    e
            );

            throw new IllegalArgumentException(
                    "Không gỡ được voucher. Hãy tải lại đơn và thử lại.",
                    e
            );

        } catch (SecurityException e) {
            throw new IllegalArgumentException(
                    "Bạn không có quyền gỡ voucher của đơn này.",
                    e
            );
        }
    }

    // ===================== Tạo thanh toán =====================
    // Đơn 0đ (voucher giảm hết) thì xác nhận luôn, không cần thanh toán
    public void payFreeBooking(Booking b) {
        if (b == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn.");
        }

        try {
            fnbCheckout.payFree(b.getBookingId());
        } catch (java.sql.SQLException e) {
            throw paymentError(e);
        }
    }

    public String createVnpayUrl(
            Booking b,
            String returnUrl,
            String clientIp) {

        if (b == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn.");
        }

        if (VNPayGateway.TMN_CODE.isBlank()
                || VNPayGateway.HASH_SECRET.isBlank()) {
            throw new IllegalArgumentException("Chưa cấu hình VNPay.");
        }

        try {
            String proposedRef =
                    b.getBookingId() + "_" + System.currentTimeMillis();

            Payment p = fnbCheckout.prepareGateway(
                    b.getBookingId(), proposedRef
            );

            return VNPayGateway.createPaymentUrl(
                    p.getTransactionCode(),
                    p.getAmount(),
                    "Thanh toan don " + b.getBookingCode(),
                    returnUrl,
                    clientIp
            );

        } catch (java.sql.SQLException e) {
            throw paymentError(e);
        }
    }

    public String processVnpayResult(Map<String, String> params) {
        if (params == null
                || VNPayGateway.TMN_CODE.isBlank()
                || VNPayGateway.HASH_SECRET.isBlank()
                || !VNPayGateway.verifySignature(params)
                || !VNPayGateway.TMN_CODE.equals(params.get("vnp_TmnCode"))) {
            return "97";
        }

        String txnRef = params.get("vnp_TxnRef");
        int bookingId = getBookingIdFromTxnRef(txnRef);
        Payment p = paymentDAO.getByBookingId(bookingId);

        if (txnRef == null || txnRef.isBlank() || p == null
                || !"VNPay".equals(p.getPaymentMethod())
                || p.getTransactionCode() == null
                || !(p.getTransactionCode().equals(txnRef)
                || p.getTransactionCode().startsWith(txnRef + "-"))) {
            return "01";
        }

        String rawAmount = params.get("vnp_Amount");

        if (rawAmount == null || !rawAmount.matches("[0-9]+")) {
            return "04";
        }

        BigDecimal paid = new BigDecimal(rawAmount)
                .divide(BigDecimal.valueOf(100));

        if (paid.compareTo(p.getAmount()) != 0) {
            return "04";
        }

        if ("Success".equals(p.getGatewayStatus())
                || "Refunded".equals(p.getGatewayStatus())) {
            return "02";
        }

        boolean success =
                "00".equals(params.get("vnp_ResponseCode"))
                        && "00".equals(params.get("vnp_TransactionStatus"));

        try {
            if (success) {
                String transactionNo = params.get("vnp_TransactionNo");

                if (transactionNo == null || transactionNo.isBlank()) {
                    return "99";
                }

                String result = confirm(
                        bookingId,
                        txnRef + "-" + transactionNo
                );

                return "ALREADY".equals(result) ? "02" : "00";
            }

            if (!"Pending".equals(p.getGatewayStatus())) {
                return "02";
            }

            return cancelUnpaidBooking(bookingId) ? "00" : "02";

        } catch (IllegalArgumentException e) {
            System.getLogger(PaymentService.class.getName()).log(
                    System.Logger.Level.ERROR,
                    "Không lưu được kết quả VNPay cho đơn " + bookingId,
                    e
            );

            return "99";
        }
    }

    public int getBookingIdFromTxnRef(String txnRef) {
        try {
            return Integer.parseInt(txnRef.split("_")[0]);
        } catch (Exception e) {
            return -1;
        }
    }

    public String checkVnpayPayment(Booking input) {
        if (input == null) {
            return "FAILED";
        }

        Booking b = bookingDAO.getBooking(input.getBookingId());

        if (b == null) {
            return "WAITING";
        }

        String state = paymentState(b.getBookingId());

        if (!"WAITING".equals(state)) {
            return state;
        }

        try {
            Map<String, String> result = VNPayGateway.queryPayment(
                    b.getTransactionCode(), "127.0.0.1"
            );

            if ("00".equals(result.get("responseCode"))
                    && "00".equals(result.get("transactionStatus"))) {

                String amount = result.get("amount");
                String number = result.get("transactionNo");

                if (amount == null || number == null || number.isBlank()
                        || new BigDecimal(amount)
                        .compareTo(b.getTotalAmount()) != 0) {
                    return "WAITING";
                }

                confirm(
                        b.getBookingId(),
                        b.getTransactionCode() + "-" + number
                );

            } else if (("00".equals(result.get("responseCode"))
                    && "02".equals(result.get("transactionStatus")))
                    || getSecondsLeft(b) <= 0) {

                cancelUnpaidBooking(b.getBookingId());
            }

            return paymentState(b.getBookingId());

        } catch (IllegalArgumentException e) {
            return "WAITING";
        }
    }

    private String paymentState(int bookingId) {
        Booking b = bookingDAO.getBooking(bookingId);

        if (b == null) {
            return "WAITING";
        }

        if ("Success".equals(b.getPaymentStatus())) {
            return "Confirmed".equals(b.getStatus())
                    ? "PAID"
                    : "REVIEW";
        }

        return "Pending".equals(b.getStatus())
                && "Pending".equals(b.getPaymentStatus())
                ? "WAITING"
                : "FAILED";
    }

    private String confirm(int bookingId, String txnCode) {
        try {
            return fnbCheckout.confirmCaptured(bookingId, txnCode);
        } catch (java.sql.SQLException e) {
            throw paymentError(e);
        }
    }

    public void reconcile(
            int paymentId,
            String newStatus,
            User admin,
            String ip) {

        Payment p = paymentDAO.getById(paymentId);

        if (p == null) {
            throw new IllegalArgumentException("Không tìm thấy giao dịch.");
        }

        Booking b = bookingDAO.getBooking(p.getBookingId());

        if (b == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn.");
        }

        try {
            service.common.AccessService.configureBranch(
                    admin, b.getBranchId()
            );
        } catch (SecurityException e) {
            throw new IllegalArgumentException(
                    "Không có quyền đối soát đơn này.", e
            );
        }

        if (!"Pending".equals(p.getGatewayStatus())) {
            throw new IllegalArgumentException(
                    "Chỉ đối soát giao dịch Pending."
            );
        }

        if ("Success".equals(newStatus)) {
            String result = confirm(p.getBookingId(), null);

            if ("LATE".equals(result) || "REVIEW".equals(result)) {
                throw new IllegalArgumentException(
                        "Đã ghi nhận tiền nhưng đơn không thể giao. "
                                + "Cần đối soát/hoàn tiền."
                );
            }

        } else if ("Failed".equals(newStatus)) {
            if (!cancelUnpaidBooking(p.getBookingId())) {
                throw new IllegalArgumentException(
                        "Trạng thái đã thay đổi. Hãy tải lại giao dịch."
                );
            }

        } else {
            throw new IllegalArgumentException(
                    "Trạng thái đối soát không hợp lệ."
            );
        }

        auditLogDAO.insert(
                admin.getUserId(),
                "PAYMENT_RECONCILE|payment=" + paymentId
                        + "|status=" + newStatus,
                ip
        );
    }

    public boolean cancelUnpaidBooking(int bookingId) {
        try {
            return fnbCheckout.cancelUnpaid(bookingId);
        } catch (java.sql.SQLException e) {
            throw paymentError(e);
        }
    }

    private IllegalArgumentException paymentError(java.sql.SQLException e) {
        System.getLogger(PaymentService.class.getName()).log(
                System.Logger.Level.ERROR,
                "Không lưu được giao dịch thanh toán",
                e
        );

        return new IllegalArgumentException(
                "Chưa lưu được giao dịch. "
                        + "Hãy tải lại để kiểm tra trạng thái.",
                e
        );
    }
}