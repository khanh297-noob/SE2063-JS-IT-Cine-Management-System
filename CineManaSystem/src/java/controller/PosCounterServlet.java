package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Booking;
import model.FnbItem;
import model.Promotion;
import model.Seat;
import model.Showtime;
import model.User;
import model.WorkShift;
import service.PaymentService;
import service.PosService;
import service.ShiftService;

/**
 * Màn hình bán vé tại quầy.
 *  GET  /pos?date=&q=&showtimeId=   : danh sách suất chiếu, sơ đồ ghế, đồ ăn, hóa đơn
 *  POST /pos action=preview         : tính tiền (áp voucher) rồi hiện lại trang
 *  POST /pos action=confirm         : tạo đơn + thu tiền -> chuyển sang trang in vé
 */
@WebServlet(name = "PosCounterServlet", urlPatterns = {"/pos"})
public class PosCounterServlet extends HttpServlet {

    private PosService posService;
    private ShiftService shiftService;
    private PaymentService paymentService;

    @Override
    public void init() throws ServletException {
        posService = new PosService();
        shiftService = new ShiftService();
        paymentService = new PaymentService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        WorkShift shift = shiftService.getActiveShift(user);
        if (shift == null) {
            session.setAttribute("flashType", "error");
            session.setAttribute("flashMsg", "Bạn cần bắt đầu ca làm việc trước khi bán vé.");
            response.sendRedirect(request.getContextPath() + "/pos/shift");
            return;
        }
        showPage(request, response, shift, new ArrayList<>(), posService.getFnbMenu());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        WorkShift shift = shiftService.getActiveShift(user);
        if (shift == null) {
            response.sendRedirect(request.getContextPath() + "/pos/shift");
            return;
        }

        // Ghế đã chọn (checkbox name="seatId")
        List<Integer> selectedIds = new ArrayList<>();
        String[] seatParams = request.getParameterValues("seatId");
        if (seatParams != null) {
            for (String s : seatParams) {
                selectedIds.add(parseInt(s, -1));
            }
        }
        // Số lượng đồ ăn (input name="fnb_{itemId}")
        List<FnbItem> menu = posService.getFnbMenu();
        for (FnbItem f : menu) {
            f.setQuantity(parseInt(request.getParameter("fnb_" + f.getItemId()), 0));
        }

        if ("confirm".equals(request.getParameter("action"))) {
            Showtime st = posService.getShowtime(parseInt(request.getParameter("showtimeId"), -1));
            List<Seat> seats = new ArrayList<>();
            if (st != null) {
                for (Seat s : posService.getSeats(st)) {
                    if (selectedIds.contains(s.getSeatId())) {
                        seats.add(s);
                    }
                }
            }
            List<FnbItem> items = new ArrayList<>();
            for (FnbItem f : menu) {
                if (f.getQuantity() > 0) {
                    items.add(f);
                }
            }
            String method = request.getParameter("paymentMethod");
            BigDecimal cashReceived = parseMoney(request.getParameter("cashReceived"));
            String voucher = getParam(request, "voucherCode").toUpperCase();
            try {
                int bookingId = posService.createCounterBooking(user, shift, st, seats, items,
                        method, cashReceived, voucher, request.getRemoteAddr());
                String msg = "Booking created successfully.";
                if ("Cash".equals(method)) {
                    Booking b = posService.getBooking(bookingId);
                    msg += " Tiền thừa trả khách: " + String.format("%,.0f ₫", cashReceived.subtract(b.getTotalAmount()));
                }
                session.setAttribute("flashType", "success");
                session.setAttribute("flashMsg", msg);
                response.sendRedirect(request.getContextPath() + "/pos/print?bookingId=" + bookingId + "&auto=1");
                return;
            } catch (IllegalArgumentException e) {
                request.setAttribute("error", e.getMessage());
            }
        }
        // action=preview hoặc tạo đơn bị lỗi -> hiện lại trang, giữ nguyên ghế/đồ ăn đã chọn
        showPage(request, response, shift, selectedIds, menu);
    }

    /**
     * Đưa dữ liệu ra counter-nam.jsp: suất chiếu, sơ đồ ghế (đánh dấu ghế đang chọn), đồ ăn, hóa đơn.
     */
    private void showPage(HttpServletRequest request, HttpServletResponse response, WorkShift shift,
            List<Integer> selectedIds, List<FnbItem> menu) throws ServletException, IOException {
        LocalDate date;
        try {
            date = LocalDate.parse(request.getParameter("date"));
        } catch (Exception e) {
            date = LocalDate.now();
        }
        String q = getParam(request, "q");
        request.setAttribute("shift", shift);
        request.setAttribute("date", date.toString());
        request.setAttribute("q", q);
        request.setAttribute("movies", posService.getShowtimesByMovie(shift.getBranchId(), date, q));
        request.setAttribute("fnbMenu", menu);

        Showtime st = posService.getShowtime(parseInt(request.getParameter("showtimeId"), -1));
        if (st != null && st.getBranchId() == shift.getBranchId()) {
            // Gom ghế theo hàng: { "A" -> [A1, A2...], "B" -> [...] } và đánh dấu ghế đang chọn
            Map<String, List<Seat>> seatRows = new LinkedHashMap<>();
            List<Seat> selectedSeats = new ArrayList<>();
            for (Seat s : posService.getSeats(st)) {
                if (selectedIds.contains(s.getSeatId()) && "AVAILABLE".equals(s.getState())) {
                    s.setSelected(true);
                    selectedSeats.add(s);
                }
                if (!seatRows.containsKey(s.getRowLabel())) {
                    seatRows.put(s.getRowLabel(), new ArrayList<>());
                }
                seatRows.get(s.getRowLabel()).add(s);
            }
            List<FnbItem> chosenItems = new ArrayList<>();
            for (FnbItem f : menu) {
                if (f.getQuantity() > 0) {
                    chosenItems.add(f);
                }
            }

            // Hóa đơn
            BigDecimal subtotal = posService.getSubtotal(selectedSeats, chosenItems);
            BigDecimal discount = BigDecimal.ZERO;
            String voucher = getParam(request, "voucherCode").toUpperCase();
            if (!voucher.isEmpty() && subtotal.signum() > 0) {
                try {
                    Promotion p = paymentService.checkVoucher(voucher, subtotal, 0);
                    discount = paymentService.getDiscount(p, subtotal);
                    request.setAttribute("voucherOk", "Promotion applied successfully.");
                } catch (IllegalArgumentException e) {
                    request.setAttribute("voucherError", e.getMessage());
                }
            }
            BigDecimal total = subtotal.subtract(discount);
            BigDecimal cash = parseMoney(request.getParameter("cashReceived"));

            request.setAttribute("st", st);
            request.setAttribute("seatRows", seatRows);
            request.setAttribute("selectedSeats", selectedSeats);
            request.setAttribute("chosenItems", chosenItems);
            request.setAttribute("subtotal", subtotal);
            request.setAttribute("discount", discount);
            request.setAttribute("total", total);
            request.setAttribute("voucherCode", voucher);
            request.setAttribute("cashReceived", cash);
            if (cash != null && cash.compareTo(total) >= 0) {
                request.setAttribute("change", cash.subtract(total));
            }
            String method = request.getParameter("paymentMethod");
            request.setAttribute("paymentMethod", method == null ? "Cash" : method);
        }
        request.getRequestDispatcher("/WEB-INF/views/pos/counter-nam.jsp").forward(request, response);
    }

    private String getParam(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private int parseInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // Ô <input type="number"> gửi dạng 150000 hoặc 150000.00
    private BigDecimal parseMoney(String s) {
        try {
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
