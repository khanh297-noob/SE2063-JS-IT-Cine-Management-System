package dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.Branch;
import model.Movie;

/**
 * Các truy vấn phục vụ báo cáo: doanh thu, suất chiếu, giờ cao điểm, danh sách vé,
 * và phân quyền chi nhánh. Mọi hàm nhận danh sách chi nhánh (branchIds):
 *   null = tất cả chi nhánh, danh sách rỗng = không có chi nhánh nào được xem.
 */
public class ReportDao extends DBContext {

    /**
     * Doanh thu chỉ tính vé còn hiệu lực (Valid / Checked-In) thuộc booking đã Confirmed
     * và đã thanh toán thành công (Payment.GatewayStatus = 'Success').
     * Vé Cancelled, booking Pending/Cancelled, thanh toán Pending/Failed đều bị loại.
     */
    private static final String REVENUE_FILTER =
            " AND t.Status IN ('Valid', 'Checked-In') "
          + " AND EXISTS (SELECT 1 FROM Booking bkx "
          + "             INNER JOIN Payment pay ON pay.BookingID = bkx.BookingID "
          + "             WHERE bkx.BookingID = t.BookingID "
          + "               AND bkx.Status = 'Confirmed' "
          + "               AND pay.GatewayStatus = 'Success') ";

    // ===== Revenue Dashboard (Vé + F&B) =====

    /**
     * CTE các vé đủ điều kiện tính doanh thu (cùng REVENUE_FILTER), kèm số vé của booking
     * để phân bổ doanh thu F&B của booking đó đều cho từng vé.
     * F&B chỉ được tính khi booking đã Confirmed và thanh toán thành công.
     * Lưu ý: Booking_F_B không lưu đơn giá lúc đặt nên dùng giá hiện tại F_B.Price.
     */
    private static final String ELIGIBLE_CTE =
            "WITH Elig AS (SELECT t.TicketID, t.BookingID, t.ShowtimeID, t.SeatID, t.FinalPrice, "
          + "       COUNT(*) OVER (PARTITION BY t.BookingID) AS BkTickets "
          + "FROM Ticket t WHERE 1=1 " + REVENUE_FILTER + "), "
          + "Fnb AS (SELECT bf.BookingID, SUM(bf.Quantity * f.Price) AS FnbTotal "
          + "        FROM Booking_F_B bf INNER JOIN F_B f ON f.ItemID = bf.ItemID "
          + "        GROUP BY bf.BookingID) ";

    private static final String FNB_SHARE =
            "CAST(ISNULL(fb.FnbTotal, 0) AS DECIMAL(18,4)) / e.BkTickets";

    private static final String ELIGIBLE_JOINS =
            "FROM Elig e "
          + "INNER JOIN Showtime s ON e.ShowtimeID = s.ShowtimeID "
          + "INNER JOIN Room r ON s.RoomID = r.RoomID "
          + "INNER JOIN Branch b ON r.BranchID = b.BranchID "
          + "LEFT JOIN Fnb fb ON fb.BookingID = e.BookingID ";

    /** Thêm điều kiện lọc theo ngày chiếu (Showtime.ShowDate) vào câu SQL nếu có chọn ngày. */
    private static void appendShowDateFilter(StringBuilder sql, List<Object> params, String fromDate, String toDate) {
        if (fromDate != null && !fromDate.trim().isEmpty()) {
            sql.append(" AND s.ShowDate >= ? ");
            params.add(Date.valueOf(fromDate.trim()));
        }
        if (toDate != null && !toDate.trim().isEmpty()) {
            sql.append(" AND s.ShowDate <= ? ");
            params.add(Date.valueOf(toDate.trim()));
        }
    }

    private static BigDecimal money(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(0, RoundingMode.HALF_UP);
    }

    /** Đóng kết nối (dùng khi DAO chỉ tạo cho một request). */
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    /** Tạo PreparedStatement từ câu SQL đã ghép và gán lần lượt các tham số. */
    private PreparedStatement prepare(StringBuilder sql, List<Object> params) throws java.sql.SQLException {
        PreparedStatement st = connection.prepareStatement(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            st.setObject(i + 1, params.get(i));
        }
        return st;
    }

