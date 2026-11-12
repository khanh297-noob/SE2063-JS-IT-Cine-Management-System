package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Đọc/ghi cấu hình giá dùng chung: giá cơ bản theo loại ngày (bảng PricingSetting)
 * và phụ thu riêng theo phim (cột Movie.SurchargeAmount).
 *
 * Các hàm nhận Connection từ nơi gọi để cùng nằm trong một transaction (xem ShowtimeService, PricingDao).
 * Khi bảng/cột chưa tạo (chưa chạy sql/pricing-config.sql), các hàm đọc trả về giá trị mặc định
 * để không làm hỏng chức năng tạo suất chiếu.
 */
public final class PricingConfigDao {

    public static final String KEY_WEEKDAY = "WEEKDAY_BASE_PRICE";
    public static final String KEY_WEEKEND = "WEEKEND_BASE_PRICE";

    /** Tiền tố khoá PricingSetting lưu phụ thu loại phòng RIÊNG của chi nhánh: ROOM_SURCHARGE_{roomTypeId}_BRANCH_{branchId}. */
    public static final String KEY_ROOM_SURCHARGE = "ROOM_SURCHARGE_";

    /**
     * Biểu thức SQL (dùng với alias rt = RoomType, r = Room): phụ thu loại phòng hiệu lực của chi nhánh chứa phòng r,
     * ưu tiên phụ thu riêng của chi nhánh, không có thì dùng phụ thu chung RoomType.Surcharge.
     */
    public static final String BRANCH_ROOM_SURCHARGE_SQL =
            "ISNULL((SELECT TOP 1 ps.SettingValue FROM PricingSetting ps WHERE ps.SettingKey = "
            + "'ROOM_SURCHARGE_' + CAST(rt.RoomTypeID AS VARCHAR(10)) + '_BRANCH_' + CAST(r.BranchID AS VARCHAR(10))), "
            + "ISNULL(rt.Surcharge, 0))";

    /** Giá cơ bản mặc định: Thứ 2 - Thứ 5. */
    public static final BigDecimal DEFAULT_WEEKDAY = new BigDecimal("70000");
    /** Giá cơ bản mặc định: Thứ 6, Thứ 7, Chủ nhật. */
    public static final BigDecimal DEFAULT_WEEKEND = new BigDecimal("90000");

    private PricingConfigDao() {
    }

    /** Hai mức giá cơ bản theo loại ngày. */
    public static final class DayPrices {
        private final BigDecimal weekday;
        private final BigDecimal weekend;

        public DayPrices(BigDecimal weekday, BigDecimal weekend) {
            this.weekday = weekday;
            this.weekend = weekend;
        }

        /** Giá cơ bản ngày thường (Thứ 2 - Thứ 5). */
        public BigDecimal getWeekday() { return weekday; }
        /** Giá cơ bản cuối tuần (Thứ 6, Thứ 7, Chủ nhật). */
        public BigDecimal getWeekend() { return weekend; }
    }

