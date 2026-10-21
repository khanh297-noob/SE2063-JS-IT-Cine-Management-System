package util;

import java.util.HashMap;
import java.util.Map;

/**
 * Chống nhập sai OTP nhiều lần và chống gửi lại OTP quá nhanh.
 * Dữ liệu lưu trong bộ nhớ server theo email, nên sẽ mất khi restart Tomcat.
 */
public class OtpGuard {

    // Được nhập sai tối đa 5 lần, sau đó bị khóa 15 phút
    public static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MS = 15 * 60 * 1000L;

    // Phải chờ 60 giây mới được gửi lại OTP
    private static final long RESEND_COOLDOWN_MS = 60 * 1000L;

    /** Trạng thái của một email: số lần sai, thời điểm hết khóa, lần gửi OTP gần nhất. */
    private static class State {
        int fails;
        long lockUntil;
        long lastSent;
    }

    private static final Map<String, State> STATES = new HashMap<>();

    /** Lấy trạng thái của email (chưa có thì tạo mới). */
    private static State getState(String email) {
        String key = email.trim().toLowerCase();
        State state = STATES.get(key);
        if (state == null) {
            state = new State();
            STATES.put(key, state);
        }
        return state;
    }

    /** Số giây còn bị khóa nhập OTP (0 nếu không bị khóa). */
    public static synchronized long lockRemainingSeconds(String email) {
        long left = getState(email).lockUntil - System.currentTimeMillis();
        return left > 0 ? (left + 999) / 1000 : 0;
    }

    /** Ghi nhận một lần nhập sai. Trả về số lần thử còn lại (0 = đã bị khóa). */
    public static synchronized int recordFailure(String email) {
        State state = getState(email);
        state.fails++;
        if (state.fails >= MAX_ATTEMPTS) {
            state.lockUntil = System.currentTimeMillis() + LOCK_MS;
            return 0;
        }
        return MAX_ATTEMPTS - state.fails;
    }

    /** Xóa số lần sai và trạng thái khóa (khi nhập đúng hoặc đã cấp OTP mới). */
    public static synchronized void reset(String email) {
        State state = getState(email);
        state.fails = 0;
        state.lockUntil = 0;
    }

    /** Số giây phải chờ trước khi được gửi lại OTP (0 nếu được phép gửi). */
    public static synchronized long resendWaitSeconds(String email) {
        long left = getState(email).lastSent + RESEND_COOLDOWN_MS - System.currentTimeMillis();
        return left > 0 ? (left + 999) / 1000 : 0;
    }

    /** Ghi nhận vừa gửi OTP. */
    public static synchronized void markSent(String email) {
        getState(email).lastSent = System.currentTimeMillis();
    }
}
