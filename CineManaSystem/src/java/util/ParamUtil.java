package util;

/**
 * Đọc tham số từ request một cách an toàn.
 */
public class ParamUtil {

    /** Chuyển chuỗi thành số nguyên. Chuỗi rỗng hoặc không phải số thì trả về null. */
    public static Integer parseIntOrNull(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
