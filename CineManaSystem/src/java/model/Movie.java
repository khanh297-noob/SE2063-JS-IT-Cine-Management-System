package model;

import java.util.Date;

public class Movie {
    private int movieID;
    private String title;
    private int duration;
    private Date releaseDate;
    private String ageRating;
    private String trailerURL;
    private String posterURL;
    private String status;

    public Movie() {
    }

    public Movie(int movieID, String title, int duration, Date releaseDate, String ageRating, String trailerURL, String posterURL, String status) {
        this.movieID = movieID;
        this.title = title;
        this.duration = duration;
        this.releaseDate = releaseDate;
        this.ageRating = ageRating;
        this.trailerURL = trailerURL;
        this.posterURL = posterURL;
        this.status = status;
    }

    public int getMovieID() {
        return movieID;
    }

    public void setMovieID(int movieID) {
        this.movieID = movieID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    public String getTrailerURL() {
        return trailerURL;
    }

    public void setTrailerURL(String trailerURL) {
        this.trailerURL = trailerURL;
    }

    public String getPosterURL() {
        return posterURL;
    }

    public void setPosterURL(String posterURL) {
        this.posterURL = posterURL;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
