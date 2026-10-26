package service;

import dao.PromotionDao;
import dao.PromotionDao.PromotionData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import util.FormatUtil;
import util.MoneyUtil;

/**
 * Kiểm tra một mã khuyến mãi có áp dụng được cho đơn hàng hay không.
 *
 * Module đặt vé tại quầy có thể gọi trực tiếp validate(...) trước khi tạo Booking.
 * Dữ liệu nhập sai (thiếu mã, số tiền không hợp lệ) thì ném IllegalArgumentException.
 * Mã không dùng được là kết quả bình thường: valid = false kèm lý do, không phải lỗi.
 *
 * Các điều kiện kiểm tra theo thứ tự:
 *   1. Mã tồn tại                              (bảng Promotion)
 *   2. Đang trong thời gian áp dụng            (StartDate đến EndDate, tính cả hai đầu)
 *   3. Áp dụng cho chi nhánh này               (PromotionBranch; không có dòng nào = toàn chuỗi)
 *   4. Đơn đạt mức chi tối thiểu               (MinSpend)
 *   5. Còn lượt sử dụng                        (UsageLimit; 0 = không giới hạn; đếm đơn chưa hủy có PromoID)
 * Giảm giá là số tiền cố định (DiscountAmount) và không vượt quá giá trị đơn.
 */
public class PromotionService implements AutoCloseable {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PromotionDao dao;

    public PromotionService() {
        this(new PromotionDao());
    }

    /** Cho phép truyền DAO khác (ví dụ DAO giả khi kiểm thử logic mà không cần database). */
    PromotionService(PromotionDao dao) {
        this.dao = dao;
    }

    @Override
    public void close() {
        dao.close();
    }

    /**
     * @param code        mã khuyến mãi do nhân viên nhập
     * @param branchId    chi nhánh phát sinh đơn
     * @param orderAmount giá trị đơn hàng (trước giảm giá), đơn vị đồng, dạng chữ từ form
     * @param today       ngày hiện tại
     */
    public PromotionResult validate(String code, int branchId, String orderAmount, LocalDate today)
            throws IllegalArgumentException {
        // Bước 1: kiểm tra dữ liệu nhập
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mã khuyến mãi!");
        }
        String promoCode = code.trim();
        if (promoCode.length() > 30) {
            throw new IllegalArgumentException("Mã khuyến mãi tối đa 30 ký tự!");
        }
        BigDecimal amount = MoneyUtil.parse(orderAmount, "Giá trị đơn hàng", false);

        // Bước 2: tìm mã
        PromotionData promo = dao.findByCode(promoCode, branchId);
        if (promo == null) {
            return PromotionResult.invalid(promoCode, amount, "Mã khuyến mãi không tồn tại.");
        }

        // Bước 3: thời gian áp dụng
        LocalDate start = promo.getStartDate().toLocalDate();
        LocalDate end = promo.getEndDate().toLocalDate();
        if (today.isBefore(start)) {
            return PromotionResult.invalid(promo.getCode(), amount,
                    "Mã chưa đến thời gian áp dụng (từ " + start.format(DATE_FORMAT) + ").");
        }
        if (today.isAfter(end)) {
            return PromotionResult.invalid(promo.getCode(), amount,
                    "Mã đã hết hạn (đến hết ngày " + end.format(DATE_FORMAT) + ").");
        }

        // Bước 4: chi nhánh (có giới hạn chi nhánh mà chi nhánh này không nằm trong đó thì không dùng được)
        if (promo.getBranchRules() > 0 && promo.getBranchMatch() == 0) {
            return PromotionResult.invalid(promo.getCode(), amount, "Mã không áp dụng cho chi nhánh này.");
        }

        // Bước 5: mức chi tối thiểu
        if (amount.compareTo(promo.getMinSpend()) < 0) {
            return PromotionResult.invalid(promo.getCode(), amount,
                    "Đơn hàng cần tối thiểu " + FormatUtil.vnd(promo.getMinSpend()) + " để dùng mã này.");
        }

        // Bước 6: số lượt sử dụng
        Integer usesLeft = null; // null = không giới hạn
        if (promo.getUsageLimit() > 0) {
            usesLeft = promo.getUsageLimit() - promo.getUsedCount();
            if (usesLeft <= 0) {
                return PromotionResult.invalid(promo.getCode(), amount, "Mã đã hết lượt sử dụng.");
            }
        }

        // Bước 7: hợp lệ, tính tiền giảm (không vượt quá giá trị đơn)
        BigDecimal discount = promo.getDiscountAmount().min(amount);
        return PromotionResult.valid(promo.getCode(), amount, discount, usesLeft);
    }

    /** Kết quả kiểm tra một mã khuyến mãi. */
    public static class PromotionResult {

        private final boolean valid;
        private final String message;
        private final String code;
        private final BigDecimal orderAmount;
        private final BigDecimal discountAmount;
        private final Integer usesLeft;

        private PromotionResult(boolean valid, String message, String code, BigDecimal orderAmount,
                BigDecimal discountAmount, Integer usesLeft) {
            this.valid = valid;
            this.message = message;
            this.code = code;
            this.orderAmount = orderAmount;
            this.discountAmount = discountAmount;
            this.usesLeft = usesLeft;
        }

        static PromotionResult invalid(String code, BigDecimal orderAmount, String reason) {
            return new PromotionResult(false, reason, code, orderAmount, BigDecimal.ZERO, null);
        }

        static PromotionResult valid(String code, BigDecimal orderAmount, BigDecimal discount, Integer usesLeft) {
            return new PromotionResult(true, "Mã hợp lệ, áp dụng được cho đơn hàng này.", code, orderAmount,
                    discount, usesLeft);
        }

        public boolean isValid() { return valid; }
        /** Lý do không dùng được, hoặc thông báo hợp lệ. */
        public String getMessage() { return message; }
        public String getCode() { return code; }
        public BigDecimal getOrderAmount() { return orderAmount; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        /** Số tiền khách phải trả sau giảm giá. */
        public BigDecimal getFinalAmount() { return orderAmount.subtract(discountAmount); }
        /** Số lượt còn lại sau đơn này chưa tính, null nếu mã không giới hạn lượt. */
        public Integer getUsesLeft() { return usesLeft; }
    }
}
