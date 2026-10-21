package controller;

import service.PaymentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * API-02 (MoMo): POST /api/v1/payment/webhook/momo
 * Máy chủ MoMo gọi thẳng vào đây (IPN) để báo kết quả, kể cả khi khách đóng trình duyệt.
 * MoMo gửi nội dung dạng JSON; xử lý xong trả HTTP 204 (No Content) theo tài liệu MoMo.
 * Cần domain public (vd ngrok) thì MoMo mới gọi tới được; chạy localhost thì MomoReturnServlet xử lý thay.
 */
@WebServlet(name = "MomoIpnServlet", urlPatterns = {"/api/v1/payment/webhook/momo"})
public class MomoIpnServlet extends HttpServlet {

    private PaymentService paymentService;

    @Override
    public void init() throws ServletException {
        paymentService = new PaymentService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Đọc toàn bộ nội dung JSON MoMo gửi tới
        String body = "";
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            body += line;
        }

        // Lấy các trường cần cho việc kiểm tra chữ ký
        String[] keys = {"partnerCode", "orderId", "requestId", "amount", "orderInfo", "orderType",
            "transId", "resultCode", "message", "payType", "responseTime", "extraData", "signature"};
        Map<String, String> params = new HashMap<>();
        for (String key : keys) {
            params.put(key, getJsonValue(body, key));
        }

        String code = paymentService.processMomoResult(params);
        System.out.println("MoMo IPN orderId=" + params.get("orderId") + " -> " + code);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);   // 204
    }

    // Lấy giá trị 1 key trong chuỗi JSON đơn giản; key không có thì trả "" (MoMo ký cả giá trị rỗng)
    private String getJsonValue(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) {
            return "";
        }
        i = json.indexOf(":", i) + 1;
        while (json.charAt(i) == ' ') {
            i++;
        }
        if (json.charAt(i) == '"') {
            return json.substring(i + 1, json.indexOf("\"", i + 1));
        }
        int end = i;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
            end++;
        }
        return json.substring(i, end).trim();
    }
}
