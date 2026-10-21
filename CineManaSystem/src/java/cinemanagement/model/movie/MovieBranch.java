package cinemanagement.model.movie;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Bảng MovieBranches: phim được phép chiếu tại chi nhánh nào, trong khoảng ngày nào. */
public class MovieBranch {

    private int movieId;
    private int branchId;
    private LocalDate availableFrom;
    private LocalDate availableTo;
    private LocalDateTime assignedAt;

    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }
    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }
    public LocalDate getAvailableFrom() { return availableFrom; }
    public void setAvailableFrom(LocalDate availableFrom) { this.availableFrom = availableFrom; }
    public LocalDate getAvailableTo() { return availableTo; }
    public void setAvailableTo(LocalDate availableTo) { this.availableTo = availableTo; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
}
