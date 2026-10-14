package cinemanagement.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cinemanagement.model.Movie;
import cinemanagement.util.DBContext;

public class MovieDAO {

    private static final String BASE = "SELECT m.*, (SELECT STRING_AGG(g.GenreName, ', ') FROM Movie_Genre mg "
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

    /** GB-01: chi phim Now Showing / Coming Soon moi duoc xep lich. */
    public List<Movie> findSchedulable() throws SQLException {
        return query(BASE + "WHERE m.Status IN ('Now Showing', 'Coming Soon') ORDER BY m.Title");
    }

    private List<Movie> query(String sql) throws SQLException {
        List<Movie> list = new ArrayList<>();
        try (Connection c = DBContext.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
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
