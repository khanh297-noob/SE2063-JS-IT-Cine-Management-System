package util;

import java.math.BigDecimal;

/**
 * Đọc và kiểm tra số tiền (đồng) người dùng nhập vào form.
 */
public class MoneyUtil {

    /** Giá trị lớn nhất cho một giá vé, giá món, phụ thu hoặc giá trị đơn hàng. */
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000");

    /**
     * Chuyển chuỗi thành số tiền nguyên (đồng).
     *
     * @param fieldName tên trường dùng trong thông báo lỗi, ví dụ "Giá vé"
     * @param allowZero true nếu cho phép bằng 0 (ví dụ phụ thu); false thì phải lớn hơn 0
     * @throws IllegalArgumentException nếu rỗng, không phải số nguyên, âm, bằng 0 (khi không cho phép) hoặc quá lớn
     */
    public static BigDecimal parse(String text, String fieldName, boolean allowZero) throws IllegalArgumentException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " không được để trống!");
        }
        String digits = text.trim().replace(" ", "");
        if (!digits.matches("\\d{1,10}")) {
            throw new IllegalArgumentException(fieldName + " phải là số nguyên (đồng), ví dụ 85000!");
        }
        BigDecimal value = new BigDecimal(digits);
        if (!allowZero && value.signum() == 0) {
            throw new IllegalArgumentException(fieldName + " phải lớn hơn 0!");
        }
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException(fieldName + " không được vượt quá " + FormatUtil.num(MAX_AMOUNT.longValue()) + " ₫!");
        }
        return value;
    }
}
