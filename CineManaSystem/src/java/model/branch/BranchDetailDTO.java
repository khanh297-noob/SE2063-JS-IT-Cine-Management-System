package model.branch;

import model.movie.MovieSummaryDTO;

import java.util.List;

/** Dữ liệu cho trang "View Branch Details". */
public class BranchDetailDTO {

    private final Branch branch;
    private final List<BranchOperatingHour> hours;
    private final List<RoomSummaryDTO> rooms;
    private final List<MovieSummaryDTO> nowShowing;
    private final boolean openNow;
    private final int todayDayOfWeek;

    public BranchDetailDTO(Branch branch, List<BranchOperatingHour> hours, List<RoomSummaryDTO> rooms,
                           List<MovieSummaryDTO> nowShowing, boolean openNow, int todayDayOfWeek) {
        this.branch = branch;
        this.hours = hours;
        this.rooms = rooms;
        this.nowShowing = nowShowing;
        this.openNow = openNow;
        this.todayDayOfWeek = todayDayOfWeek;
    }

    public Branch getBranch() { return branch; }
    public List<BranchOperatingHour> getHours() { return hours; }
    public List<RoomSummaryDTO> getRooms() { return rooms; }
    public List<MovieSummaryDTO> getNowShowing() { return nowShowing; }
    public boolean isOpenNow() { return openNow; }
    public int getTodayDayOfWeek() { return todayDayOfWeek; }
}
