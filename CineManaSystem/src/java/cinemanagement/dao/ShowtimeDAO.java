package cinemanagement.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import cinemanagement.model.Showtime;
import cinemanagement.util.DBContext;

public class ShowtimeDAO {
    private static final String BASE =
            "SELECT s.ShowtimeID, s.MovieID, s.RoomID, s.ShowDate, s.StartTime, s.EndTime, s.BasePrice, s.Status, "
            + "m.Title, m.Duration, m.AgeRating, m.PosterURL, r.RoomName, r.RoomType, rt.Surcharge AS RoomSurcharge, "
            + "r.BranchID, b.Name AS BranchName, "
            + "(SELECT COUNT(*) FROM Ticket t WHERE t.ShowtimeID = s.ShowtimeID AND t.Status <> 'Cancelled') AS SoldCount, "
            + "(SELECT COUNT(*) FROM Seat se WHERE se.RoomID = s.RoomID AND se.Status = 'Active') AS SeatCount "
            + "FROM Showtime s JOIN Movie m ON m.MovieID = s.MovieID "
            + "JOIN Room r ON r.RoomID = s.RoomID JOIN Branch b ON b.BranchID = r.BranchID "
            + "LEFT JOIN RoomType rt ON rt.Name = r.RoomType ";

    public Showtime findById(int id) throws SQLException {
        try (Connection c = DBContext.getConnection()) {
            return findById(c, id);
        }
    }

    public Showtime findById(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(BASE + "WHERE s.ShowtimeID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Showtime> search(Integer branchId, LocalDate from, LocalDate to, Integer roomId, Integer movieId,
                                 boolean includeCancelled) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE).append("WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (branchId != null) {
            sql.append(" AND r.BranchID = ?");
            params.add(branchId);
        }
        if (from != null) {
            sql.append(" AND s.ShowDate >= ?");
            params.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND s.ShowDate <= ?");
            params.add(Date.valueOf(to));
        }
        if (roomId != null) {
            sql.append(" AND s.RoomID = ?");
            params.add(roomId);
        }
        if (movieId != null) {
            sql.append(" AND s.MovieID = ?");
            params.add(movieId);
        }
        if (!includeCancelled) {
            sql.append(" AND s.Status <> 'Cancelled'");
        }
        sql.append(" ORDER BY s.ShowDate, s.StartTime, r.RoomName");
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return list(ps);
        }
    }

    public List<Showtime> findByRoomAround(Connection c, int roomId, LocalDate date, Integer excludeId)
            throws SQLException {
        String sql = BASE + "WHERE s.RoomID = ? AND s.ShowDate BETWEEN ? AND ? AND s.Status <> 'Cancelled'"
                + " AND s.ShowtimeID <> ? ORDER BY s.ShowDate, s.StartTime";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setDate(2, Date.valueOf(date.minusDays(1)));
            ps.setDate(3, Date.valueOf(date.plusDays(1)));
            ps.setInt(4, excludeId == null ? -1 : excludeId);
            return list(ps);
        }
    }

    public List<Showtime> findBookedByUserAround(int userId, LocalDate date) throws SQLException {
        String sql = BASE + "WHERE s.ShowtimeID IN (SELECT t.ShowtimeID FROM Ticket t JOIN Booking bk ON bk.BookingID = t.BookingID "
                + "WHERE bk.UserID = ? AND bk.Status = 'Confirmed' AND t.Status <> 'Cancelled') "
                + "AND s.ShowDate BETWEEN ? AND ? AND s.Status <> 'Cancelled'";
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(date.minusDays(1)));
            ps.setDate(3, Date.valueOf(date.plusDays(1)));
            return list(ps);
        }
    }

    public int insert(Connection c, Showtime s) throws SQLException {
        String sql = "INSERT INTO Showtime (MovieID, RoomID, ShowDate, StartTime, EndTime, BasePrice, Status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, s);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void update(Connection c, Showtime s) throws SQLException {
        String sql = "UPDATE Showtime SET MovieID = ?, RoomID = ?, ShowDate = ?, StartTime = ?, EndTime = ?, "
                + "BasePrice = ?, Status = ? WHERE ShowtimeID = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            fill(ps, s);
            ps.setInt(8, s.getShowtimeId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection c, int id, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE Showtime SET Status = ? WHERE ShowtimeID = ?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM Showtime WHERE ShowtimeID = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int countTickets(int showtimeId, boolean activeOnly) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Ticket WHERE ShowtimeID = ?" + (activeOnly ? " AND Status <> 'Cancelled'" : "");
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void fill(PreparedStatement ps, Showtime s) throws SQLException {
        ps.setInt(1, s.getMovieId());
        ps.setInt(2, s.getRoomId());
        ps.setDate(3, Date.valueOf(s.getShowDate()));
        ps.setTime(4, Time.valueOf(s.getStartTime()));
        ps.setTime(5, Time.valueOf(s.getEndTime()));
        ps.setBigDecimal(6, s.getBasePrice());
        ps.setString(7, s.getStatus());
    }

    private List<Showtime> list(PreparedStatement ps) throws SQLException {
        List<Showtime> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    static Showtime map(ResultSet rs) throws SQLException {
        Showtime s = new Showtime();
        s.setShowtimeId(rs.getInt("ShowtimeID"));
        s.setMovieId(rs.getInt("MovieID"));
        s.setRoomId(rs.getInt("RoomID"));
        s.setShowDate(rs.getDate("ShowDate").toLocalDate());
        s.setStartTime(rs.getTime("StartTime").toLocalTime());
        s.setEndTime(rs.getTime("EndTime").toLocalTime());
        s.setBasePrice(rs.getBigDecimal("BasePrice"));
        s.setStatus(rs.getString("Status"));
        s.setMovieTitle(rs.getString("Title"));
        s.setMovieDuration(rs.getInt("Duration"));
        s.setAgeRating(rs.getString("AgeRating"));
        s.setPosterUrl(rs.getString("PosterURL"));
        s.setRoomName(rs.getString("RoomName"));
        s.setRoomType(rs.getString("RoomType"));
        s.setRoomSurcharge(rs.getBigDecimal("RoomSurcharge"));
        s.setBranchId(rs.getInt("BranchID"));
        s.setBranchName(rs.getString("BranchName"));
        s.setSoldCount(rs.getInt("SoldCount"));
        s.setSeatCount(rs.getInt("SeatCount"));
        return s;
    }
}
