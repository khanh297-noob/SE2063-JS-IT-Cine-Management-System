package dao.movie;

import model.movie.MovieReview;
import util.JdbcUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bảng MovieReview. Một dòng là một trong hai loại:
 * - Đánh giá: liked = 0, có rating (1-5) và content.
 * - Lượt yêu thích: liked = 1, rating và content để NULL.
 */
public class MovieReviewDAO {

    /** Chỉ các dòng đánh giá (có số sao), mới nhất trước. */
    public List<MovieReview> findByMovie(Connection con, int movieId) throws SQLException {
        String sql = "SELECT ReviewID, MovieID, ReviewerName, rating, content, liked, CreatedAt "
                + "FROM MovieReview WHERE MovieID = ? AND rating IS NOT NULL "
                + "ORDER BY CreatedAt DESC, ReviewID DESC";
        List<MovieReview> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MovieReview r = new MovieReview();
                    r.setReviewId(rs.getInt("ReviewID"));
                    r.setMovieId(rs.getInt("MovieID"));
                    r.setReviewerName(rs.getString("ReviewerName"));
                    r.setRating(rs.getInt("rating"));
                    r.setContent(rs.getString("content"));
                    r.setLiked(rs.getBoolean("liked"));
                    r.setCreatedAt(JdbcUtil.getDateTime(rs, "CreatedAt"));
                    list.add(r);
                }
            }
        }
        return list;
    }

    /** Thêm một dòng đánh giá (liked = 0). */
    public int insert(Connection con, MovieReview r) throws SQLException {
        String sql = "INSERT INTO MovieReview (MovieID, ReviewerName, rating, content, liked) VALUES (?, ?, ?, ?, 0)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getMovieId());
            ps.setString(2, r.getReviewerName());
            ps.setInt(3, r.getRating());
            ps.setString(4, r.getContent());
            ps.executeUpdate();
            return JdbcUtil.generatedKey(ps);
        }
    }

    /** Thêm một lượt yêu thích (liked = 1, không có rating / content). Trả về ReviewID của dòng đó. */
    public int insertLike(Connection con, int movieId, String reviewerName) throws SQLException {
        String sql = "INSERT INTO MovieReview (MovieID, ReviewerName, liked) VALUES (?, ?, 1)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, movieId);
            ps.setString(2, reviewerName);
            ps.executeUpdate();
            return JdbcUtil.generatedKey(ps);
        }
    }

    /** Bỏ một lượt yêu thích. Chỉ xoá dòng liked = 1 đúng phim, không bao giờ xoá dòng đánh giá. */
    public void deleteLike(Connection con, int reviewId, int movieId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM MovieReview WHERE ReviewID = ? AND MovieID = ? AND liked = 1")) {
            ps.setInt(1, reviewId);
            ps.setInt(2, movieId);
            ps.executeUpdate();
        }
    }

    /** MovieID -> số lượt yêu thích. */
    public Map<Integer, Integer> countLikesPerMovie(Connection con) throws SQLException {
        Map<Integer, Integer> map = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT MovieID, COUNT(*) FROM MovieReview WHERE liked = 1 GROUP BY MovieID");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getInt(1), rs.getInt(2));
        }
        return map;
    }
}
