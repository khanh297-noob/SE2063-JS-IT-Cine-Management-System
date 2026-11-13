package model.movie;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Một dòng trong danh sách phim: Movie + thể loại, poster chính, số chi nhánh. */
public class MovieSummaryDTO {

    private final Movie movie;
    private final List<String> genreNames;
    private final String posterUrl;
    private final int branchCount;
    /** Trang khách, phim Sắp chiếu: ngày bắt đầu khung chiếu sớm nhất còn ở tương lai (null nếu không có). */
    private LocalDate screeningFrom;
    /** Trang khách, phim Sắp chiếu: phim từng được chiếu ở rạp, đợt này là chiếu lại. */
    private boolean rerun;

    public MovieSummaryDTO(Movie movie, List<String> genreNames, String posterUrl, int branchCount) {
        this.movie = movie;
        this.genreNames = genreNames == null ? List.of() : genreNames;
        this.posterUrl = posterUrl;
        this.branchCount = branchCount;
    }

    public String getGenreText() { return String.join(", ", genreNames); }

    /** Phim không còn thể loại nào (vd thể loại duy nhất đã bị xoá): không được phát sóng. */
    public boolean isGenreless() { return genreNames.isEmpty(); }

    public Movie getMovie() { return movie; }
    public List<String> getGenreNames() { return genreNames; }
    public String getPosterUrl() { return posterUrl; }
    public int getBranchCount() { return branchCount; }

    public LocalDate getScreeningFrom() { return screeningFrom; }
    public void setScreeningFrom(LocalDate screeningFrom) { this.screeningFrom = screeningFrom; }
    public String getScreeningFromText() {
        return screeningFrom == null ? "" : screeningFrom.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
    /**
     * Dòng ngày trên thẻ phim Sắp chiếu: "Chiếu từ" ngày bắt đầu khung chiếu (khi muộn hơn ngày khởi chiếu),
     * "Khởi chiếu" khi phim chưa ra rạp lần đầu, rỗng nếu đã có chi nhánh đang trong khung chiếu.
     */
    public String getComingSoonText() {
        LocalDate release = movie.getReleaseDate();
        if (screeningFrom != null && (release == null || screeningFrom.isAfter(release)))
            return "Chiếu từ " + getScreeningFromText();
        if (release != null && release.isAfter(LocalDate.now())) return "Khởi chiếu " + movie.getReleaseDateText();
        return "";
    }

    public boolean isRerun() { return rerun; }
    public void setRerun(boolean rerun) { this.rerun = rerun; }
}
