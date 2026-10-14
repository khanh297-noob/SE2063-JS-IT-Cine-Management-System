package cinemanagement.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cinemanagement.dao.AuditLogDAO;
import cinemanagement.dao.BookingDAO;
import cinemanagement.dao.PaymentDAO;
import cinemanagement.dao.SeatDAO;
import cinemanagement.dao.SeatHoldDAO;
import cinemanagement.dao.ShowtimeDAO;
import cinemanagement.dao.TicketDAO;
import cinemanagement.dao.UserDAO;
import cinemanagement.model.Booking;
import cinemanagement.model.HoldSummary;
import cinemanagement.model.Seat;
import cinemanagement.model.SeatRow;
import cinemanagement.model.SeatView;
import cinemanagement.model.Showtime;
import cinemanagement.model.Ticket;
import cinemanagement.model.User;
import cinemanagement.util.AppConfig;
import cinemanagement.util.BusinessException;
import cinemanagement.util.DBContext;
import cinemanagement.util.TicketCodeGenerator;

/**
 * Booking & Seat: Select Seat, Seat Holding, Booking Ticket, Booking Cancellation, (du lieu cho) Generate QR.
 * Dung cho ca Customer (online) va Ticket Cashier (POS).
 *
 * Chong ban trung ghe 2 lop:
 *  1. SeatHold UNIQUE(ShowtimeID, SeatID)     -> 2 nguoi khong the giu cung 1 ghe
 *  2. Ticket filtered UNIQUE (Status <> 'Cancelled') -> ke ca co loi logic cung khong the co 2 ve active
 */
public class BookingService {

    public static final List<String> ONLINE_METHODS = List.of("VNPay", "MoMo", "Credit Card");
    public static final List<String> POS_METHODS = List.of("Cash", "Credit Card");

    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final SeatDAO seatDAO = new SeatDAO();
    private final SeatHoldDAO holdDAO = new SeatHoldDAO();
    private final TicketDAO ticketDAO = new TicketDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final UserDAO userDAO = new UserDAO();
    private final AuditLogDAO audit = new AuditLogDAO();

    // ================================================================ SELECT SEAT

    /** Lay suat chieu dat ve duoc; kiem tra quyen POS theo chi nhanh. */
    public Showtime getBookableShowtime(User actor, int showtimeId) throws SQLException, BusinessException {
        Showtime st = showtimeDAO.findById(showtimeId);
        if (st == null) {
            throw new BusinessException("Suất chiếu không tồn tại.");
        }
        if (actor.isCashier() && !Objects.equals(actor.getBranchId(), st.getBranchId())) {
            throw new BusinessException("Thu ngân chỉ được bán vé suất chiếu tại chi nhánh của mình.");
        }
        if (!st.isBookable()) {
            throw new BusinessException("Suất chiếu " + st.getStartStr() + " " + st.getDateStr()
                    + " không còn mở bán (" + st.getDisplayStatus() + ").");
        }
        return st;
    }

    /** So do ghe theo hang, kem trang thai & gia tung ghe. */
    public List<SeatRow> seatMap(User actor, Showtime st) throws SQLException {
        Set<Integer> sold = ticketDAO.soldSeatIds(st.getShowtimeId());
        Map<Integer, Integer> holds = holdDAO.findActiveByShowtime(st.getShowtimeId());
        Map<String, SeatRow> rows = new LinkedHashMap<>();
        for (Seat seat : seatDAO.findByRoom(st.getRoomId())) {
            String state;
            if (sold.contains(seat.getSeatId())) {
                state = SeatView.SOLD;
            } else if (holds.containsKey(seat.getSeatId())) {
                state = holds.get(seat.getSeatId()) == actor.getUserId() ? SeatView.MINE : SeatView.HELD;
            } else {
                state = SeatView.AVAILABLE;
            }
            BigDecimal price = PriceCalculator.finalPrice(st.getBasePrice(), seat.getSeatType(), st.getRoomType());
            rows.computeIfAbsent(seat.getRowLabel(), SeatRow::new).getSeats().add(new SeatView(seat, state, price));
        }
        return new ArrayList<>(rows.values());
    }

    /** Bang gia theo loai ghe cho chu thich. */
    public Map<String, BigDecimal> priceTable(Showtime st) {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        for (String type : List.of("Standard", "VIP", "Sweetbox")) {
            m.put(type, PriceCalculator.finalPrice(st.getBasePrice(), type, st.getRoomType()));
        }
        return m;
    }

