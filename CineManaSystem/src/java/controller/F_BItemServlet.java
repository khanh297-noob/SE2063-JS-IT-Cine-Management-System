package controller;

import model.User;
import service.F_BItemService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.UUID;

@WebServlet("/admin/fnb-items")
public class F_BItemServlet extends HttpServlet {

    private final F_BItemService service = new F_BItemService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || !(session.getAttribute("currentUser") instanceof User actor)) {

            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            request.setAttribute("itemList", service.getAllItems(actor));

            if (session.getAttribute("csrfToken") == null) {
                session.setAttribute("csrfToken", UUID.randomUUID().toString());
            }

            response.setHeader("Cache-Control", "no-store");

            request.getRequestDispatcher("/WEB-INF/views/admin/" + "f_b-item-management-khanh.jsp"
            ).forward(request, response);

        } catch (SecurityException exception) {
            response.sendError(403);
        } catch (SQLException exception) {
            getServletContext().log("Không đọc được danh mục F&B.", exception);
            response.sendError(500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null || !(session.getAttribute("currentUser") instanceof User actor)) {

            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String expected =
                (String) session.getAttribute("csrfToken");
        String submitted = request.getParameter("csrfToken");

        if (expected == null || !expected.equals(submitted)) {
            response.sendError(403, "Yêu cầu không hợp lệ.");
            return;
        }

        try {
            String action = request.getParameter("action");

            if ("save".equals(action)) {
                int itemId = Integer.parseInt(request.getParameter("itemId"));

                BigDecimal price = new BigDecimal(request.getParameter("price"));

                service.save(actor, itemId, request.getParameter("itemName"), request.getParameter("category"), price);

            } else if ("activate".equals(action) || "deactivate".equals(action)) {

                int itemId = Integer.parseInt(request.getParameter("itemId"));

                service.changeActive(actor, itemId, "activate".equals(action));

            } else {
                throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }

            session.setAttribute("toastSuccess", "Đã cập nhật sản phẩm.");

        } catch (NumberFormatException exception) {
            session.setAttribute("toastError", "ID hoặc giá không đúng định dạng.");

        } catch (IllegalArgumentException exception) {
            session.setAttribute("toastError", exception.getMessage());

        } catch (SecurityException exception) {
            response.sendError(403);
            return;

        } catch (SQLException exception) {
            getServletContext().log("Không cập nhật được F&B.", exception);

            session.setAttribute("toastError", "Không lưu được dữ liệu. Hãy thử lại.");
        }

        response.sendRedirect(request.getContextPath() + "/admin/fnb-items");
    }
}