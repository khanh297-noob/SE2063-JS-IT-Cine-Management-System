package service;

import dao.ReportDao;
import dao.ReportDao.RevenueBreakdownRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Tổng hợp dữ liệu cho trang Revenue Dashboard (doanh thu vé + F&B).
 */
public class RevenueDashboardService {

    private final ReportDao reportDao;

    public RevenueDashboardService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    /**
     * Dựng dữ liệu dashboard.
     *
     * @param branchScope danh sách chi nhánh được xem (null = tất cả)
     */
    public RevenueDashboard build(List<Integer> branchScope, LocalDate from, LocalDate to) {
        String fromText = from.toString();
        String toText = to.toString();

        RevenueDashboard dashboard = new RevenueDashboard();
        dashboard.setFromDate(fromText);
        dashboard.setToDate(toText);
        dashboard.setScopeLabel(scopeLabel(branchScope));

        // Bước 1: biểu đồ doanh thu gom theo tuần (tính từ ngày bắt đầu)
        fillWeeklyChart(dashboard, branchScope, from, to);

        // Bước 2: ba bảng chi tiết (mỗi bảng là một tab)
        List<BreakdownTable> tables = new ArrayList<>();
        tables.add(new BreakdownTable("movie", "Theo Phim (By Movie)", "Tên phim", "Tổng doanh thu phim",
                reportDao.getRevenueBreakdown("movie", branchScope, fromText, toText)));
        tables.add(new BreakdownTable("seat", "Theo Loại Ghế (By Seat Type)", "Loại ghế", "Tổng doanh thu loại ghế",
                reportDao.getRevenueBreakdown("seat", branchScope, fromText, toText)));
        tables.add(new BreakdownTable("payment", "Theo Thanh Toán (By Payment Method)", "Phương thức thanh toán",
                "Tổng doanh thu", reportDao.getRevenueBreakdown("payment", branchScope, fromText, toText)));
        dashboard.setTables(tables);
        return dashboard;
    }

    /** Gom doanh thu vé và F&B theo từng tuần (7 ngày một tuần, tính từ ngày bắt đầu). */
    private void fillWeeklyChart(RevenueDashboard dashboard, List<Integer> branchScope, LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        int weeks = (int) Math.ceil(days / 7.0);

        // Tạo sẵn các tuần với doanh thu bằng 0
        List<String> labels = new ArrayList<>();
        List<BigDecimal> ticketRevenue = new ArrayList<>();
        List<BigDecimal> fnbRevenue = new ArrayList<>();
        for (int i = 0; i < weeks; i++) {
            labels.add("Tuần " + (i + 1));
            ticketRevenue.add(BigDecimal.ZERO);
            fnbRevenue.add(BigDecimal.ZERO);
        }

        // Cộng doanh thu từng ngày vào đúng tuần của ngày đó
        for (Object[] row : reportDao.getDailyRevenueSplit(branchScope, from.toString(), to.toString())) {
            LocalDate day = ((java.sql.Date) row[0]).toLocalDate();
            int weekIndex = (int) (ChronoUnit.DAYS.between(from, day) / 7);
            if (weekIndex >= 0 && weekIndex < weeks) {
                ticketRevenue.set(weekIndex, ticketRevenue.get(weekIndex).add((BigDecimal) row[1]));
                fnbRevenue.set(weekIndex, fnbRevenue.get(weekIndex).add((BigDecimal) row[2]));
            }
        }

        dashboard.setWeekLabels(labels);
        dashboard.setWeeklyTicketRevenue(ticketRevenue);
        dashboard.setWeeklyFnbRevenue(fnbRevenue);
    }

    /** Tên phạm vi chi nhánh để hiển thị. */
    public static String scopeLabel(List<Integer> branchScope) {
        if (branchScope == null) {
            return "Tất cả chi nhánh";
        }
        if (branchScope.size() == 1) {
            return String.format("Branch #%02d Data Only", branchScope.get(0));
        }
        if (branchScope.isEmpty()) {
            return "Chưa được phân quyền chi nhánh";
        }
        return branchScope.size() + " chi nhánh";
    }

