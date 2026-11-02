package service.f_b;

import dao.DBContext;
import dao.admin.AuditLogDAO;
import dao.f_b.BranchFBStockDAO;
import dao.f_b.F_BCheckoutDAO;
import dao.f_b.F_BOrderDAO;
import dao.promotion.PricingDAO;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import model.Booking;
import model.F_BOrder;
import model.User;
import model.payment.Payment;
import service.common.AccessService;
import service.promotion.PromotionService;

/**
 * Service tích hợp của Khánh, được Payment/POS của Nam gọi.
 * Không gọi API ngân hàng và không giả lập thanh toán.
 * payCash chỉ dùng khi nhân viên đã thực sự thu tiền mặt.
 */
public class F_BCheckoutService {
    private final F_BOrderDAO orders = new F_BOrderDAO();
    private final F_BCheckoutDAO payments = new F_BCheckoutDAO();
    private final BranchFBStockDAO stocks = new BranchFBStockDAO();
    private final PricingDAO pricing = new PricingDAO();
    private final F_BOrderService food = new F_BOrderService();
    private final PromotionService promotions = new PromotionService();
    private final AuditLogDAO logs = new AuditLogDAO();

    public void apply(User actor, int bookingId, String code) throws SQLException {
        promotions.check(actor, bookingId, code, true);
    }

    public void remove(User actor, int bookingId) throws SQLException {
        promotions.remove(actor, bookingId);
    }

    /** Nhân viên thu tiền cho đơn chỉ F&B đã có sẵn; không tạo Booking thứ hai. */
    public BigDecimal payCash(User actor, int bookingId, BigDecimal received, String ip)
            throws SQLException {
        AccessService.fnb(actor);
        return DBContext.transaction(c -> {
            Booking b = requireBooking(c, bookingId);
            AccessService.branch(actor, b.getBranchId());
            if (orders.hasTicket(c, bookingId)) {
                throw new IllegalArgumentException("Đơn có vé: dùng POS/Payment của Nam.");
            }
            if (received == null || received.signum() < 0 || received.scale() > 2
                    || received.precision() > 12 || received.compareTo(b.getTotalAmount()) < 0) {
                throw new IllegalArgumentException("Tiền khách đưa không hợp lệ hoặc không đủ.");
            }
            Payment p = payments.payment(c, bookingId);
            // Gửi lại cùng đơn không tạo thêm Payment, không trừ kho/ghi bán hàng lần hai.
            if ("Confirmed".equals(b.getStatus()) && p != null
                    && "Success".equals(p.getGatewayStatus())
                    && "Cash".equals(p.getPaymentMethod())) {
                return received.subtract(b.getTotalAmount());
            }
            requirePending(b);
            if (p != null) throw new IllegalArgumentException(
                    "Đơn đã có giao dịch; không thu tiền mặt đè lên thanh toán cũ.");
            if ("Ticket Cashier".equals(actor.getRole())) {
                model.pos.WorkShift shift = new service.pos.ShiftService().getActiveShift(actor);
                if (shift == null || shift.getBranchId() != b.getBranchId()) {
                    throw new IllegalArgumentException("Hãy vào ca tại đúng chi nhánh trước khi thu tiền.");
                }
            }
            validateOffer(c, b);
            promotions.revalidateInPayment(c, bookingId);
            payments.setCounterStaff(c, bookingId, actor.getUserID());
            payments.insertPayment(c, bookingId, b.getTotalAmount(), "Cash",
                    "FNB-CASH-" + bookingId, "Success");
            food.confirmLinesInPayment(c, bookingId);
            if (payments.confirmBooking(c, bookingId) != 1) {
                throw new SQLException("Đơn đã thay đổi trạng thái.");
            }
            BigDecimal change = received.subtract(b.getTotalAmount());
            logs.insert(c, actor.getUserID(),
                    "COUNTER_SALE|booking=" + bookingId + "|amount=" + b.getTotalAmount().toPlainString()
                            + "|method=Cash|received=" + received.toPlainString()
                            + "|change=" + change.toPlainString(), ip);
            return change;
        });
    }