    /** JSON nho cho AJAX cap nhat trang thai ghe. */
    public Map<Integer, String> seatStates(User actor, int showtimeId) throws SQLException {
        Set<Integer> sold = ticketDAO.soldSeatIds(showtimeId);
        Map<Integer, Integer> holds = holdDAO.findActiveByShowtime(showtimeId);
        Map<Integer, String> states = new HashMap<>();
        for (int id : sold) {
            states.put(id, SeatView.SOLD);
        }
        for (Map.Entry<Integer, Integer> e : holds.entrySet()) {
            if (!states.containsKey(e.getKey())) {
                states.put(e.getKey(), e.getValue() == actor.getUserId() ? SeatView.MINE : SeatView.HELD);
            }
        }
        return states;
    }

    /**
     * Check Overlap phia Customer: canh bao neu khach da co ve o suat khac trung gio.
     * Khong chan dat ve (co the mua ho nguoi khac), chi canh bao.
     */
    public List<Showtime> personalConflicts(User customer, Showtime st) throws SQLException {
        List<Showtime> result = new ArrayList<>();
        if (!customer.isCustomer()) {
            return result;
        }
        for (Showtime o : showtimeDAO.findBookedByUserAround(customer.getUserId(), st.getShowDate())) {
            if (o.getShowtimeId() == st.getShowtimeId()) {
                continue;
            }
            if (st.getStartAt().isBefore(o.getEndAt()) && o.getStartAt().isBefore(st.getEndAt())) {
                result.add(o);
            }
        }
        return result;
    }

    // ================================================================ SEAT HOLDING

