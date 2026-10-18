package util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import service.BranchReportService.BranchReportView;
import service.RevenueDashboardService.BreakdownTable;
import service.BranchReportService.ReportTable;
import dao.ReportDao.RevenueBreakdownRow;
import service.RevenueDashboardService.RevenueDashboard;
import dao.ReportDao.TicketReportItem;

/**
 * Tạo nội dung file CSV cho 3 loại báo cáo: vé đã bán, doanh thu vé + F&B, báo cáo rạp.
 * Mỗi file bắt đầu bằng ký tự BOM UTF-8 để Excel mở tiếng Việt không bị lỗi font.
 */
public class ExportUtil {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,###");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    /** CSV danh sách vé đã bán (mỗi vé một dòng). */
    public static String generateTicketSalesCsv(List<TicketReportItem> items) {
        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM (Byte Order Mark) để Microsoft Excel mở tiếng Việt không bị lỗi font
        sb.append('\ufeff');

        // CSV Header
        sb.append("Mã vé,Tên phim,Chi nhánh,Phòng chiếu,Số ghế,Ngày chiếu,Giờ chiếu,Khách hàng,Email,Thời gian đặt,Giá vé (VNĐ),Trạng thái\n");

        if (items != null) {
            for (TicketReportItem item : items) {
                sb.append(escapeCsv(item.getTicketCode())).append(",");
                sb.append(escapeCsv(item.getMovieTitle())).append(",");
                sb.append(escapeCsv(item.getBranchName())).append(",");
                sb.append(escapeCsv(item.getRoomName())).append(",");
                sb.append(escapeCsv(item.getSeatName())).append(",");
                sb.append(item.getShowDate() != null ? DATE_FORMAT.format(item.getShowDate()) : "").append(",");
                sb.append(item.getStartTime() != null ? TIME_FORMAT.format(item.getStartTime()) : "").append(",");
                sb.append(escapeCsv(item.getCustomerName())).append(",");
                sb.append(escapeCsv(item.getCustomerEmail())).append(",");
                sb.append(item.getBookingDate() != null ? DATETIME_FORMAT.format(item.getBookingDate()) : "").append(",");
                sb.append(item.getFinalPrice() != null ? item.getFinalPrice().toPlainString() : "0").append(",");
                sb.append(escapeCsv(item.getTicketStatus())).append("\n");
            }
        }
        return sb.toString();
    }

    /** CSV Revenue Dashboard: doanh thu theo tuần và các bảng phân rã (vé + F&B). */
    public static String generateRevenueDashboardCsv(RevenueDashboard d) {
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff');

        sb.append("Báo cáo doanh thu vé và F&B\n");
        sb.append("Phạm vi,").append(escapeCsv(d.getScopeLabel())).append("\n");
        sb.append("Từ ngày,").append(escapeCsv(d.getFromDate())).append("\n");
        sb.append("Đến ngày,").append(escapeCsv(d.getToDate())).append("\n\n");

        sb.append("Doanh thu theo tuần\n");
        sb.append("Tuần,Doanh thu vé (VNĐ),Doanh thu F&B (VNĐ),Tổng (VNĐ)\n");
        for (int i = 0; i < d.getWeekLabels().size(); i++) {
            BigDecimal ticket = d.getWeeklyTicketRevenue().get(i);
            BigDecimal fnb = d.getWeeklyFnbRevenue().get(i);
            sb.append(escapeCsv(d.getWeekLabels().get(i))).append(",")
              .append(ticket.toPlainString()).append(",")
              .append(fnb.toPlainString()).append(",")
              .append(ticket.add(fnb).toPlainString()).append("\n");
        }
        sb.append("\n");

        for (BreakdownTable table : d.getTables()) {
            sb.append(escapeCsv(table.getTitle())).append("\n");
            sb.append(escapeCsv(table.getNameHeader()))
              .append(",Tổng số vé bán,Doanh thu vé (VNĐ),Doanh thu F&B đi kèm (VNĐ),")
              .append(escapeCsv(table.getTotalHeader())).append(" (VNĐ),Tỷ trọng (%)\n");
            for (RevenueBreakdownRow r : table.getRows()) {
                sb.append(escapeCsv(r.getName())).append(",")
                  .append(r.getTickets()).append(",")
                  .append(r.getTicketRevenue().toPlainString()).append(",")
                  .append(r.getFnbRevenue().toPlainString()).append(",")
                  .append(r.getTotalRevenue().toPlainString()).append(",")
                  .append(String.format(Locale.US, "%.1f", r.getShare())).append("\n");
            }
            sb.append("Tổng cộng,")
              .append(table.getTotalTickets()).append(",")
              .append(table.getTotalTicketRevenue().toPlainString()).append(",")
              .append(table.getTotalFnbRevenue().toPlainString()).append(",")
              .append(table.getGrandTotal().toPlainString()).append(",100.0\n\n");
        }
        return sb.toString();
    }

    /** CSV Báo Cáo Rạp: thông tin lọc, tổng quan và các bảng (giá trị thô, không có định dạng tiền tệ). */
    public static String generateBranchReportCsv(BranchReportView v) {
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff');

        sb.append(escapeCsv(v.getTypeLabel())).append("\n");
        sb.append("Phạm vi,").append(escapeCsv(v.getScopeLabel())).append("\n");
        sb.append("Từ ngày,").append(escapeCsv(v.getFromDate())).append("\n");
        sb.append("Đến ngày,").append(escapeCsv(v.getToDate())).append("\n\n");

        sb.append(escapeCsv(v.getOverviewTitle())).append("\n");
        for (String[] line : v.getOverview()) {
            sb.append(escapeCsv(line[0])).append(",").append(escapeCsv(line[1])).append("\n");
        }
        sb.append("\n");

        for (ReportTable table : v.getTables()) {
            sb.append(escapeCsv(table.getTitle())).append("\n");
            for (int i = 0; i < table.getHeaders().size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(escapeCsv(table.getHeaders().get(i)));
            }
            sb.append("\n");
            for (List<String[]> row : table.getRows()) {
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(escapeCsv(row.get(i)[2]));
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        if (v.getNote() != null) {
            sb.append("Ghi chú,").append(escapeCsv(v.getNote())).append("\n");
        }
        return sb.toString();
    }

    /** Bọc giá trị trong dấu ngoặc kép nếu có dấu phẩy, ngoặc kép hoặc xuống dòng, để không vỡ cột CSV. */
    private static String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
