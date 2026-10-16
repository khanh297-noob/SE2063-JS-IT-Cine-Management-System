package controller;

import service.PaymentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * MoMo chuyển trình duyệt của khách về đây sau khi thanh toán (redirectUrl).
 * Chạy localhost thì MoMo không gọi được IPN, nên trang này cũng cập nhật kết quả
 * (có kiểm tra chữ ký + số tiền, chỉ cập nhật giao dịch còn Pending nên gọi 2 lần không bị trùng).
 */
@WebServlet(name = "MomoReturnServlet", urlPatterns = {"/payment/momo-return"})
public class MomoReturnServlet extends HttpServlet {

    private PaymentService paymentService;

    @Override
    public void init() throws ServletException {
        paymentService = new PaymentService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Lấy toàn bộ tham số MoMo gửi về trên URL
        Map<String, String> params = new HashMap<>();
        for (String name : request.getParameterMap().keySet()) {
            params.put(name, request.getParameter(name));
        }

        String code = paymentService.processMomoResult(params);
        int bookingId = paymentService.getBookingIdFromTxnRef(params.get("orderId"));
        if ("97".equals(code) || "01".equals(code) || "04".equals(code)) {
            request.setAttribute("errorMsg", "Invalid payment response (code " + code + "). "
                    + "Please contact support if your account was charged.");
            request.getRequestDispatcher("/WEB-INF/views/common/error-nam.jsp").forward(request, response);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/payment/result?bookingId=" + bookingId);
    }
}