    /** Khóa báo giá khi bắt đầu VNPay. Retry dùng lại reference cũ, không ghi đè Success. */
    public Payment prepareGateway(int bookingId, String proposedReference) throws SQLException {
        return DBContext.transaction(c -> {
            Booking b = requireBooking(c, bookingId);
            requirePending(b);
            if (b.getTotalAmount().signum() <= 0) {
                throw new IllegalArgumentException(
                        "Đơn 0 đồng: dùng luồng xác nhận miễn phí."
                );
            }
            Payment p = payments.payment(c, bookingId);
            if (p != null) {
                if (!"Pending".equals(p.getGatewayStatus()) || !"VNPay".equals(p.getPaymentMethod())
                        || p.getAmount().compareTo(b.getTotalAmount()) != 0) {
                    throw new IllegalArgumentException("Giao dịch đã xử lý hoặc số tiền không còn khớp.");
                }
                if (p.getTransactionCode() != null
                        && !"VNPAY-PENDING".equals(p.getTransactionCode())) return p;
            }
            validateOffer(c, b);
            promotions.revalidateInPayment(c, bookingId);
            if (p == null) {
                payments.insertPayment(c, bookingId, b.getTotalAmount(),
                        "VNPay", proposedReference, "Pending");
            } else if (payments.bindReference(c, bookingId, proposedReference) != 1) {
                throw new SQLException("Không chốt được mã giao dịch.");
            }
            return payments.payment(c, bookingId);
        });
    }

    /** Đơn được giảm hết: xác nhận 0 đồng thật, không gọi VNPay. */
    public void payFree(int bookingId) throws SQLException {
        DBContext.transaction(c -> {
            Booking b = requireBooking(c, bookingId);
            Payment p = payments.payment(c, bookingId);
            if ("Confirmed".equals(b.getStatus()) && p != null
                    && "Success".equals(p.getGatewayStatus())) return null;
            requirePending(b);
            if (b.getTotalAmount().signum() != 0 || p != null) {
                throw new IllegalArgumentException("Đơn không đủ điều kiện xác nhận 0 đồng.");
            }
            validateOffer(c, b);
            promotions.revalidateInPayment(c, bookingId);
            payments.insertPayment(c, bookingId, BigDecimal.ZERO, "Cash",
                    "FREE-" + bookingId, "Success");
            food.confirmLinesInPayment(c, bookingId);
            if (payments.confirmBooking(c, bookingId) != 1) throw new SQLException("Không xác nhận được đơn.");
            logs.insert(c, b.getUserId(), "FREE_ORDER|booking=" + bookingId, "system");
            return null;
        });
    }

    /**
     * Chỉ gọi SAU khi Nam kiểm tra chữ ký VNPay, reference và số tiền callback.
     * Payment đã chốt báo giá: không tự áp cấu hình giảm giá mới lên tiền đã thu.
     * LATE/REVIEW: ghi nhận tiền đã về; đơn không giao, cần Nam đối soát/hoàn thực tế.
     */
    public String confirmCaptured(int bookingId, String reference) throws SQLException {
        return DBContext.transaction(c -> {
            Booking b = requireBooking(c, bookingId);
            Payment p = payments.payment(c, bookingId);
            if (p == null) throw new IllegalArgumentException("Không có giao dịch.");
            if ("Success".equals(p.getGatewayStatus())) return "ALREADY";
            if ("Refunded".equals(p.getGatewayStatus())) return "ALREADY";
            if ("Failed".equals(p.getGatewayStatus())) {
                if (payments.capturedAfterFailure(c, bookingId, reference) != 1)
                    throw new SQLException("Không ghi nhận được tiền về muộn.");
                payments.cancelPending(c, bookingId);
                logs.insert(c, b.getUserId(), "PAYMENT_REVIEW|booking=" + bookingId
                                + "|amount=" + p.getAmount().toPlainString()
                                + "|reference=" + clean(reference) + "|needRefund=true|reason=after_failed",
                        "system");
                return "REVIEW";
            }
            if (!"Pending".equals(p.getGatewayStatus()))
                throw new IllegalArgumentException("Trạng thái giao dịch không hợp lệ.");
            if (p.getAmount().compareTo(b.getTotalAmount()) != 0) {
                throw new IllegalArgumentException("Số tiền Payment không khớp đơn.");
            }
            if (payments.paymentSuccess(c, bookingId, reference) != 1) {
                throw new SQLException("Không ghi nhận được tiền thanh toán.");
            }
            if ("Confirmed".equals(b.getStatus())) {
                payments.cancelForReview(c, bookingId);
                logs.insert(c, b.getUserId(), "PAYMENT_REVIEW|booking=" + bookingId
                        + "|reason=legacy_confirmed_with_pending_payment", "system");
                return "REVIEW";
            }
            if (!"Pending".equals(b.getStatus())
                    || b.getBookingDate().toLocalDateTime().plusMinutes(10).isBefore(LocalDateTime.now())) {
                payments.cancelPending(c, bookingId);
                logs.insert(c, b.getUserId(), "PAYMENT_LATE|booking=" + bookingId
                        + "|amount=" + p.getAmount().toPlainString() + "|needRefund=true", "system");
                return "LATE";
            }
            Savepoint afterMoney = c.setSavepoint();
            try {
                if (orders.hasTicket(c, bookingId) && !payments.ticketPaymentAllowed(c, bookingId)) {
                    throw new IllegalArgumentException("Vé/suất chiếu không còn đủ điều kiện.");
                }
                food.confirmLinesInPayment(c, bookingId);
                if (payments.confirmBooking(c, bookingId) != 1) throw new SQLException("Không xác nhận được đơn.");
                return "OK";
            } catch (IllegalArgumentException exception) {
                // Hoàn tác tồn/trạng thái món, giữ việc ghi nhận tiền thực tế đã về.
                c.rollback(afterMoney);
                payments.cancelPending(c, bookingId);
                logs.insert(c, b.getUserId(), "PAYMENT_REVIEW|booking=" + bookingId
                        + "|amount=" + p.getAmount().toPlainString()
                        + "|needRefund=true|reason=" + clean(exception.getMessage()), "system");
                return "REVIEW";
            }
        });
    }