    /**
     * Giu cac ghe da chon trong N phut (SystemSetting SEAT_HOLD_MINUTES, mac dinh 10).
     * Cac ghe nguoi dung dang giu truoc do cho suat nay se duoc thay bang lua chon moi.
     */
    public void holdSeats(User actor, int showtimeId, List<Integer> seatIds) throws SQLException, BusinessException {
        User fresh = userDAO.findById(actor.getUserId());
        if (fresh == null || !fresh.isActive()) {
            throw new BusinessException("Tài khoản của bạn hiện không khả dụng, không thể đặt vé (GB-35).");
        }
        if (seatIds == null || seatIds.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất 1 ghế.");
        }
        int max = AppConfig.maxSeatsPerBooking();
        if (seatIds.size() > max) {
            throw new BusinessException("Mỗi đơn chỉ được đặt tối đa " + max + " ghế (GB-10).");
        }
        Showtime st = getBookableShowtime(actor, showtimeId);

        Map<Integer, Seat> roomSeats = new HashMap<>();
        for (Seat s : seatDAO.findByRoom(st.getRoomId())) {
            roomSeats.put(s.getSeatId(), s);
        }
        for (int id : seatIds) {
            if (!roomSeats.containsKey(id)) {
                throw new BusinessException("Ghế không thuộc phòng chiếu của suất này (GB-07).");
            }
        }

        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                holdDAO.deleteByUserShowtime(c, actor.getUserId(), showtimeId);
                holdDAO.deleteExpiredForShowtime(c, showtimeId);
                int minutes = AppConfig.seatHoldMinutes();
                for (int seatId : seatIds) {
                    try {
                        holdDAO.insert(c, showtimeId, seatId, actor.getUserId(), minutes);
                    } catch (SQLException e) {
                        if (DBContext.isDuplicateKey(e)) {
                            throw new BusinessException("Ghế " + roomSeats.get(seatId).getCode()
                                    + " vừa được người khác giữ. Vui lòng chọn ghế khác.");
                        }
                        throw e;
                    }
                }
                // Kiem tra SAU khi giu: neu ve da duoc ban (commit truoc do) thi huy
                if (ticketDAO.countSold(c, showtimeId, seatIds) > 0) {
                    throw new BusinessException("Một số ghế bạn chọn đã được bán. Vui lòng chọn lại.");
                }
                c.commit();
            } catch (SQLException | BusinessException | RuntimeException e) {
                DBContext.rollbackQuietly(c);
                throw e;
            }
        }
    }

    /** Thong tin giu ghe hien tai; null neu khong con ghe nao (het han hoac chua giu). */
    public HoldSummary getHold(User actor, int showtimeId) throws SQLException {
        Showtime st = showtimeDAO.findById(showtimeId);
        if (st == null) {
            return null;
        }
        List<Integer> held = holdDAO.activeSeatsOfUser(actor.getUserId(), showtimeId);
        if (held.isEmpty()) {
            return null;
        }
        int remaining = holdDAO.remainingSeconds(actor.getUserId(), showtimeId);
        List<SeatView> seats = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Seat seat : seatDAO.findByRoom(st.getRoomId())) {
            if (held.contains(seat.getSeatId())) {
                BigDecimal price = PriceCalculator.finalPrice(st.getBasePrice(), seat.getSeatType(), st.getRoomType());
                seats.add(new SeatView(seat, SeatView.MINE, price));
                total = total.add(price);
            }
        }
        return new HoldSummary(st, seats, total, remaining);
    }

    public void releaseHold(User actor, int showtimeId) throws SQLException {
        holdDAO.deleteByUserShowtime(actor.getUserId(), showtimeId);
    }

    // ================================================================ BOOKING TICKET

    /**
     * Xac nhan dat ve tu cac ghe dang giu.
     * Online: phuong thuc VNPay/MoMo/Credit Card - hien tai la cong thanh toan GIA LAP (xem PaymentGateway).
     * POS:    Cash/Credit Card; customerKey (SDT/email) tuy chon de gan don vao tai khoan khach.
     * @return BookingID
     */
    public int confirm(User actor, int showtimeId, String method, String customerKey, String ip)
            throws SQLException, BusinessException {
        boolean pos = actor.isCashier();
        if (!(pos ? POS_METHODS : ONLINE_METHODS).contains(method)) {
            throw new BusinessException("Phương thức thanh toán không hợp lệ.");
        }
        Showtime st = getBookableShowtime(actor, showtimeId);

        int ownerId = actor.getUserId();
        if (pos && customerKey != null && !customerKey.isBlank()) {
            User customer = userDAO.findCustomerByPhoneOrEmail(customerKey.trim());
            if (customer == null) {
                throw new BusinessException("Không tìm thấy khách hàng với SĐT/email \"" + customerKey.trim()
                        + "\". Để trống nếu là khách vãng lai.");
            }
            if (!customer.isActive()) {
                throw new BusinessException("Tài khoản khách hàng đang bị khóa/vô hiệu hóa (GB-35).");
            }
            ownerId = customer.getUserId();
        }

        Map<Integer, Seat> roomSeats = new HashMap<>();
        for (Seat s : seatDAO.findByRoom(st.getRoomId())) {
            roomSeats.put(s.getSeatId(), s);
        }

        int bookingId;
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                List<Integer> held = holdDAO.activeSeatsOfUser(c, actor.getUserId(), showtimeId, true);
                if (held.isEmpty()) {
                    throw new BusinessException("Thời gian giữ ghế đã hết. Vui lòng chọn lại ghế.");
                }

                List<Ticket> tickets = new ArrayList<>();
                BigDecimal total = BigDecimal.ZERO;
                for (int seatId : held) {
                    Seat seat = roomSeats.get(seatId);
                    Ticket t = new Ticket();
                    t.setShowtimeId(showtimeId);
                    t.setSeatId(seatId);
                    t.setTicketCode(TicketCodeGenerator.next());
                    t.setFinalPrice(PriceCalculator.finalPrice(st.getBasePrice(), seat.getSeatType(), st.getRoomType()));
                    total = total.add(t.getFinalPrice());
                    tickets.add(t);
                }

                // GB-09: chi xac nhan khi thanh toan thanh cong
                PaymentGateway.Result pay = PaymentGateway.charge(method, total, pos);
                if (!pay.success()) {
                    throw new BusinessException("Thanh toán thất bại: " + pay.message());
                }

                Booking b = new Booking();
                b.setUserId(ownerId);
                b.setTotalAmount(total);
                b.setStatus("Confirmed");
                b.setChannel(pos ? "POS" : "Online");
                b.setStaffId(pos ? actor.getUserId() : null);
                bookingId = bookingDAO.insert(c, b);

                for (Ticket t : tickets) {
                    t.setBookingId(bookingId);
                    try {
                        ticketDAO.insert(c, t);
                    } catch (SQLException e) {
                        if (DBContext.isDuplicateKey(e)) {
                            throw new BusinessException("Ghế " + roomSeats.get(t.getSeatId()).getCode()
                                    + " vừa được bán cho người khác. Vui lòng chọn lại.");
                        }
                        throw e;
                    }
                }
                paymentDAO.insert(c, bookingId, total, method, pay.transactionCode(), "Success");
                holdDAO.deleteByUserShowtime(c, actor.getUserId(), showtimeId);
                c.commit();
            } catch (SQLException | BusinessException | RuntimeException e) {
                DBContext.rollbackQuietly(c);
                throw e;
            }
        }
        audit.log(actor.getUserId(), "Confirm booking #" + bookingId + (pos ? " (POS)" : ""), ip);
        // TODO (Notification Service): gui email e-ticket kem QR cho khach.
        return bookingId;
    }

    // ================================================================ VIEW + CANCEL

    /** Lay don kem ve; kiem tra quyen xem. */
    public Booking getBooking(User actor, int bookingId) throws SQLException, BusinessException {
        Booking b = bookingDAO.findById(bookingId);
        if (b == null || !canView(actor, b)) {
            throw new BusinessException("Không tìm thấy đơn đặt vé.");
        }
        b.setTickets(ticketDAO.findByBooking(bookingId));
        b.setCancelBlockReason(cancelBlockReason(actor, b));
        return b;
    }

    public List<Booking> history(User customer) throws SQLException {
        List<Booking> list = bookingDAO.findByUser(customer.getUserId());
        for (Booking b : list) {
            b.setTickets(ticketDAO.findByBooking(b.getBookingId()));
            b.setCancelBlockReason(cancelBlockReason(customer, b));
        }
        return list;
    }

    public List<Booking> searchForPos(User cashier, String keyword) throws SQLException, BusinessException {
        if (cashier.getBranchId() == null) {
            throw new BusinessException("Tài khoản chưa được gán chi nhánh.");
        }
        List<Booking> list = bookingDAO.searchInBranch(cashier.getBranchId(), keyword);
        for (Booking b : list) {
            b.setTickets(ticketDAO.findByBooking(b.getBookingId()));
            b.setCancelBlockReason(cancelBlockReason(cashier, b));
        }
        return list;
    }

    private boolean canView(User actor, Booking b) {
        if (actor.isAdmin() || actor.getUserId() == b.getUserId()) {
            return true;
        }
        boolean staffOfBranch = (actor.isCashier() || actor.isManager() || actor.isChecker())
                && b.getShowtime() != null && Objects.equals(actor.getBranchId(), b.getShowtime().getBranchId());
        return staffOfBranch;
    }

    /**
     * Ly do khong duoc huy; null = duoc huy.
     * Customer: truoc gio chieu CANCEL_CUTOFF_HOURS gio (GB-33). Thu ngan: truoc gio chieu, tai chi nhanh minh.
     */
    public String cancelBlockReason(User actor, Booking b) {
        if (!b.isConfirmed()) {
            return "Đơn không ở trạng thái đã xác nhận.";
        }
        Showtime st = b.getShowtime();
        if (st == null) {
            return "Đơn không có vé.";
        }
        for (Ticket t : b.getTickets()) {
            if (t.isCheckedIn()) {
                return "Đơn đã có vé check-in, không thể hủy.";
            }
        }
        LocalDateTime now = LocalDateTime.now();
        if (actor.isCashier()) {
            if (!Objects.equals(actor.getBranchId(), st.getBranchId())) {
                return "Đơn thuộc chi nhánh khác.";
            }
            return now.isBefore(st.getStartAt()) ? null : "Suất chiếu đã bắt đầu.";
        }
        if (actor.isAdmin()) {
            return now.isBefore(st.getStartAt()) ? null : "Suất chiếu đã bắt đầu.";
        }
        if (actor.isCustomer() && actor.getUserId() == b.getUserId()) {
            int cutoff = AppConfig.cancelCutoffHours();
            return now.isBefore(st.getStartAt().minusHours(cutoff)) ? null
                    : "Chỉ được hủy trước giờ chiếu " + cutoff + " giờ.";
        }
        return "Bạn không có quyền hủy đơn này.";
    }

    /** Booking Cancellation: huy don + ve, hoan 100% (chinh sach co the mo rong). */
    public BigDecimal cancel(User actor, int bookingId, String ip) throws SQLException, BusinessException {
        Booking b = getBooking(actor, bookingId);
        String reason = b.getCancelBlockReason();
        if (reason != null) {
            throw new BusinessException("Không thể hủy đơn " + b.getBookingCode() + ": " + reason);
        }
        BigDecimal refund;
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                refund = bookingDAO.lockConfirmedTotal(c, bookingId);
                if (refund == null) {
                    throw new BusinessException("Đơn đã được hủy trước đó.");
                }
                if (ticketDAO.countCheckedIn(c, bookingId) > 0) {
                    throw new BusinessException("Đơn đã có vé check-in, không thể hủy.");
                }
                ticketDAO.cancelByBooking(c, bookingId);
                bookingDAO.markCancelled(c, bookingId, refund);
                paymentDAO.markRefunded(c, bookingId);
                c.commit();
            } catch (SQLException | BusinessException | RuntimeException e) {
                DBContext.rollbackQuietly(c);
                throw e;
            }
        }
        audit.log(actor.getUserId(), "Cancel booking #" + bookingId + ", refund " + refund, ip);
        return refund;
    }
}
