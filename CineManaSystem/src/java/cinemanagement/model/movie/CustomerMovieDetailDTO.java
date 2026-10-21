package cinemanagement.model.movie;

import java.util.List;
import java.util.Locale;

/** Dữ liệu cho trang chi tiết phim phía khách hàng: thông tin phim, trailer, đánh giá. */
public class CustomerMovieDetailDTO {

    private final MovieSummaryDTO summary;
    private final String trailerEmbedUrl;
    private final List<MovieReview> reviews;

    public CustomerMovieDetailDTO(MovieSummaryDTO summary, String trailerEmbedUrl, List<MovieReview> reviews) {
        this.summary = summary;
        this.trailerEmbedUrl = trailerEmbedUrl;
        this.reviews = reviews;
    }

    public int getReviewCount() { return reviews.size(); }

    /** Điểm trung bình 0-5 (0 khi chưa có đánh giá). */
    public double getAverageRating() {
        if (reviews.isEmpty()) return 0;
        int sum = 0;
        for (MovieReview r : reviews) sum += r.getRating();
        return (double) sum / reviews.size();
    }

    /** Vd "4,3". */
    public String getAverageText() { return String.format(Locale.forLanguageTag("vi"), "%.1f", getAverageRating()); }

    /** Số sao tô đầy khi hiển thị trung bình (làm tròn). */
    public int getAverageStars() { return (int) Math.round(getAverageRating()); }

    public MovieSummaryDTO getSummary() { return summary; }
    public Movie getMovie() { return summary.getMovie(); }
    public String getTrailerEmbedUrl() { return trailerEmbedUrl; }
    public List<MovieReview> getReviews() { return reviews; }
}