    /** POS của Nam gọi trước commit, kể cả Cash và VNPay-Pending. */
    public void finishCounter(Connection c, int bookingId, BigDecimal discount) throws SQLException {
        if (c.getAutoCommit()) throw new IllegalStateException("Cần transaction POS chung.");
        Booking b = requireBooking(c, bookingId);
        requirePending(b);
        payments.setDiscount(c, bookingId, discount);
        validateOffer(c, b);
        promotions.revalidateInPayment(c, bookingId);
        Payment p = payments.payment(c, bookingId);
        if (p == null) throw new IllegalArgumentException("POS chưa tạo Payment.");
        if ("Pending".equals(p.getGatewayStatus()) && b.getTotalAmount().signum() == 0) {
            payments.zeroPayment(c, bookingId);
            p = payments.payment(c, bookingId);
        }
        if ("Success".equals(p.getGatewayStatus())) {
            food.confirmLinesInPayment(c, bookingId);
            if (payments.confirmBooking(c, bookingId) != 1) throw new SQLException("Không xác nhận được đơn POS.");
        }
    }

    /**
     * Nam/Hiệp chỉ gọi sau bước duyệt hoàn tương ứng. Đây là ghi nhận hoàn trong DB;
     * với VNPay, việc hoàn tiền thật do module Payment đối soát/thực hiện.
     */
    /**
     * true: đã ghi nhận hoàn.
     * false: mới tiếp nhận yêu cầu hoàn VNPay.
     */
    public boolean refundApproved(User actor, int bookingId, String ip)
            throws SQLException {
        AccessService.active(actor);

        return DBContext.transaction(c -> {
            Booking b = requireBooking(c, bookingId);
            boolean customer = "Customer".equals(actor.getRole());

            if (customer) {
                if (b.getUserId() != actor.getUserID()) {
                    throw new SecurityException(
                            "Đơn không thuộc tài khoản này.");
                }
            } else {
                // Thu ngân phải đi qua refundCounter để có quản lý duyệt.
                AccessService.configureBranch(actor, b.getBranchId());
            }

            Payment p = payments.payment(c, bookingId);

            if (alreadyRefunded(b, p)) {
                return true;
            }

            requireRefundable(c, b, p, customer ? 120 : 60);

            return finishRefund(
                    c,
                    b,
                    p,
                    actor.getUserID(),
                    actor.getUserID(),
                    "ORDER_REFUND_RECORDED",
                    "customer_or_staff_request",
                    ip);
        });
    }



    public boolean cancelUnpaid(int bookingId) throws SQLException {
        return DBContext.transaction(c -> {
            Booking b = requireBooking(c, bookingId);

            if (!"Pending".equals(b.getStatus())) {
                return false;
            }

            Payment p = payments.payment(c, bookingId);

            if (p != null && !"Pending".equals(p.getGatewayStatus())) {
                return false;
            }

            payments.failPending(c, bookingId);
            payments.cancelPending(c, bookingId);

            return true;
        });
    }

