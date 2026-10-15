package service;

import dao.ReportDao;
import dao.ReportDao.RevenueBreakdownRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Tổng hợp dữ liệu Revenue Dashboard (doanh thu vé + F&B). */
public class RevenueDashboardService {

    /** Khoảng thời gian tối đa cho một báo cáo (ngày). */
    public static final int MAX_RANGE_DAYS = 366;

    private final ReportDao reportDao;

    public RevenueDashboardService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    /** Trả về null nếu chuỗi rỗng hoặc sai định dạng yyyy-MM-dd. */
    public static LocalDate parseDate(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** @param branchScope null = tất cả chi nhánh */
    public RevenueDashboard build(List<Integer> branchScope, LocalDate from, LocalDate to) {
        String f = from.toString();
        String t = to.toString();

        RevenueDashboard d = new RevenueDashboard();
        d.setFromDate(f);
        d.setToDate(t);
        d.setScopeLabel(scopeLabel(branchScope));

        // Biểu đồ: gom theo tuần tính từ ngày bắt đầu
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        int weeks = (int) Math.ceil(days / 7.0);
        BigDecimal[] ticket = new BigDecimal[weeks];
        BigDecimal[] fnb = new BigDecimal[weeks];
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < weeks; i++) {
            ticket[i] = BigDecimal.ZERO;
            fnb[i] = BigDecimal.ZERO;
            labels.add("Tuần " + (i + 1));
        }
        for (Object[] row : reportDao.getDailyRevenueSplit(branchScope, f, t)) {
            LocalDate day = ((java.sql.Date) row[0]).toLocalDate();
            int idx = (int) (ChronoUnit.DAYS.between(from, day) / 7);
            if (idx >= 0 && idx < weeks) {
                ticket[idx] = ticket[idx].add((BigDecimal) row[1]);
                fnb[idx] = fnb[idx].add((BigDecimal) row[2]);
            }
        }
        List<BigDecimal> ticketList = new ArrayList<>();
        List<BigDecimal> fnbList = new ArrayList<>();
        for (int i = 0; i < weeks; i++) {
            ticketList.add(ticket[i]);
            fnbList.add(fnb[i]);
        }
        d.setWeekLabels(labels);
        d.setWeeklyTicketRevenue(ticketList);
        d.setWeeklyFnbRevenue(fnbList);

        // Bảng phân rã
        List<BreakdownTable> tables = new ArrayList<>();
        tables.add(new BreakdownTable("movie", "Theo Phim (By Movie)", "Tên phim", "Tổng doanh thu phim",
                reportDao.getRevenueBreakdown("movie", branchScope, f, t)));
        tables.add(new BreakdownTable("seat", "Theo Loại Ghế (By Seat Type)", "Loại ghế", "Tổng doanh thu loại ghế",
                reportDao.getRevenueBreakdown("seat", branchScope, f, t)));
        tables.add(new BreakdownTable("payment", "Theo Thanh Toán (By Payment Method)", "Phương thức thanh toán",
                "Tổng doanh thu", reportDao.getRevenueBreakdown("payment", branchScope, f, t)));
        d.setTables(tables);
        return d;
    }

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

    // ===== Nested data classes (previously package dto) =====

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
