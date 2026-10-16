package dao;

import util.DBContext;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Seat;
import model.Showtime;

public class ShowtimeDAO extends DBContext {

    private static final String SELECT_SHOWTIME
            = "SELECT st.ShowtimeID, st.MovieID, m.Title, m.AgeRating, m.Duration, "
            + "       r.RoomID, r.RoomName, r.RoomType, br.BranchID, br.Name AS BranchName, "
            + "       st.ShowDate, st.StartTime, st.EndTime, st.BasePrice "
            + "FROM Showtime st "
            + "JOIN Movie m ON m.MovieID = st.MovieID "
            + "JOIN Room r ON r.RoomID = st.RoomID "
            + "JOIN Branch br ON br.BranchID = r.BranchID ";

    // Suất chiếu của 1 chi nhánh trong 1 ngày, lọc theo tên phim
    public List<Showtime> getByBranchAndDate(int branchId, Date date, String keyword) {
        List<Showtime> list = new ArrayList<>();
        String sql = SELECT_SHOWTIME
                + "WHERE br.BranchID = ? AND st.ShowDate = ? AND m.Title LIKE ? "
                + "ORDER BY m.Title, st.StartTime";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, date);
            ps.setString(3, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapShowtime(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi getByBranchAndDate: " + e.getMessage());
        }
        return list;
    }

    public Showtime getById(int showtimeId) {
        String sql = SELECT_SHOWTIME + "WHERE st.ShowtimeID = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapShowtime(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi ShowtimeDAO.getById: " + e.getMessage());
        }
        return null;
    }

    /**
     * Sơ đồ ghế của 1 suất chiếu (chưa có giá, giá do PosService tính).
     * Ghế đã có vé: đơn Pending -> HELD (đang giữ), còn lại -> BOOKED.
     * Lưu ý: DB có UNIQUE(ShowtimeID, SeatID) nên vé đã hủy vẫn chiếm ghế.
     */
    public List<Seat> getSeats(int showtimeId, int roomId) {
        List<Seat> list = new ArrayList<>();
        String sql = "SELECT s.SeatID, s.RowLabel, s.SeatNumber, s.SeatType, "
                + "  (SELECT TOP 1 b.Status FROM Ticket t JOIN Booking b ON b.BookingID = t.BookingID "
                + "   WHERE t.ShowtimeID = ? AND t.SeatID = s.SeatID) AS BookingStatus "
                + "FROM Seat s WHERE s.RoomID = ? ORDER BY s.RowLabel, s.SeatNumber";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            ps.setInt(2, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Seat s = new Seat();
                    s.setSeatId(rs.getInt("SeatID"));
                    s.setRowLabel(rs.getString("RowLabel"));
                    s.setSeatNumber(rs.getInt("SeatNumber"));
                    s.setSeatType(rs.getString("SeatType"));
                    String bookingStatus = rs.getString("BookingStatus");
                    if (bookingStatus == null) {
                        s.setState("AVAILABLE");
                    } else if ("Pending".equals(bookingStatus)) {
                        s.setState("HELD");
                    } else {
                        s.setState("BOOKED");
                    }
                    list.add(s);
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi getSeats: " + e.getMessage());
        }
        return list;
    }

    private Showtime mapShowtime(ResultSet rs) throws SQLException {
        Showtime s = new Showtime();
        s.setShowtimeId(rs.getInt("ShowtimeID"));
        s.setMovieId(rs.getInt("MovieID"));
        s.setMovieTitle(rs.getString("Title"));
        s.setAgeRating(rs.getString("AgeRating"));
        s.setDuration(rs.getInt("Duration"));
        s.setRoomId(rs.getInt("RoomID"));
        s.setRoomName(rs.getString("RoomName"));
        s.setRoomType(rs.getString("RoomType"));
        s.setBranchId(rs.getInt("BranchID"));
        s.setBranchName(rs.getString("BranchName"));
        s.setShowDate(rs.getDate("ShowDate"));
        s.setStartTime(rs.getTime("StartTime"));
        s.setEndTime(rs.getTime("EndTime"));
        s.setBasePrice(rs.getBigDecimal("BasePrice"));
        return s;
    }
}