    /**
     * Nam gọi khi thu ngân yêu cầu hủy và quản lý đã nhập mật khẩu duyệt.
     */
    public boolean refundCounter(
            User cashier,
            User supervisor,
            int bookingId,
            String reason,
            String ip) throws SQLException {

        String normalizedReason = reason == null ? "" : reason.trim();

        if (normalizedReason.isEmpty()) {
            throw new IllegalArgumentException("Cần nhập lý do hủy.");
        }

        if (cashier == null || supervisor == null) {
            throw new SecurityException(
                    "Thiếu người thao tác hoặc người duyệt.");
        }

        return DBContext.transaction(c -> {
            // Đọc lại quyền hiện tại, không chỉ tin dữ liệu cũ trong session.
            dao.admin.AdminUserDAO users = new dao.admin.AdminUserDAO();

            User freshCashier = users.findById(c, cashier.getUserID());
            User freshSupervisor = users.findById(c, supervisor.getUserID());

            Booking b = requireBooking(c, bookingId);

            AccessService.branch(freshCashier, b.getBranchId());

            if (!"Ticket Cashier".equals(freshCashier.getRole())
                    || !"Counter".equals(b.getChannel())
                    || !Integer.valueOf(freshCashier.getUserID())
                    .equals(payments.staff(c, bookingId))) {
                throw new SecurityException(
                        "Không phải đơn do thu ngân này bán.");
            }

            // Admin được toàn hệ thống; quản lý chỉ duyệt đúng chi nhánh.
            AccessService.configureBranch(
                    freshSupervisor,
                    b.getBranchId());

            Payment p = payments.payment(c, bookingId);

            if (alreadyRefunded(b, p)) {
                return true;
            }

            requireRefundable(c, b, p, 60);

            return finishRefund(
                    c,
                    b,
                    p,
                    freshCashier.getUserID(),
                    freshSupervisor.getUserID(),
                    "COUNTER_REFUND",
                    normalizedReason,
                    ip);
        });
    }

    private boolean alreadyRefunded(Booking b, Payment p) {
        return "Cancelled".equals(b.getStatus())
                && p != null
                && "Refunded".equals(p.getGatewayStatus());
    }

    private void requireRefundable(

            Connection c,
            Booking b,
            Payment p,
            int cutoffMinutes) throws SQLException {
        if (!orders.hasTicket(c, b.getBookingId())) {
            throw new IllegalArgumentException(
                    "Không hỗ trợ hủy/hoàn đơn F&B đã thanh toán.");
        }
        if (!"Confirmed".equals(b.getStatus())
                || p == null
                || !"Success".equals(p.getGatewayStatus())) {
            throw new IllegalArgumentException(
                    "Đơn chưa đủ điều kiện hoàn.");
        }

        if (p.getAmount().compareTo(b.getTotalAmount()) != 0) {
            throw new IllegalArgumentException(
                    "Số tiền đơn và thanh toán không khớp; cần đối soát.");
        }

        // Giữ mốc thời gian đang dùng: khách 120 phút, tại quầy 60 phút.
        if (orders.hasTicket(c, b.getBookingId())
                && !payments.ticketRefundAllowed(
                c,
                b.getBookingId(),
                cutoffMinutes)) {
            throw new IllegalArgumentException(
                    "Vé đã check-in hoặc không còn đủ thời gian hủy.");
        }

        for (F_BOrder line : orders.lines(c, b.getBookingId())) {
            if (!"Confirmed".equals(line.getOrderStatus())) {
                throw new IllegalArgumentException(
                        "Món đã chế biến/giao hoặc chưa xác nhận; "
                                + "cần đối soát riêng.");
            }
        }
    }

    private boolean finishRefund(
            Connection c,
            Booking b,
            Payment p,
            int actorId,
            int approverId,
            String cashLog,
            String reason,
            String ip) throws SQLException {

        if ("VNPay".equals(p.getPaymentMethod())) {
            // Chưa có API hoàn tiền: chỉ ghi yêu cầu.
            // Giữ nguyên Booking, Payment và tồn kho.
            if (!payments.hasRefundRequest(c, b.getBookingId())) {
                logs.insert(
                        c,
                        actorId,
                        "REFUND_REQUEST|booking=" + b.getBookingId()
                                + "|amount="
                                + b.getTotalAmount().toPlainString()
                                + "|method=VNPay"
                                + "|requestedBy=" + actorId
                                + "|supervisor=" + approverId
                                + "|reason=" + clean(reason),
                        ip);
            }

            return false;
        }

        if (!"Cash".equals(p.getPaymentMethod())) {
            throw new IllegalArgumentException(
                    "Phương thức hoàn chưa được hỗ trợ.");
        }

        // Tiền mặt: thao tác này ghi nhận việc nhân viên xác nhận hoàn.
        payments.refund(c, b.getBookingId());
        food.returnLinesInRefund(c, b.getBookingId());

        logs.insert(
                c,
                actorId,
                cashLog + "|booking=" + b.getBookingId()
                        + "|amount=" + b.getTotalAmount().toPlainString()
                        + "|method=Cash"
                        + "|approvedBy=" + approverId
                        + "|reason=" + clean(reason),
                ip);

        return true;
    }

