package controller.f_b;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import model.User;
import service.common.AccessService;
import service.f_b.F_BCheckoutService;
import util.FormUtil;
import util.SessionUtil;

@WebServlet("/staff/fnb/checkout")
public class F_BCheckoutServlet extends HttpServlet {
    private final F_BCheckoutService service = new F_BCheckoutService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Integer bookingId = null;
        try {
            FormUtil.checkCsrf(request);
            User actor = SessionUtil.getLoggedInUser(request);
            AccessService.fnb(actor);
            bookingId = FormUtil.integer(request, "bookingId");
            String action = request.getParameter("action");
            switch (action == null ? "" : action) {
                case "apply" -> service.apply(actor, bookingId, request.getParameter("promoCode"));
                case "remove" -> service.remove(actor, bookingId);
                case "cash" -> {
                    BigDecimal change = service.payCash(actor, bookingId,
                            FormUtil.money(request, "cashReceived"), request.getRemoteAddr());
                    request.getSession().setAttribute("toastSuccess",
                            "Đã thu tiền. Tiền thừa: " + change.toPlainString() + " đ.");
                }
                case "refund" -> service.refundFoodCash(actor, bookingId, request.getRemoteAddr());
                default -> throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
            if (request.getSession().getAttribute("toastSuccess") == null) {
                request.getSession().setAttribute("toastSuccess", "Đã cập nhật đơn.");
            }
        } catch (SecurityException exception) {
            response.sendError(403, "Không có quyền thực hiện thao tác.");
            return;
        } catch (IllegalArgumentException exception) {
            request.getSession().setAttribute("toastError", exception.getMessage());
        } catch (SQLException exception) {
            getServletContext().log("Không xử lý được thanh toán F&B", exception);
            request.getSession().setAttribute("toastError", "Không lưu được giao dịch; hãy kiểm tra log.");
        }
        response.sendRedirect(request.getContextPath() + "/staff/fnb/orders"
                + (bookingId == null ? "" : "?bookingId=" + bookingId));
    }
}
