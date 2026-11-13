package dao.movie;

import model.movie.Movie;
import util.JdbcUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Bảng Movie. */
public class MovieDAO {

    private static final String COLUMNS = "m.MovieID, m.title, m.description, m.director, "
            + "m.CastMembers, m.language, m.duration, m.ReleaseDate, m.AgeRating, m.status, m.CreatedAt";

    private static final String ORDER = " ORDER BY CASE m.status WHEN 'Now Showing' THEN 0 "
            + "WHEN 'Coming Soon' THEN 1 ELSE 2 END, m.ReleaseDate DESC, m.title";

    /** Lọc theo trạng thái / thể loại. Tìm theo tên làm ở MovieService để không phân biệt dấu. */
    public List<Movie> findAll(Connection con, String status, Integer genreId) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM Movie m WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (status != null) {
            sql.append("AND m.status = ? ");
            params.add(status);
        }
        if (genreId != null) {
            sql.append("AND EXISTS (SELECT 1 FROM MovieGenre mg WHERE mg.MovieID = m.MovieID AND mg.GenreID = ?) ");
            params.add(genreId);
        }
        sql.append(ORDER);
        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            return mapList(ps);
        }
    }

    public Movie findById(Connection con, int movieId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNS + " FROM Movie m WHERE m.MovieID = ?")) {
            ps.setInt(1, movieId);
            List<Movie> list = mapList(ps);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    /**
     * Phim khác (trừ excludeId) cùng tên và cùng năm khởi chiếu; null nếu không có.
     * So tên không phân biệt hoa thường nhưng phân biệt dấu ("Mai" khác "Mãi"), bất kể collation của cột.
     * Giữ khoá phạm vi tới hết giao dịch để hai người thêm cùng một phim cùng lúc không lọt cả hai.
     */
    public Movie findSameTitleAndYear(Connection con, String title, int year, int excludeId) throws SQLException {
        String sql = "SELECT TOP 1 " + COLUMNS + " FROM Movie m WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE m.title COLLATE Vietnamese_CI_AS = ? COLLATE Vietnamese_CI_AS "
                + "AND YEAR(m.ReleaseDate) = ? AND m.MovieID <> ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setInt(2, year);
            ps.setInt(3, excludeId);
            List<Movie> list = mapList(ps);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    /**
     * Như findById nhưng giữ khoá cập nhật trên dòng phim tới hết giao dịch: hai thao tác đổi trạng thái /
     * xếp suất chiếu cùng phim phải chờ nhau, không cùng kiểm tra trên dữ liệu cũ.
     */
    public Movie findByIdForUpdate(Connection con, int movieId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT " + COLUMNS + " FROM Movie m WITH (UPDLOCK, ROWLOCK) WHERE m.MovieID = ?")) {
            ps.setInt(1, movieId);
            List<Movie> list = mapList(ps);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    /**
     * Số suất chiếu sắp tới của phim (cùng nghĩa với ShowtimeDAO.countUpcomingByMovie), giữ khoá phạm vi tới hết
     * giao dịch để không ai thêm suất chiếu cho phim này trong lúc đang ngưng chiếu.
     */
    public int countUpcomingShowtimesLocked(Connection con, int movieId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM Showtime s WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE s.MovieID = ? AND s.ShowDate >= CAST(GETDATE() AS DATE) AND s.Status <> 'Cancelled'")) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** MovieID của các phim từng có suất chiếu (chưa huỷ) trước hôm nay: phim "chiếu lại" khi đang Sắp chiếu. */
    public Set<Integer> findIdsScreenedBefore(Connection con, LocalDate date) throws SQLException {
        Set<Integer> ids = new HashSet<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT DISTINCT s.MovieID FROM Showtime s WHERE s.ShowDate < ? AND s.Status <> 'Cancelled'")) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    /** Phim đang chiếu tại chi nhánh vào ngày cho trước (theo MovieBranch). */
    public List<Movie> findNowShowingAtBranch(Connection con, int branchId, LocalDate date) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM Movie m "
                + "JOIN MovieBranch mb ON mb.MovieID = m.MovieID AND mb.BranchID = ? "
                + "WHERE m.status = 'Now Showing' "
                + "AND (mb.AvailableFrom IS NULL OR mb.AvailableFrom <= ?) "
                + "AND (mb.AvailableTo IS NULL OR mb.AvailableTo >= ?)" + ORDER;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(date));
            ps.setDate(3, Date.valueOf(date));
            return mapList(ps);
        }
    }

    public int insert(Connection con, Movie m) throws SQLException {
        String sql = "INSERT INTO Movie (title, description, director, CastMembers, language, "
                + "duration, ReleaseDate, AgeRating, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindEditable(ps, m);
            ps.setString(9, m.getStatus());
            ps.executeUpdate();
            return JdbcUtil.generatedKey(ps);
        }
    }

    /** Không sửa status ở đây: trạng thái đổi qua MovieStatusService. */
    public void update(Connection con, Movie m) throws SQLException {
        String sql = "UPDATE Movie SET title = ?, description = ?, director = ?, "
                + "CastMembers = ?, language = ?, duration = ?, ReleaseDate = ?, AgeRating = ? WHERE MovieID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bindEditable(ps, m);
            ps.setInt(9, m.getMovieId());
            ps.executeUpdate();
        }
    }

    /**
     * Chép poster / trailer CHÍNH từ MovieMedia sang Movie.PosterURL / TrailerURL
     * (module đặt vé, báo cáo của nhóm đọc thẳng 2 cột này). Gọi sau mỗi lần đổi media chính.
     */
    public void syncPrimaryMedia(Connection con, int movieId) throws SQLException {
        String sql = "UPDATE Movie SET "
                + "PosterURL = (SELECT TOP 1 URL FROM MovieMedia WHERE MovieID = ? AND MediaType = 'POSTER' AND IsPrimary = 1), "
                + "TrailerURL = (SELECT TOP 1 URL FROM MovieMedia WHERE MovieID = ? AND MediaType = 'TRAILER' AND IsPrimary = 1) "
                + "WHERE MovieID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, movieId);
            ps.setInt(3, movieId);
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection con, int movieId, String status) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE Movie SET status = ? WHERE MovieID = ?")) {
            ps.setString(1, status);
            ps.setInt(2, movieId);
            ps.executeUpdate();
        }
    }

    /** Chỉ đổi khi phim vẫn đang ở trạng thái from; false nếu vừa bị người khác đổi. */
    public boolean updateStatusIf(Connection con, int movieId, String from, String to) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE Movie SET status = ? WHERE MovieID = ? AND status = ?")) {
            ps.setString(1, to);
            ps.setInt(2, movieId);
            ps.setString(3, from);
            return ps.executeUpdate() == 1;
        }
    }

    private void bindEditable(PreparedStatement ps, Movie m) throws SQLException {
        ps.setString(1, m.getTitle());
        JdbcUtil.setString(ps, 2, m.getDescription());
        JdbcUtil.setString(ps, 3, m.getDirector());
        JdbcUtil.setString(ps, 4, m.getCastMembers());
        JdbcUtil.setString(ps, 5, m.getLanguage());
        ps.setInt(6, m.getDuration());
        JdbcUtil.setDate(ps, 7, m.getReleaseDate());
        ps.setString(8, m.getAgeRating());
    }

    private List<Movie> mapList(PreparedStatement ps) throws SQLException {
        List<Movie> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Movie m = new Movie();
                m.setMovieId(rs.getInt("MovieID"));
                m.setTitle(rs.getString("title"));
                m.setDescription(rs.getString("description"));
                m.setDirector(rs.getString("director"));
                m.setCastMembers(rs.getString("CastMembers"));
                m.setLanguage(rs.getString("language"));
                m.setDuration(rs.getInt("duration"));
                m.setReleaseDate(JdbcUtil.getDate(rs, "ReleaseDate"));
                m.setAgeRating(rs.getString("AgeRating"));
                m.setStatus(rs.getString("status"));
                m.setCreatedAt(JdbcUtil.getDateTime(rs, "CreatedAt"));
                list.add(m);
            }
        }
        return list;
    }
}
