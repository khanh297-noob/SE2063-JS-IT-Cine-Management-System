package service;

import dao.ReportDao;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Dựng dữ liệu cho Dashboard Tổng Quan của chi nhánh (doanh thu hôm nay / tuần này, lấp đầy, suất chiếu hôm nay). */
public class BranchDashboardService {

    /** Múi giờ dùng để xác định "hôm nay" và giờ hiện tại (DB chưa lưu múi giờ chi nhánh). */
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ReportDao reportDao;
    private final BranchReportService reportService;

    public BranchDashboardService(ReportDao reportDao) {
        this.reportDao = reportDao;
        this.reportService = new BranchReportService(reportDao);
    }

    /** @return [0] = doanh thu vé, [1] = doanh thu F&B trong khoảng ngày chiếu [from, to] */
    private BigDecimal[] revenue(List<Integer> scope, LocalDate from, LocalDate to) {
        BigDecimal ticket = BigDecimal.ZERO;
        BigDecimal fnb = BigDecimal.ZERO;
        for (Object[] row : reportDao.getDailyRevenueSplit(scope, from.toString(), to.toString())) {
            ticket = ticket.add((BigDecimal) row[1]);
            fnb = fnb.add((BigDecimal) row[2]);
        }
        return new BigDecimal[]{ticket, fnb};
    }

