package cinemanagement.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import cinemanagement.model.Movie;
import cinemanagement.util.DBContext;

public class MovieDAO {
    private static final String BASE = "SELECT m.*, (SELECT STRING_AGG(g.Name, ', ') FROM MovieGenre mg "
            + "JOIN Genre g ON g.GenreID = mg.GenreID WHERE mg.MovieID = m.MovieID) AS Genres FROM Movie m ";

    public Movie findById(int id) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + "WHERE m.MovieID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Movie> findSchedulable() throws SQLException {
        return query(BASE + "WHERE m.Status IN ('Now Showing', 'Coming Soon') ORDER BY m.Title");
    }

    public List<Movie> findSchedulableForBranch(int branchId) throws SQLException {
        return query(BASE + "WHERE m.Status IN ('Now Showing', 'Coming Soon') AND EXISTS (SELECT 1 FROM MovieBranch mb "
                + "WHERE mb.MovieID = m.MovieID AND mb.BranchID = ?) ORDER BY m.Title", branchId);
    }

    public LocalDate[] findBranchWindow(int movieId, int branchId) throws SQLException {
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT AvailableFrom, AvailableTo FROM MovieBranch WHERE MovieID = ? AND BranchID = ?")) {
            ps.setInt(1, movieId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Date from = rs.getDate(1);
                Date to = rs.getDate(2);
                return new LocalDate[] {from == null ? null : from.toLocalDate(), to == null ? null : to.toLocalDate()};
            }
        }
    }

    private List<Movie> query(String sql, Object... params) throws SQLException {
        List<Movie> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private Movie map(ResultSet rs) throws SQLException {
        Movie m = new Movie();
        m.setMovieId(rs.getInt("MovieID"));
        m.setTitle(rs.getString("Title"));
        m.setDuration(rs.getInt("Duration"));
        Date d = rs.getDate("ReleaseDate");
        m.setReleaseDate(d == null ? null : d.toLocalDate());
        m.setAgeRating(rs.getString("AgeRating"));
        m.setTrailerUrl(rs.getString("TrailerURL"));
        m.setPosterUrl(rs.getString("PosterURL"));
        m.setStatus(rs.getString("Status"));
        m.setGenres(rs.getString("Genres"));
        return m;
    }
}
