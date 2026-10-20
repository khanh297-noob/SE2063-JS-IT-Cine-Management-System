package util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Định dạng số, tiền, phần trăm để hiển thị (dấu chấm ngăn cách hàng nghìn kiểu Việt Nam).
 */
public class FormatUtil {

    /** Số nguyên có dấu chấm ngăn cách hàng nghìn. Ví dụ 1234567 thành "1.234.567". */
    public static String num(long value) {
        return vietnameseFormat("#,##0").format(value);
    }

    /** Tiền đồng. Ví dụ "1.234.567 ₫". */
    public static String vnd(BigDecimal value) {
        return num(value.longValue()) + " ₫";
    }

    /** Tiền rút gọn theo triệu. Ví dụ "1,2 Tr". */
    public static String million(BigDecimal value) {
        return vietnameseFormat("#,##0.#").format(value.doubleValue() / 1000000.0) + " Tr";
    }

    /** Phần trăm một chữ số thập phân. Ví dụ "85.5%". */
    public static String pct(double value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    /** Số một chữ số thập phân, dùng làm giá trị thô cho file CSV. Ví dụ "85.5". */
    public static String decimal1(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    /** Màu hiển thị theo tỷ lệ lấp đầy: từ 80% là good, từ 50% là warn, còn lại là bad. */
    public static String occCss(double occupancy) {
        if (occupancy >= 80) {
            return "good";
        }
        if (occupancy >= 50) {
            return "warn";
        }
        return "bad";
    }

    private static DecimalFormat vietnameseFormat(String pattern) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat(pattern, symbols);
    }
}