    // ==================== DỮ LIỆU HIỂN THỊ (class lồng) ====================

    /** Dữ liệu cho trang Revenue Dashboard (doanh thu vé + F&B). */
    public static class RevenueDashboard {

        private String fromDate;
        private String toDate;
        private String scopeLabel;
        private List<String> weekLabels = new ArrayList<>();
        private List<BigDecimal> weeklyTicketRevenue = new ArrayList<>();
        private List<BigDecimal> weeklyFnbRevenue = new ArrayList<>();
        private List<BreakdownTable> tables = new ArrayList<>();

        public String getFromDate() { return fromDate; }
        public void setFromDate(String fromDate) { this.fromDate = fromDate; }

        public String getToDate() { return toDate; }
        public void setToDate(String toDate) { this.toDate = toDate; }

        public String getScopeLabel() { return scopeLabel; }
        public void setScopeLabel(String scopeLabel) { this.scopeLabel = scopeLabel; }

        public List<String> getWeekLabels() { return weekLabels; }
        public void setWeekLabels(List<String> weekLabels) { this.weekLabels = weekLabels; }

        public List<BigDecimal> getWeeklyTicketRevenue() { return weeklyTicketRevenue; }
        public void setWeeklyTicketRevenue(List<BigDecimal> v) { this.weeklyTicketRevenue = v; }

        public List<BigDecimal> getWeeklyFnbRevenue() { return weeklyFnbRevenue; }
        public void setWeeklyFnbRevenue(List<BigDecimal> v) { this.weeklyFnbRevenue = v; }

        public List<BreakdownTable> getTables() { return tables; }
        public void setTables(List<BreakdownTable> tables) { this.tables = tables; }
    }

    /** Một bảng phân rã (một tab) trong Revenue Dashboard. */
    public static class BreakdownTable {

        private final String key;
        private final String title;
        private final String nameHeader;
        private final String totalHeader;
        private final List<RevenueBreakdownRow> rows;

        public BreakdownTable(String key, String title, String nameHeader, String totalHeader,
                              List<RevenueBreakdownRow> rows) {
            this.key = key;
            this.title = title;
            this.nameHeader = nameHeader;
            this.totalHeader = totalHeader;
            this.rows = rows != null ? rows : new ArrayList<RevenueBreakdownRow>();

            BigDecimal grand = BigDecimal.ZERO;
            for (RevenueBreakdownRow r : this.rows) {
                grand = grand.add(r.getTotalRevenue());
            }
            for (RevenueBreakdownRow r : this.rows) {
                r.setShare(grand.signum() > 0
                        ? r.getTotalRevenue().multiply(BigDecimal.valueOf(100)).doubleValue() / grand.doubleValue()
                        : 0);
            }
        }

        public String getKey() { return key; }
        public String getTitle() { return title; }
        public String getNameHeader() { return nameHeader; }
        public String getTotalHeader() { return totalHeader; }
        public List<RevenueBreakdownRow> getRows() { return rows; }

        public int getTotalTickets() {
            int n = 0;
            for (RevenueBreakdownRow r : rows) n += r.getTickets();
            return n;
        }

        public BigDecimal getTotalTicketRevenue() {
            BigDecimal s = BigDecimal.ZERO;
            for (RevenueBreakdownRow r : rows) s = s.add(r.getTicketRevenue());
            return s;
        }

        public BigDecimal getTotalFnbRevenue() {
            BigDecimal s = BigDecimal.ZERO;
            for (RevenueBreakdownRow r : rows) s = s.add(r.getFnbRevenue());
            return s;
        }

        public BigDecimal getGrandTotal() {
            return getTotalTicketRevenue().add(getTotalFnbRevenue());
        }
    }
}
