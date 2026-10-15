package util;

import java.util.HashMap;
import java.util.Map;

/**
 * Giới hạn số lần nhập sai OTP và thời gian chờ giữa các lần gửi lại mã.
 * Lưu trong bộ nhớ server (theo email) nên mất khi restart Tomcat.
 */
public class OtpGuard {

    public static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MS = 15 * 60 * 1000L;
    private static final long RESEND_COOLDOWN_MS = 60 * 1000L;

    private static class State {
        int fails;
        long lockUntil;
        long lastSent;
    }

    private static final Map<String, State> STATES = new HashMap<>();

    private static State state(String email) {
        String key = email.trim().toLowerCase();
        State s = STATES.get(key);
        if (s == null) {
            s = new State();
            STATES.put(key, s);
        }
        return s;
    }

    /** Số giây còn bị khóa nhập OTP (0 nếu không bị khóa). */
    public static synchronized long lockRemainingSeconds(String email) {
        State s = state(email);
        long left = s.lockUntil - System.currentTimeMillis();
        return left > 0 ? (left + 999) / 1000 : 0;
    }

    /** Ghi nhận một lần nhập sai, trả về số lần thử còn lại (0 = đã bị khóa). */
    public static synchronized int recordFailure(String email) {
        State s = state(email);
        s.fails++;
        if (s.fails >= MAX_ATTEMPTS) {
            s.lockUntil = System.currentTimeMillis() + LOCK_MS;
            return 0;
        }
        return MAX_ATTEMPTS - s.fails;
    }

    /** Xóa số lần sai và trạng thái khóa (khi xác thực đúng hoặc đã cấp OTP mới). */
    public static synchronized void reset(String email) {
        State s = state(email);
        s.fails = 0;
        s.lockUntil = 0;
    }

    /** Số giây phải chờ trước khi được gửi lại OTP (0 nếu được phép gửi). */
    public static synchronized long resendWaitSeconds(String email) {
        State s = state(email);
        long left = s.lastSent + RESEND_COOLDOWN_MS - System.currentTimeMillis();
        return left > 0 ? (left + 999) / 1000 : 0;
    }

    public static synchronized void markSent(String email) {
        state(email).lastSent = System.currentTimeMillis();
    }
}
