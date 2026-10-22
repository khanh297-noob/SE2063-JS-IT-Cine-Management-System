package model.movie;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import model.branch.Branch;
import util.DateTimeHelper;

public class ScheduleView {
    public static class DayColumn {
        private final LocalDate date;
        public DayColumn(LocalDate date) { this.date = date; }
        public String getIso() { return date.toString(); }
        public String getLabel() { return DateTimeHelper.weekday(date) + " " + date.format(DateTimeHelper.SHORT_DATE); }
        public boolean isToday() { return date.equals(LocalDate.now()); }
    }

    public static class Row {
        private final ShowtimeRoom room;
        private final List<List<Showtime>> cells = new ArrayList<>();
        public Row(ShowtimeRoom room) { this.room = room; }
        public ShowtimeRoom getRoom() { return room; }
        public List<List<Showtime>> getCells() { return cells; }
    }

    public static class MovieStat {
        private final String title;
        private int showtimes;
        private int sold;
        private int seats;
        public MovieStat(String title) { this.title = title; }
        public String getTitle() { return title; }
        public int getShowtimes() { return showtimes; }
        public int getSold() { return sold; }
        public int getSeats() { return seats; }
        public int getOccupancy() { return seats == 0 ? 0 : (int) Math.round(sold * 100.0 / seats); }
        public void add(Showtime s) { showtimes++; sold += s.getSoldCount(); seats += s.getSeatCount(); }
    }

    private final Branch branch;
    private final LocalDate from;
    private final LocalDate to;
    private final List<DayColumn> days = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final List<MovieStat> movieStats = new ArrayList<>();
    private int totalShowtimes;
    private int cancelledShowtimes;
    private int totalSold;
    private int totalSeats;

    public ScheduleView(Branch branch, LocalDate from, LocalDate to) {
        this.branch = branch;
        this.from = from;
        this.to = to;
    }

    public Branch getBranch() { return branch; }
    public String getFromIso() { return from.toString(); }
    public String getFromStr() { return DateTimeHelper.format(from); }
    public String getToStr() { return DateTimeHelper.format(to); }
    public String getPrevIso() { return from.minusDays(7).toString(); }
    public String getNextIso() { return from.plusDays(7).toString(); }
    public List<DayColumn> getDays() { return days; }
    public List<Row> getRows() { return rows; }
    public List<MovieStat> getMovieStats() { return movieStats; }
    public int getTotalShowtimes() { return totalShowtimes; }
    public void setTotalShowtimes(int v) { totalShowtimes = v; }
    public int getCancelledShowtimes() { return cancelledShowtimes; }
    public void setCancelledShowtimes(int v) { cancelledShowtimes = v; }
    public int getTotalSold() { return totalSold; }
    public void setTotalSold(int v) { totalSold = v; }
    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int v) { totalSeats = v; }
    public int getOccupancy() { return totalSeats == 0 ? 0 : (int) Math.round(totalSold * 100.0 / totalSeats); }
}
