package controller.payment;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import service.PaymentService;

/**
 * IPN: máy chủ VNPay gọi thẳng vào link này để báo kết quả (cần domain public, vd ngrok).
 * Cấu hình link IPN trong trang quản trị merchant VNPay. VNPay yêu cầu trả về JSON {RspCode, Message}.
 */
@WebServlet(name = "VnpayIpnServlet", urlPatterns = {"/api/v1/payment/webhook/vnpay"})
public class VnpayIpnServlet extends HttpServlet {

    private PaymentService paymentService;

    @Override
    public void init() throws ServletException {
        paymentService = new PaymentService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Map<String, String> params = new HashMap<>();
        for (String name : request.getParameterMap().keySet()) {
            params.put(name, request.getParameter(name));
        }

        String code;
        try {
            code = paymentService.processVnpayResult(params);
        } catch (Exception e) {
            code = "99";
        }
        String message;
        switch (code) {
            case "00":
                message = "Confirm Success";
                break;
            case "01":
                message = "Order not Found";
                break;
            case "02":
                message = "Order already confirmed";
                break;
            case "04":
                message = "Invalid Amount";
                break;
            case "97":
                message = "Invalid Checksum";
                break;
            default:
                message = "Unknow error";
        }
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.print("{\"RspCode\": \"" + code + "\", \"Message\": \"" + message + "\"}");
    }
}
