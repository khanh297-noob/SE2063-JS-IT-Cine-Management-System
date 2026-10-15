package util;

import java.security.SecureRandom;

public class OtpUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate6DigitOtp() {
        int number = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(number);
    }
    
    // Tạo mật khẩu ngẫu nhiên
    public static String generatePassword() {
        // Loại bỏ ký tự dễ nhầm: l, I, 1, O, 0 và ký tự đặc biệt
        // để user dễ đọc và copy-paste từ email
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghjkmnpqrstuvwxyz";
        String digits = "23456789";
        String all = upper + lower + digits;

        int length = 6;
        StringBuilder password = new StringBuilder();

        // Đảm bảo có ít nhất 1 chữ hoa, 1 chữ thường, 1 số
        password.append(upper.charAt(RANDOM.nextInt(upper.length())));
        password.append(lower.charAt(RANDOM.nextInt(lower.length())));
        password.append(digits.charAt(RANDOM.nextInt(digits.length())));

        for (int i = 3; i < length; i++) {
            password.append(all.charAt(RANDOM.nextInt(all.length())));
        }

        // Xáo trộn vị trí
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }
}
