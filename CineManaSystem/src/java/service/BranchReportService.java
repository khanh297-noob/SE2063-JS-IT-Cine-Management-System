package service;

import dao.ReportDao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import model.Branch;
import util.FormatUtil;
import dao.ReportDao.OccupancyRow;
import dao.ReportDao.PeakRow;
import dao.ReportDao.RevenueBreakdownRow;

/**
 * Dựng dữ liệu cho trang Báo Cáo Rạp (Branch Report Dashboard).
 * Loại báo cáo: revenue, by-branch, by-movie, peak, occupancy.
 */
public class BranchReportService {

    public static final String DEFAULT_TYPE = "revenue";

    private static final String RED = "#e11d48";
    private static final String ORANGE = "#f59e0b";
    private static final String PINK = "#fda4af";

    private static final String[] WEEKDAYS = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"};

    private final ReportDao reportDao;

    public BranchReportService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public static Map<String, String> typeLabels() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("revenue", "Báo cáo Doanh Thu (Revenue)");
        m.put("by-branch", "Doanh Thu Theo Chi Nhánh (Revenue By Branch)");
        m.put("by-movie", "Doanh Thu Theo Phim (Revenue By Movie)");
        m.put("peak", "Khung Giờ Cao Điểm Đặt Vé (Peak Booking Period)");
        m.put("occupancy", "Tỷ Lệ Lấp Đầy Ghế (Seat Vacancy Rate)");
        return m;
    }

    public static String normalizeType(String type) {
        return type != null && typeLabels().containsKey(type) ? type : DEFAULT_TYPE;
    }

    /** Một dòng trong khung "Tổng quan": {nhãn, giá trị, kiểu hiển thị}. */
    private static String[] line(String label, String value, String css) {
        return new String[]{label, value, css};
    }

    /** Tạo danh sách chuỗi từ các phần tử truyền vào. */
    private static List<String> list(String... items) {
        List<String> l = new ArrayList<>();
        Collections.addAll(l, items);
        return l;
    }

    /** Tạo một dòng của bảng từ các ô truyền vào. */
    private static List<String[]> row(String[]... cells) {
        List<String[]> r = new ArrayList<>();
        Collections.addAll(r, cells);
        return r;
    }

    /** Tạo một dữ liệu (một đường/một cột màu) cho biểu đồ Chart.js. */
    private static Map<String, Object> dataset(String label, List<? extends Number> data, Object color) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("data", data);
        m.put("backgroundColor", color);
        return m;
    }

    // ===== Dựng báo cáo =====
    public BranchReportView build(String type, List<Integer> scope, LocalDate from, LocalDate to) {
        type = normalizeType(type);
        String f = from.toString();
        String t = to.toString();

        BranchReportView v = new BranchReportView();
        v.setType(type);
        v.setTypeLabel(typeLabels().get(type));
        v.setFromDate(f);
        v.setToDate(t);
        v.setScopeLabel(RevenueDashboardService.scopeLabel(scope));
        applyScopeTags(v, scope);

        if ("by-branch".equals(type)) {
            buildRevenueByDimension(v, "branch", scope, f, t);
        } else if ("by-movie".equals(type)) {
            buildRevenueByDimension(v, "movie", scope, f, t);
        } else if ("peak".equals(type)) {
            buildPeak(v, scope, f, t);
        } else if ("occupancy".equals(type)) {
            buildOccupancy(v, scope, from, to);
        } else {
            buildRevenue(v, scope, from, to);
        }
        return v;
    }

    /** Nhãn chi nhánh hiển thị trên thanh điều hướng, ví dụ "CineGo Cầu Giấy (Branch #01)". */
    public String scopePill(List<Integer> scope) {
        BranchReportView tmp = new BranchReportView();
        applyScopeTags(tmp, scope);
        return tmp.getBranchPill();
    }

    /** Ghi tên phạm vi chi nhánh (branchPill, dataTag) vào báo cáo. */
    private void applyScopeTags(BranchReportView v, List<Integer> scope) {
        if (scope != null && scope.size() == 1) {
            String name = "";
            List<Branch> bs = reportDao.getBranches(scope);
            if (!bs.isEmpty()) {
                name = bs.get(0).getName() + " ";
            }
            String id = String.format("Branch #%02d", scope.get(0));
            v.setBranchPill(name + "(" + id + ")");
            v.setDataTag("Dữ liệu nội bộ " + id);
        } else if (scope == null) {
            v.setBranchPill("Tất cả chi nhánh");
            v.setDataTag("Dữ liệu toàn hệ thống");
        } else if (scope.isEmpty()) {
            v.setBranchPill("Chưa được phân quyền chi nhánh");
            v.setDataTag("Không có dữ liệu");
        } else {
            v.setBranchPill(scope.size() + " chi nhánh");
            v.setDataTag("Dữ liệu " + scope.size() + " chi nhánh");
        }
    }

    // ----- Dòng gộp: thống kê suất chiếu + doanh thu của một phim/chi nhánh -----
    private static class Merged {
        int id;
        String name;
        int shows;
        int sold;
        int totalSeats;
        BigDecimal ticketRev = BigDecimal.ZERO;
        BigDecimal fnbRev = BigDecimal.ZERO;
        int tickets;

        BigDecimal total() { return ticketRev.add(fnbRev); }
        double occ() { return totalSeats > 0 ? sold * 100.0 / totalSeats : 0; }
    }

    /** Ghép số liệu suất chiếu/ghế với doanh thu theo cùng một nhóm (phim hoặc chi nhánh), sắp xếp theo tổng doanh thu giảm dần. */
    private List<Merged> merged(String dim, List<Integer> scope, String f, String t) {
        List<OccupancyRow> stats = reportDao.getShowtimeStats(dim, scope, f, t);
        Map<Integer, RevenueBreakdownRow> revById = new HashMap<>();
        for (RevenueBreakdownRow r : reportDao.getRevenueBreakdown(dim, scope, f, t)) {
            revById.put(r.getId(), r);
        }
        List<Merged> out = new ArrayList<>();
        for (OccupancyRow s : stats) {
            Merged m = new Merged();
            m.id = s.getId();
            m.name = s.getName();
            m.shows = s.getShows();
            m.sold = s.getSoldSeats();
            m.totalSeats = s.getTotalSeats();
            RevenueBreakdownRow r = revById.get(s.getId());
            if (r != null) {
                m.ticketRev = r.getTicketRevenue();
                m.fnbRev = r.getFnbRevenue();
                m.tickets = r.getTickets();
            }
            out.add(m);
        }
        Collections.sort(out, new Comparator<Merged>() {
            @Override
            public int compare(Merged a, Merged b) {
                int c = b.total().compareTo(a.total());
                return c != 0 ? c : Integer.compare(b.sold, a.sold);
            }
        });
        return out;
    }

    /** Điền khung "Tổng quan": tổng doanh thu vé, F&B, tỷ lệ lấp đầy và tổng doanh thu. */
    private void fillRevenueOverview(BranchReportView v, List<Merged> rows) {
        BigDecimal ticket = BigDecimal.ZERO;
        BigDecimal fnb = BigDecimal.ZERO;
        int sold = 0;
        int seats = 0;
        for (Merged m : rows) {
            ticket = ticket.add(m.ticketRev);
            fnb = fnb.add(m.fnbRev);
            sold += m.sold;
            seats += m.totalSeats;
        }
        v.getOverview().add(line("Tổng doanh thu vé:", FormatUtil.vnd(ticket), "strong"));
        v.getOverview().add(line("Tổng doanh thu F&B:", FormatUtil.vnd(fnb), "strong"));
        v.getOverview().add(line("Tỷ lệ lấp đầy ghế:", FormatUtil.pct(seats > 0 ? sold * 100.0 / seats : 0), "strong"));
        v.getOverview().add(line("Tổng doanh thu Rạp:", FormatUtil.vnd(ticket.add(fnb)), "total"));
    }

    /** Điền khung bên cạnh: top 3 dòng đầu của danh sách đã sắp xếp. */
    private void fillTopSide(BranchReportView v, String title, List<Merged> rows) {
        v.setSideTitle(title);
        for (int i = 0; i < rows.size() && i < 3; i++) {
            Merged m = rows.get(i);
            v.getSideItems().add(new String[]{(i + 1) + ". " + m.name.toUpperCase(Locale.ROOT),
                FormatUtil.million(m.total()) + " (" + Math.round(m.occ()) + "% Seat)"});
        }
    }

    /** Tạo bảng chi tiết doanh thu. ranked = true thì thêm cột Hạng và Tỷ trọng. */
    private ReportTable revenueTable(String title, String nameHeader, List<Merged> rows, boolean ranked) {
        List<String> headers = new ArrayList<>();
        List<String> aligns = new ArrayList<>();
        if (ranked) {
            headers.add("Hạng");
            aligns.add("center");
        }
        headers.addAll(list(nameHeader, "Số suất chiếu", "Số vé đã bán", "Tỷ lệ lấp đầy (%)",
                "Doanh thu vé", "Doanh thu F&B", "Tổng doanh thu"));
        aligns.addAll(list("left", "center", "center", "center", "right", "right", "right"));
        if (ranked) {
            headers.add("Tỷ trọng (%)");
            aligns.add("right");
        }
        ReportTable table = new ReportTable(title, headers, aligns);

        BigDecimal grand = BigDecimal.ZERO;
        for (Merged m : rows) {
            grand = grand.add(m.total());
        }
        int rank = 1;
        for (Merged m : rows) {
            List<String[]> r = new ArrayList<>();
            if (ranked) {
                r.add(ReportTable.cell(String.valueOf(rank++), "bold"));
            }
            r.add(ReportTable.cell(m.name.toUpperCase(Locale.ROOT), "bold", m.name));
            r.add(ReportTable.cell(FormatUtil.num(m.shows) + " Suất", "bold", String.valueOf(m.shows)));
            r.add(ReportTable.cell(FormatUtil.num(m.tickets) + " Vé", "bold", String.valueOf(m.tickets)));
            r.add(ReportTable.cell(FormatUtil.pct(m.occ()), FormatUtil.occCss(m.occ()) + " bold", FormatUtil.decimal1(m.occ())));
            r.add(ReportTable.cell(FormatUtil.vnd(m.ticketRev), "", m.ticketRev.toPlainString()));
            r.add(ReportTable.cell(FormatUtil.vnd(m.fnbRev), "", m.fnbRev.toPlainString()));
            r.add(ReportTable.cell(FormatUtil.vnd(m.total()), "total", m.total().toPlainString()));
            if (ranked) {
                double share = grand.signum() > 0 ? m.total().doubleValue() * 100.0 / grand.doubleValue() : 0;
                r.add(ReportTable.cell(FormatUtil.pct(share), "share", FormatUtil.decimal1(share)));
            }
            table.addRow(r);
        }
        return table;
    }

    // ----- 1. Báo cáo doanh thu (mặc định) -----
    private void buildRevenue(BranchReportView v, List<Integer> scope, LocalDate from, LocalDate to) {
        String f = from.toString();
        String t = to.toString();

        BigDecimal[][] weekly = weeklyRevenue(reportDao.getDailyRevenueSplit(scope, f, t), from, to);
        List<String> labels = new ArrayList<>();
        List<BigDecimal> totals = new ArrayList<>();
        for (int i = 0; i < weekly[0].length; i++) {
            labels.add("Tuần " + (i + 1));
            totals.add(weekly[0][i].add(weekly[1][i]));
        }
        v.setChartTitle("Biểu Đồ Doanh Thu Theo Tuần (" + periodLabel(from, to) + ")");
        v.setChartUnit("VND");
        v.setChartLabels(labels);
        v.getChartDatasets().add(dataset("Doanh Thu (VND)", totals, RED));

        List<Merged> rows = merged("movie", scope, f, t);
        fillRevenueOverview(v, rows);
        fillTopSide(v, "Hiệu Suất Phim Hàng Đầu", rows);

        ReportTable table = revenueTable("Bảng chi tiết doanh thu theo phim", "Tên phim / Suất chiếu", rows, false);
        table.setTag(v.getScopeLabel());
        v.getTables().add(table);
        v.setNote("Doanh thu tính theo ngày chiếu; chỉ gồm vé còn hiệu lực của booking đã xác nhận và thanh toán thành công. "
                + "Doanh thu F&B của mỗi booking được chia đều cho các vé của booking đó.");
    }

    // ----- 2/3. Doanh thu theo chi nhánh / theo phim -----
    private void buildRevenueByDimension(BranchReportView v, String dim, List<Integer> scope, String f, String t) {
        boolean branch = "branch".equals(dim);
        List<Merged> rows = merged(dim, scope, f, t);

        List<String> labels = new ArrayList<>();
        List<BigDecimal> ticket = new ArrayList<>();
        List<BigDecimal> fnb = new ArrayList<>();
        for (int i = 0; i < rows.size() && i < 10; i++) {
            labels.add(rows.get(i).name);
            ticket.add(rows.get(i).ticketRev);
            fnb.add(rows.get(i).fnbRev);
        }
        v.setChartTitle(branch ? "Doanh Thu Theo Chi Nhánh (Vé + F&B)" : "Doanh Thu Theo Phim (Top 10, Vé + F&B)");
        v.setChartUnit("VND");
        v.setChartStacked(true);
        v.setChartLabels(labels);
        v.getChartDatasets().add(dataset("Doanh thu vé", ticket, RED));
        v.getChartDatasets().add(dataset("Doanh thu F&B", fnb, ORANGE));

        fillRevenueOverview(v, rows);
        fillTopSide(v, branch ? "Chi Nhánh Dẫn Đầu" : "Hiệu Suất Phim Hàng Đầu", rows);

        ReportTable table = revenueTable(
                branch ? "Bảng chi tiết doanh thu theo chi nhánh" : "Bảng xếp hạng doanh thu theo phim",
                branch ? "Chi nhánh" : "Tên phim", rows, true);
        table.setTag(v.getScopeLabel());
        v.getTables().add(table);
        v.setNote("Doanh thu tính theo ngày chiếu trong khoảng đã chọn. Tỷ lệ lấp đầy = ghế đã bán / tổng ghế của các suất chiếu.");
    }

    // ----- 4. Khung giờ cao điểm đặt vé -----
    private void buildPeak(BranchReportView v, List<Integer> scope, String f, String t) {
        PeakRow[] hours = new PeakRow[24];
        for (int i = 0; i < 24; i++) hours[i] = new PeakRow(i);
        for (PeakRow r : reportDao.getPeakBooking("hour", scope, f, t)) {
            if (r.getSlot() >= 0 && r.getSlot() < 24) hours[r.getSlot()] = r;
        }
        PeakRow[] days = new PeakRow[7];
        for (int i = 0; i < 7; i++) days[i] = new PeakRow(i + 1);
        for (PeakRow r : reportDao.getPeakBooking("weekday", scope, f, t)) {
            if (r.getSlot() >= 1 && r.getSlot() <= 7) days[r.getSlot() - 1] = r;
        }

        // Xếp hạng khung giờ theo số lượt đặt
        List<PeakRow> ranked = new ArrayList<>();
        int totalBookings = 0;
        int totalTickets = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (PeakRow r : hours) {
            totalBookings += r.getBookings();
            totalTickets += r.getTickets();
            totalRevenue = totalRevenue.add(r.getRevenue());
            if (r.getBookings() > 0) ranked.add(r);
        }
        Collections.sort(ranked, new Comparator<PeakRow>() {
            @Override
            public int compare(PeakRow a, PeakRow b) {
                int c = Integer.compare(b.getBookings(), a.getBookings());
                return c != 0 ? c : Integer.compare(a.getSlot(), b.getSlot());
            }
        });

        List<String> labels = new ArrayList<>();
        List<Integer> data = new ArrayList<>();
        List<String> colors = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            labels.add(String.format("%02dh", i));
            data.add(hours[i].getBookings());
            boolean top = false;
            for (int k = 0; k < ranked.size() && k < 3; k++) {
                if (ranked.get(k).getSlot() == i) top = true;
            }
            colors.add(top ? RED : PINK);
        }
        v.setChartTitle("Lượt Đặt Vé Theo Khung Giờ (3 khung giờ cao điểm nhất được tô đậm)");
        v.setChartUnit("lượt đặt");
        v.setChartLabels(labels);
        v.getChartDatasets().add(dataset("Lượt đặt vé", data, colors));

        v.getOverview().add(line("Tổng lượt đặt vé:", FormatUtil.num(totalBookings), "strong"));
        v.getOverview().add(line("Tổng số vé:", FormatUtil.num(totalTickets), "strong"));
        v.getOverview().add(line("Doanh thu vé:", FormatUtil.vnd(totalRevenue), "strong"));
        v.getOverview().add(line("Khung giờ cao điểm nhất:",
                ranked.isEmpty() ? "Chưa có dữ liệu" : slotLabel(ranked.get(0).getSlot()), "total"));

        v.setSideTitle("Khung Giờ Cao Điểm");
        for (int i = 0; i < ranked.size() && i < 3; i++) {
            PeakRow r = ranked.get(i);
            v.getSideItems().add(new String[]{(i + 1) + ". " + slotLabel(r.getSlot()), FormatUtil.num(r.getBookings()) + " lượt đặt"});
        }

        ReportTable byHour = new ReportTable("Top khung giờ có nhiều lượt đặt vé nhất",
                list("Hạng", "Khung giờ", "Lượt đặt", "Số vé", "Doanh thu vé", "Tỷ trọng lượt đặt"),
                list("center", "left", "center", "center", "right", "right"));
        byHour.setTag(v.getScopeLabel());
        for (int i = 0; i < ranked.size() && i < 10; i++) {
            PeakRow r = ranked.get(i);
            double share = totalBookings > 0 ? r.getBookings() * 100.0 / totalBookings : 0;
            byHour.addRow(row(
                    ReportTable.cell(String.valueOf(i + 1), "bold"),
                    ReportTable.cell(slotLabel(r.getSlot()), "bold"),
                    ReportTable.cell(FormatUtil.num(r.getBookings()), "bold", String.valueOf(r.getBookings())),
                    ReportTable.cell(FormatUtil.num(r.getTickets()), "", String.valueOf(r.getTickets())),
                    ReportTable.cell(FormatUtil.vnd(r.getRevenue()), "", r.getRevenue().toPlainString()),
                    ReportTable.cell(FormatUtil.pct(share), "share", FormatUtil.decimal1(share))));
        }
        v.getTables().add(byHour);

        ReportTable byDay = new ReportTable("Hoạt động đặt vé theo ngày trong tuần",
                list("Ngày trong tuần", "Lượt đặt", "Số vé", "Doanh thu vé", "Tỷ trọng lượt đặt"),
                list("left", "center", "center", "right", "right"));
        for (PeakRow r : days) {
            double share = totalBookings > 0 ? r.getBookings() * 100.0 / totalBookings : 0;
            byDay.addRow(row(
                    ReportTable.cell(WEEKDAYS[r.getSlot() - 1], "bold"),
                    ReportTable.cell(FormatUtil.num(r.getBookings()), "bold", String.valueOf(r.getBookings())),
                    ReportTable.cell(FormatUtil.num(r.getTickets()), "", String.valueOf(r.getTickets())),
                    ReportTable.cell(FormatUtil.vnd(r.getRevenue()), "", r.getRevenue().toPlainString()),
                    ReportTable.cell(FormatUtil.pct(share), "share", FormatUtil.decimal1(share))));
        }
        v.getTables().add(byDay);

        v.setNote("Khung giờ tính từ thời điểm đặt vé (Booking.BookingDate) trong khoảng ngày đã chọn, chỉ gồm booking đã xác nhận và "
                + "thanh toán thành công. Hệ thống chưa lưu múi giờ cho từng chi nhánh nên giờ được hiểu là giờ địa phương Việt Nam (UTC+7).");
    }

    private static String slotLabel(int hour) {
        return String.format("%02d:00 - %02d:59", hour, hour);
    }

    // ----- 5. Tỷ lệ lấp đầy / ghế trống -----
    private void buildOccupancy(BranchReportView v, List<Integer> scope, LocalDate from, LocalDate to) {
        String f = from.toString();
        String t = to.toString();

        // Xu hướng theo tuần
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        int weeks = (int) Math.ceil(days / 7.0);
        long[] sold = new long[weeks];
        long[] total = new long[weeks];
        for (OccupancyRow r : reportDao.getShowtimeStats("day", scope, f, t)) {
            LocalDate d = LocalDate.parse(r.getName());
            int idx = (int) (ChronoUnit.DAYS.between(from, d) / 7);
            if (idx >= 0 && idx < weeks) {
                sold[idx] += r.getSoldSeats();
                total[idx] += r.getTotalSeats();
            }
        }
        List<String> labels = new ArrayList<>();
        List<Double> occ = new ArrayList<>();
        for (int i = 0; i < weeks; i++) {
            labels.add("Tuần " + (i + 1));
            occ.add(total[i] > 0 ? Math.round(sold[i] * 1000.0 / total[i]) / 10.0 : 0.0);
        }
        v.setChartTitle("Tỷ Lệ Lấp Đầy Ghế Theo Tuần (" + periodLabel(from, to) + ")");
        v.setChartUnit("%");
        v.setChartPercent(true);
        v.setChartLabels(labels);
        v.getChartDatasets().add(dataset("Tỷ lệ lấp đầy (%)", occ, RED));

        List<OccupancyRow> rooms = reportDao.getShowtimeStats("room", scope, f, t);
        int shows = 0;
        long soldAll = 0;
        long totalAll = 0;
        for (OccupancyRow r : rooms) {
            shows += r.getShows();
            soldAll += r.getSoldSeats();
            totalAll += r.getTotalSeats();
        }
        double occAll = totalAll > 0 ? soldAll * 100.0 / totalAll : 0;
        v.getOverview().add(line("Tổng số suất chiếu:", FormatUtil.num(shows), "strong"));
        v.getOverview().add(line("Tổng ghế khả dụng:", FormatUtil.num(totalAll), "strong"));
        v.getOverview().add(line("Ghế đã bán:", FormatUtil.num(soldAll), "strong"));
        v.getOverview().add(line("Tỷ lệ lấp đầy:", FormatUtil.pct(occAll), "strong"));
        v.getOverview().add(line("Tỷ lệ ghế trống:", FormatUtil.pct(totalAll > 0 ? 100.0 - occAll : 0), "total"));

        // Top phòng lấp đầy cao nhất
        List<OccupancyRow> rankedRooms = new ArrayList<>();
        for (OccupancyRow r : rooms) {
            if (r.getTotalSeats() > 0) rankedRooms.add(r);
        }
        Collections.sort(rankedRooms, new Comparator<OccupancyRow>() {
            @Override
            public int compare(OccupancyRow a, OccupancyRow b) {
                return Double.compare(b.getOccupancy(), a.getOccupancy());
            }
        });
        v.setSideTitle("Phòng Chiếu Lấp Đầy Cao Nhất");
        for (int i = 0; i < rankedRooms.size() && i < 3; i++) {
            OccupancyRow r = rankedRooms.get(i);
            v.getSideItems().add(new String[]{(i + 1) + ". " + r.getName().toUpperCase(Locale.ROOT) + " (" + r.getSubName() + ")",
                FormatUtil.pct(r.getOccupancy())});
        }

        ReportTable table = new ReportTable("Bảng chi tiết lấp đầy ghế theo phòng chiếu",
                list("Phòng chiếu", "Chi nhánh / Loại phòng", "Số suất chiếu", "Ghế đã bán", "Tổng ghế khả dụng",
                        "Tỷ lệ lấp đầy (%)", "Tỷ lệ ghế trống (%)"),
                list("left", "left", "center", "center", "center", "center", "center"));
        table.setTag(v.getScopeLabel());
        for (OccupancyRow r : rooms) {
            table.addRow(row(
                    ReportTable.cell(r.getName().toUpperCase(Locale.ROOT), "bold", r.getName()),
                    ReportTable.cell(r.getSubName()),
                    ReportTable.cell(FormatUtil.num(r.getShows()) + " Suất", "bold", String.valueOf(r.getShows())),
                    ReportTable.cell(FormatUtil.num(r.getSoldSeats()), "", String.valueOf(r.getSoldSeats())),
                    ReportTable.cell(FormatUtil.num(r.getTotalSeats()), "", String.valueOf(r.getTotalSeats())),
                    ReportTable.cell(FormatUtil.pct(r.getOccupancy()), FormatUtil.occCss(r.getOccupancy()) + " bold",
                            FormatUtil.decimal1(r.getOccupancy())),
                    ReportTable.cell(FormatUtil.pct(r.getVacancy()), "", FormatUtil.decimal1(r.getVacancy()))));
        }
        v.getTables().add(table);
        v.setNote("Tỷ lệ lấp đầy = ghế đã bán / tổng ghế khả dụng của các suất chiếu trong khoảng ngày chọn (gồm cả suất chưa chiếu). "
                + "Ghế đã bán là vé còn hiệu lực của booking đã xác nhận và thanh toán thành công; tổng ghế lấy theo số ghế thực tế của phòng.");
    }

    // ----- Hàm hỗ trợ -----
    /** @return mảng [0] = doanh thu vé theo tuần, [1] = doanh thu F&B theo tuần */
    private static BigDecimal[][] weeklyRevenue(List<Object[]> daily, LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        int weeks = (int) Math.ceil(days / 7.0);
        BigDecimal[][] out = new BigDecimal[2][weeks];
        for (int i = 0; i < weeks; i++) {
            out[0][i] = BigDecimal.ZERO;
            out[1][i] = BigDecimal.ZERO;
        }
        for (Object[] row : daily) {
            LocalDate day = ((java.sql.Date) row[0]).toLocalDate();
            int idx = (int) (ChronoUnit.DAYS.between(from, day) / 7);
            if (idx >= 0 && idx < weeks) {
                out[0][idx] = out[0][idx].add((BigDecimal) row[1]);
                out[1][idx] = out[1][idx].add((BigDecimal) row[2]);
            }
        }
        return out;
    }

    private static String periodLabel(LocalDate from, LocalDate to) {
        if (from.getYear() == to.getYear() && from.getMonthValue() == to.getMonthValue()) {
            return String.format("Tháng %02d/%d", from.getMonthValue(), from.getYear());
        }
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return from.format(fmt) + " - " + to.format(fmt);
    }

    // ==================== DỮ LIỆU HIỂN THỊ (class lồng) ====================

    /** Toàn bộ dữ liệu hiển thị của trang Báo Cáo Rạp (Branch Report Dashboard). */
    public static class BranchReportView {

        private String type;
        private String typeLabel;
        private String fromDate;
        private String toDate;
        private String scopeLabel;
        private String branchPill;
        private String dataTag;

        private String chartTitle;
        private String chartUnit;
        private boolean chartStacked;
        private boolean chartPercent;
        private List<String> chartLabels = new ArrayList<>();
        private List<Map<String, Object>> chartDatasets = new ArrayList<>();

        private String overviewTitle = "Tổng Quan Chỉ Số Báo Cáo";
        /** Mỗi phần tử: {nhãn, giá trị, css: "" | "strong" | "total"}. */
        private List<String[]> overview = new ArrayList<>();

        private String sideTitle;
        /** Mỗi phần tử: {tên, giá trị bên phải}. */
        private List<String[]> sideItems = new ArrayList<>();

        private List<ReportTable> tables = new ArrayList<>();
        private String note;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTypeLabel() { return typeLabel; }
        public void setTypeLabel(String typeLabel) { this.typeLabel = typeLabel; }
        public String getFromDate() { return fromDate; }
        public void setFromDate(String fromDate) { this.fromDate = fromDate; }
        public String getToDate() { return toDate; }
        public void setToDate(String toDate) { this.toDate = toDate; }
        public String getScopeLabel() { return scopeLabel; }
        public void setScopeLabel(String scopeLabel) { this.scopeLabel = scopeLabel; }
        public String getBranchPill() { return branchPill; }
        public void setBranchPill(String branchPill) { this.branchPill = branchPill; }
        public String getDataTag() { return dataTag; }
        public void setDataTag(String dataTag) { this.dataTag = dataTag; }

        public String getChartTitle() { return chartTitle; }
        public void setChartTitle(String chartTitle) { this.chartTitle = chartTitle; }
        public String getChartUnit() { return chartUnit; }
        public void setChartUnit(String chartUnit) { this.chartUnit = chartUnit; }
        public boolean isChartStacked() { return chartStacked; }
        public void setChartStacked(boolean chartStacked) { this.chartStacked = chartStacked; }
        public boolean isChartPercent() { return chartPercent; }
        public void setChartPercent(boolean chartPercent) { this.chartPercent = chartPercent; }
        public List<String> getChartLabels() { return chartLabels; }
        public void setChartLabels(List<String> chartLabels) { this.chartLabels = chartLabels; }
        public List<Map<String, Object>> getChartDatasets() { return chartDatasets; }
        public void setChartDatasets(List<Map<String, Object>> chartDatasets) { this.chartDatasets = chartDatasets; }

        public String getOverviewTitle() { return overviewTitle; }
        public void setOverviewTitle(String overviewTitle) { this.overviewTitle = overviewTitle; }
        public List<String[]> getOverview() { return overview; }
        public void setOverview(List<String[]> overview) { this.overview = overview; }

        public String getSideTitle() { return sideTitle; }
        public void setSideTitle(String sideTitle) { this.sideTitle = sideTitle; }
        public List<String[]> getSideItems() { return sideItems; }
        public void setSideItems(List<String[]> sideItems) { this.sideItems = sideItems; }

        public List<ReportTable> getTables() { return tables; }
        public void setTables(List<ReportTable> tables) { this.tables = tables; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    /** Bảng dữ liệu tổng quát cho trang báo cáo chi nhánh. Mỗi ô: {hiển thị, css class, giá trị thô cho CSV}. */
    public static class ReportTable {

        private final String title;
        private String tag = "";
        private final List<String> headers;
        private final List<String> aligns;
        private final List<List<String[]>> rows = new ArrayList<>();

        public ReportTable(String title, List<String> headers, List<String> aligns) {
            this.title = title;
            this.headers = headers;
            this.aligns = aligns;
        }

        public static String[] cell(String text) {
            return new String[]{text, "", text};
        }

        public static String[] cell(String text, String css) {
            return new String[]{text, css, text};
        }

        public static String[] cell(String text, String css, String raw) {
            return new String[]{text, css, raw};
        }

        public void addRow(List<String[]> row) { rows.add(row); }

        public String getTitle() { return title; }
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
        public List<String> getHeaders() { return headers; }
        public List<String> getAligns() { return aligns; }
        public List<List<String[]>> getRows() { return rows; }
    }
}