    public BranchDashboard build(List<Integer> scope, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDate yesterday = today.minusDays(1);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        BranchDashboard d = new BranchDashboard();
        d.setBranchPill(reportService.scopePill(scope));
        d.setTodayLabel(today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        d.setUpdatedAt(now.format(DateTimeFormatter.ofPattern("HH:mm")));

        // Doanh thu hôm nay và so sánh với hôm qua
        BigDecimal[] t = revenue(scope, today, today);
        BigDecimal[] y = revenue(scope, yesterday, yesterday);
        BigDecimal todayTotal = t[0].add(t[1]);
        BigDecimal yesterdayTotal = y[0].add(y[1]);
        d.setTodayRevenue(BranchReportService.vnd(todayTotal));
        if (yesterdayTotal.signum() > 0) {
            double change = (todayTotal.doubleValue() - yesterdayTotal.doubleValue()) * 100.0 / yesterdayTotal.doubleValue();
            long rounded = Math.round(change);
            d.setChangeText((rounded >= 0 ? "+" : "") + rounded + "% so với hôm qua");
            d.setChangeCss(rounded > 0 ? "up" : (rounded < 0 ? "down" : "flat"));
        } else if (todayTotal.signum() > 0) {
            d.setChangeText("Hôm qua chưa có doanh thu");
            d.setChangeCss("up");
        } else {
            d.setChangeText("Chưa có doanh thu hôm nay và hôm qua");
            d.setChangeCss("flat");
        }

        // Doanh thu tuần này (từ thứ Hai đến hôm nay)
        BigDecimal[] w = revenue(scope, weekStart, today);
        BigDecimal weekTotal = w[0].add(w[1]);
        d.setWeekRevenue(BranchReportService.vnd(weekTotal));
        if (weekTotal.signum() > 0) {
            long ticketPct = Math.round(w[0].doubleValue() * 100.0 / weekTotal.doubleValue());
            d.setWeekShareText("Tỷ trọng: " + ticketPct + "% vé + " + (100 - ticketPct) + "% F&B Combo");
        } else {
            d.setWeekShareText("Chưa có doanh thu trong tuần này");
        }

        // Suất chiếu hôm nay
        List<ReportDao.TodayShowtime> shows = reportDao.getShowtimesOnDate(scope, today.toString());
        boolean multiBranch = scope == null || scope.size() != 1;
        LocalTime nowTime = now.toLocalTime();
        long sold = 0;
        long total = 0;
        List<DashboardShowtime> rows = new ArrayList<>();
        List<String> roomOptions = new ArrayList<>();

        for (ReportDao.TodayShowtime s : shows) {
            sold += s.soldSeats;
            total += s.totalSeats;
            double occ = s.totalSeats > 0 ? s.soldSeats * 100.0 / s.totalSeats : 0;

            DashboardShowtime r = new DashboardShowtime();
            LocalTime start = s.startTime.toLocalTime();
            LocalTime end = s.endTime.toLocalTime();
            r.setTimeLabel(start.format(DateTimeFormatter.ofPattern("HH:mm")));
            r.setMovieTitle(s.movieTitle.toUpperCase(Locale.ROOT));
            r.setDurationText("Thời lượng: " + s.duration + " Phút");
            r.setRoomLabel(s.roomName + " (" + s.roomType + ")");
            r.setBranchName(multiBranch ? s.branchName : "");
            String key = multiBranch ? s.branchName + " - " + s.roomName : s.roomName;
            r.setRoomKey(key);
            if (!roomOptions.contains(key)) {
                roomOptions.add(key);
            }
            if ("IMAX".equals(s.roomType)) {
                r.setFormatLabel("IMAX");
                r.setFormatCss("fmt-imax");
            } else if ("3D".equals(s.roomType)) {
                r.setFormatLabel("3D");
                r.setFormatCss("fmt-3d");
            } else {
                r.setFormatLabel("2D Standard");
                r.setFormatCss("fmt-2d");
            }
            r.setSeatsText(BranchReportService.num(s.soldSeats) + " / " + BranchReportService.num(s.totalSeats));
            r.setOccupancyText(BranchReportService.pct(occ));
            r.setOccupancyCss(BranchReportService.occCss(occ));
            r.setRevenueText(BranchReportService.vnd(s.ticketRevenue));

            // Suất kết thúc sau nửa đêm (giờ kết thúc nhỏ hơn giờ bắt đầu) được coi là đang chiếu cho đến hết ngày
            if (nowTime.isBefore(start)) {
                r.setStatusKey("upcoming");
                r.setStatusLabel("Upcoming (Sắp chiếu)");
            } else if (end.isBefore(start) || !nowTime.isAfter(end)) {
                r.setStatusKey("ongoing");
                r.setStatusLabel("On-going (Đang chiếu)");
            } else {
                r.setStatusKey("closed");
                r.setStatusLabel("Closed (Đã kết thúc)");
            }
            rows.add(r);
        }
        d.setShowtimes(rows);
        d.setRoomOptions(roomOptions);

        double occAll = total > 0 ? sold * 100.0 / total : 0;
        d.setOccupancyText(BranchReportService.pct(occAll));
        d.setGaugeWidth(String.format(Locale.US, "%.1f", occAll));
        d.setOccupancyCss(BranchReportService.occCss(occAll));
        d.setSeatsText("Đã bán: " + BranchReportService.num(sold) + " / " + BranchReportService.num(total) + " ghế (các suất hôm nay)");
        d.setRoomsText("Phòng chiếu hoạt động: " + reportDao.countActiveRooms(scope));
        return d;
    }

    // ===== Nested data classes (previously package dto) =====

    /** Dữ liệu trang Dashboard Tổng Quan của chi nhánh (đã định dạng sẵn để hiển thị). */
    public static class BranchDashboard {

        private String branchPill;
        private String todayLabel;
        private String updatedAt;

        private String todayRevenue;
        private String changeText;
        /** "up", "down" hoặc "flat". */
        private String changeCss = "flat";

        private String weekRevenue;
        private String weekShareText;

        private String occupancyText;
        private String gaugeWidth = "0";
        private String occupancyCss = "bad";
        private String seatsText;
        private String roomsText;

        private List<DashboardShowtime> showtimes = new ArrayList<>();
        private List<String> roomOptions = new ArrayList<>();

        public String getBranchPill() { return branchPill; }
        public void setBranchPill(String v) { this.branchPill = v; }
        public String getTodayLabel() { return todayLabel; }
        public void setTodayLabel(String v) { this.todayLabel = v; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String v) { this.updatedAt = v; }
        public String getTodayRevenue() { return todayRevenue; }
        public void setTodayRevenue(String v) { this.todayRevenue = v; }
        public String getChangeText() { return changeText; }
        public void setChangeText(String v) { this.changeText = v; }
        public String getChangeCss() { return changeCss; }
        public void setChangeCss(String v) { this.changeCss = v; }
        public String getWeekRevenue() { return weekRevenue; }
        public void setWeekRevenue(String v) { this.weekRevenue = v; }
        public String getWeekShareText() { return weekShareText; }
        public void setWeekShareText(String v) { this.weekShareText = v; }
        public String getOccupancyText() { return occupancyText; }
        public void setOccupancyText(String v) { this.occupancyText = v; }
        public String getGaugeWidth() { return gaugeWidth; }
        public void setGaugeWidth(String v) { this.gaugeWidth = v; }
        public String getOccupancyCss() { return occupancyCss; }
        public void setOccupancyCss(String v) { this.occupancyCss = v; }
        public String getSeatsText() { return seatsText; }
        public void setSeatsText(String v) { this.seatsText = v; }
        public String getRoomsText() { return roomsText; }
        public void setRoomsText(String v) { this.roomsText = v; }
        public List<DashboardShowtime> getShowtimes() { return showtimes; }
        public void setShowtimes(List<DashboardShowtime> v) { this.showtimes = v; }
        public List<String> getRoomOptions() { return roomOptions; }
        public void setRoomOptions(List<String> v) { this.roomOptions = v; }
    }

    /** Một dòng trong widget "Suất chiếu hôm nay" (đã định dạng sẵn để hiển thị). */
    public static class DashboardShowtime {

        private String timeLabel;
        private String movieTitle;
        private String durationText;
        private String roomLabel;
        private String roomKey;
        private String branchName;
        private String formatLabel;
        private String formatCss;
        private String seatsText;
        private String occupancyText;
        private String occupancyCss;
        private String revenueText;
        private String statusKey;
        private String statusLabel;

        public String getTimeLabel() { return timeLabel; }
        public void setTimeLabel(String v) { this.timeLabel = v; }
        public String getMovieTitle() { return movieTitle; }
        public void setMovieTitle(String v) { this.movieTitle = v; }
        public String getDurationText() { return durationText; }
        public void setDurationText(String v) { this.durationText = v; }
        public String getRoomLabel() { return roomLabel; }
        public void setRoomLabel(String v) { this.roomLabel = v; }
        public String getRoomKey() { return roomKey; }
        public void setRoomKey(String v) { this.roomKey = v; }
        public String getBranchName() { return branchName; }
        public void setBranchName(String v) { this.branchName = v; }
        public String getFormatLabel() { return formatLabel; }
        public void setFormatLabel(String v) { this.formatLabel = v; }
        public String getFormatCss() { return formatCss; }
        public void setFormatCss(String v) { this.formatCss = v; }
        public String getSeatsText() { return seatsText; }
        public void setSeatsText(String v) { this.seatsText = v; }
        public String getOccupancyText() { return occupancyText; }
        public void setOccupancyText(String v) { this.occupancyText = v; }
        public String getOccupancyCss() { return occupancyCss; }
        public void setOccupancyCss(String v) { this.occupancyCss = v; }
        public String getRevenueText() { return revenueText; }
        public void setRevenueText(String v) { this.revenueText = v; }
        public String getStatusKey() { return statusKey; }
        public void setStatusKey(String v) { this.statusKey = v; }
        public String getStatusLabel() { return statusLabel; }
        public void setStatusLabel(String v) { this.statusLabel = v; }
    }
}
