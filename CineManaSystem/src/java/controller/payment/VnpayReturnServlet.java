package controller.payment;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import service.PaymentService;

/**
 * VNPay chuyển trình duyệt về đây sau khi khách thanh toán.
 * Chạy localhost thì VNPay không gọi được IPN, nên trang này cũng cập nhật kết quả
 * (có kiểm tra chữ ký + số tiền, chỉ cập nhật đơn còn Pending nên gọi 2 lần không bị trùng).
 */
@WebServlet(name = "VnpayReturnServlet", urlPatterns = {"/payment/vnpay-return"})
public class VnpayReturnServlet extends HttpServlet {

    private PaymentService paymentService;

    @Override
    public void init() throws ServletException {
        paymentService = new PaymentService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Lấy toàn bộ tham số vnp_* VNPay gửi về
        Map<String, String> params = new HashMap<>();
        for (String name : request.getParameterMap().keySet()) {
            params.put(name, request.getParameter(name));
        }

        String code = paymentService.processVnpayResult(params);
        int bookingId = paymentService.getBookingIdFromTxnRef(params.get("vnp_TxnRef"));
        if ("97".equals(code) || "01".equals(code) || "04".equals(code)) {
            request.setAttribute("errorMsg", "Invalid payment response (code " + code + "). "
                    + "Please contact support if your account was charged.");
            request.getRequestDispatcher("/WEB-INF/view/common/error-nam.jsp").forward(request, response);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/payment/result?bookingId=" + bookingId);
    }
}