    /** Doanh thu vé và F&B theo từng ngày chiếu: {java.sql.Date, ticketRevenue, fnbRevenue}. */
    public List<Object[]> getDailyRevenueSplit(List<Integer> branchIds, String fromDate, String toDate) {
        List<Object[]> data = new ArrayList<>();
        StringBuilder sql = new StringBuilder(ELIGIBLE_CTE);
        sql.append("SELECT s.ShowDate, SUM(e.FinalPrice) AS TicketRev, SUM(").append(FNB_SHARE).append(") AS FnbRev ")
           .append(ELIGIBLE_JOINS)
           .append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        appendBranchFilter(sql, params, branchIds);
        appendShowDateFilter(sql, params, fromDate, toDate);
        sql.append(" GROUP BY s.ShowDate ORDER BY s.ShowDate ASC");

        try {
            PreparedStatement st = prepare(sql, params);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                data.add(new Object[]{rs.getDate("ShowDate"), money(rs.getBigDecimal("TicketRev")), money(rs.getBigDecimal("FnbRev"))});
            }
        } catch (Exception e) {
            System.err.println("ReportDao.getDailyRevenueSplit error: " + e.getMessage());
        }
        return data;
    }

    /**
     * Phân rã doanh thu vé + F&B đi kèm.
     * @param dimension "movie", "branch", "seat" (loại ghế) hoặc "payment" (phương thức thanh toán)
     */
    public List<RevenueBreakdownRow> getRevenueBreakdown(String dimension, List<Integer> branchIds,
                                                         String fromDate, String toDate) {
        String idExpr;
        String nameExpr;
        String extraJoin;
        String groupBy;
        if ("seat".equals(dimension)) {
            idExpr = "0";
            nameExpr = "st.SeatType";
            extraJoin = "INNER JOIN Seat st ON st.SeatID = e.SeatID ";
            groupBy = nameExpr;
        } else if ("payment".equals(dimension)) {
            idExpr = "0";
            nameExpr = "pm.PaymentMethod";
            extraJoin = "INNER JOIN Payment pm ON pm.BookingID = e.BookingID ";
            groupBy = nameExpr;
        } else if ("branch".equals(dimension)) {
            idExpr = "b.BranchID";
            nameExpr = "b.Name";
            extraJoin = "";
            groupBy = "b.BranchID, b.Name";
        } else {
            idExpr = "m.MovieID";
            nameExpr = "m.Title";
            extraJoin = "INNER JOIN Movie m ON m.MovieID = s.MovieID ";
            groupBy = "m.MovieID, m.Title";
        }

        List<RevenueBreakdownRow> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(ELIGIBLE_CTE);
        sql.append("SELECT ").append(idExpr).append(" AS Id, ").append(nameExpr).append(" AS Name, COUNT(e.TicketID) AS Tickets, ")
           .append("SUM(e.FinalPrice) AS TicketRev, SUM(").append(FNB_SHARE).append(") AS FnbRev ")
           .append(ELIGIBLE_JOINS)
           .append(extraJoin)
           .append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        appendBranchFilter(sql, params, branchIds);
        appendShowDateFilter(sql, params, fromDate, toDate);
        sql.append(" GROUP BY ").append(groupBy)
           .append(" ORDER BY SUM(e.FinalPrice) + SUM(").append(FNB_SHARE).append(") DESC");

        try {
            PreparedStatement st = prepare(sql, params);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                RevenueBreakdownRow row = new RevenueBreakdownRow();
                row.setId(rs.getInt("Id"));
                row.setName(rs.getString("Name"));
                row.setTickets(rs.getInt("Tickets"));
                row.setTicketRevenue(money(rs.getBigDecimal("TicketRev")));
                row.setFnbRevenue(money(rs.getBigDecimal("FnbRev")));
                list.add(row);
            }
        } catch (Exception e) {
            System.err.println("ReportDao.getRevenueBreakdown(" + dimension + ") error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Số suất chiếu và mức lấp đầy ghế theo nhóm.
     * Tổng ghế khả dụng của mỗi suất = số ghế thực tế trong bảng Seat của phòng (nếu phòng chưa có ghế thì dùng Room.Capacity).
     * Ghế đã bán = vé đủ điều kiện tính doanh thu. Suất chưa bán vé nào vẫn được tính vào mẫu số.
     * @param dimension "movie", "branch", "room" hoặc "day"
     */
    public List<OccupancyRow> getShowtimeStats(String dimension, List<Integer> branchIds,
                                               String fromDate, String toDate) {
        String select;
        String join;
        String groupBy;
        String orderBy;
        if ("branch".equals(dimension)) {
            select = "b.BranchID AS Id, b.Name AS Name, '' AS SubName";
            join = "";
            groupBy = "b.BranchID, b.Name";
            orderBy = "Sold DESC";
        } else if ("room".equals(dimension)) {
            select = "r.RoomID AS Id, r.RoomName AS Name, CONCAT(b.Name, ' - ', r.RoomType) AS SubName";
            join = "";
            groupBy = "r.RoomID, r.RoomName, b.Name, r.RoomType";
            orderBy = "Sold DESC";
        } else if ("day".equals(dimension)) {
            select = "0 AS Id, CONVERT(VARCHAR(10), s.ShowDate, 23) AS Name, '' AS SubName";
            join = "";
            groupBy = "CONVERT(VARCHAR(10), s.ShowDate, 23)";
            orderBy = "CONVERT(VARCHAR(10), s.ShowDate, 23) ASC";
        } else {
            select = "m.MovieID AS Id, m.Title AS Name, '' AS SubName";
            join = "INNER JOIN Movie m ON m.MovieID = s.MovieID ";
            groupBy = "m.MovieID, m.Title";
            orderBy = "Sold DESC";
        }

        List<OccupancyRow> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("WITH SeatCnt AS (SELECT RoomID, COUNT(*) AS SeatCount FROM Seat GROUP BY RoomID), ")
           .append("SoldCte AS (SELECT t.ShowtimeID, COUNT(*) AS SoldSeats FROM Ticket t WHERE 1=1 ")
           .append(REVENUE_FILTER).append(" GROUP BY t.ShowtimeID) ")
           .append("SELECT ").append(select).append(", COUNT(s.ShowtimeID) AS Shows, ")
           .append("SUM(ISNULL(sd.SoldSeats, 0)) AS Sold, ")
           .append("SUM(ISNULL(NULLIF(sc.SeatCount, 0), r.Capacity)) AS TotalSeats ")
           .append("FROM Showtime s ")
           .append("INNER JOIN Room r ON r.RoomID = s.RoomID ")
           .append("INNER JOIN Branch b ON b.BranchID = r.BranchID ")
           .append("LEFT JOIN SeatCnt sc ON sc.RoomID = r.RoomID ")
           .append("LEFT JOIN SoldCte sd ON sd.ShowtimeID = s.ShowtimeID ")
           .append(join)
           .append("WHERE s.Status <> 'Cancelled' ");
        List<Object> params = new ArrayList<>();
        appendBranchFilter(sql, params, branchIds);
        appendShowDateFilter(sql, params, fromDate, toDate);
        sql.append(" GROUP BY ").append(groupBy).append(" ORDER BY ").append(orderBy);

        try {
            PreparedStatement st = prepare(sql, params);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                OccupancyRow row = new OccupancyRow();
                row.setId(rs.getInt("Id"));
                row.setName(rs.getString("Name"));
                row.setSubName(rs.getString("SubName"));
                row.setShows(rs.getInt("Shows"));
                row.setSoldSeats(rs.getInt("Sold"));
                row.setTotalSeats(rs.getInt("TotalSeats"));
                list.add(row);
            }
        } catch (Exception e) {
            System.err.println("ReportDao.getShowtimeStats(" + dimension + ") error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Hoạt động đặt vé theo khung giờ hoặc ngày trong tuần, tính từ thời điểm đặt (Booking.BookingDate)
     * trong khoảng [fromDate, toDate]. Chỉ tính booking đã Confirmed và thanh toán thành công.
     * @param granularity "hour" (0-23) hoặc "weekday" (1 = Thứ Hai ... 7 = Chủ Nhật)
     */
    public List<PeakRow> getPeakBooking(String granularity, List<Integer> branchIds, String fromDate, String toDate) {
        String slotExpr = "weekday".equals(granularity)
                ? "((DATEPART(WEEKDAY, bk.BookingDate) + @@DATEFIRST - 2) % 7) + 1"
                : "DATEPART(HOUR, bk.BookingDate)";

        List<PeakRow> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(ELIGIBLE_CTE);
        sql.append("SELECT ").append(slotExpr).append(" AS Slot, COUNT(DISTINCT e.BookingID) AS Bookings, ")
           .append("COUNT(e.TicketID) AS Tickets, SUM(e.FinalPrice) AS Revenue ")
           .append(ELIGIBLE_JOINS)
           .append("INNER JOIN Booking bk ON bk.BookingID = e.BookingID ")
           .append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        appendBranchFilter(sql, params, branchIds);
        if (fromDate != null && !fromDate.trim().isEmpty()) {
            sql.append(" AND bk.BookingDate >= ? ");
            params.add(Date.valueOf(fromDate.trim()));
        }
        if (toDate != null && !toDate.trim().isEmpty()) {
            sql.append(" AND bk.BookingDate < ? ");
            params.add(Date.valueOf(java.time.LocalDate.parse(toDate.trim()).plusDays(1)));
        }
        sql.append(" GROUP BY ").append(slotExpr).append(" ORDER BY Slot ASC");

        try {
            PreparedStatement st = prepare(sql, params);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                PeakRow row = new PeakRow(rs.getInt("Slot"));
                row.setBookings(rs.getInt("Bookings"));
                row.setTickets(rs.getInt("Tickets"));
                row.setRevenue(money(rs.getBigDecimal("Revenue")));
                list.add(row);
            }
        } catch (Exception e) {
            System.err.println("ReportDao.getPeakBooking(" + granularity + ") error: " + e.getMessage());
        }
        return list;
    }

    // ===== Dashboard tổng quan chi nhánh =====

    /** Một suất chiếu trong ngày, kèm số ghế đã bán và doanh thu vé. */
    public static class TodayShowtime {
        public java.sql.Time startTime;
        public java.sql.Time endTime;
        public String movieTitle;
        public int duration;
        public String roomName;
        public String roomType;
        public String branchName;
        public int soldSeats;
        public int totalSeats;
        public BigDecimal ticketRevenue = BigDecimal.ZERO;
    }

    /** Các suất chiếu trong một ngày (theo chi nhánh được phép), sắp xếp theo giờ bắt đầu. */
    public List<TodayShowtime> getShowtimesOnDate(List<Integer> branchIds, String date) {
        List<TodayShowtime> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("WITH SeatCnt AS (SELECT RoomID, COUNT(*) AS SeatCount FROM Seat GROUP BY RoomID), ")
           .append("SoldCte AS (SELECT t.ShowtimeID, COUNT(*) AS SoldSeats, SUM(t.FinalPrice) AS Rev FROM Ticket t WHERE 1=1 ")
           .append(REVENUE_FILTER).append(" GROUP BY t.ShowtimeID) ")
           .append("SELECT s.StartTime, s.EndTime, m.Title, m.Duration, r.RoomName, r.RoomType, b.Name AS BranchName, ")
           .append("ISNULL(sd.SoldSeats, 0) AS Sold, ISNULL(NULLIF(sc.SeatCount, 0), r.Capacity) AS TotalSeats, ")
           .append("ISNULL(sd.Rev, 0) AS Rev ")
           .append("FROM Showtime s ")
           .append("INNER JOIN Movie m ON m.MovieID = s.MovieID ")
           .append("INNER JOIN Room r ON r.RoomID = s.RoomID ")
           .append("INNER JOIN Branch b ON b.BranchID = r.BranchID ")
           .append("LEFT JOIN SeatCnt sc ON sc.RoomID = r.RoomID ")
           .append("LEFT JOIN SoldCte sd ON sd.ShowtimeID = s.ShowtimeID ")
           .append("WHERE s.ShowDate = ? AND s.Status <> 'Cancelled' ");
        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(date));
        appendBranchFilter(sql, params, branchIds);
        sql.append(" ORDER BY s.StartTime ASC, b.Name ASC, r.RoomName ASC");

        try {
            PreparedStatement st = prepare(sql, params);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                TodayShowtime t = new TodayShowtime();
                t.startTime = rs.getTime("StartTime");
                t.endTime = rs.getTime("EndTime");
                t.movieTitle = rs.getString("Title");
                t.duration = rs.getInt("Duration");
                t.roomName = rs.getString("RoomName");
                t.roomType = rs.getString("RoomType");
                t.branchName = rs.getString("BranchName");
                t.soldSeats = rs.getInt("Sold");
                t.totalSeats = rs.getInt("TotalSeats");
                t.ticketRevenue = money(rs.getBigDecimal("Rev"));
                list.add(t);
            }
        } catch (Exception e) {
            System.err.println("ReportDao.getShowtimesOnDate error: " + e.getMessage());
        }
        return list;
    }

    /** Số phòng chiếu đang hoạt động (Room.Status = 'Active') trong phạm vi chi nhánh. */
    public int countActiveRooms(List<Integer> branchIds) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM Room r INNER JOIN Branch b ON b.BranchID = r.BranchID WHERE r.Status = 'Active' ");
        List<Object> params = new ArrayList<>();
        appendBranchFilter(sql, params, branchIds);
        try {
            PreparedStatement st = prepare(sql, params);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            System.err.println("ReportDao.countActiveRooms error: " + e.getMessage());
        }
        return 0;
    }

    /** branchIds == null: không giới hạn (admin); danh sách rỗng: không có chi nhánh nào được phép. */
    private static void appendBranchFilter(StringBuilder sql, List<Object> params, List<Integer> branchIds) {
        if (branchIds == null) {
            return;
        }
        if (branchIds.isEmpty()) {
            sql.append(" AND 1 = 0 ");
            return;
        }
        sql.append(" AND b.BranchID IN (");
        for (int i = 0; i < branchIds.size(); i++) {
            sql.append(i == 0 ? "?" : ",?");
            params.add(branchIds.get(i));
        }
        sql.append(") ");
    }

    /** Danh sách chi nhánh cho bộ lọc; branchIds == null nghĩa là tất cả. */
    public List<Branch> getBranches(List<Integer> branchIds) {
        if (branchIds == null) {
            return getAllBranches();
        }
        List<Branch> list = new ArrayList<>();
        if (branchIds.isEmpty()) {
            return list;
        }
        try {
            StringBuilder sql = new StringBuilder("SELECT BranchID, Name, Address, City, Hotline FROM Branch WHERE BranchID IN (");
            for (int i = 0; i < branchIds.size(); i++) {
                sql.append(i == 0 ? "?" : ",?");
            }
            sql.append(") ORDER BY Name");
            PreparedStatement st = connection.prepareStatement(sql.toString());
            for (int i = 0; i < branchIds.size(); i++) {
                st.setInt(i + 1, branchIds.get(i));
            }
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                list.add(new Branch(rs.getInt("BranchID"), rs.getString("Name"),
                        rs.getString("Address"), rs.getString("City"), rs.getString("Hotline")));
            }
        } catch (Exception e) {
            System.err.println("ReportDao.getBranches error: " + e.getMessage());
        }
        return list;
    }

    /** Danh sách tất cả chi nhánh, sắp xếp theo tên. */
    public List<Branch> getAllBranches() {
        List<Branch> list = new ArrayList<>();
        try {
            String sql = "SELECT BranchID, Name, Address, City, Hotline FROM Branch ORDER BY Name";
            PreparedStatement st = connection.prepareStatement(sql);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                Branch b = new Branch(
                    rs.getInt("BranchID"),
                    rs.getString("Name"),
                    rs.getString("Address"),
                    rs.getString("City"),
                    rs.getString("Hotline")
                );
                list.add(b);
            }
        } catch (Exception e) {
            System.err.println("ReportDao.getAllBranches error: " + e.getMessage());
        }
        return list;
    }

    /** Danh sách tất cả phim, sắp xếp theo tên (dùng cho ô lọc phim). */
    public List<Movie> getAllMovies() {
        List<Movie> list = new ArrayList<>();
        try {
            String sql = "SELECT MovieID, Title, Duration, ReleaseDate, AgeRating, TrailerURL, PosterURL, Status FROM Movie ORDER BY Title";
            PreparedStatement st = connection.prepareStatement(sql);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                Movie m = new Movie(
                    rs.getInt("MovieID"),
                    rs.getString("Title"),
                    rs.getInt("Duration"),
                    rs.getDate("ReleaseDate"),
                    rs.getString("AgeRating"),
                    rs.getString("TrailerURL"),
                    rs.getString("PosterURL"),
                    rs.getString("Status")
                );
                list.add(m);
            }
        } catch (Exception e) {
            System.err.println("ReportDao.getAllMovies error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Danh sách phim cho ô lọc phim theo chi nhánh được xem.
     * Gồm phim được gán cho chi nhánh (bảng MovieBranch) và phim đã có suất chiếu ở phòng của chi nhánh
     * (trừ suất đã hủy), nên phim đã ngừng chiếu vẫn xem lại được doanh thu.
     * Không giới hạn theo ngày AvailableFrom/AvailableTo vì báo cáo xem lại cả những ngày đã qua.
     * branchIds == null: tất cả phim (Admin). Danh sách rỗng: không có phim nào.
     */
    public List<Movie> getMovies(List<Integer> branchIds) {
        if (branchIds == null) {
            return getAllMovies();
        }
        List<Movie> list = new ArrayList<>();
        if (branchIds.isEmpty()) {
            return list;
        }

        // Câu SQL có 2 danh sách chi nhánh (một cho MovieBranch, một cho Showtime) nên gán tham số 2 lần
        StringBuilder sql = new StringBuilder(moviesByBranchSql(branchIds.size()));
        List<Object> params = new ArrayList<>();
        params.addAll(branchIds);
        params.addAll(branchIds);

        try {
            PreparedStatement st = prepare(sql, params);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                list.add(new Movie(
                        rs.getInt("MovieID"),
                        rs.getString("Title"),
                        rs.getInt("Duration"),
                        rs.getDate("ReleaseDate"),
                        rs.getString("AgeRating"),
                        rs.getString("TrailerURL"),
                        rs.getString("PosterURL"),
                        rs.getString("Status")));
            }
        } catch (Exception e) {
            System.err.println("ReportDao.getMovies error: " + e.getMessage());
        }
        return list;
    }

    /** Ghép câu SQL lấy phim theo chi nhánh; branchCount là số chi nhánh (mỗi chi nhánh một dấu ?). */
    private static String moviesByBranchSql(int branchCount) {
        StringBuilder marks = new StringBuilder();
        for (int i = 0; i < branchCount; i++) {
            marks.append(i == 0 ? "?" : ",?");
        }
        return "SELECT m.MovieID, m.Title, m.Duration, m.ReleaseDate, m.AgeRating, m.TrailerURL, m.PosterURL, m.Status "
                + "FROM Movie m "
                + "WHERE EXISTS (SELECT 1 FROM MovieBranch mb WHERE mb.MovieID = m.MovieID AND mb.BranchID IN (" + marks + ")) "
                + "OR EXISTS (SELECT 1 FROM Showtime s INNER JOIN Room r ON r.RoomID = s.RoomID "
                + "           WHERE s.MovieID = m.MovieID AND s.Status <> 'Cancelled' AND r.BranchID IN (" + marks + ")) "
                + "ORDER BY m.Title";
    }

    /** Danh sách vé đã bán (đủ điều kiện tính doanh thu), mới nhất trước. */
    public List<TicketReportItem> getTicketSalesReport(List<Integer> branchIds, Integer movieId, String fromDate, String toDate) {
        List<TicketReportItem> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.TicketID, t.TicketCode, t.FinalPrice, t.Status AS TicketStatus, ")
           .append("       m.Title AS MovieTitle, b.Name AS BranchName, r.RoomName, ")
           .append("       ISNULL(CONCAT(st.RowLabel, st.SeatNumber), 'N/A') AS SeatName, ")
           .append("       s.ShowDate, s.StartTime, ")
           .append("       bk.BookingDate, ISNULL(u.FullName, N'Khách vãng lai') AS CustomerName, ISNULL(u.Email, '') AS CustomerEmail ")
           .append("FROM Ticket t ")
           .append("INNER JOIN Showtime s ON t.ShowtimeID = s.ShowtimeID ")
           .append("INNER JOIN Movie m ON s.MovieID = m.MovieID ")
           .append("INNER JOIN Room r ON s.RoomID = r.RoomID ")
           .append("INNER JOIN Branch b ON r.BranchID = b.BranchID ")
           .append("LEFT JOIN Seat st ON t.SeatID = st.SeatID ")
           .append("LEFT JOIN Booking bk ON t.BookingID = bk.BookingID ")
           .append("LEFT JOIN [User] u ON bk.UserID = u.UserID ")
           .append("WHERE 1=1 " + REVENUE_FILTER);

        List<Object> params = new ArrayList<>();

        appendBranchFilter(sql, params, branchIds);
        if (movieId != null && movieId > 0) {
            sql.append(" AND m.MovieID = ? ");
            params.add(movieId);
        }
        appendShowDateFilter(sql, params, fromDate, toDate);

        sql.append("ORDER BY s.ShowDate DESC, s.StartTime DESC, t.TicketID DESC");

        try {
            PreparedStatement st = prepare(sql, params);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                TicketReportItem item = new TicketReportItem();
                item.setTicketID(rs.getInt("TicketID"));
                item.setTicketCode(rs.getString("TicketCode"));
                item.setMovieTitle(rs.getString("MovieTitle"));
                item.setBranchName(rs.getString("BranchName"));
                item.setRoomName(rs.getString("RoomName"));
                item.setSeatName(rs.getString("SeatName"));
                item.setShowDate(rs.getDate("ShowDate"));
                item.setStartTime(rs.getTime("StartTime"));
                item.setBookingDate(rs.getTimestamp("BookingDate"));
                item.setCustomerName(rs.getString("CustomerName"));
                item.setCustomerEmail(rs.getString("CustomerEmail"));
                item.setFinalPrice(rs.getBigDecimal("FinalPrice"));
                item.setTicketStatus(rs.getString("TicketStatus"));
                list.add(item);
            }
        } catch (Exception e) {
            System.err.println("ReportDao.getTicketSalesReport error: " + e.getMessage());
        }
        return list;
    }

    /** Tổng hợp: số vé, doanh thu, số booking và phim bán chạy nhất. */
    public SalesSummary getSalesSummary(List<Integer> branchIds, Integer movieId, String fromDate, String toDate) {
        SalesSummary summary = new SalesSummary();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(t.TicketID) AS TotalTickets, ")
           .append("       ISNULL(SUM(t.FinalPrice), 0) AS TotalRevenue, ")
           .append("       COUNT(DISTINCT t.BookingID) AS TotalBookings ")
           .append("FROM Ticket t ")
           .append("INNER JOIN Showtime s ON t.ShowtimeID = s.ShowtimeID ")
           .append("INNER JOIN Room r ON s.RoomID = r.RoomID ")
           .append("INNER JOIN Branch b ON r.BranchID = b.BranchID ")
           .append("WHERE 1=1 " + REVENUE_FILTER);

        List<Object> params = new ArrayList<>();
        appendBranchFilter(sql, params, branchIds);
        if (movieId != null && movieId > 0) {
            sql.append(" AND s.MovieID = ? ");
            params.add(movieId);
        }
        appendShowDateFilter(sql, params, fromDate, toDate);

        try {
            PreparedStatement st = prepare(sql, params);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                summary.setTotalTickets(rs.getInt("TotalTickets"));
                summary.setTotalRevenue(rs.getBigDecimal("TotalRevenue"));
                summary.setTotalBookings(rs.getInt("TotalBookings"));
            }
        } catch (Exception e) {
            System.err.println("ReportDao.getSalesSummary metrics error: " + e.getMessage());
        }

        // Top Movie
        try {
            StringBuilder topMovieSql = new StringBuilder();
            topMovieSql.append("SELECT TOP 1 m.Title, COUNT(t.TicketID) AS TicketsSold, SUM(t.FinalPrice) AS MovieRevenue ")
                       .append("FROM Ticket t ")
                       .append("INNER JOIN Showtime s ON t.ShowtimeID = s.ShowtimeID ")
                       .append("INNER JOIN Movie m ON s.MovieID = m.MovieID ")
                       .append("INNER JOIN Room r ON s.RoomID = r.RoomID ")
                       .append("INNER JOIN Branch b ON r.BranchID = b.BranchID ")
                       .append("WHERE 1=1 " + REVENUE_FILTER);

            List<Object> topParams = new ArrayList<>();
            appendBranchFilter(topMovieSql, topParams, branchIds);
            appendShowDateFilter(topMovieSql, topParams, fromDate, toDate);
            topMovieSql.append("GROUP BY m.Title ORDER BY TicketsSold DESC, MovieRevenue DESC");

            PreparedStatement topSt = prepare(topMovieSql, topParams);
            ResultSet rsTop = topSt.executeQuery();
            if (rsTop.next()) {
                summary.setTopMovieTitle(rsTop.getString("Title"));
                summary.setTopMovieTickets(rsTop.getInt("TicketsSold"));
                summary.setTopMovieRevenue(rsTop.getBigDecimal("MovieRevenue"));
            }
        } catch (Exception e) {
            System.err.println("ReportDao.getSalesSummary topMovie error: " + e.getMessage());
        }

        return summary;
    }

    /** Dữ liệu biểu đồ doanh thu theo ngày: {ngày, số vé, doanh thu}. */
    public List<Object[]> getDailyRevenueChart(List<Integer> branchIds, Integer movieId, String fromDate, String toDate) {
        List<Object[]> data = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT s.ShowDate, COUNT(t.TicketID) AS Tickets, ISNULL(SUM(t.FinalPrice), 0) AS Revenue ")
           .append("FROM Ticket t ")
           .append("INNER JOIN Showtime s ON t.ShowtimeID = s.ShowtimeID ")
           .append("INNER JOIN Room r ON s.RoomID = r.RoomID ")
           .append("INNER JOIN Branch b ON r.BranchID = b.BranchID ")
           .append("WHERE 1=1 " + REVENUE_FILTER);

        List<Object> params = new ArrayList<>();
        appendBranchFilter(sql, params, branchIds);
        if (movieId != null && movieId > 0) {
            sql.append(" AND s.MovieID = ? ");
            params.add(movieId);
        }
        appendShowDateFilter(sql, params, fromDate, toDate);
        sql.append("GROUP BY s.ShowDate ORDER BY s.ShowDate ASC");

        try {
            PreparedStatement st = prepare(sql, params);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                data.add(new Object[]{
                    rs.getDate("ShowDate").toString(),
                    rs.getInt("Tickets"),
                    rs.getBigDecimal("Revenue")
                });
            }
        } catch (Exception e) {
            System.err.println("ReportDao.getDailyRevenueChart error: " + e.getMessage());
        }
        return data;
    }

    /** Dữ liệu biểu đồ doanh thu theo phim: {tên phim, số vé, doanh thu}. */
    public List<Object[]> getRevenueByMovieChart(List<Integer> branchIds, String fromDate, String toDate) {
        List<Object[]> data = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT m.Title, COUNT(t.TicketID) AS Tickets, ISNULL(SUM(t.FinalPrice), 0) AS Revenue ")
           .append("FROM Ticket t ")
           .append("INNER JOIN Showtime s ON t.ShowtimeID = s.ShowtimeID ")
           .append("INNER JOIN Movie m ON s.MovieID = m.MovieID ")
           .append("INNER JOIN Room r ON s.RoomID = r.RoomID ")
           .append("INNER JOIN Branch b ON r.BranchID = b.BranchID ")
           .append("WHERE 1=1 " + REVENUE_FILTER);

        List<Object> params = new ArrayList<>();
        appendBranchFilter(sql, params, branchIds);
        appendShowDateFilter(sql, params, fromDate, toDate);
        sql.append("GROUP BY m.Title ORDER BY Revenue DESC");

        try {
            PreparedStatement st = prepare(sql, params);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                data.add(new Object[]{
                    rs.getString("Title"),
                    rs.getInt("Tickets"),
                    rs.getBigDecimal("Revenue")
                });
            }
        } catch (Exception e) {
            System.err.println("ReportDao.getRevenueByMovieChart error: " + e.getMessage());
        }
        return data;
    }

    // ==================== DỮ LIỆU TRẢ VỀ (class lồng) ====================

    /** Thống kê suất chiếu và mức lấp đầy ghế theo một nhóm (phim, chi nhánh, phòng, ngày). */
    public static class OccupancyRow {

        private int id;
        private String name;
        private String subName;
        private int shows;
        private int soldSeats;
        private int totalSeats;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSubName() { return subName; }
        public void setSubName(String subName) { this.subName = subName; }

        public int getShows() { return shows; }
        public void setShows(int shows) { this.shows = shows; }

        public int getSoldSeats() { return soldSeats; }
        public void setSoldSeats(int soldSeats) { this.soldSeats = soldSeats; }

        public int getTotalSeats() { return totalSeats; }
        public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

        /** Tỷ lệ lấp đầy (%) = ghế đã bán / tổng ghế khả dụng của các suất chiếu. */
        public double getOccupancy() {
            return totalSeats > 0 ? soldSeats * 100.0 / totalSeats : 0;
        }

        /** Tỷ lệ ghế trống (%). */
        public double getVacancy() {
            return totalSeats > 0 ? 100.0 - getOccupancy() : 0;
        }
    }

    /** Hoạt động đặt vé theo một khung giờ (0-23) hoặc một ngày trong tuần (1 = Thứ Hai ... 7 = Chủ Nhật). */
    public static class PeakRow {

        private int slot;
        private int bookings;
        private int tickets;
        private BigDecimal revenue = BigDecimal.ZERO;

        public PeakRow() { }

        public PeakRow(int slot) { this.slot = slot; }

        public int getSlot() { return slot; }
        public void setSlot(int slot) { this.slot = slot; }

        public int getBookings() { return bookings; }
        public void setBookings(int bookings) { this.bookings = bookings; }

        public int getTickets() { return tickets; }
        public void setTickets(int tickets) { this.tickets = tickets; }

        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue != null ? revenue : BigDecimal.ZERO; }
    }

    /** Một dòng trong bảng phân rã doanh thu (theo phim / loại ghế / phương thức thanh toán). */
    public static class RevenueBreakdownRow {

        private int id;
        private String name;
        private int tickets;
        private BigDecimal ticketRevenue = BigDecimal.ZERO;
        private BigDecimal fnbRevenue = BigDecimal.ZERO;
        private double share;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getTickets() { return tickets; }
        public void setTickets(int tickets) { this.tickets = tickets; }

        public BigDecimal getTicketRevenue() { return ticketRevenue; }
        public void setTicketRevenue(BigDecimal v) { this.ticketRevenue = v != null ? v : BigDecimal.ZERO; }

        public BigDecimal getFnbRevenue() { return fnbRevenue; }
        public void setFnbRevenue(BigDecimal v) { this.fnbRevenue = v != null ? v : BigDecimal.ZERO; }

        public BigDecimal getTotalRevenue() { return ticketRevenue.add(fnbRevenue); }

        /** Tỷ trọng (%) trong tổng doanh thu của bảng. */
        public double getShare() { return share; }
        public void setShare(double share) { this.share = share; }
    }

    public static class SalesSummary {
        private int totalTickets;
        private BigDecimal totalRevenue;
        private int totalBookings;
        private String topMovieTitle;
        private int topMovieTickets;
        private BigDecimal topMovieRevenue;
        private String topBranchName;
        private BigDecimal topBranchRevenue;

        public SalesSummary() {
            this.totalTickets = 0;
            this.totalRevenue = BigDecimal.ZERO;
            this.totalBookings = 0;
            this.topMovieTitle = "Chưa có dữ liệu";
            this.topMovieTickets = 0;
            this.topMovieRevenue = BigDecimal.ZERO;
            this.topBranchName = "Chưa có dữ liệu";
            this.topBranchRevenue = BigDecimal.ZERO;
        }

        public int getTotalTickets() {
            return totalTickets;
        }

        public void setTotalTickets(int totalTickets) {
            this.totalTickets = totalTickets;
        }

        public BigDecimal getTotalRevenue() {
            return totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
        }

        public void setTotalRevenue(BigDecimal totalRevenue) {
            this.totalRevenue = totalRevenue;
        }

        public int getTotalBookings() {
            return totalBookings;
        }

        public void setTotalBookings(int totalBookings) {
            this.totalBookings = totalBookings;
        }

        public String getTopMovieTitle() {
            return topMovieTitle;
        }

        public void setTopMovieTitle(String topMovieTitle) {
            this.topMovieTitle = topMovieTitle;
        }

        public int getTopMovieTickets() {
            return topMovieTickets;
        }

        public void setTopMovieTickets(int topMovieTickets) {
            this.topMovieTickets = topMovieTickets;
        }

        public BigDecimal getTopMovieRevenue() {
            return topMovieRevenue != null ? topMovieRevenue : BigDecimal.ZERO;
        }

        public void setTopMovieRevenue(BigDecimal topMovieRevenue) {
            this.topMovieRevenue = topMovieRevenue;
        }

        public String getTopBranchName() {
            return topBranchName;
        }

        public void setTopBranchName(String topBranchName) {
            this.topBranchName = topBranchName;
        }

        public BigDecimal getTopBranchRevenue() {
            return topBranchRevenue != null ? topBranchRevenue : BigDecimal.ZERO;
        }

        public void setTopBranchRevenue(BigDecimal topBranchRevenue) {
            this.topBranchRevenue = topBranchRevenue;
        }
    }

    public static class TicketReportItem {
        private int ticketID;
        private String ticketCode;
        private String movieTitle;
        private String branchName;
        private String roomName;
        private String seatName;
        private Date showDate;
        private Time startTime;
        private Timestamp bookingDate;
        private String customerName;
        private String customerEmail;
        private BigDecimal finalPrice;
        private String ticketStatus;

        public TicketReportItem() {
        }

        public int getTicketID() {
            return ticketID;
        }

        public void setTicketID(int ticketID) {
            this.ticketID = ticketID;
        }

        public String getTicketCode() {
            return ticketCode;
        }

        public void setTicketCode(String ticketCode) {
            this.ticketCode = ticketCode;
        }

        public String getMovieTitle() {
            return movieTitle;
        }

        public void setMovieTitle(String movieTitle) {
            this.movieTitle = movieTitle;
        }

        public String getBranchName() {
            return branchName;
        }

        public void setBranchName(String branchName) {
            this.branchName = branchName;
        }

        public String getRoomName() {
            return roomName;
        }

        public void setRoomName(String roomName) {
            this.roomName = roomName;
        }

        public String getSeatName() {
            return seatName;
        }

        public void setSeatName(String seatName) {
            this.seatName = seatName;
        }

        public Date getShowDate() {
            return showDate;
        }

        public void setShowDate(Date showDate) {
            this.showDate = showDate;
        }

        public Time getStartTime() {
            return startTime;
        }

        public void setStartTime(Time startTime) {
            this.startTime = startTime;
        }

        public Timestamp getBookingDate() {
            return bookingDate;
        }

        public void setBookingDate(Timestamp bookingDate) {
            this.bookingDate = bookingDate;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public String getCustomerEmail() {
            return customerEmail;
        }

        public void setCustomerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
        }

        public BigDecimal getFinalPrice() {
            return finalPrice != null ? finalPrice : BigDecimal.ZERO;
        }

        public void setFinalPrice(BigDecimal finalPrice) {
            this.finalPrice = finalPrice;
        }

        public String getTicketStatus() {
            return ticketStatus;
        }

        public void setTicketStatus(String ticketStatus) {
            this.ticketStatus = ticketStatus;
        }
    }
}
