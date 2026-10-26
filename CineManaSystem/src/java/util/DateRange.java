package util;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Khoảng ngày (từ ngày - đến ngày) dùng cho các trang báo cáo.
 * Đọc từ tham số fromDate, toDate (định dạng yyyy-MM-dd).
 * Mặc định là từ ngày đầu đến ngày cuối của tháng hiện tại.
 * Nếu người dùng chọn khoảng sai thì dùng lại mặc định và ghi lỗi vào biến error.
 */
public class DateRange {

    /** Chỉ cho xem tối đa 366 ngày mỗi lần. */
    public static final int MAX_RANGE_DAYS = 366;

    private LocalDate from;
    private LocalDate to;
    private String error;

    /** Khoảng ngày mặc định là cả tháng hiện tại. */
    public static DateRange fromRequest(HttpServletRequest request) {
        LocalDate today = LocalDate.now();
        return fromRequest(request, today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
    }

    /** Như trên nhưng tự chọn khoảng ngày mặc định (dùng khi không muốn mặc định là cả tháng). */
    public static DateRange fromRequest(HttpServletRequest request, LocalDate defaultFrom, LocalDate defaultTo) {
        DateRange range = new DateRange();
        range.from = parseDate(request.getParameter("fromDate"));
        range.to = parseDate(request.getParameter("toDate"));

        // Bước 1: thiếu ngày nào thì dùng ngày mặc định
        if (range.from == null) {
            range.from = defaultFrom;
        }
        if (range.to == null) {
            range.to = defaultTo;
        }

        // Bước 2: kiểm tra khoảng ngày hợp lệ
        if (range.from.isAfter(range.to)) {
            range.error = "Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc. Đã dùng khoảng thời gian mặc định.";
        } else if (ChronoUnit.DAYS.between(range.from, range.to) + 1 > MAX_RANGE_DAYS) {
            range.error = "Chỉ xem được tối đa " + MAX_RANGE_DAYS
                    + " ngày mỗi lần. Đã dùng khoảng thời gian mặc định.";
        }

        // Bước 3: khoảng sai thì quay về mặc định
        if (range.error != null) {
            range.from = defaultFrom;
            range.to = defaultTo;
        }
        return range;
    }

    /** Chuyển chuỗi yyyy-MM-dd thành ngày. Rỗng hoặc sai định dạng thì trả về null. */
    public static LocalDate parseDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public LocalDate getFrom() { return from; }

    public LocalDate getTo() { return to; }

    /** Thông báo lỗi (null nếu khoảng ngày hợp lệ). */
    public String getError() { return error; }

    public boolean hasError() { return error != null; }
}
