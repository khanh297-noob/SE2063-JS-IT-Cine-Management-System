package cinemanagement.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.mindrot.jbcrypt.BCrypt;

/**
 * SDS: util.PasswordHasher (BCrypt).
 * verify() chap nhan ca mat khau BCrypt ($2a$...) va mat khau plaintext cu trong du lieu mau,
 * de khong lam hong code dang nhap cua cac ban khac trong nhom.
 */
public final class PasswordHasher {

    private PasswordHasher() {
    }

    public static String hash(String raw) {
        return BCrypt.hashpw(raw, BCrypt.gensalt(10));
    }

    public static boolean verify(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        if (stored.startsWith("$2")) {
            try {
                return BCrypt.checkpw(raw, stored);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8), raw.getBytes(StandardCharsets.UTF_8));
    }

    /** SRS 2.2: toi thieu 8 ky tu, co it nhat 1 chu va 1 so. Tra ve null neu hop le. */
    public static String validateStrength(String pw) {
        if (pw == null || pw.length() < 8) {
            return "Mật khẩu mới phải có ít nhất 8 ký tự.";
        }
        if (!pw.matches(".*[A-Za-z].*") || !pw.matches(".*\\d.*")) {
            return "Mật khẩu mới phải có ít nhất 1 chữ cái và 1 chữ số.";
        }
        return null;
    }
}
