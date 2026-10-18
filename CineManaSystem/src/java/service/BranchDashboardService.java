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
import util.FormatUtil;

/**
 * Dựng dữ liệu cho Dashboard Tổng Quan của chi nhánh:
 * doanh thu hôm nay, doanh thu tuần này, tỷ lệ lấp đầy và danh sách suất chiếu hôm nay.
 */
public class BranchDashboardService {

    /** Múi giờ dùng để xác định "hôm nay" và giờ hiện tại (DB chưa lưu múi giờ của chi nhánh). */
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ReportDao reportDao;
    private final BranchReportService reportService;

    public BranchDashboardService(ReportDao reportDao) {
        this.reportDao = reportDao;
        this.reportService = new BranchReportService(reportDao);
    }

    /**
     * Dựng toàn bộ dữ liệu dashboard.
     *
     * @param scope danh sách chi nhánh được xem (null = tất cả)
     * @param now   thời điểm hiện tại
     */
    public BranchDashboard build(List<Integer> scope, LocalDateTime now) {
        LocalDate today = now.toLocalDate();

        BranchDashboard dashboard = new BranchDashboard();
        dashboard.setBranchPill(reportService.scopePill(scope));
        dashboard.setTodayLabel(today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dashboard.setUpdatedAt(now.format(DateTimeFormatter.ofPattern("HH:mm")));

        fillTodayRevenue(dashboard, scope, today);
        fillWeekRevenue(dashboard, scope, today);
        fillShowtimes(dashboard, scope, now);
        return dashboard;
    }

    // ==================== DOANH THU HÔM NAY ====================

    /** Doanh thu hôm nay và so sánh phần trăm với hôm qua. */
    private void fillTodayRevenue(BranchDashboard dashboard, List<Integer> scope, LocalDate today) {
        BigDecimal todayTotal = totalRevenue(scope, today, today);
        BigDecimal yesterdayTotal = totalRevenue(scope, today.minusDays(1), today.minusDays(1));

        dashboard.setTodayRevenue(FormatUtil.vnd(todayTotal));

        if (yesterdayTotal.signum() > 0) {
            // Hôm qua có doanh thu: tính phần trăm tăng giảm
            double change = (todayTotal.doubleValue() - yesterdayTotal.doubleValue()) * 100.0
                    / yesterdayTotal.doubleValue();
            long rounded = Math.round(change);
            dashboard.setChangeText((rounded >= 0 ? "+" : "") + rounded + "% so với hôm qua");
            dashboard.setChangeCss(rounded > 0 ? "up" : (rounded < 0 ? "down" : "flat"));
        } else if (todayTotal.signum() > 0) {
            dashboard.setChangeText("Hôm qua chưa có doanh thu");
            dashboard.setChangeCss("up");
        } else {
            dashboard.setChangeText("Chưa có doanh thu hôm nay và hôm qua");
            dashboard.setChangeCss("flat");
        }
    }

    // ==================== DOANH THU TUẦN NÀY ====================

    /** Doanh thu từ thứ Hai đến hôm nay, kèm tỷ trọng vé và F&B. */
    private void fillWeekRevenue(BranchDashboard dashboard, List<Integer> scope, LocalDate today) {
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        BigDecimal[] week = revenue(scope, weekStart, today); // [0] = vé, [1] = F&B
        BigDecimal weekTotal = week[0].add(week[1]);

        dashboard.setWeekRevenue(FormatUtil.vnd(weekTotal));

        if (weekTotal.signum() > 0) {
            long ticketPercent = Math.round(week[0].doubleValue() * 100.0 / weekTotal.doubleValue());
            dashboard.setWeekShareText("Tỷ trọng: " + ticketPercent + "% vé + " + (100 - ticketPercent) + "% F&B Combo");
        } else {
            dashboard.setWeekShareText("Chưa có doanh thu trong tuần này");
        }
    }

    // ==================== SUẤT CHIẾU HÔM NAY ====================

    /** Danh sách suất chiếu hôm nay và tỷ lệ lấp đầy chung. */
    private void fillShowtimes(BranchDashboard dashboard, List<Integer> scope, LocalDateTime now) {
        List<ReportDao.TodayShowtime> shows = reportDao.getShowtimesOnDate(scope, now.toLocalDate().toString());

        // Xem nhiều chi nhánh thì phải hiện tên chi nhánh cạnh tên phòng
        boolean showBranchName = scope == null || scope.size() != 1;

        long soldAll = 0;
        long totalAll = 0;
        List<DashboardShowtime> rows = new ArrayList<>();
        List<String> roomOptions = new ArrayList<>();

        for (ReportDao.TodayShowtime show : shows) {
            soldAll += show.soldSeats;
            totalAll += show.totalSeats;

            DashboardShowtime row = toRow(show, now.toLocalTime(), showBranchName);
            rows.add(row);
            if (!roomOptions.contains(row.getRoomKey())) {
                roomOptions.add(row.getRoomKey());
            }
        }
        dashboard.setShowtimes(rows);
        dashboard.setRoomOptions(roomOptions);

        // Tỷ lệ lấp đầy chung của tất cả suất chiếu hôm nay
        double occupancy = totalAll > 0 ? soldAll * 100.0 / totalAll : 0;
        dashboard.setOccupancyText(FormatUtil.pct(occupancy));
        dashboard.setGaugeWidth(String.format(Locale.US, "%.1f", occupancy));
        dashboard.setOccupancyCss(FormatUtil.occCss(occupancy));
        dashboard.setSeatsText("Đã bán: " + FormatUtil.num(soldAll) + " / " + FormatUtil.num(totalAll)
                + " ghế (các suất hôm nay)");
        dashboard.setRoomsText("Phòng chiếu hoạt động: " + reportDao.countActiveRooms(scope));
    }

    /** Chuyển một suất chiếu từ database thành một dòng hiển thị trên dashboard. */
    private DashboardShowtime toRow(ReportDao.TodayShowtime show, LocalTime nowTime, boolean showBranchName) {
        DashboardShowtime row = new DashboardShowtime();
        LocalTime start = show.startTime.toLocalTime();
        LocalTime end = show.endTime.toLocalTime();
        double occupancy = show.totalSeats > 0 ? show.soldSeats * 100.0 / show.totalSeats : 0;

        row.setTimeLabel(start.format(DateTimeFormatter.ofPattern("HH:mm")));
        row.setMovieTitle(show.movieTitle.toUpperCase(Locale.ROOT));
        row.setDurationText("Thời lượng: " + show.duration + " Phút");
        row.setRoomLabel(show.roomName + " (" + show.roomType + ")");
        row.setBranchName(showBranchName ? show.branchName : "");
        row.setRoomKey(showBranchName ? show.branchName + " - " + show.roomName : show.roomName);

        // Định dạng chiếu theo loại phòng
        if ("IMAX".equals(show.roomType)) {
            row.setFormatLabel("IMAX");
            row.setFormatCss("fmt-imax");
        } else if ("3D".equals(show.roomType)) {
            row.setFormatLabel("3D");
            row.setFormatCss("fmt-3d");
        } else {
            row.setFormatLabel("2D Standard");
            row.setFormatCss("fmt-2d");
        }

        row.setSeatsText(FormatUtil.num(show.soldSeats) + " / " + FormatUtil.num(show.totalSeats));
        row.setOccupancyText(FormatUtil.pct(occupancy));
        row.setOccupancyCss(FormatUtil.occCss(occupancy));
        row.setRevenueText(FormatUtil.vnd(show.ticketRevenue));

        // Trạng thái suất chiếu theo giờ hiện tại.
        // Suất kết thúc sau nửa đêm (giờ kết thúc nhỏ hơn giờ bắt đầu) được coi là đang chiếu đến hết ngày.
        if (nowTime.isBefore(start)) {
            row.setStatusKey("upcoming");
            row.setStatusLabel("Upcoming (Sắp chiếu)");
        } else if (end.isBefore(start) || !nowTime.isAfter(end)) {
            row.setStatusKey("ongoing");
            row.setStatusLabel("On-going (Đang chiếu)");
        } else {
            row.setStatusKey("closed");
            row.setStatusLabel("Closed (Đã kết thúc)");
        }
        return row;
    }

    // ==================== HÀM HỖ TRỢ ====================

    /** Doanh thu trong khoảng ngày chiếu [from, to]. Trả về mảng: [0] = doanh thu vé, [1] = doanh thu F&B. */
    private BigDecimal[] revenue(List<Integer> scope, LocalDate from, LocalDate to) {
        BigDecimal ticket = BigDecimal.ZERO;
        BigDecimal fnb = BigDecimal.ZERO;
        for (Object[] row : reportDao.getDailyRevenueSplit(scope, from.toString(), to.toString())) {
            ticket = ticket.add((BigDecimal) row[1]);
            fnb = fnb.add((BigDecimal) row[2]);
        }
        return new BigDecimal[]{ticket, fnb};
    }

    /** Tổng doanh thu (vé + F&B) trong khoảng ngày. */
    private BigDecimal totalRevenue(List<Integer> scope, LocalDate from, LocalDate to) {
        BigDecimal[] r = revenue(scope, from, to);
        return r[0].add(r[1]);
    }

    // ==================== DỮ LIỆU HIỂN THỊ (class lồng) ====================

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
