package util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Mã hóa và kiểm tra mật khẩu bằng BCrypt.
 */
public class PasswordUtil {

    /** Mã hóa mật khẩu (dùng khi đăng ký, đổi mật khẩu). */
    public static String hash(String passwordPlain) {
        return BCrypt.hashpw(passwordPlain, BCrypt.gensalt(12));
    }

    /** Kiểm tra mật khẩu người dùng nhập có khớp với mã hóa trong database không. */
    public static boolean verify(String passwordPlain, String passwordHash) {
        if (passwordPlain == null || passwordHash == null) {
            return false;
        }
        return BCrypt.checkpw(passwordPlain, passwordHash);
    }

    public static void main(String[] args) {
        System.out.println(hash("123456"));
    }
}