    private Booking requireBooking(Connection c, int id) throws SQLException {
        if (id <= 0) throw new IllegalArgumentException("ID đơn không hợp lệ.");
        Booking b = orders.findBooking(c, id, true);
        if (b == null || b.getBranchId() == null) throw new IllegalArgumentException("Không tìm thấy đơn/chi nhánh.");
        return b;
    }

    private void requirePending(Booking b) {
        if (!"Pending".equals(b.getStatus())
                || b.getBookingDate().toLocalDateTime().plusMinutes(10).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Đơn không còn Pending hoặc đã quá 10 phút.");
        }
    }

    private void validateOffer(Connection c, Booking b) throws SQLException {
        List<F_BOrder> lines = orders.lines(c, b.getBookingId());
        boolean hasTicket = orders.hasTicket(c, b.getBookingId());
        if (!hasTicket && lines.isEmpty()) throw new IllegalArgumentException("Đơn không có vé hoặc món.");
        if (hasTicket && !payments.ticketPaymentAllowed(c, b.getBookingId())) {
            throw new IllegalArgumentException("Vé hoặc suất chiếu không còn hợp lệ.");
        }
        pricing.subtotal(c, b.getBookingId()); // Chặn dữ liệu cũ không có UnitPrice.
        for (F_BOrder line : lines) {
            model.BranchFBStock menu = stocks.findSellableForUpdate(c, b.getBranchId(), line.getItemId());
            if (menu == null || menu.getStockQuantity() < line.getQuantity()) {
                throw new IllegalArgumentException("Món đã ngừng bán hoặc không đủ tồn.");
            }
        }
    }

    private String clean(String value) {
        if (value == null) return "";
        String s = value.replace('|', '/').replace('\r', ' ').replace('\n', ' ');
        return s.substring(0, Math.min(s.length(), 160));
    }

    public void verifyCounterOffer(Connection c, User actor, model.booking.Showtime showtime,
                                   List<model.booking.Seat> seats,
                                   List<model.booking.FnbItem> items) throws SQLException {
        AccessService.branch(actor, showtime.getBranchId());
        BigDecimal base = payments.currentShowtimeBase(c, showtime.getShowtimeId(), showtime.getBranchId());
        if (base == null || base.compareTo(showtime.getBasePrice()) != 0)
            throw new IllegalArgumentException("Suất chiếu/giá vừa thay đổi. Hãy chọn lại.");
        Set<Integer> selectedSeats = new java.util.HashSet<>();
        for (model.booking.Seat offered : seats) {
            model.booking.Seat fresh = payments.currentSeat(c, showtime.getShowtimeId(),
                    showtime.getBranchId(), offered.getSeatId());
            if (!selectedSeats.add(offered.getSeatId()) || fresh == null
                    || offered.getPrice() == null
                    || base.add(fresh.getSurcharge()).compareTo(offered.getPrice()) != 0) {
                throw new IllegalArgumentException("Ghế/giá ghế vừa thay đổi, hãy chọn lại.");
            }
        }
        Set<Integer> selectedItems = new java.util.HashSet<>();
        for (model.booking.FnbItem offered : items) {
            model.BranchFBStock fresh = stocks.findSellableForUpdate(c, showtime.getBranchId(),
                    offered.getItemId());
            if (!selectedItems.add(offered.getItemId()) || offered.getQuantity() < 1
                    || fresh == null || fresh.getStockQuantity() < offered.getQuantity()
                    || offered.getPrice() == null
                    || fresh.getEffectivePrice().compareTo(offered.getPrice()) != 0) {
                throw new IllegalArgumentException("Món/giá/kho vừa thay đổi, hãy chọn lại.");
            }
        }
    }
}
