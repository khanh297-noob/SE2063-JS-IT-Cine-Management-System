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
import java.util.Map;

/**
 * Truy vấn và cập nhật các loại giá:
 *   - Giá vé: Showtime.BasePrice (giá hiện tại của suất, quản lý được đổi; giá gốc không lưu mà tính lại từ
 *            phụ thu loại phòng và ngày chiếu), RoomType.Surcharge, SeatType.Surcharge (phụ thu)
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
                + "m.Title, r.RoomName, r.RoomType, ISNULL(rt.Surcharge, 0) AS RoomSurcharge, s.ShowDate, s.StartTime, s.BasePrice, s.Status, "
                + "(SELECT COUNT(*) FROM Ticket t WHERE t.ShowtimeID = s.ShowtimeID AND t.Status <> 'Cancelled') AS SoldTickets, "
                + "CASE WHEN " + EDITABLE_SHOWTIME + " THEN 1 ELSE 0 END AS IsEditable "
                + "FROM Showtime s "
                + "INNER JOIN Movie m ON m.MovieID = s.MovieID "
                + "INNER JOIN Room r ON r.RoomID = s.RoomID "
                + "LEFT JOIN RoomType rt ON rt.Name = r.RoomType "
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
                row.currentPrice = rs.getBigDecimal("BasePrice");
                row.roomSurcharge = rs.getBigDecimal("RoomSurcharge");
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
     * Đổi giá của MỘT suất chiếu theo GIÁ CƠ BẢN (chưa gồm phụ thu loại phòng): cột BasePrice được lưu là
     * giá cơ bản + phụ thu loại phòng của phòng chiếu. Chỉ đổi được khi suất thuộc chi nhánh được phép và chưa bắt đầu.
     * Trả về số dòng được cập nhật (0 nghĩa là không đổi được).
     */
    public int updateShowtimePrice(int showtimeId, BigDecimal basePrice, List<Integer> branchIds) {
        BigDecimal price = basePrice;
        StringBuilder sql = new StringBuilder(
                "UPDATE s SET s.BasePrice = ? + ISNULL(rt.Surcharge, 0) FROM Showtime s "
                + "INNER JOIN Room r ON r.RoomID = s.RoomID "
                + "LEFT JOIN RoomType rt ON rt.Name = r.RoomType "
                + "WHERE s.ShowtimeID = ? AND " + EDITABLE_SHOWTIME);
        List<Object> params = new ArrayList<>();
        params.add(price);
        params.add(showtimeId);
        appendBranchFilter(sql, params, "r.BranchID", branchIds);
        return executeUpdate(sql, params);
    }

    /**
     * Đổi giá cơ bản của NHIỀU suất chiếu cùng lúc theo bộ lọc phim, ngày, chi nhánh. Mỗi suất lưu vào BasePrice
     * = giá cơ bản + phụ thu loại phòng của chính phòng đó. Trả về số suất được cập nhật.
     */
    public int updateShowtimePrices(List<Integer> branchIds, Integer movieId, String fromDate, String toDate,
            BigDecimal price) {
        StringBuilder sql = new StringBuilder(
                "UPDATE s SET s.BasePrice = ? + ISNULL(rt.Surcharge, 0) FROM Showtime s "
                + "INNER JOIN Room r ON r.RoomID = s.RoomID "
                + "LEFT JOIN RoomType rt ON rt.Name = r.RoomType "
                + "WHERE " + EDITABLE_SHOWTIME);
        List<Object> params = new ArrayList<>();
        params.add(price);
        appendFilters(sql, params, "r.BranchID", branchIds, movieId, fromDate, toDate);
        return executeUpdate(sql, params);
    }

    /**
     * Cộng (hoặc trừ, nếu delta âm) một khoản vào giá hiện tại của các suất sắp chiếu thuộc một loại phòng
     * trong phạm vi chi nhánh. Suất nào sau khi cộng ra ngoài khoảng 0 đến MAX_PRICE thì bỏ qua.
     * Trả về số suất được cập nhật.
     */
    public int adjustShowtimePricesByRoomType(List<Integer> branchIds, int roomTypeId, BigDecimal delta,
            BigDecimal maxPrice) {
        StringBuilder sql = new StringBuilder(
                "UPDATE s SET s.BasePrice = s.BasePrice + ? FROM Showtime s "
                + "INNER JOIN Room r ON r.RoomID = s.RoomID "
                + "INNER JOIN RoomType rt ON rt.Name = r.RoomType "
                + "WHERE rt.RoomTypeID = ? AND s.BasePrice + ? BETWEEN 0 AND ? AND " + EDITABLE_SHOWTIME);
        List<Object> params = params(delta, roomTypeId, delta, maxPrice);
        appendBranchFilter(sql, params, "r.BranchID", branchIds);
        return executeUpdate(sql, params);
    }

    // ==================== GIÁ VÉ: PHỤ THU LOẠI PHÒNG, LOẠI GHẾ ====================

    public List<Surcharge> findRoomTypes() {
        return findSurcharges("SELECT RoomTypeID AS ID, Name, Surcharge FROM RoomType ORDER BY Name");
    }

    public List<Surcharge> findSeatTypes() {
        return findSurcharges("SELECT SeatTypeID AS ID, Name, Surcharge FROM SeatType ORDER BY Name");
    }

    /** Loại ghế kèm phụ thu chung và phụ thu riêng của một chi nhánh (branchSurcharge null = dùng phụ thu chung). */
    public List<Surcharge> findSeatTypesForBranch(int branchId) {
        List<Surcharge> list = findSeatTypes();
        try {
            Map<Integer, BigDecimal> overrides = SeatSurchargeDao.findOverrides(connection, branchId);
            for (Surcharge item : list) {
                item.branchSurcharge = overrides.get(item.id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Giá vé công khai theo suất chiếu: các suất đang bán hoặc chưa mở bán, chưa bắt đầu, trong {@code days} ngày tới.
     * Giá lấy đúng cột Showtime.BasePrice, tức giá hiện tại mà quản lý đổi ở trang Quản lý giá vé.
     * Khách trả thêm phụ thu loại ghế, nên mỗi suất có giá thấp nhất và cao nhất theo các loại ghế đang có
     * trong phòng (phụ thu loại ghế là phụ thu riêng của chi nhánh nếu có, không thì phụ thu chung).
     */
    public List<PublicShowtimePrice> findPublicShowtimePrices(int days, int maxRows) {
        List<PublicShowtimePrice> list = new ArrayList<>();
        String sql = "SELECT TOP " + maxRows + " s.ShowtimeID, b.BranchID, b.Name AS BranchName, m.MovieID, m.Title, "
                + "r.RoomID, r.RoomName, r.RoomType, s.ShowDate, s.StartTime, s.BasePrice "
                + "FROM Showtime s "
                + "INNER JOIN Movie m ON m.MovieID = s.MovieID "
                + "INNER JOIN Room r ON r.RoomID = s.RoomID "
                + "INNER JOIN Branch b ON b.BranchID = r.BranchID "
                + "WHERE b.Status = 'Active' AND r.Status = 'Active' AND " + EDITABLE_SHOWTIME
                + "AND s.ShowDate <= DATEADD(DAY, ?, CAST(GETDATE() AS DATE)) "
                + "ORDER BY s.ShowDate, s.StartTime, b.Name, r.RoomName";

        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setInt(1, days - 1);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    PublicShowtimePrice row = new PublicShowtimePrice();
                    row.showtimeId = rs.getInt("ShowtimeID");
                    row.branchId = rs.getInt("BranchID");
                    row.branchName = rs.getString("BranchName");
                    row.movieId = rs.getInt("MovieID");
                    row.movieTitle = rs.getString("Title");
                    row.roomId = rs.getInt("RoomID");
                    row.roomName = rs.getString("RoomName");
                    row.roomType = rs.getString("RoomType");
                    row.showDate = rs.getDate("ShowDate");
                    row.startTime = rs.getTime("StartTime");
                    row.basePrice = rs.getBigDecimal("BasePrice");
                    list.add(row);
                }
            }
            addSeatSurchargeRange(list);
        } catch (SQLException e) {
            e.printStackTrace();
            list.clear();
        }
        return list;
    }

    /** Bảng giá vé theo từng (chi nhánh, loại phòng), xếp theo tên chi nhánh rồi loại phòng. */
    public List<RoomTypePriceBoard> findPriceBoards(int days, int maxRows) {
        Map<String, RoomTypePriceBoard> boards = new java.util.LinkedHashMap<>();
        for (PublicShowtimePrice s : findPublicShowtimePrices(days, maxRows)) {
            String key = s.getBranchId() + "|" + s.getRoomType();
            RoomTypePriceBoard board = boards.get(key);
            if (board == null) {
                board = new RoomTypePriceBoard();
                board.branchId = s.getBranchId();
                board.branchName = s.getBranchName();
                board.roomType = s.getRoomType();
                boards.put(key, board);
            }
            board.add(s);
        }
        List<RoomTypePriceBoard> list = new ArrayList<>(boards.values());
        list.sort(java.util.Comparator
                .comparing((RoomTypePriceBoard b) -> b.branchName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(b -> b.roomType == null ? "" : b.roomType, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    /** Tính giá thấp nhất / cao nhất của mỗi suất = giá suất + phụ thu loại ghế nhỏ nhất / lớn nhất trong phòng. */
    private void addSeatSurchargeRange(List<PublicShowtimePrice> rows) throws SQLException {
        // Phụ thu của từng loại ghế đang có trong từng phòng: roomId -> (seatTypeId -> phụ thu chung)
        Map<Integer, Map<Integer, BigDecimal>> seatTypesByRoom = new java.util.HashMap<>();
        Map<Integer, String> seatTypeNames = new java.util.HashMap<>();
        String sql = "SELECT DISTINCT se.RoomID, sty.SeatTypeID, se.SeatType, ISNULL(sty.Surcharge, 0) AS Surcharge "
                + "FROM Seat se LEFT JOIN SeatType sty ON sty.Name = se.SeatType "
                + "WHERE se.Status = 'Active'";
        try (PreparedStatement st = connection.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                int seatTypeId = rs.getInt("SeatTypeID");
                if (rs.wasNull()) {
                    seatTypeId = -rs.getInt("RoomID"); // ghế không khớp loại ghế nào: phụ thu 0
                }
                seatTypeNames.put(seatTypeId, rs.getString("SeatType"));
                seatTypesByRoom.computeIfAbsent(rs.getInt("RoomID"), k -> new java.util.HashMap<>())
                        .put(seatTypeId, rs.getBigDecimal("Surcharge"));
            }
        }

        Map<Integer, Map<Integer, BigDecimal>> overridesByBranch = new java.util.HashMap<>();
        for (PublicShowtimePrice row : rows) {
            Map<Integer, BigDecimal> overrides = overridesByBranch.get(row.branchId);
            if (overrides == null) {
                overrides = SeatSurchargeDao.findOverrides(connection, row.branchId);
                overridesByBranch.put(row.branchId, overrides);
            }

            BigDecimal min = null;
            BigDecimal max = null;
            Map<Integer, BigDecimal> seatTypes = seatTypesByRoom.get(row.roomId);
            if (seatTypes != null) {
                for (Map.Entry<Integer, BigDecimal> e : seatTypes.entrySet()) {
                    BigDecimal own = overrides.get(e.getKey());
                    BigDecimal surcharge = own != null ? own : e.getValue();
                    min = (min == null || surcharge.compareTo(min) < 0) ? surcharge : min;
                    max = (max == null || surcharge.compareTo(max) > 0) ? surcharge : max;
                    row.seatSurcharges.put(seatTypeNames.get(e.getKey()), surcharge);
                }
            }
            BigDecimal base = row.basePrice == null ? BigDecimal.ZERO : row.basePrice;
            row.minPrice = base.add(min == null ? BigDecimal.ZERO : min);
            row.maxPrice = base.add(max == null ? BigDecimal.ZERO : max);
        }
    }

    /** Tên loại ghế, null nếu không tồn tại. */
    public String findSeatTypeName(int seatTypeId) {
        try (PreparedStatement st = connection.prepareStatement("SELECT Name FROM SeatType WHERE SeatTypeID = ?")) {
            st.setInt(1, seatTypeId);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Đặt phụ thu loại ghế riêng cho chi nhánh. Trả false nếu ghi lỗi. */
    public boolean saveBranchSeatTypeSurcharge(int userId, String ip, int branchId, int seatTypeId,
            String seatTypeName, BigDecimal amount) {
        try {
            SeatSurchargeDao.saveOverride(connection, userId, ip, branchId, seatTypeId, seatTypeName, amount);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Bỏ phụ thu riêng của chi nhánh. Trả false nếu chi nhánh chưa có phụ thu riêng hoặc ghi lỗi. */
    public boolean resetBranchSeatTypeSurcharge(int userId, String ip, int branchId, int seatTypeId,
            String seatTypeName) {
        try {
            if (!SeatSurchargeDao.findOverrides(connection, branchId).containsKey(seatTypeId)) {
                return false;
            }
            SeatSurchargeDao.resetOverride(connection, userId, ip, branchId, seatTypeId, seatTypeName);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Admin đổi phụ thu loại phòng. Vì BasePrice của suất = giá cơ bản + phụ thu loại phòng, các suất sắp chiếu thuộc
     * loại phòng này được cộng thêm đúng phần chênh lệch (phụ thu mới - phụ thu cũ) để GIÁ CƠ BẢN không đổi
     * và giá bán đổi theo phụ thu mới. Suất nào sau khi cộng bị âm thì bỏ qua. Làm trong một transaction.
     * Trả về 1 nếu thành công, 0 nếu không tìm thấy loại phòng hoặc lỗi.
     */
    public int updateRoomTypeSurchargeKeepingBasePrice(int roomTypeId, BigDecimal newSurcharge) {
        boolean oldAutoCommit = true;
        try {
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            BigDecimal oldSurcharge = null;
            try (PreparedStatement st = connection.prepareStatement(
                    "SELECT ISNULL(Surcharge, 0) FROM RoomType WHERE RoomTypeID = ?")) {
                st.setInt(1, roomTypeId);
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        oldSurcharge = rs.getBigDecimal(1);
                    }
                }
            }
            if (oldSurcharge == null) {
                connection.rollback();
                return 0;
            }

            BigDecimal delta = newSurcharge.subtract(oldSurcharge);
            if (delta.signum() != 0) {
                StringBuilder sql = new StringBuilder(
                        "UPDATE s SET s.BasePrice = s.BasePrice + ? FROM Showtime s "
                        + "INNER JOIN Room r ON r.RoomID = s.RoomID "
                        + "INNER JOIN RoomType rt ON rt.Name = r.RoomType "
                        + "WHERE rt.RoomTypeID = ? AND s.BasePrice + ? >= 0 AND " + EDITABLE_SHOWTIME);
                try (PreparedStatement st = prepare(sql, params(delta, roomTypeId, delta))) {
                    st.executeUpdate();
                }
            }
            try (PreparedStatement st = connection.prepareStatement(
                    "UPDATE RoomType SET Surcharge = ? WHERE RoomTypeID = ?")) {
                st.setBigDecimal(1, newSurcharge);
                st.setInt(2, roomTypeId);
                st.executeUpdate();
            }
            connection.commit();
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException ignored) {
                // không còn gì để làm thêm
            }
            return 0;
        } finally {
            try {
                connection.setAutoCommit(oldAutoCommit);
            } catch (SQLException ignored) {
                // đóng kết nối ở close()
            }
        }
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

    /** Một suất chiếu kèm giá gốc (tính lại, cố định) và giá hiện tại (đọc từ Showtime.BasePrice, sửa được). */
    public static class ShowtimePrice {
        private int showtimeId;
        private int branchId;
        private String branchName;
        private String movieTitle;
        private String roomName;
        private String roomType;
        private Date showDate;
        private Time startTime;
        private BigDecimal currentPrice;
        private BigDecimal roomSurcharge;
        private BigDecimal basePrice;
        private BigDecimal originalPrice;
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
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public BigDecimal getRoomSurcharge() { return roomSurcharge; }
        /** Giá cơ bản = giá hiện tại (BasePrice) trừ phụ thu loại phòng; đây là giá quản lý nhập và sửa. */
        public BigDecimal getBasePrice() { return basePrice; }
        public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
        /** Giá gốc: giá ban đầu khi tạo suất, tính từ phụ thu loại phòng và ngày chiếu. */
        public BigDecimal getOriginalPrice() { return originalPrice; }
        public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
        public String getStatus() { return status; }
        public int getSoldTickets() { return soldTickets; }
        public boolean isEditable() { return editable; }
    }

    /** Phụ thu của một loại phòng hoặc một loại ghế. */
    public static class Surcharge {
        private int id;
        private String name;
        private BigDecimal surcharge;
        private BigDecimal branchSurcharge;

        public int getId() { return id; }
        public String getName() { return name; }
        /** Phụ thu chung của cả chuỗi (Admin đặt). */
        public BigDecimal getSurcharge() { return surcharge; }
        /** Phụ thu riêng của chi nhánh, null nếu chi nhánh dùng phụ thu chung (chỉ loại ghế mới có). */
        public BigDecimal getBranchSurcharge() { return branchSurcharge; }
        /** Phụ thu đang áp dụng tại chi nhánh: giá riêng nếu có, không thì giá chung. */
        public BigDecimal getEffectiveSurcharge() { return branchSurcharge != null ? branchSurcharge : surcharge; }
    }

    /** Một suất chiếu trong bảng giá công khai: giá suất (Showtime.BasePrice) và khoảng giá vé theo loại ghế. */
    public static class PublicShowtimePrice {
        private int showtimeId;
        private int branchId;
        private String branchName;
        private int movieId;
        private String movieTitle;
        private int roomId;
        private String roomName;
        private String roomType;
        private Date showDate;
        private Time startTime;
        private BigDecimal basePrice;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private Map<String, BigDecimal> seatSurcharges = new java.util.LinkedHashMap<>();

        public int getShowtimeId() { return showtimeId; }
        public int getBranchId() { return branchId; }
        public String getBranchName() { return branchName; }
        public int getMovieId() { return movieId; }
        public String getMovieTitle() { return movieTitle; }
        public String getRoomName() { return roomName; }
        public String getRoomType() { return roomType; }
        public Date getShowDate() { return showDate; }
        public Time getStartTime() { return startTime; }
        /** Giá hiện tại của suất (cột Showtime.BasePrice), chưa gồm phụ thu loại ghế. */
        public BigDecimal getBasePrice() { return basePrice; }
        /** Giá vé thấp nhất của suất (giá suất + phụ thu loại ghế rẻ nhất trong phòng). */
        public BigDecimal getMinPrice() { return minPrice; }
        /** Giá vé cao nhất của suất (giá suất + phụ thu loại ghế đắt nhất trong phòng). */
        public BigDecimal getMaxPrice() { return maxPrice; }
        /** Phụ thu đang áp dụng của từng loại ghế có trong phòng (tên loại ghế -> phụ thu). */
        public Map<String, BigDecimal> getSeatSurcharges() { return seatSurcharges; }
        public boolean isPriceRange() { return minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) != 0; }
    }

    /** Khoảng giá (thấp nhất - cao nhất) của một ô trong bảng giá; không có suất nào thì hiện "—". */
    public static class PriceRange {
        private BigDecimal min;
        private BigDecimal max;

        void add(BigDecimal price) {
            min = (min == null || price.compareTo(min) < 0) ? price : min;
            max = (max == null || price.compareTo(max) > 0) ? price : max;
        }

        public boolean isAvailable() { return min != null; }
        public BigDecimal getMin() { return min; }
        public BigDecimal getMax() { return max; }

        /** Văn bản hiển thị, ví dụ "75,000 ₫" hoặc "75,000 - 85,000 ₫". */
        public String getText() {
            if (min == null) {
                return "\u2014";
            }
            java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,##0",
                    java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US));
            String text = fmt.format(min);
            if (min.compareTo(max) != 0) {
                text += " - " + fmt.format(max);
            }
            return text + " \u20AB";
        }
    }

    /** Phụ thu của một loại ghế so với ghế rẻ nhất trong phòng, ví dụ "VIP +10,000 ₫". */
    public static class SeatExtra {
        private final String name;
        private final BigDecimal amount;

        SeatExtra(String name, BigDecimal amount) {
            this.name = name;
            this.amount = amount;
        }

        public String getName() { return name; }
        public BigDecimal getAmount() { return amount; }

        public String getText() {
            java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,##0",
                    java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US));
            return name + " +" + fmt.format(amount) + " \u20AB";
        }
    }

    /**
     * Bảng giá vé của một loại phòng tại một chi nhánh (như bảng giá 2D / 3D ở rạp):
     * giá thấp nhất - cao nhất theo ngày trong tuần (thứ 2-6, thứ 7 và Chủ nhật) và khung giờ (trước 18h, từ 18h).
     * Giá là giá vé ghế rẻ nhất = Showtime.BasePrice (giá quản lý đặt) + phụ thu loại ghế rẻ nhất.
     */
    public static class RoomTypePriceBoard {
        private int branchId;
        private String branchName;
        private String roomType;
        private final PriceRange weekdayBefore = new PriceRange();
        private final PriceRange weekdayAfter = new PriceRange();
        private final PriceRange weekendBefore = new PriceRange();
        private final PriceRange weekendAfter = new PriceRange();
        private final Map<String, BigDecimal> seatSurcharges = new java.util.LinkedHashMap<>();
        private int showtimeCount;

        public int getBranchId() { return branchId; }
        public String getBranchName() { return branchName; }
        public String getRoomType() { return roomType; }
        public PriceRange getWeekdayBefore() { return weekdayBefore; }
        public PriceRange getWeekdayAfter() { return weekdayAfter; }
        public PriceRange getWeekendBefore() { return weekendBefore; }
        public PriceRange getWeekendAfter() { return weekendAfter; }
        public int getShowtimeCount() { return showtimeCount; }

        /** Các loại ghế có phụ thu cao hơn ghế rẻ nhất, xếp theo phụ thu tăng dần. */
        public List<SeatExtra> getSeatExtras() {
            BigDecimal cheapest = null;
            for (BigDecimal v : seatSurcharges.values()) {
                cheapest = (cheapest == null || v.compareTo(cheapest) < 0) ? v : cheapest;
            }
            List<SeatExtra> extras = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> e : seatSurcharges.entrySet()) {
                BigDecimal extra = e.getValue().subtract(cheapest);
                if (extra.signum() > 0) {
                    extras.add(new SeatExtra(e.getKey(), extra));
                }
            }
            extras.sort(java.util.Comparator.comparing(SeatExtra::getAmount));
            return extras;
        }

        /** Gộp một suất chiếu vào ô tương ứng (ngày trong tuần x khung giờ). */
        void add(PublicShowtimePrice s) {
            java.time.DayOfWeek dow = s.getShowDate().toLocalDate().getDayOfWeek();
            boolean weekend = dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY;
            boolean before18 = s.getStartTime().toLocalTime().getHour() < 18;
            PriceRange cell = weekend ? (before18 ? weekendBefore : weekendAfter)
                                      : (before18 ? weekdayBefore : weekdayAfter);
            cell.add(s.getMinPrice());
            for (Map.Entry<String, BigDecimal> e : s.getSeatSurcharges().entrySet()) {
                seatSurcharges.putIfAbsent(e.getKey(), e.getValue());
            }
            showtimeCount++;
        }
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
