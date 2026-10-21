package model.movie;

import java.util.List;

/** Một dòng trong danh sách phim: Movie + thể loại, poster chính, số chi nhánh. */
public class MovieSummaryDTO {

    private final Movie movie;
    private final List<String> genreNames;
    private final String posterUrl;
    private final int branchCount;

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
}
