package service.promotion;

import dao.DBContext;
import dao.admin.BranchDAO;
import dao.f_b.F_BOrderDAO;
import dao.promotion.PricingDAO;
import dao.promotion.PromotionDAO;
import service.common.AccessService;
import model.*;
import util.FormUtil;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;

public class PromotionService {
    private final PromotionDAO promos = new PromotionDAO();
    private final F_BOrderDAO bookings = new F_BOrderDAO();
    private final PricingDAO pricing = new PricingDAO();
    private final BranchDAO branches = new BranchDAO();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    public List<Promotion> list(User actor) throws SQLException {
        configureRole(actor);
        Integer branch = "Administrator".equals(actor.getRole()) ? null : actor.getBranchID();
        if (branch == null && !"Administrator".equals(actor.getRole()))
            throw new SecurityException("Tài khoản chưa gán chi nhánh.");
        try (Connection c = DBContext.getConnection()) { return promos.list(c, branch); }
    }

    public int save(User actor, Promotion p, boolean global, Set<Integer> requested) throws SQLException {
        configureRole(actor);
        if (p == null || p.getPromoId() < 0 || p.getStartDate() == null || p.getEndDate() == null)
            throw new IllegalArgumentException("Thông tin voucher không hợp lệ.");
        if (p.getEndDate().before(p.getStartDate())) throw new IllegalArgumentException("Ngày kết thúc không trước ngày bắt đầu.");
        p.setDiscountAmount(FormUtil.amount(p.getDiscountAmount(), false));
        p.setMinSpend(FormUtil.amount(p.getMinSpend(), true));
        if (p.getUsageLimit() < 1 || requested == null) throw new IllegalArgumentException("Giới hạn/phạm vi không hợp lệ.");
        Set<Integer> scope = new TreeSet<>(requested);
        if (global) { AccessService.admin(actor); scope.clear(); }
        else if (scope.isEmpty()) throw new IllegalArgumentException("Phải chọn chi nhánh áp dụng.");
        if (!"Administrator".equals(actor.getRole())
                && (actor.getBranchID() == null || scope.size() != 1 || !scope.contains(actor.getBranchID())))
            throw new SecurityException("Chỉ tạo voucher cho chi nhánh của mình.");
        boolean creating = p.getPromoId() == 0;
        for (int attempt = 0; attempt < 3; attempt++) {
            if (creating) p.setPromoCode(newCode());
            try {
                return DBContext.transaction(c -> {
                    if (!creating) {
                        requireExistingForEdit(actor, c, p.getPromoId());
                        if (p.getUsageLimit() < promos.usedCount(c, p.getPromoId()))
                            throw new IllegalArgumentException("Giới hạn nhỏ hơn số lượt đã dùng.");
                    }
                    for (int branch : scope)
                        if (branch <= 0 || !branches.existsActive(c, branch))
                            throw new IllegalArgumentException("Chi nhánh không hoạt động.");
                    int id;
                    if (creating) id = promos.insert(c, p);
                    else {
                        id = p.getPromoId();
                        if (promos.save(c, p) != 1) throw new SQLException("Không lưu được voucher.");
                    }
                    promos.replaceBranches(c, id, scope);
                    return id;
                });
            } catch (SQLException e) {
                if (!creating || (e.getErrorCode() != 2601 && e.getErrorCode() != 2627) || attempt == 2) throw e;
            }
        }
        throw new SQLException("Không sinh được mã voucher.");
    }

    public void discount(User actor, int id, BigDecimal amount) throws SQLException {
        configureRole(actor);
        BigDecimal checked = FormUtil.amount(amount, false);
        DBContext.transaction(c -> {
            requireExistingForEdit(actor, c, id);
            if (promos.setDiscount(c, id, checked) != 1) throw new SQLException("Không lưu được mức giảm.");
            return null;
        });
    }
    public void limit(User actor, int id, int limit) throws SQLException {
        configureRole(actor);
        if (limit < 1) throw new IllegalArgumentException("Giới hạn phải >= 1.");
        DBContext.transaction(c -> {
            requireExistingForEdit(actor, c, id);
            if (limit < promos.usedCount(c, id)) throw new IllegalArgumentException("Thấp hơn số lượt đã dùng.");
            if (promos.setLimit(c, id, limit) != 1) throw new SQLException("Không lưu được giới hạn.");
            return null;
        });
    }
    public void delete(User actor, int id) throws SQLException {
        configureRole(actor);
        DBContext.transaction(c -> {
            requireExistingForEdit(actor, c, id);
            if (promos.deleteUnused(c, id) != 1) throw new IllegalArgumentException("Voucher đã gắn vào đơn, không được xóa.");
            return null;
        });
    }

