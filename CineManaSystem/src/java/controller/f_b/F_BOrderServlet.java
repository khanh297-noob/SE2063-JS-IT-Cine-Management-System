package controller.f_b;

import service.f_b.F_BOrderService;
import service.f_b.F_BStockService;
import util.FormUtil;
import util.SessionUtil;
import model.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

@WebServlet(urlPatterns = {"/staff/fnb/orders", "/staff/fnb/order-status", "/staff/fnb/quantity"})
public class F_BOrderServlet extends HttpServlet {
    private final F_BOrderService service = new F_BOrderService();
    private final F_BStockService stocks = new F_BStockService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User actor = SessionUtil.getLoggedInUser(request);
            request.setAttribute("branchList", stocks.allowedBranches(actor));
            request.setAttribute("customers", service.customers(actor));
            Integer branch = optionalInt(request, "branchId");
            Integer id = optionalInt(request, "bookingId");
            if (id != null) {
                Booking booking = service.detail(actor, id);
                if (branch != null && !branch.equals(booking.getBranchId()))
                    throw new IllegalArgumentException("Đơn không thuộc chi nhánh đang chọn.");
                branch = booking.getBranchId();
                request.setAttribute("selectedBooking", booking);
                request.setAttribute("orderLines", service.lines(actor, id));
            }
            if (branch != null) {
                request.setAttribute("selectedBranchId", branch);
                request.setAttribute("orderList", service.list(actor, branch));
                request.setAttribute("saleMenu", stocks.saleMenu(actor, branch));
            }
            FormUtil.csrf(request);
            String page = switch (request.getServletPath()) {
                case "/staff/fnb/order-status" -> "order-status";
                case "/staff/fnb/quantity" -> "quantity";
                default -> "orders";
            };
            request.getRequestDispatcher("/WEB-INF/view/fnb/" + page + ".jsp").forward(request, response);
        } catch (SecurityException e) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
        } catch (IllegalArgumentException e) {
            request.getSession().setAttribute("toastError", e.getMessage());
            response.sendRedirect(request.getContextPath() + request.getServletPath());
        } catch (SQLException e) {
            getServletContext().log("Không đọc được đơn F&B", e);
            response.sendError(500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer branch = null;
        Integer booking = null;
        try {
            FormUtil.checkCsrf(request);
            User actor = SessionUtil.getLoggedInUser(request);
            branch = optionalInt(request, "branchId");
            String action = request.getParameter("action");
            if ("create".equals(action)) {
                if (branch == null) throw new IllegalArgumentException("Hãy chọn chi nhánh.");
                String[] ids = request.getParameterValues("itemId");
                String[] quantities = request.getParameterValues("quantity");
                if (ids == null || quantities == null || ids.length != quantities.length || ids.length > 100)
                    throw new IllegalArgumentException("Giỏ hàng không hợp lệ.");
                Map<Integer, Integer> cart = new TreeMap<>();
                for (int i = 0; i < ids.length; i++) {
                    int item = Integer.parseInt(ids[i]);
                    int qty = Integer.parseInt(quantities[i]);
                    if (qty < 0) throw new IllegalArgumentException("Số lượng không được âm.");
                    if (qty == 0) continue;
                    if (cart.putIfAbsent(item, qty) != null) throw new IllegalArgumentException("Món bị gửi trùng.");
                }
                booking = service.create(actor, branch, FormUtil.integer(request, "customerId"), cart);
            } else {
                booking = FormUtil.integer(request, "bookingId");
                if ("quantity".equals(action)) {
                    service.quantity(actor, booking, FormUtil.integer(request, "itemId"), FormUtil.integer(request, "quantity"));
                } else if ("progress".equals(action)) {
                    service.progress(actor, booking, request.getParameter("nextStatus"));
                } else if ("cancel".equals(action)) {
                    service.cancel(actor, booking);
                } else throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
            request.getSession().setAttribute("toastSuccess", "Đã xử lý đơn #" + booking);
        } catch (SecurityException e) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        } catch (IllegalArgumentException e) {
            request.getSession().setAttribute("toastError", e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Không xử lý được đơn F&B", e);
            request.getSession().setAttribute("toastError", "Không lưu được đơn.");
        }
        String url = request.getContextPath() + request.getServletPath();
        if (branch != null) url += "?branchId=" + branch;
        if (booking != null) url += (branch == null ? "?" : "&") + "bookingId=" + booking;
        response.sendRedirect(url);
    }

    private Integer optionalInt(HttpServletRequest request, String name) {
        String raw = request.getParameter(name);
        return raw == null || raw.isBlank() ? null : FormUtil.integer(request, name);
    }
}
