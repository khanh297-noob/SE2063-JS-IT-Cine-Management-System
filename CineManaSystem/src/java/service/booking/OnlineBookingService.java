package service.booking;

import dao.UserDao;
import dao.booking.BookingDAO;
import dao.booking.FnbDAO;
import dao.booking.OnlineSeatDAO;
import dao.booking.SeatHoldDAO;
import dao.movie.ShowtimeDAO;
import dao.pos.AuditLogDAO;
import model.User;
import model.booking.Booking;
import model.booking.FnbItem;
import model.booking.HoldSummary;
import model.booking.SeatRow;
import model.booking.SeatView;
import model.movie.Showtime;
import service.movie.ShowtimePriceCalculator;
import service.payment.PaymentService;
import util.BusinessException;
import util.JdbcContext;
import util.JdbcUtil;
import util.PasswordUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Đặt vé online của khách: chọn phim -> chọn suất -> chọn ghế (giữ ghế 10 phút) -> đồ ăn & nước
 * -> tạo đơn Pending rồi chuyển sang trang thanh toán chung (/payment/checkout, PaymentService):
 * áp voucher + thanh toán VNPay giống VNPay QR của POS. Thanh toán xong PaymentService chuyển đơn sang Confirmed.
 * Ngoài ra: Vé của tôi, xem đơn + QR, khách tự huỷ đơn, tự vô hiệu hoá tài khoản.
 */
public class OnlineBookingService {

    public static final int MAX_SEATS = 8;                 // GB-10, giống POS
    public static final int MAX_FNB_QUANTITY = 10;         // tối đa mỗi món
    public static final int CANCEL_CUTOFF_HOURS = 2;       // GB-33: khách tự huỷ trước giờ chiếu 2 tiếng
    public static final int SHOWTIME_DAYS = 7;             // số ngày hiện lịch chiếu cho khách chọn

    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final OnlineSeatDAO seatDAO = new OnlineSeatDAO();
    private final SeatHoldDAO holdDAO = new SeatHoldDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final FnbDAO fnbDAO = new FnbDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public static int holdMinutes() {
        return PaymentService.HOLD_MINUTES;
    }

    // ===================== Chọn suất chiếu =====================

    /** Suất còn mở bán của phim trong ngày date, gom theo chi nhánh: { "Tên rạp" -> [suất...] }. */
    public Map<String, List<Showtime>> showtimesOfMovie(int movieId, LocalDate date) throws SQLException, BusinessException {
        List<Showtime> list = JdbcContext.query(con -> showtimeDAO.findOpenByMovie(con, movieId, date, date));
        Map<String, List<Showtime>> byBranch = new LinkedHashMap<>();
        for (Showtime s : list) {
            if (!s.isBookable()) continue;      // suất hôm nay đã qua giờ chiếu
            byBranch.computeIfAbsent(s.getBranchName(), k -> new ArrayList<>()).add(s);
        }
        return byBranch;
    }

    /** Các ngày có suất mở bán của phim trong SHOWTIME_DAYS ngày tới (để hiện thanh chọn ngày). */
    public Set<LocalDate> datesWithShowtimes(int movieId) throws SQLException, BusinessException {
        LocalDate today = LocalDate.now();
        List<Showtime> list = JdbcContext.query(con ->
                showtimeDAO.findOpenByMovie(con, movieId, today, today.plusDays(SHOWTIME_DAYS - 1L)));
        Set<LocalDate> dates = new java.util.TreeSet<>();
        for (Showtime s : list) {
            if (s.isBookable()) dates.add(s.getShowDate());
        }
        return dates;
    }

    public Showtime getBookableShowtime(int showtimeId) throws SQLException, BusinessException {
        Showtime st = JdbcContext.query(con -> showtimeDAO.findById(con, showtimeId));
        if (st == null) {
            throw new BusinessException("Suất chiếu không tồn tại.");
        }
        if (!st.isBookable()) {
            throw new BusinessException("Suất chiếu " + st.getStartStr() + " " + st.getDateStr()
                    + " không còn mở bán (" + st.getDisplayStatus() + ").");
        }
        return st;
    }

    // ===================== Sơ đồ ghế =====================

