package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Phụ thu loại ghế RIÊNG của từng chi nhánh.
 *
 * Database không có chỗ lưu riêng nên phụ thu được ghi vào bảng AuditLog (cùng cách POS lưu ca làm việc
 * SHIFT_START / SHIFT_END), mỗi lần quản lý chi nhánh đổi là một dòng log:
 *   SEAT_SURCHARGE|branch=7|seatTypeId=2|seatType=VIP|amount=15000   đặt phụ thu riêng
 *   SEAT_SURCHARGE_RESET|branch=7|seatTypeId=2|seatType=VIP          bỏ phụ thu riêng, dùng lại phụ thu chung
 * Với mỗi (chi nhánh, loại ghế), dòng log MỚI NHẤT quyết định. Không có dòng nào thì chi nhánh dùng
 * phụ thu chung SeatType.Surcharge do Admin đặt. Nhờ vậy mỗi lần đổi đều có người sửa, giờ sửa trong nhật ký.
 */
public final class SeatSurchargeDao {

    public static final String SET = "SEAT_SURCHARGE";
    public static final String RESET = "SEAT_SURCHARGE_RESET";

    private SeatSurchargeDao() {
    }

    /** Phụ thu riêng đang có của chi nhánh: seatTypeId -> số tiền. Loại ghế không có trong map thì dùng phụ thu chung. */
    public static Map<Integer, BigDecimal> findOverrides(Connection con, int branchId) throws SQLException {
        Map<Integer, BigDecimal> overrides = new HashMap<>();
        // Dấu | đứng trước và sau "branch=7" để không nhầm với chi nhánh 17, 70...
        String sql = "SELECT Action FROM AuditLog WHERE Action LIKE ? ORDER BY LogID";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, SET + "%|branch=" + branchId + "|%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    apply(overrides, rs.getString(1));
                }
            }
        }
        return overrides;
    }

    /** Ghi một dòng log đặt phụ thu riêng cho chi nhánh. */
    public static void saveOverride(Connection con, int userId, String ip, int branchId, int seatTypeId,
            String seatTypeName, BigDecimal amount) throws SQLException {
        insertLog(con, userId, ip, SET + "|branch=" + branchId + "|seatTypeId=" + seatTypeId
                + "|seatType=" + clean(seatTypeName) + "|amount=" + amount.toPlainString());
    }

    /** Ghi một dòng log bỏ phụ thu riêng, chi nhánh quay về dùng phụ thu chung. */
    public static void resetOverride(Connection con, int userId, String ip, int branchId, int seatTypeId,
            String seatTypeName) throws SQLException {
        insertLog(con, userId, ip, RESET + "|branch=" + branchId + "|seatTypeId=" + seatTypeId
                + "|seatType=" + clean(seatTypeName));
    }

    private static void insertLog(Connection con, int userId, String ip, String action) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO AuditLog (UserID, Action, IPAddress) VALUES (?, ?, ?)")) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, ip);
            ps.executeUpdate();
        }
    }

    /** Đọc một dòng log và cập nhật map; dòng sai định dạng thì bỏ qua. */
    private static void apply(Map<Integer, BigDecimal> overrides, String action) {
        if (action == null) {
            return;
        }
        String[] parts = action.split("\\|");
        boolean reset = RESET.equals(parts[0]);
        if (!reset && !SET.equals(parts[0])) {
            return;
        }
        Integer seatTypeId = null;
        BigDecimal amount = null;
        try {
            for (int i = 1; i < parts.length; i++) {
                int eq = parts[i].indexOf('=');
                if (eq < 0) {
                    continue;
                }
                String key = parts[i].substring(0, eq);
                String value = parts[i].substring(eq + 1);
                if ("seatTypeId".equals(key)) {
                    seatTypeId = Integer.valueOf(value);
                } else if ("amount".equals(key)) {
                    amount = new BigDecimal(value);
                }
            }
        } catch (NumberFormatException e) {
            return;
        }
        if (seatTypeId == null) {
            return;
        }
        if (reset) {
            overrides.remove(seatTypeId);
        } else if (amount != null && amount.signum() >= 0) {
            overrides.put(seatTypeId, amount);
        }
    }

    /** Bỏ ký tự | và = trong tên loại ghế để không làm hỏng định dạng log. */
    private static String clean(String name) {
        return name == null ? "" : name.replace('|', ' ').replace('=', ' ');
    }
}