    public BigDecimal check(User actor, int booking, String rawCode, boolean apply) throws SQLException {
        if (rawCode == null) throw new IllegalArgumentException("Thiếu mã voucher.");
        String code = rawCode.trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9-]{4,20}")) throw new IllegalArgumentException("Mã voucher không hợp lệ.");
        return DBContext.transaction(c -> {
            Booking b = bookings.findBooking(c, booking, true);
            requireBooking(actor, b);
            if (!"Pending".equals(b.getStatus())) throw new IllegalArgumentException("Đơn không còn Pending.");
            if (apply && bookings.paymentExists(c, booking)) throw new IllegalArgumentException("Đơn đã vào thanh toán.");
            Promotion p = promos.findByCode(c, code, true);
            BigDecimal subtotal = FormUtil.amount(pricing.subtotal(c, booking), false);
            BigDecimal discount = validate(c, p, b, subtotal);
            if (apply && promos.apply(c, booking, p.getPromoId(), discount, subtotal.subtract(discount)) != 1)
                throw new SQLException("Không áp dụng được voucher.");
            return discount;
        });
    }
    public void remove(User actor, int booking) throws SQLException {
        DBContext.transaction(c -> {
            Booking b = bookings.findBooking(c, booking, true);
            requireBooking(actor, b);
            if (!"Pending".equals(b.getStatus()) || bookings.paymentExists(c, booking))
                throw new IllegalArgumentException("Đơn đã vào thanh toán.");
            BigDecimal subtotal = FormUtil.amount(pricing.subtotal(c, booking), false);
            if (promos.apply(c, booking, null, BigDecimal.ZERO, subtotal) != 1)
                throw new SQLException("Không gỡ được mã.");
            return null;
        });
    }

    // Nam gọi trước khi chuyển Booking Confirmed và commit, cùng Connection.
    public void revalidateInPayment(Connection c, int booking) throws SQLException {
        if (c.getAutoCommit()) throw new IllegalStateException("Phải gọi trong transaction thanh toán.");
        Booking b = bookings.findBooking(c, booking, true);
        if (b == null) throw new IllegalArgumentException("Không tìm thấy đơn.");
        if ("Confirmed".equals(b.getStatus())) return;
        if (!"Pending".equals(b.getStatus())) throw new IllegalArgumentException("Đơn không còn Pending.");
        BigDecimal subtotal = FormUtil.amount(pricing.subtotal(c, booking), false);
        BigDecimal discount = b.getPromoId() == null ? BigDecimal.ZERO
                : validate(c, promos.findById(c, b.getPromoId(), true), b, subtotal);
        if (subtotal.subtract(discount).compareTo(b.getTotalAmount()) != 0
                || discount.compareTo(b.getDiscountApplied()) != 0)
            throw new IllegalArgumentException("Giá thay đổi, cần báo giá/thanh toán lại.");
    }

    private BigDecimal validate(Connection c, Promotion p, Booking b, BigDecimal subtotal) throws SQLException {
        if (p == null) throw new IllegalArgumentException("Không tìm thấy voucher.");
        java.sql.Date today = promos.today(c);
        if (today.before(p.getStartDate()) || today.after(p.getEndDate()))
            throw new IllegalArgumentException("Voucher chưa có hiệu lực hoặc đã hết hạn.");
        Set<Integer> scope = promos.branches(c, p.getPromoId());
        if (!scope.isEmpty() && !scope.contains(b.getBranchId()))
            throw new IllegalArgumentException("Voucher không áp dụng tại chi nhánh.");
        if (subtotal.compareTo(p.getMinSpend()) < 0) throw new IllegalArgumentException("Chưa đạt MinSpend.");
        if (promos.usedCount(c, p.getPromoId(), b.getBookingId()) >= p.getUsageLimit())
            throw new IllegalArgumentException("MSG36: Hết lượt sử dụng.");
        if (b.getPromoId() != null && b.getPromoId() != p.getPromoId())
            throw new IllegalArgumentException("MSG49: Không được cộng dồn mã.");
        return FormUtil.amount(p.getDiscountAmount(), true).min(subtotal);
    }
    private void requireExistingForEdit(User actor, Connection c, int id) throws SQLException {
        if (id <= 0 || promos.findById(c, id, true) == null) throw new IllegalArgumentException("Không có voucher.");
        if ("Administrator".equals(actor.getRole())) return;
        Set<Integer> scope = promos.branches(c, id);
        if (actor.getBranchID() == null || scope.size() != 1 || !scope.contains(actor.getBranchID()))
            throw new SecurityException("Không được sửa voucher chung/chi nhánh khác.");
    }
    private void requireBooking(User actor, Booking b) {
        AccessService.active(actor);
        if (b == null || b.getBranchId() == null) throw new IllegalArgumentException("Đơn không có hoặc thiếu chi nhánh.");
        if ("Customer".equals(actor.getRole())) {
            if (b.getUserId() != actor.getUserID()) throw new SecurityException("Không được xem đơn người khác.");
        } else AccessService.branch(actor, b.getBranchId());
    }
    private void configureRole(User actor) {
        AccessService.active(actor);
        if (!Set.of("Administrator", "Branch Management").contains(actor.getRole()))
            throw new SecurityException("Chỉ quản lý được cấu hình khuyến mãi.");
    }
    private String newCode() {
        StringBuilder code = new StringBuilder("CINE-");
        for (int i = 0; i < 12; i++) code.append(LETTERS[RANDOM.nextInt(LETTERS.length)]);
        return code.toString();
    }

    public Promotion editable(User actor, int id) throws SQLException {
        configureRole(actor);
        return DBContext.transaction(c -> {
            requireExistingForEdit(actor, c, id);
            Promotion p = promos.findById(c, id, false);
            p.setBranchIds(promos.branches(c, id));
            p.setUsedCount(promos.usedCount(c, id));
            return p;
        });
    }
}
