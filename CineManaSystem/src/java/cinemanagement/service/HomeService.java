package cinemanagement.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cinemanagement.dao.BranchDAO;
import cinemanagement.dao.ShowtimeDAO;
import cinemanagement.model.Branch;
import cinemanagement.model.MovieSchedule;
import cinemanagement.model.ScheduleView;
import cinemanagement.model.Showtime;

public class HomeService {
    private final BranchDAO branchDAO = new BranchDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();

    public List<Branch> getAllBranches() throws SQLException {
        return branchDAO.findAll();
    }

    public Integer resolveBranchId(Integer requested, List<Branch> branches) {
        if (requested != null) {
            return requested;
        }
        return branches.isEmpty() ? null : branches.get(0).getBranchId();
    }

    public LocalDate resolveDate(LocalDate requested, int maxDaysAhead) {
        LocalDate today = LocalDate.now();
        if (requested == null || requested.isBefore(today) || requested.isAfter(today.plusDays(maxDaysAhead))) {
            return today;
        }
        return requested;
    }

    public List<ScheduleView.DayColumn> buildDayTabs(int count) {
        LocalDate today = LocalDate.now();
        List<ScheduleView.DayColumn> days = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            days.add(new ScheduleView.DayColumn(today.plusDays(i)));
        }
        return days;
    }

    public List<MovieSchedule> getMovieSchedules(Integer branchId, LocalDate date) throws SQLException {
        Map<Integer, MovieSchedule> groups = new LinkedHashMap<>();
        if (branchId != null) {
            for (Showtime s : showtimeDAO.search(branchId, date, date, null, null, false)) {
                if ("Scheduled".equals(s.getStatus()) || s.isFinished()) {
                    continue;
                }
                groups.computeIfAbsent(s.getMovieId(), k -> new MovieSchedule(s)).getShowtimes().add(s);
            }
        }
        return new ArrayList<>(groups.values());
    }
}
