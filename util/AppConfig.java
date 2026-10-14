package cinemanagement.util;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Doc tham so he thong tu bang SystemSetting (SRS 7.3). Cache 60 giay.
 * Neu bang chua ton tai thi dung gia tri mac dinh.
 */
public final class AppConfig {

    private static final long TTL_MS = 60_000;
    private static volatile Map<String, String> cache = new HashMap<>();
    private static volatile long loadedAt = 0;

    private AppConfig() {
    }

    private static Map<String, String> settings() {
        long now = System.currentTimeMillis();
        if (now - loadedAt > TTL_MS) {
            Map<String, String> fresh = new HashMap<>();
            try (Connection c = DBContext.getConnection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT SettingKey, SettingValue FROM SystemSetting")) {
                while (rs.next()) {
                    fresh.put(rs.getString(1), rs.getString(2));
                }
            } catch (SQLException e) {
                // bang chua co -> dung mac dinh
            }
            cache = fresh;
            loadedAt = now;
        }
        return cache;
    }

    public static int getInt(String key, int def) {
        String v = settings().get(key);
        if (v == null) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static BigDecimal getMoney(String key, long def) {
        String v = settings().get(key);
        if (v == null) {
            return BigDecimal.valueOf(def);
        }
        try {
            return new BigDecimal(v.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.valueOf(def);
        }
    }

    public static int seatHoldMinutes() {
        return getInt("SEAT_HOLD_MINUTES", 10);
    }

    public static int cleaningBufferMinutes() {
        return getInt("CLEANING_BUFFER_MINUTES", 15);
    }

    public static int cancelCutoffHours() {
        return getInt("CANCEL_CUTOFF_HOURS", 2);
    }

    public static int maxSeatsPerBooking() {
        return getInt("MAX_SEATS_PER_BOOKING", 8);
    }
}
