package cinemanagement.model.movie;

import java.util.List;

/** Dữ liệu cho màn "Movie Status": trạng thái hiện tại, trạng thái được chuyển và các điều kiện. */
public class MovieStatusDTO {

    private final Movie movie;
    private final List<String> allowedTargets;
    private final int genreCount;
    private final int branchCount;
    private final int posterCount;
    private final int upcomingShowtimes;
    private final boolean released;

    public MovieStatusDTO(Movie movie, List<String> allowedTargets, int genreCount, int branchCount,
                          int posterCount, int upcomingShowtimes, boolean released) {
        this.movie = movie;
        this.allowedTargets = allowedTargets;
        this.genreCount = genreCount;
        this.branchCount = branchCount;
        this.posterCount = posterCount;
        this.upcomingShowtimes = upcomingShowtimes;
        this.released = released;
    }

    /** Đủ điều kiện chuyển sang "Now Showing". */
    public boolean isReadyToShow() { return released && genreCount > 0 && branchCount > 0 && posterCount > 0; }

    /** Đủ điều kiện chuyển sang "End of Screening". */
    public boolean isReadyToEnd() { return upcomingShowtimes == 0; }

    public Movie getMovie() { return movie; }
    public List<String> getAllowedTargets() { return allowedTargets; }
    public int getGenreCount() { return genreCount; }
    public int getBranchCount() { return branchCount; }
    public int getPosterCount() { return posterCount; }
    public int getUpcomingShowtimes() { return upcomingShowtimes; }
    public boolean isReleased() { return released; }
}
