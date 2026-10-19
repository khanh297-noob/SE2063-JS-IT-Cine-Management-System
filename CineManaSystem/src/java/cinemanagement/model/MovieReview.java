package cinemanagement.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Bảng MovieReviews: đánh giá (rating 1-5 sao + nội dung) hoặc lượt yêu thích (liked = 1). */
public class MovieReview {

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;
    public static final int MAX_CONTENT = 1000;
    public static final int MAX_NAME = 50;

    private static final DateTimeFormatter VN_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int reviewId;
    private int movieId;
    private String reviewerName;
    private int rating;
    private String content;
    private boolean liked;
    private LocalDateTime createdAt;

    public String getCreatedAtText() { return createdAt == null ? "" : createdAt.format(VN_DATE_TIME); }

    public int getReviewId() { return reviewId; }
    public void setReviewId(int reviewId) { this.reviewId = reviewId; }
    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    /** true: dòng lượt yêu thích (không có rating / content). */
    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
