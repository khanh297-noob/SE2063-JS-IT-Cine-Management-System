package service.checker;

import dao.booking.BookingDAO;
import dao.booking.TicketDAO;
import dao.pos.AuditLogDAO;
import model.User;
import model.booking.Booking;
import model.booking.Ticket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Soát vé (phần 7.3): kiểm tra mã QR và check-in. Mỗi vé chỉ check-in được 1 lần.
 */
public class TicketService {

    private final TicketDAO ticketDAO = new TicketDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public Ticket getByCode(String code) {
        return ticketDAO.getByCode(code.trim().toUpperCase());
    }

    public Ticket getById(int ticketId) {
        return ticketDAO.getById(ticketId);
    }

    /**
     * Kiểm tra vé rồi check-in. Vé không hợp lệ thì ném IllegalArgumentException(lý do).
     * @param gateBranchId rạp đang soát vé
     */
    public void checkIn(Ticket t, int gateBranchId, User checker, String ip) {
        if (t == null) {
            throw new IllegalArgumentException("Vé không tồn tại trong hệ thống.");
        }
        if ("Checked-In".equals(t.getStatus())) {
            throw new IllegalArgumentException("Vé đã được sử dụng (Checked-In).");
        }
        if ("Cancelled".equals(t.getStatus()) || !"Confirmed".equals(t.getBookingStatus())) {
            throw new IllegalArgumentException("Đơn hàng chưa thanh toán hoặc đã bị hủy.");
        }
        if (t.getBranchId() != gateBranchId) {
            throw new IllegalArgumentException("Vé thuộc rạp khác: " + t.getBranchName() + ".");
        }
        // Chỉ cho vào từ 30 phút trước giờ chiếu đến khi phim kết thúc
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime openTime = t.getStartDateTime().minusMinutes(30);
        if (now.isBefore(openTime)) {
            throw new IllegalArgumentException("Chưa đến giờ soát vé. Mở soát vé lúc "
                    + openTime.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) + ".");
        }
        if (now.isAfter(t.getEndDateTime())) {
            throw new IllegalArgumentException("Suất chiếu đã kết thúc.");
        }
        // UPDATE có điều kiện Status = 'Valid' => 2 cổng quét cùng lúc chỉ 1 cổng thành công
        if (!ticketDAO.checkIn(t.getTicketId())) {
            throw new IllegalArgumentException("Vé đã được sử dụng (Checked-In).");
        }
        t.setStatus("Checked-In");
        auditLogDAO.insert(checker.getUserId(), "CHECK_IN|ticket=" + t.getTicketCode()
                + "|booking=" + t.getBookingId(), ip);
    }

    // Tra cứu đơn theo mã đơn (CG-00000012) / SĐT / email / mã vé
    public List<Booking> lookup(String keyword, int branchId) {
        int bookingId = -1;
        String k = keyword.toUpperCase().replace("#", "");
        if (k.startsWith("CG-")) {
            try {
                bookingId = Integer.parseInt(k.substring(3));
            } catch (NumberFormatException e) {
                bookingId = -1;
            }
        }
        return bookingDAO.lookup(keyword, bookingId, branchId);
    }
}
