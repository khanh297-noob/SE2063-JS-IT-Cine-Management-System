package cinemanagement.model;

import java.util.ArrayList;
import java.util.List;

/** Nhom cac suat chieu theo phim (trang Lich chieu). */
public class MovieSchedule {
    private final int movieId;
    private final String title;
    private final String ageRating;
    private final int duration;
    private final String posterUrl;
    private final List<Showtime> showtimes = new ArrayList<>();

    public MovieSchedule(Showtime s) {
        this.movieId = s.getMovieId();
        this.title = s.getMovieTitle();
        this.ageRating = s.getAgeRating();
        this.duration = s.getMovieDuration();
        this.posterUrl = s.getPosterUrl();
    }

    public int getMovieId() { return movieId; }
    public String getTitle() { return title; }
    public String getAgeRating() { return ageRating; }
    public int getDuration() { return duration; }
    public String getPosterUrl() { return posterUrl; }
    public List<Showtime> getShowtimes() { return showtimes; }
}
