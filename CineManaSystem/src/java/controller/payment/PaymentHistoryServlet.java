package controller.payment;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.User;
import service.PaymentService;

/**
 * Lịch sử thanh toán của khách đang đăng nhập.
 */
@WebServlet(name = "PaymentHistoryServlet", urlPatterns = {"/payment/history"})
public class PaymentHistoryServlet extends HttpServlet {

    private PaymentService paymentService;

    @Override
    public void init() throws ServletException {
        paymentService = new PaymentService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("User");
        String status = request.getParameter("status");
        if (status == null) {
            status = "";
        }
        request.setAttribute("payments", paymentService.getHistory(user.getUserId(), status));
        request.setAttribute("status", status);
        request.getRequestDispatcher("/WEB-INF/view/payment/history-nam.jsp").forward(request, response);
    }
}
