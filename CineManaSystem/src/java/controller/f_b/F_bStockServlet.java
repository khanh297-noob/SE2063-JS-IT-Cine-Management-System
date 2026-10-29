package controller.f_b;

import service.f_b.F_BStockService;
import util.FormUtil;
import util.SessionUtil;
import model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

@WebServlet("/staff/fnb/stock")
public class F_bStockServlet extends HttpServlet {
    private final F_BStockService service = new F_BStockService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User actor = SessionUtil.getLoggedInUser(request);
            request.setAttribute("branchList", service.allowedBranches(actor));
            boolean configure = service.canConfigure(actor);
            request.setAttribute("canConfigure", configure);
            String raw = request.getParameter("branchId");
            if (raw != null && !raw.isBlank()) {
                int branch = FormUtil.integer(request, "branchId");
                request.setAttribute("selectedBranchId", branch);
                request.setAttribute("stockList", service.list(actor, branch));
                if (configure) request.setAttribute("catalog", service.catalog(actor, branch));
            }
            FormUtil.csrf(request);
            request.getRequestDispatcher("/WEB-INF/view/fnb/stock.jsp").forward(request, response);
        } catch (SecurityException e) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
        } catch (IllegalArgumentException e) {
            request.getSession().setAttribute("toastError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/staff/fnb/stock");
        } catch (SQLException e) {
            getServletContext().log("Không đọc được kho", e);
            response.sendError(500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer branch = null;
        try {
            FormUtil.checkCsrf(request);
            User actor = SessionUtil.getLoggedInUser(request);
            branch = FormUtil.integer(request, "branchId");
            int item = FormUtil.integer(request, "itemId");
            String action = request.getParameter("action");
            if ("configure".equals(action)) {
                String raw = request.getParameter("overridePrice");
                BigDecimal price = raw == null || raw.isBlank() ? null : FormUtil.money(request, "overridePrice");
                service.configure(actor, branch, item, price, request.getParameter("status"));
            } else if ("adjust-stock".equals(action)) {
                service.adjust(actor, branch, item, FormUtil.integer(request, "delta"), request.getRemoteAddr());
            } else throw new IllegalArgumentException("Thao tác không hợp lệ.");
            request.getSession().setAttribute("toastSuccess", "Đã cập nhật kho/thực đơn.");
        } catch (SecurityException e) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return;
        } catch (IllegalArgumentException e) {
            request.getSession().setAttribute("toastError", e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Không cập nhật được kho", e);
            request.getSession().setAttribute("toastError", "Không lưu được dữ liệu kho.");
        }
        response.sendRedirect(request.getContextPath() + "/staff/fnb/stock"
                + (branch == null ? "" : "?branchId=" + branch));
    }
}
