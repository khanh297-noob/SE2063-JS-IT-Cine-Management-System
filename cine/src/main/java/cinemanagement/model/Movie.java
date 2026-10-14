package cinemanagement.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Bảng Movies. */
public class Movie {

    public static final String STATUS_COMING_SOON = "Coming Soon";
    public static final String STATUS_NOW_SHOWING = "Now Showing";
    public static final String STATUS_ENDED = "End of Screening";

    /** Phân loại theo quy định của Việt Nam (bỏ 'C' vì không được phép chiếu). */
    public static final List<String> AGE_RATINGS = List.of("P", "K", "T13", "T16", "T18");

    private int movieId;
    private String title;
    private String description;
    private String director;
    private String castMembers;
    private String language;
    private Integer duration;
    private LocalDate releaseDate;
    private String ageRating;
    private String status = STATUS_COMING_SOON;
    private LocalDateTime createdAt;

    private static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Ngày khởi chiếu dạng dd/MM/yyyy để hiển thị. */
    public String getReleaseDateText() { return releaseDate == null ? "" : releaseDate.format(VN_DATE); }

    /** cast_members lưu các tên nối bằng ", "; tách ra để hiển thị từng diễn viên thành card. */
    public List<String> getCastList() {
        List<String> list = new ArrayList<>();
        if (castMembers == null) return list;
        for (String name : castMembers.split(",")) {
            String n = name.trim();
            if (!n.isEmpty()) list.add(n);
        }
        return list;
    }

    public boolean isEnded() { return STATUS_ENDED.equals(status); }
    public boolean isNowShowing() { return STATUS_NOW_SHOWING.equals(status); }

    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }
    public String getCastMembers() { return castMembers; }
    public void setCastMembers(String castMembers) { this.castMembers = castMembers; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public String getAgeRating() { return ageRating; }
    public void setAgeRating(String ageRating) { this.ageRating = ageRating; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