    private BigDecimal priceOf(Showtime st, SeatView seat) {
        return ShowtimePriceCalculator.ticketPrice(st.getBasePrice(), seat.getSurcharge());
    }

    public List<SeatRow> seatMap(User user, Showtime st) throws SQLException, BusinessException {
        return JdbcContext.query(con -> {
            Set<Integer> sold = seatDAO.soldSeatIds(con, st.getShowtimeId());
            Map<Integer, Integer> holds = holdDAO.findActiveByShowtime(con, st.getShowtimeId());
            Map<String, SeatRow> rows = new LinkedHashMap<>();
            for (SeatView seat : seatDAO.findByRoom(con, st.getRoomId())) {
                if (seat.isBroken()) {
                    seat.setState(SeatView.BROKEN);
                } else if (sold.contains(seat.getSeatId())) {
                    seat.setState(SeatView.SOLD);
                } else if (holds.containsKey(seat.getSeatId())) {
                    seat.setState(holds.get(seat.getSeatId()) == user.getUserId() ? SeatView.MINE : SeatView.HELD);
                }
                seat.setPrice(priceOf(st, seat));
                rows.computeIfAbsent(seat.getRowLabel(), SeatRow::new).getSeats().add(seat);
            }
            return new ArrayList<>(rows.values());
        });
    }

    public int seatColumns(List<SeatRow> rows) {
        int max = 0;
        for (SeatRow row : rows) {
            for (SeatView sv : row.getSeats()) max = Math.max(max, sv.getColNum());
        }
        return max;
    }

    /** Bảng giá theo loại ghế (1 ghế đại diện / loại), rẻ trước. */
    public List<SeatView> priceTable(List<SeatRow> rows) {
        Map<String, SeatView> m = new LinkedHashMap<>();
        for (SeatRow row : rows) {
            for (SeatView sv : row.getSeats()) m.putIfAbsent(sv.getSeatType(), sv);
        }
        List<SeatView> list = new ArrayList<>(m.values());
        list.sort((a, b) -> a.getPrice().compareTo(b.getPrice()));
        return list;
    }

    /** SeatID -> trạng thái (sold / held / mine) để trang chọn ghế tự cập nhật mỗi 10 giây. */
    public Map<Integer, String> seatStates(User user, int showtimeId) throws SQLException, BusinessException {
        return JdbcContext.query(con -> {
            Map<Integer, String> states = new HashMap<>();
            for (int id : seatDAO.soldSeatIds(con, showtimeId)) states.put(id, SeatView.SOLD);
            for (Map.Entry<Integer, Integer> e : holdDAO.findActiveByShowtime(con, showtimeId).entrySet()) {
                states.putIfAbsent(e.getKey(), e.getValue() == user.getUserId() ? SeatView.MINE : SeatView.HELD);
            }
            return states;
        });
    }

    /** Suất khác khách đã có vé bị trùng giờ với suất đang chọn (chỉ cảnh báo, vẫn cho đặt). */
    public List<Showtime> personalConflicts(User user, Showtime st) throws SQLException, BusinessException {
        List<Showtime> result = new ArrayList<>();
        for (Showtime o : JdbcContext.query(con -> showtimeDAO.findBookedByUserAround(con, user.getUserId(), st.getShowDate()))) {
            if (o.getShowtimeId() == st.getShowtimeId()) continue;
            if (st.getStartAt().isBefore(o.getEndAt()) && o.getStartAt().isBefore(st.getEndAt())) result.add(o);
        }
        return result;
    }

    // ===================== Giữ ghế =====================

