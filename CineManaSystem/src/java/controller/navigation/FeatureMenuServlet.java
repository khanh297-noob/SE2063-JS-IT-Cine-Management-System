package controller.navigation;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import model.User;
import service.common.AccessService;
import util.SessionUtil;

@WebServlet(urlPatterns = {"/admin/system", "/staff/fnb", "/staff/promotions"})
public class FeatureMenuServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User actor = SessionUtil.getLoggedInUser(request);
            String target;

            switch (request.getServletPath()) {
                case "/admin/system" -> {
                    AccessService.admin(actor);
                    target = "/admin/users";
                }
                case "/staff/fnb" -> {
                    AccessService.fnb(actor);
                    target = "Administrator".equals(actor.getRole()) ? "/staff/fnb/items" : "/staff/fnb/orders";
                }
                case "/staff/promotions" -> {
                    AccessService.promotion(actor);
                    target = "Ticket Cashier".equals(actor.getRole())
                            ? "/staff/promotions/validate"
                            : "/staff/promotions/manage";
                }
                default -> {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }

            // Sang Servlet chức năng để Servlet đó nạp dữ liệu đầy đủ
            response.sendRedirect(request.getContextPath() + target);
        } catch (SecurityException exception) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
        }
    }
}
