package service.notification;

import dao.notification.NotificationDAO;
import model.notification.Notification;
import util.BusinessException;
import util.JdbcContext;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thông báo trong web (chuông trên header).
 *
 * BR-NT-01  Gửi thông báo không bao giờ làm hỏng nghiệp vụ chính: các hàm notify* tự bắt lỗi, chỉ ghi log
 *           (vd chưa chạy script tạo bảng Notification). Gọi sau khi nghiệp vụ chính đã commit.
 * BR-NT-02  Người dùng chỉ xem / đánh dấu đã đọc thông báo của chính mình (mọi truy vấn lọc theo UserID).
 * BR-NT-03  Link chỉ là đường dẫn trong ứng dụng (bắt đầu bằng "/"), giao diện tự thêm context path.
 *
 * BR-NT-04  Người vừa thao tác không nhận thông báo về chính thao tác đó (vd Quản lý tự sửa chi nhánh mình).
 *
 * Sự kiện đang gửi:
 *   - Phim chuyển sang "Now Showing" (MovieStatusService.changeStatus) -> mọi khách hàng Active.
 *   - Phim đổi trạng thái, kể cả do đối soát (MovieStatusService) -> Quản lý các chi nhánh có phim đó.
 *   - Đối soát phim (MovieStatusService.enforceStatusRules) -> mọi Admin Active: phim bị chuyển về "Coming Soon"
 *     hoặc sang "End of Screening" vì hết khung chiếu; phim "Coming Soon" đã đủ điều kiện bắt đầu chiếu
 *     (1 lần mỗi đợt chiếu); phim đang chiếu thiếu điều kiện nhưng còn suất chiếu (tối đa 1 lần / ngày).
 *   - Phim được thêm / gỡ / đổi ngày chiếu ở chi nhánh (MovieBranchService.save) -> Quản lý chi nhánh đó.
 *   - Chi nhánh / phòng chiếu / giờ mở cửa / sơ đồ ghế bị sửa (các servlet quản lý chi nhánh) -> Quản lý chi nhánh đó.
 *   - Đặt vé (OnlineBookingService.createPendingBooking), khách tự huỷ đơn (OnlineBookingService.cancel) -> khách đặt đơn.
 *   - Thanh toán thành công / thất bại / hết giờ giữ ghế / tiền về trễ (PaymentService) -> khách đặt đơn.
 *   - Huỷ suất chiếu làm huỷ đơn (ShowtimeService.cancel) -> khách có đơn.
 */
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());
    private static final List<String> CUSTOMER_ROLES = List.of("Customer");
    private static final List<String> ADMIN_ROLES = List.of("Administrator", "Admin");
    /** Giống PermissionUtil.isBranchManager. */
    private static final List<String> MANAGER_ROLES = List.of("Branch Management", "BranchManager", "Manager");
    public static final int PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationDAO dao = new NotificationDAO();

    // ---------------- gửi (không ném lỗi) ----------------

    public void notifyUser(int userId, String type, String title, String content, String link) {
        try {
            JdbcContext.transaction(con -> { dao.insert(con, userId, type, title, content, safeLink(link)); return null; });
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Không gửi được thông báo cho user #" + userId + ": " + title, e);
        }
    }

    public void notifyAllCustomers(String type, String title, String content, String link) {
        notifyRoles(CUSTOMER_ROLES, type, title, content, link);
    }

    public void notifyAdmins(String type, String title, String content, String link) {
        notifyRoles(ADMIN_ROLES, type, title, content, link);
    }

    /**
     * Gửi mọi Admin, trừ người đã nhận thông báo cùng loại, tiêu đề và link từ thời điểm since
     * (cho các lần đối soát chạy lặp lại). link phải là đường dẫn trong ứng dụng.
     */
    public void notifyAdminsOnce(String type, String title, String content, String link, LocalDateTime since) {
        String safe = safeLink(link);
        if (safe == null) return;
        try {
            JdbcContext.transaction(con -> dao.insertForRolesOnce(con, ADMIN_ROLES, type, title, content, safe, since));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Không gửi được thông báo cho Admin: " + title, e);
        }
    }

    /** Quản lý của chi nhánh branchId, trừ exceptUserId (người vừa thao tác, null = không trừ ai). */
    public void notifyBranchManagers(int branchId, Integer exceptUserId, String type, String title, String content, String link) {
        try {
            JdbcContext.transaction(con -> dao.insertForBranchManagers(con, MANAGER_ROLES, branchId, exceptUserId,
                    type, title, content, safeLink(link)));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Không gửi được thông báo cho Quản lý chi nhánh #" + branchId + ": " + title, e);
        }
    }

    /** Quản lý của mọi chi nhánh đang được gán phim movieId. */
    public void notifyManagersOfMovie(int movieId, String type, String title, String content, String link) {
        try {
            JdbcContext.transaction(con -> dao.insertForManagersOfMovie(con, MANAGER_ROLES, movieId,
                    type, title, content, safeLink(link)));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Không gửi được thông báo cho Quản lý các chi nhánh của phim #" + movieId + ": " + title, e);
        }
    }

    // ---------------- đặt vé / thanh toán (module Booking / Payment gọi, sau khi đã lưu đơn) ----------------

    /** "19:30 12/11/2026 - CineGo Quận 1" cho tham số showText bên dưới; thiếu dữ liệu thì bỏ phần đó. */
    public static String showText(LocalDateTime start, String branchName) {
        String time = start == null ? "" : start.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
        if (branchName == null || branchName.isBlank()) return time;
        return time.isEmpty() ? branchName : time + " - " + branchName;
    }

    /** Đơn online vừa tạo: ghế đang được giữ, chờ thanh toán. showText vd "19:30 12/11/2026 - CineGo Quận 1". */
    public void notifyBookingCreated(int userId, int bookingId, String bookingCode, String movieTitle, String showText,
                                     int holdMinutes) {
        notifyUser(userId, Notification.TYPE_BOOKING,
                "Đặt vé thành công, chờ thanh toán: " + bookingCode,
                movieTitle + " - " + showText + ". Ghế được giữ trong " + holdMinutes
                        + " phút, hãy thanh toán trước khi hết giờ.",
                "/payment/checkout?bookingId=" + bookingId);
    }

    /** Đã nhận tiền, đơn Confirmed. amountText vd "180.000 đ". */
    public void notifyPaymentSuccess(int userId, int bookingId, String bookingCode, String movieTitle, String showText,
                                     String seatLabels, String amountText) {
        notifyUser(userId, Notification.TYPE_PAYMENT,
                "Thanh toán thành công: " + bookingCode,
                movieTitle + " - " + showText + ". Ghế " + seatLabels + ". Đã thanh toán " + amountText
                        + ". Xuất trình mã đơn khi vào rạp.",
                "/booking/detail?id=" + bookingId);
    }

    /** Tiền về sau khi đơn đã bị huỷ do hết giờ giữ ghế: cần hoàn tiền thủ công. */
    public void notifyPaymentLate(int userId, int bookingId, String bookingCode, String amountText) {
        notifyUser(userId, Notification.TYPE_PAYMENT,
                "Đơn " + bookingCode + " đã hết giờ giữ ghế",
                "Hệ thống nhận được " + amountText + " sau khi đơn đã bị huỷ. Rạp sẽ liên hệ để hoàn tiền.",
                "/booking/detail?id=" + bookingId);
    }

    /** Thanh toán thất bại / hết giờ giữ ghế: đơn Pending bị huỷ, ghế đã được nhả. */
    public void notifyPaymentFailed(int userId, int bookingId, String bookingCode, String movieTitle, boolean expired) {
        notifyUser(userId, Notification.TYPE_PAYMENT,
                (expired ? "Hết giờ thanh toán: " : "Thanh toán không thành công: ") + bookingCode,
                movieTitle + ". Đơn đã bị huỷ và ghế đã được nhả, bạn có thể đặt lại.",
                "/booking/detail?id=" + bookingId);
    }

    /** Khách tự huỷ đơn đã thanh toán. */
    public void notifyBookingCancelled(int userId, int bookingId, String bookingCode, String movieTitle, String refundText) {
        notifyUser(userId, Notification.TYPE_BOOKING,
                "Đã huỷ đơn " + bookingCode,
                movieTitle + ". Số tiền hoàn: " + refundText + ".",
                "/booking/detail?id=" + bookingId);
    }

    /** Suất chiếu bị huỷ nên đơn của khách bị huỷ theo. */
    public void notifyShowtimeCancelled(int userId, int bookingId, String bookingCode, String movieTitle, String showText) {
        notifyUser(userId, Notification.TYPE_BOOKING,
                "Suất chiếu bị huỷ: " + movieTitle,
                "Suất " + showText + " đã bị huỷ nên đơn " + bookingCode + " được huỷ theo. Rạp sẽ hoàn tiền nếu bạn đã thanh toán.",
                "/booking/detail?id=" + bookingId);
    }

    private void notifyRoles(List<String> roles, String type, String title, String content, String link) {
        try {
            int n = JdbcContext.transaction(con -> dao.insertForRoles(con, roles, type, title, content, safeLink(link)));
            LOG.fine("Đã gửi thông báo \"" + title + "\" cho " + n + " tài khoản " + roles);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Không gửi được thông báo cho " + roles + ": " + title, e);
        }
    }

    // ---------------- đọc / đánh dấu (của chính người đang đăng nhập) ----------------

    public int countUnread(int userId) throws SQLException, BusinessException {
        return JdbcContext.query(con -> dao.countUnread(con, userId));
    }

    /** Lấy limit + 1 dòng để biết còn thông báo cũ hơn không; trả về tối đa limit dòng. */
    public Page list(int userId, Integer beforeId, int limit) throws SQLException, BusinessException {
        int size = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
        return JdbcContext.query(con -> {
            List<Notification> rows = dao.findByUser(con, userId, beforeId, size + 1);
            boolean more = rows.size() > size;
            return new Page(more ? rows.subList(0, size) : rows, more, dao.countUnread(con, userId));
        });
    }

    public void markRead(int userId, int notificationId) throws SQLException, BusinessException {
        JdbcContext.transaction(con -> dao.markRead(con, userId, notificationId));
    }

    public void markAllRead(int userId) throws SQLException, BusinessException {
        JdbcContext.transaction(con -> dao.markAllRead(con, userId));
    }

    /** Một trang thông báo cho popup. */
    public static final class Page {
        private final List<Notification> items;
        private final boolean hasMore;
        private final int unread;

        Page(List<Notification> items, boolean hasMore, int unread) {
            this.items = items;
            this.hasMore = hasMore;
            this.unread = unread;
        }

        public List<Notification> getItems() { return items; }
        public boolean isHasMore() { return hasMore; }
        public int getUnread() { return unread; }
    }

    /** BR-NT-03: chỉ giữ đường dẫn nội bộ, tránh link ra trang ngoài. */
    private static String safeLink(String link) {
        if (link == null || !link.startsWith("/") || link.startsWith("//")) return null;
        return link;
    }
}