    /** Đọc giá cơ bản theo loại ngày; thiếu dòng hoặc lỗi thì dùng giá mặc định. */
    public static DayPrices findDayBasePrices(Connection con) {
        BigDecimal weekday = DEFAULT_WEEKDAY;
        BigDecimal weekend = DEFAULT_WEEKEND;
        String sql = "SELECT SettingKey, SettingValue FROM PricingSetting WHERE SettingKey IN (?, ?)";
        try (PreparedStatement st = con.prepareStatement(sql)) {
            st.setString(1, KEY_WEEKDAY);
            st.setString(2, KEY_WEEKEND);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    // Giá cơ bản phải lớn hơn 0; dòng bị bỏ trống hoặc nhập 0 thì dùng giá mặc định
                    BigDecimal value = rs.getBigDecimal(2);
                    boolean valid = value != null && value.signum() > 0;
                    if (valid && KEY_WEEKDAY.equals(rs.getString(1))) {
                        weekday = value;
                    } else if (valid && KEY_WEEKEND.equals(rs.getString(1))) {
                        weekend = value;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new DayPrices(weekday, weekend);
    }

    /** Đọc giá cơ bản riêng của chi nhánh; chưa cài thì dùng giá chung toàn chuỗi. */
    public static DayPrices findDayBasePricesForBranch(Connection con, int branchId) {
        DayPrices common = findDayBasePrices(con);
        String keyWd = KEY_WEEKDAY + "_BRANCH_" + branchId;
        String keyWe = KEY_WEEKEND + "_BRANCH_" + branchId;
        BigDecimal weekday = common.getWeekday();
        BigDecimal weekend = common.getWeekend();
        String sql = "SELECT SettingKey, SettingValue FROM PricingSetting WHERE SettingKey IN (?, ?)";
        try (PreparedStatement st = con.prepareStatement(sql)) {
            st.setString(1, keyWd);
            st.setString(2, keyWe);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    BigDecimal val = rs.getBigDecimal(2);
                    if (val != null && val.signum() > 0) {
                        if (keyWd.equals(rs.getString(1))) {
                            weekday = val;
                        } else if (keyWe.equals(rs.getString(1))) {
                            weekend = val;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new DayPrices(weekday, weekend);
    }

    /** Lưu giá cơ bản riêng cho chi nhánh. */
    public static void saveDayBasePricesForBranch(Connection con, int branchId, BigDecimal weekday, BigDecimal weekend) throws SQLException {
        if (weekday != null && weekday.signum() > 0) {
            saveSetting(con, KEY_WEEKDAY + "_BRANCH_" + branchId, weekday);
        }
        if (weekend != null && weekend.signum() > 0) {
            saveSetting(con, KEY_WEEKEND + "_BRANCH_" + branchId, weekend);
        }
    }

    /** Phụ thu loại phòng riêng đang có của chi nhánh: roomTypeId -> số tiền. Loại phòng không có trong map thì dùng phụ thu chung. */
    public static Map<Integer, BigDecimal> findRoomSurchargeOverrides(Connection con, int branchId) {
        Map<Integer, BigDecimal> result = new HashMap<>();
        String suffix = "_BRANCH_" + branchId;
        try (PreparedStatement st = con.prepareStatement(
                "SELECT SettingKey, SettingValue FROM PricingSetting WHERE SettingKey LIKE ?")) {
            st.setString(1, KEY_ROOM_SURCHARGE + "%");
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString(1);
                    BigDecimal value = rs.getBigDecimal(2);
                    // Kiểm tra lại ở Java vì _ trong LIKE là ký tự đại diện; endsWith tránh nhầm chi nhánh 7 với 17
                    if (key == null || value == null || value.signum() < 0 || !key.endsWith(suffix)) {
                        continue;
                    }
                    String idText = key.substring(KEY_ROOM_SURCHARGE.length(), key.length() - suffix.length());
                    try {
                        result.put(Integer.valueOf(idText), value);
                    } catch (NumberFormatException ignored) {
                        // khoá sai định dạng thì bỏ qua
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /** Xoá một dòng cấu hình (chi nhánh quay về dùng giá trị chung). */
    public static void deleteSetting(Connection con, String key) throws SQLException {
        try (PreparedStatement st = con.prepareStatement("DELETE FROM PricingSetting WHERE SettingKey = ?")) {
            st.setString(1, key);
            st.executeUpdate();
        }
    }

    /** Ghi một mức giá cơ bản: có dòng thì sửa, chưa có thì thêm. */
    public static void saveSetting(Connection con, String key, BigDecimal value) throws SQLException {
        try (PreparedStatement st = con.prepareStatement(
                "UPDATE PricingSetting SET SettingValue = ?, UpdatedAt = SYSDATETIME() WHERE SettingKey = ?")) {
            st.setBigDecimal(1, value);
            st.setString(2, key);
            if (st.executeUpdate() > 0) {
                return;
            }
        }
        try (PreparedStatement st = con.prepareStatement(
                "INSERT INTO PricingSetting (SettingKey, SettingValue) VALUES (?, ?)")) {
            st.setString(1, key);
            st.setBigDecimal(2, value);
            st.executeUpdate();
        }
    }

    /** Phụ thu riêng của một phim (0 nếu không có hoặc cột chưa được tạo). */
    public static BigDecimal findMovieSurcharge(Connection con, int movieId) {
        try (PreparedStatement st = con.prepareStatement(
                "SELECT ISNULL(SurchargeAmount, 0) FROM Movie WHERE MovieID = ?")) {
            st.setInt(1, movieId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }
}
