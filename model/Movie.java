package cinemanagement.model;

import java.time.LocalDate;
import cinemanagement.util.DateTimeHelper;

public class Movie {
    private int movieId;
    private String title;
    private int duration;
    private LocalDate releaseDate;
    private String ageRating;
    private String trailerUrl;
    private String posterUrl;
    private String status;
    private String genres;

    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public String getAgeRating() { return ageRating; }
    public void setAgeRating(String ageRating) { this.ageRating = ageRating; }
    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getGenres() { return genres; }
    public void setGenres(String genres) { this.genres = genres; }

    public String getReleaseDateStr() { return DateTimeHelper.format(releaseDate); }
    public boolean isSchedulable() { return "Now Showing".equals(status) || "Coming Soon".equals(status); }
}
