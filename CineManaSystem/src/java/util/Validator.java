package util;

/**
 * Các hàm kiểm tra định dạng dữ liệu nhập vào (email, số điện thoại).
 */
public class Validator {

    /** Email hợp lệ có dạng: ten@ten-mien */
    public static boolean isValidEmail(String email) {
        return email != null && email.trim().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    /** Số điện thoại hợp lệ: bắt đầu bằng 0 hoặc +84, theo sau là 9 chữ số. */
    public static boolean isValidPhone(String phone) {
        return phone != null && phone.trim().matches("^(0|\\+84)[0-9]{9}$");
    }
}
