package dao;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn và cập nhật các loại giá:
 *   - Giá vé: Showtime.BasePrice (giá gốc mỗi suất), RoomType.Surcharge, SeatType.Surcharge (phụ thu)
 *   - Giá F&B: F_B.Price (giá gốc toàn chuỗi), Branch_F_B.Price và Status (thực đơn từng chi nhánh)
 *
 * Mọi hàm nhận danh sách chi nhánh (branchIds): null = tất cả, danh sách rỗng = không có chi nhánh nào.
 */
public class PricingDao extends DBContext {

    /** Tối đa số suất chiếu trả về cho một trang. */
    public static final int MAX_ROWS = 500;

    /** Chỉ suất đang bán hoặc chưa mở bán, và chưa bắt đầu chiếu (suất đã chiếu thì không đổi giá nữa). */
    private static final String EDITABLE_SHOWTIME =
            " s.Status IN ('Scheduled', 'Open') "
            + "AND (s.ShowDate > CAST(GETDATE() AS DATE) "
            + "     OR (s.ShowDate = CAST(GETDATE() AS DATE) AND s.StartTime > CAST(GETDATE() AS TIME(0)))) ";

    // ==================== GIÁ VÉ: SUẤT CHIẾU ====================

    /** Danh sách suất chiếu sắp tới (có thể đổi giá), kèm số vé đã bán. */
    public List<ShowtimePrice> findShowtimePrices(List<Integer> branchIds, Integer movieId,
            String fromDate, String toDate) {
        List<ShowtimePrice> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT TOP " + MAX_ROWS + " s.ShowtimeID, b.BranchID, b.Name AS BranchName, "
                + "m.Title, r.RoomName, r.RoomType, s.ShowDate, s.StartTime, s.BasePrice, s.Status, "
                + "(SELECT COUNT(*) FROM Ticket t WHERE t.ShowtimeID = s.ShowtimeID AND t.Status <> 'Cancelled') AS SoldTickets, "
                + "CASE WHEN " + EDITABLE_SHOWTIME + " THEN 1 ELSE 0 END AS IsEditable "
                + "FROM Showtime s "
                + "INNER JOIN Movie m ON m.MovieID = s.MovieID "
                + "INNER JOIN Room r ON r.RoomID = s.RoomID "
                + "INNER JOIN Branch b ON b.BranchID = r.BranchID "
                + "WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, "b.BranchID", branchIds, movieId, fromDate, toDate);
        sql.append(" ORDER BY s.ShowDate, s.StartTime, b.Name, r.RoomName");

        try (PreparedStatement st = prepare(sql, params); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                ShowtimePrice row = new ShowtimePrice();
                row.showtimeId = rs.getInt("ShowtimeID");
                row.branchId = rs.getInt("BranchID");
                row.branchName = rs.getString("BranchName");
                row.movieTitle = rs.getString("Title");
                row.roomName = rs.getString("RoomName");
                row.roomType = rs.getString("RoomType");
                row.showDate = rs.getDate("ShowDate");
                row.startTime = rs.getTime("StartTime");
                row.basePrice = rs.getBigDecimal("BasePrice");
                row.status = rs.getString("Status");
                row.soldTickets = rs.getInt("SoldTickets");
                row.editable = rs.getInt("IsEditable") == 1;
                list.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Đổi giá gốc của MỘT suất chiếu. Chỉ đổi được khi suất thuộc chi nhánh được phép và chưa bắt đầu.
     * Trả về số dòng được cập nhật (0 nghĩa là không đổi được).
     */
    public int updateShowtimePrice(int showtimeId, BigDecimal price, List<Integer> branchIds) {
        StringBuilder sql = new StringBuilder(
                "UPDATE s SET s.BasePrice = ? FROM Showtime s "
                + "INNER JOIN Room r ON r.RoomID = s.RoomID "
                + "WHERE s.ShowtimeID = ? AND " + EDITABLE_SHOWTIME);
        List<Object> params = new ArrayList<>();
        params.add(price);
        params.add(showtimeId);
        appendBranchFilter(sql, params, "r.BranchID", branchIds);
        return executeUpdate(sql, params);
    }

    /** Đổi giá gốc của NHIỀU suất chiếu cùng lúc theo bộ lọc phim, ngày, chi nhánh. Trả về số suất được cập nhật. */
    public int updateShowtimePrices(List<Integer> branchIds, Integer movieId, String fromDate, String toDate,
            BigDecimal price) {
        StringBuilder sql = new StringBuilder(
                "UPDATE s SET s.BasePrice = ? FROM Showtime s "
                + "INNER JOIN Room r ON r.RoomID = s.RoomID "
                + "WHERE " + EDITABLE_SHOWTIME);
        List<Object> params = new ArrayList<>();
        params.add(price);
        appendFilters(sql, params, "r.BranchID", branchIds, movieId, fromDate, toDate);
        return executeUpdate(sql, params);
    }

    // ==================== GIÁ VÉ: PHỤ THU LOẠI PHÒNG, LOẠI GHẾ ====================

    public List<Surcharge> findRoomTypes() {
        return findSurcharges("SELECT RoomTypeID AS ID, Name, Surcharge FROM RoomType ORDER BY Name");
    }

    public List<Surcharge> findSeatTypes() {
        return findSurcharges("SELECT SeatTypeID AS ID, Name, Surcharge FROM SeatType ORDER BY Name");
    }

    public int updateRoomTypeSurcharge(int roomTypeId, BigDecimal surcharge) {
        return executeUpdate(new StringBuilder("UPDATE RoomType SET Surcharge = ? WHERE RoomTypeID = ?"),
                params(surcharge, roomTypeId));
    }

    public int updateSeatTypeSurcharge(int seatTypeId, BigDecimal surcharge) {
        return executeUpdate(new StringBuilder("UPDATE SeatType SET Surcharge = ? WHERE SeatTypeID = ?"),
                params(surcharge, seatTypeId));
    }

    private List<Surcharge> findSurcharges(String sql) {
        List<Surcharge> list = new ArrayList<>();
        try (PreparedStatement st = connection.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                Surcharge item = new Surcharge();
                item.id = rs.getInt("ID");
                item.name = rs.getString("Name");
                item.surcharge = rs.getBigDecimal("Surcharge");
                list.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // ==================== GIÁ F&B ====================

    /** Toàn bộ món/combo của chuỗi, kèm giá và trạng thái tại chi nhánh (chưa bán thì branchPrice, status = null). */
    public List<MenuItem> findBranchMenu(int branchId) {
        List<MenuItem> list = new ArrayList<>();
        String sql = "SELECT f.ItemID, f.ItemName, f.Category, f.Price AS BasePrice, "
                + "bf.Price AS BranchPrice, bf.Status, "
                + "CASE WHEN bf.ItemID IS NULL THEN 0 ELSE 1 END AS InMenu "
                + "FROM F_B f LEFT JOIN Branch_F_B bf ON bf.ItemID = f.ItemID AND bf.BranchID = ? "
                + "ORDER BY f.Category, f.ItemName";
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setInt(1, branchId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    MenuItem item = new MenuItem();
                    item.itemId = rs.getInt("ItemID");
                    item.name = rs.getString("ItemName");
                    item.category = rs.getString("Category");
                    item.basePrice = rs.getBigDecimal("BasePrice");
                    item.branchPrice = rs.getBigDecimal("BranchPrice"); // null = dùng giá gốc
                    item.status = rs.getString("Status");
                    item.inMenu = rs.getInt("InMenu") == 1;
                    list.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Lưu giá và trạng thái của một món tại chi nhánh. Chưa có trong thực đơn thì thêm mới.
     *
     * @param price giá riêng của chi nhánh (null = dùng giá gốc)
     * @return true nếu lưu được (món hoặc chi nhánh không tồn tại thì false)
     */
    public boolean saveBranchItem(int branchId, int itemId, BigDecimal price, String status) {
        try {
            // Bước 1: thử cập nhật dòng đã có
            try (PreparedStatement st = connection.prepareStatement(
                    "UPDATE Branch_F_B SET Price = ?, Status = ? WHERE BranchID = ? AND ItemID = ?")) {
                st.setObject(1, price, Types.DECIMAL);
                st.setString(2, status);
                st.setInt(3, branchId);
                st.setInt(4, itemId);
                if (st.executeUpdate() > 0) {
                    return true;
                }
            }
            // Bước 2: chưa có thì thêm vào thực đơn
            try (PreparedStatement st = connection.prepareStatement(
                    "INSERT INTO Branch_F_B (BranchID, ItemID, Price, Status) VALUES (?, ?, ?, ?)")) {
                st.setInt(1, branchId);
                st.setInt(2, itemId);
                st.setObject(3, price, Types.DECIMAL);
                st.setString(4, status);
                return st.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Đổi giá gốc của một món (áp dụng cho mọi chi nhánh chưa đặt giá riêng). */
    public int updateItemBasePrice(int itemId, BigDecimal price) {
        return executeUpdate(new StringBuilder("UPDATE F_B SET Price = ? WHERE ItemID = ?"), params(price, itemId));
    }

    // ==================== HÀM HỖ TRỢ ====================

    /** Đóng kết nối (dùng khi DAO chỉ tạo cho một request). */
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Thêm các điều kiện lọc chi nhánh, phim, ngày chiếu vào câu SQL. */
    private static void appendFilters(StringBuilder sql, List<Object> params, String branchColumn,
            List<Integer> branchIds, Integer movieId, String fromDate, String toDate) {
        appendBranchFilter(sql, params, branchColumn, branchIds);
        if (movieId != null) {
            sql.append(" AND s.MovieID = ? ");
            params.add(movieId);
        }
        if (fromDate != null && !fromDate.trim().isEmpty()) {
            sql.append(" AND s.ShowDate >= ? ");
            params.add(Date.valueOf(fromDate.trim()));
        }
        if (toDate != null && !toDate.trim().isEmpty()) {
            sql.append(" AND s.ShowDate <= ? ");
            params.add(Date.valueOf(toDate.trim()));
        }
    }

    /** null: không lọc. Rỗng: không có chi nhánh nào (AND 1 = 0). Còn lại: AND cột IN (...). */
    private static void appendBranchFilter(StringBuilder sql, List<Object> params, String column,
            List<Integer> branchIds) {
        if (branchIds == null) {
            return;
        }
        if (branchIds.isEmpty()) {
            sql.append(" AND 1 = 0 ");
            return;
        }
        sql.append(" AND ").append(column).append(" IN (");
        for (int i = 0; i < branchIds.size(); i++) {
            sql.append(i == 0 ? "?" : ",?");
            params.add(branchIds.get(i));
        }
        sql.append(") ");
    }

    private static List<Object> params(Object... values) {
        List<Object> list = new ArrayList<>();
        for (Object value : values) {
            list.add(value);
        }
        return list;
    }

    private PreparedStatement prepare(StringBuilder sql, List<Object> params) throws SQLException {
        PreparedStatement st = connection.prepareStatement(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            st.setObject(i + 1, params.get(i));
        }
        return st;
    }

    /** Chạy câu UPDATE, trả về số dòng thay đổi (lỗi SQL thì trả 0). */
    private int executeUpdate(StringBuilder sql, List<Object> params) {
        try (PreparedStatement st = prepare(sql, params)) {
            return st.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // ==================== DỮ LIỆU TRẢ VỀ (class lồng) ====================

    /** Một suất chiếu và giá gốc hiện tại. */
    public static class ShowtimePrice {
        private int showtimeId;
        private int branchId;
        private String branchName;
        private String movieTitle;
        private String roomName;
        private String roomType;
        private Date showDate;
        private Time startTime;
        private BigDecimal basePrice;
        private String status;
        private int soldTickets;
        private boolean editable;

        public int getShowtimeId() { return showtimeId; }
        public int getBranchId() { return branchId; }
        public String getBranchName() { return branchName; }
        public String getMovieTitle() { return movieTitle; }
        public String getRoomName() { return roomName; }
        public String getRoomType() { return roomType; }
        public Date getShowDate() { return showDate; }
        public Time getStartTime() { return startTime; }
        public BigDecimal getBasePrice() { return basePrice; }
        public String getStatus() { return status; }
        public int getSoldTickets() { return soldTickets; }
        public boolean isEditable() { return editable; }
    }

    /** Phụ thu của một loại phòng hoặc một loại ghế. */
    public static class Surcharge {
        private int id;
        private String name;
        private BigDecimal surcharge;

        public int getId() { return id; }
        public String getName() { return name; }
        public BigDecimal getSurcharge() { return surcharge; }
    }

    /** Một món trong thực đơn của chi nhánh. */
    public static class MenuItem {
        private int itemId;
        private String name;
        private String category;
        private BigDecimal basePrice;
        private BigDecimal branchPrice;
        private String status;
        private boolean inMenu;

        public int getItemId() { return itemId; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public BigDecimal getBasePrice() { return basePrice; }
        /** Giá riêng của chi nhánh, null nếu dùng giá gốc. */
        public BigDecimal getBranchPrice() { return branchPrice; }
        /** Giá đang áp dụng tại chi nhánh: giá riêng nếu có, không thì giá gốc. */
        public BigDecimal getEffectivePrice() { return branchPrice != null ? branchPrice : basePrice; }
        public String getStatus() { return status; }
        public boolean isInMenu() { return inMenu; }
    }
}
