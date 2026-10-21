package util;

import java.security.SecureRandom;

/**
 * Sinh mã OTP và mật khẩu ngẫu nhiên.
 */
public class OtpUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    // Bỏ các ký tự dễ nhầm (l, I, 1, O, 0) để người dùng dễ đọc từ email
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghjkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";

    /** Sinh mã OTP gồm 6 chữ số. */
    public static String generate6DigitOtp() {
        int number = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(number);
    }

    /** Sinh mật khẩu ngẫu nhiên 6 ký tự, có ít nhất 1 chữ hoa, 1 chữ thường, 1 số. */
    public static String generatePassword() {
        String all = UPPER + LOWER + DIGITS;
        StringBuilder password = new StringBuilder();

        // Bước 1: đảm bảo có đủ 3 loại ký tự
        password.append(randomChar(UPPER));
        password.append(randomChar(LOWER));
        password.append(randomChar(DIGITS));

        // Bước 2: thêm 3 ký tự bất kỳ cho đủ 6
        for (int i = 0; i < 3; i++) {
            password.append(randomChar(all));
        }

        // Bước 3: xáo trộn vị trí các ký tự
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }

    private static char randomChar(String source) {
        return source.charAt(RANDOM.nextInt(source.length()));
    }
}