    /**
     * Giữ các ghế đã chọn trong holdMinutes() phút. Chống 2 người chọn cùng ghế:
     * UNIQUE(ShowtimeID, SeatID) của SeatHold chỉ cho 1 người giữ, sau đó kiểm tra lại ghế chưa có vé.
     */
    public void holdSeats(User user, int showtimeId, List<Integer> seatIds) throws SQLException, BusinessException {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất 1 ghế.");
        }
        if (seatIds.size() > MAX_SEATS) {
            throw new BusinessException("Mỗi đơn chỉ được đặt tối đa " + MAX_SEATS + " ghế (GB-10).");
        }
        Showtime st = getBookableShowtime(showtimeId);
        if (bookingDAO.findPendingOnlineBooking(user.getUserId(), showtimeId) != null) {
            throw new BusinessException("Bạn đang có đơn chờ thanh toán cho suất này. Hãy thanh toán hoặc đợi đơn hết hạn.");
        }
        JdbcContext.transaction(con -> {
            Map<Integer, SeatView> roomSeats = new HashMap<>();
            for (SeatView s : seatDAO.findByRoom(con, st.getRoomId())) roomSeats.put(s.getSeatId(), s);
            for (int id : seatIds) {
                SeatView s = roomSeats.get(id);
                if (s == null) throw new BusinessException("Ghế không thuộc phòng chiếu của suất này (GB-07).");
                if (s.isBroken()) throw new BusinessException("Ghế " + s.getCode() + " đang hỏng, vui lòng chọn ghế khác.");
            }
            holdDAO.deleteByUserShowtime(con, user.getUserId(), showtimeId);
            holdDAO.deleteExpiredForShowtime(con, showtimeId);
            for (int seatId : seatIds) {
                try {
                    holdDAO.insert(con, showtimeId, seatId, user.getUserId(), holdMinutes());
                } catch (SQLException e) {
                    if (JdbcUtil.isUniqueViolation(e)) {
                        throw new BusinessException("Ghế " + roomSeats.get(seatId).getCode()
                                + " vừa được người khác giữ. Vui lòng chọn ghế khác.");
                    }
                    throw e;
                }
            }
            if (seatDAO.countSold(con, showtimeId, seatIds) > 0) {
                throw new BusinessException("Một số ghế bạn chọn đã được bán. Vui lòng chọn lại.");
            }
            return null;
        });
    }

    /** Ghế khách đang giữ của suất, null nếu không còn giữ ghế nào (hết giờ / chưa chọn). */
    public HoldSummary getHold(User user, int showtimeId) throws SQLException, BusinessException {
        Showtime st = JdbcContext.query(con -> showtimeDAO.findById(con, showtimeId));
        if (st == null) return null;
        return JdbcContext.query(con -> {
            List<Integer> held = holdDAO.activeSeatsOfUser(con, user.getUserId(), showtimeId, false);
            if (held.isEmpty()) return null;
            int remaining = holdDAO.remainingSeconds(con, user.getUserId(), showtimeId);
            List<SeatView> seats = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (SeatView seat : seatDAO.findByRoom(con, st.getRoomId())) {
                if (held.contains(seat.getSeatId())) {
                    seat.setState(SeatView.MINE);
                    seat.setPrice(priceOf(st, seat));
                    seats.add(seat);
                    total = total.add(seat.getPrice());
                }
            }
            return new HoldSummary(st, seats, total, remaining);
        });
    }

    public void releaseHold(User user, int showtimeId) throws SQLException, BusinessException {
        JdbcContext.transaction(con -> {
            holdDAO.deleteByUserShowtime(con, user.getUserId(), showtimeId);
            return null;
        });
    }

    // ===================== Đồ ăn & nước =====================

    /** Thực đơn của chi nhánh (bảng Branch_F_B, dùng chung với POS) kèm số lượng khách đã chọn. */
    public List<FnbItem> fnbMenu(int branchId, Map<Integer, Integer> cart) {
        List<FnbItem> menu = fnbDAO.getMenu(branchId);
        for (FnbItem item : menu) {
            Integer qty = cart == null ? null : cart.get(item.getItemId());
            item.setQuantity(qty == null ? 0 : qty);
        }
        return menu;
    }

    /** Kiểm tra giỏ đồ ăn: món phải thuộc thực đơn chi nhánh, số lượng 0..MAX_FNB_QUANTITY. Bỏ các món số lượng 0. */
    public Map<Integer, Integer> normalizeCart(Showtime st, Map<Integer, Integer> requested) throws BusinessException {
        Map<Integer, FnbItem> menu = new LinkedHashMap<>();
        for (FnbItem item : fnbDAO.getMenu(st.getBranchId())) menu.put(item.getItemId(), item);
        Map<Integer, Integer> cart = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> e : requested.entrySet()) {
            int qty = e.getValue() == null ? 0 : e.getValue();
            if (qty == 0) continue;
            FnbItem item = menu.get(e.getKey());
            if (item == null) {
                throw new BusinessException("Có món không thuộc thực đơn của " + st.getBranchName()
                        + " hoặc đã ngừng bán. Vui lòng chọn lại.");
            }
            if (qty < 0 || qty > MAX_FNB_QUANTITY) {
                throw new BusinessException("Số lượng " + item.getItemName() + " phải từ 0 đến " + MAX_FNB_QUANTITY + ".");
            }
            cart.put(item.getItemId(), qty);
        }
        return cart;
    }

    /** Các dòng đồ ăn của giỏ, giá theo thực đơn chi nhánh hiện tại. */
    public List<FnbItem> cartLines(int branchId, Map<Integer, Integer> cart) {
        List<FnbItem> lines = new ArrayList<>();
        if (cart == null || cart.isEmpty()) return lines;
        for (FnbItem item : fnbDAO.getMenu(branchId)) {
            Integer qty = cart.get(item.getItemId());
            if (qty != null && qty > 0) {
                item.setQuantity(qty);
                lines.add(item);
            }
        }
        return lines;
    }

    // ===================== Tạo đơn chờ thanh toán =====================

    /**
     * Chuyển ghế đang giữ + giỏ đồ ăn thành đơn Pending (1 transaction), xoá giữ ghế.
     * Từ giờ ghế được giữ bằng chính vé của đơn Pending; PaymentSyncJob tự huỷ đơn nếu quá 10 phút chưa trả tiền.
     * @return BookingID để chuyển sang /payment/checkout?bookingId=
     */
    public int createPendingBooking(User user, int showtimeId, Map<Integer, Integer> cart, String ip)
            throws SQLException, BusinessException {
        Showtime st = getBookableShowtime(showtimeId);
        Map<Integer, Integer> safeCart = normalizeCart(st, cart == null ? new HashMap<>() : cart);
        List<FnbItem> fnbLines = cartLines(st.getBranchId(), safeCart);
        if (fnbLines.size() < safeCart.size()) {
            throw new BusinessException("Có món đồ ăn vừa ngừng bán tại " + st.getBranchName() + ". Vui lòng chọn lại đồ ăn & nước.");
        }
        int bookingId = JdbcContext.transaction(con -> {
            List<Integer> held = holdDAO.activeSeatsOfUser(con, user.getUserId(), showtimeId, true);
            if (held.isEmpty()) {
                throw new BusinessException("Thời gian giữ ghế đã hết. Vui lòng chọn lại ghế.");
            }
            List<SeatView> seats = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (SeatView seat : seatDAO.findByRoom(con, st.getRoomId())) {
                if (!held.contains(seat.getSeatId())) continue;
                if (seat.isBroken()) {
                    throw new BusinessException("Ghế " + seat.getCode() + " vừa được báo hỏng. Vui lòng chọn lại ghế.");
                }
                seat.setPrice(priceOf(st, seat));
                total = total.add(seat.getPrice());
                seats.add(seat);
            }
            for (FnbItem f : fnbLines) total = total.add(f.getLineTotal());
            int id;
            try {
                id = bookingDAO.createOnlineBooking(con, user.getUserId(), st.getBranchId(), showtimeId, seats, fnbLines, total);
            } catch (SQLException e) {
                if (JdbcUtil.isUniqueViolation(e)) {
                    throw new BusinessException("Có ghế vừa được bán cho người khác. Vui lòng chọn lại ghế.");
                }
                throw e;
            }
            holdDAO.deleteByUserShowtime(con, user.getUserId(), showtimeId);
            return id;
        });
        auditLogDAO.insert(user.getUserId(), "ONLINE_BOOKING|booking=" + bookingId + "|showtime=" + showtimeId, ip);
        return bookingId;
    }

    /** Đơn đang chờ thanh toán của khách cho suất này (để hiện nút "Tiếp tục thanh toán"). */
    public Integer pendingBookingId(User user, int showtimeId) {
        return bookingDAO.findPendingOnlineBooking(user.getUserId(), showtimeId);
    }

    // ===================== Vé của tôi =====================

    public List<Booking> history(User user) {
        return bookingDAO.getOnlineBookings(user.getUserId());
    }

    /** Chủ đơn, Admin, hoặc nhân sự của chi nhánh có suất chiếu mới được xem. */
    public Booking getBooking(User user, int bookingId) throws BusinessException {
        Booking b = bookingDAO.getBooking(bookingId);
        if (b == null || !canView(user, b)) {
            throw new BusinessException("Không tìm thấy đơn đặt vé.");
        }
        return b;
    }

    private boolean canView(User user, Booking b) {
        String role = user.getRole() == null ? "" : user.getRole();
        if (user.getUserId() == b.getUserId() || "Administrator".equalsIgnoreCase(role) || "Admin".equalsIgnoreCase(role)) {
            return true;
        }
        return !"Customer".equalsIgnoreCase(role) && Objects.equals(user.getBranchID(), b.getBranchId());
    }

    /** Lý do không huỷ được đơn, null = huỷ được. Khách chỉ huỷ đơn của mình, trước giờ chiếu CANCEL_CUTOFF_HOURS giờ. */
    public String cancelBlockReason(User user, Booking b) {
        if (!"Confirmed".equals(b.getStatus())) return "Đơn không ở trạng thái đã thanh toán.";
        if (b.getStartDateTime() == null) return "Đơn không có vé.";
        if (b.isAnyCheckedIn()) return "Đơn đã có vé check-in, không thể hủy.";
        if (user.getUserId() != b.getUserId()) return "Bạn không có quyền hủy đơn này.";
        if (!LocalDateTime.now().isBefore(b.getStartDateTime().minusHours(CANCEL_CUTOFF_HOURS))) {
            return "Chỉ được hủy trước giờ chiếu " + CANCEL_CUTOFF_HOURS + " giờ.";
        }
        return null;
    }

    /**
     * Khách tự huỷ đơn đã thanh toán: vé -> Cancelled (nhả ghế), Payment -> Refunded,
     * Booking -> Cancelled với RefundAmount = toàn bộ tiền (hoàn 100% trước hạn huỷ).
     * @return số tiền hoàn
     */
    public BigDecimal cancel(User user, int bookingId, String ip) throws BusinessException {
        Booking b = getBooking(user, bookingId);
        String reason = cancelBlockReason(user, b);
        if (reason != null) {
            throw new BusinessException("Không thể hủy đơn " + b.getBookingCode() + ": " + reason);
        }
        if (!bookingDAO.cancelCounterBooking(bookingId)) {
            throw new BusinessException("Đơn đã được hủy trước đó.");
        }
        auditLogDAO.insert(user.getUserId(), "ONLINE_CANCEL|booking=" + bookingId
                + "|refund=" + b.getTotalAmount().toPlainString(), ip);
        return b.getTotalAmount();
    }

    // ===================== Vô hiệu hoá tài khoản =====================

    public void deactivate(User user, String password, boolean confirmed, String ip) throws SQLException, BusinessException {
        if (!confirmed) {
            throw new BusinessException("Vui lòng tích xác nhận bạn hiểu hậu quả của việc vô hiệu hóa tài khoản.");
        }
        UserDao userDao = new UserDao();
        try {
            User fresh = userDao.findById(user.getUserId());
            if (fresh == null || password == null || !PasswordUtil.verify(password, fresh.getPasswordHash())) {
                throw new BusinessException("Mật khẩu không đúng.");
            }
            int upcoming = bookingDAO.countUpcomingConfirmed(fresh.getUserId());
            if (upcoming > 0) {
                throw new BusinessException("Bạn còn " + upcoming
                        + " đơn vé sắp chiếu. Hãy hủy hoặc sử dụng hết trước khi vô hiệu hóa tài khoản.");
            }
            JdbcContext.transaction(con -> {
                holdDAO.deleteByUser(con, fresh.getUserId());
                return null;
            });
            userDao.updateStatus(fresh.getUserId(), "Deactivated");
        } finally {
            userDao.close();
        }
        auditLogDAO.insert(user.getUserId(), "SELF_DEACTIVATE", ip);
    }
}
