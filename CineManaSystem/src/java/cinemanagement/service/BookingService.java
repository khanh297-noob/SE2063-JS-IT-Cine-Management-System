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

public class BookingService {
    public static final List<String> PAYMENT_METHODS = List.of("VNPay");

    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final SeatDAO seatDAO = new SeatDAO();
    private final SeatHoldDAO holdDAO = new SeatHoldDAO();
    private final TicketDAO ticketDAO = new TicketDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final UserDAO userDAO = new UserDAO();
    private final AuditLogDAO audit = new AuditLogDAO();

    public Showtime getBookableShowtime(User actor, int showtimeId) throws SQLException, BusinessException {
        Showtime st = showtimeDAO.findById(showtimeId);
        if (st == null) {
            throw new BusinessException("Suất chiếu không tồn tại.");
        }
        if (!st.isBookable()) {
            throw new BusinessException("Suất chiếu " + st.getStartStr() + " " + st.getDateStr()
                    + " không còn mở bán (" + st.getDisplayStatus() + ").");
        }
        return st;
    }

    public List<SeatRow> seatMap(User actor, Showtime st) throws SQLException {
        Set<Integer> sold = ticketDAO.soldSeatIds(st.getShowtimeId());
        Map<Integer, Integer> holds = holdDAO.findActiveByShowtime(st.getShowtimeId());
        Map<String, SeatRow> rows = new LinkedHashMap<>();
        for (Seat seat : seatDAO.findByRoom(st.getRoomId())) {
            String state;
            if (seat.isBroken()) {
                state = SeatView.BROKEN;
            } else if (sold.contains(seat.getSeatId())) {
                state = SeatView.SOLD;
            } else if (holds.containsKey(seat.getSeatId())) {
                state = holds.get(seat.getSeatId()) == actor.getUserId() ? SeatView.MINE : SeatView.HELD;
            } else {
                state = SeatView.AVAILABLE;
            }
            BigDecimal price = priceOf(st, seat);
            rows.computeIfAbsent(seat.getRowLabel(), SeatRow::new).getSeats().add(new SeatView(seat, state, price));
        }
        return new ArrayList<>(rows.values());
    }

    public int seatColumns(List<SeatRow> rows) {
        int max = 0;
        for (SeatRow row : rows) {
            for (SeatView sv : row.getSeats()) {
                max = Math.max(max, sv.getColNum());
            }
        }
        return max;
    }

    public List<SeatView> priceTable(List<SeatRow> rows) {
        Map<String, SeatView> m = new LinkedHashMap<>();
        for (SeatRow row : rows) {
            for (SeatView sv : row.getSeats()) {
                m.putIfAbsent(sv.getSeatType(), sv);
            }
        }
        List<SeatView> list = new ArrayList<>(m.values());
        list.sort((a, b) -> a.getPrice().compareTo(b.getPrice()));
        return list;
    }

    private BigDecimal priceOf(Showtime st, Seat seat) {
        return PriceCalculator.finalPrice(st.getBasePrice(), seat.getSurcharge(), st.getRoomSurcharge());
    }

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
            if (roomSeats.get(id).isBroken()) {
                throw new BusinessException("Ghế " + roomSeats.get(id).getCode() + " đang hỏng, vui lòng chọn ghế khác.");
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
                BigDecimal price = priceOf(st, seat);
                seats.add(new SeatView(seat, SeatView.MINE, price));
                total = total.add(price);
            }
        }
        return new HoldSummary(st, seats, total, remaining);
    }

    public void releaseHold(User actor, int showtimeId) throws SQLException {
        holdDAO.deleteByUserShowtime(actor.getUserId(), showtimeId);
    }

    public int confirm(User actor, int showtimeId, String method, String ip)
            throws SQLException, BusinessException {
        if (!PAYMENT_METHODS.contains(method)) {
            throw new BusinessException("Phương thức thanh toán không hợp lệ. Hệ thống chỉ hỗ trợ VNPay.");
        }
        Showtime st = getBookableShowtime(actor, showtimeId);

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
                    if (seat == null || seat.isBroken()) {
                        throw new BusinessException("Có ghế đang giữ không còn bán được. Vui lòng chọn lại ghế.");
                    }
                    t.setFinalPrice(priceOf(st, seat));
                    total = total.add(t.getFinalPrice());
                    tickets.add(t);
                }

                PaymentGateway.Result pay = PaymentGateway.charge(method, total);
                if (!pay.success()) {
                    throw new BusinessException("Thanh toán thất bại: " + pay.message());
                }

                Booking b = new Booking();
                b.setUserId(actor.getUserId());
                b.setTotalAmount(total);
                b.setStatus("Confirmed");
                b.setChannel("Online");
                b.setStaffId(null);
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
        audit.log(actor.getUserId(), "Confirm booking #" + bookingId, ip);
        return bookingId;
    }

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

    private boolean canView(User actor, Booking b) {
        if (actor.isAdmin() || actor.getUserId() == b.getUserId()) {
            return true;
        }
        boolean staffOfBranch = (actor.isManager() || actor.isChecker())
                && b.getShowtime() != null && Objects.equals(actor.getBranchId(), b.getShowtime().getBranchId());
        return staffOfBranch;
    }

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
