package thirdparty;

import util.HashUtil;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Cổng thanh toán MoMo (môi trường TEST / Sandbox, tiền ảo).
 * Tài liệu: https://developers.momo.vn  (API "Thanh toán qua ví MoMo" - v2)
 * PARTNER_CODE / ACCESS_KEY / SECRET_KEY bên dưới là bộ key TEST công khai trong code mẫu của MoMo.
 * Khi nhóm có tài khoản MoMo Business test riêng thì thay bằng key của nhóm.
 *
 * Luồng: createPayment() gọi API của MoMo -> MoMo trả về payUrl -> chuyển khách sang payUrl
 *        -> khách thanh toán -> MoMo chuyển trình duyệt về redirectUrl (+ gọi ipnUrl) kèm kết quả & chữ ký.
 */
public class MomoGateway {

    public static final String CREATE_URL = "https://test-payment.momo.vn/v2/gateway/api/create";
    public static final String PARTNER_CODE = "MOMO";
    public static final String ACCESS_KEY = "F8BBA842ECF85";
    public static final String SECRET_KEY = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
    // payWithMethod: trang MoMo cho chọn quét QR bằng app MoMo test hoặc thẻ ATM test
    public static final String REQUEST_TYPE = "payWithMethod";

    /**
     * Gọi API tạo giao dịch của MoMo.
     * @param orderId     mã giao dịch, dạng "{bookingId}_{thời gian}" (không trùng)
     * @param amount      số tiền VND (số nguyên)
     * @param redirectUrl link MoMo chuyển trình duyệt về sau khi thanh toán
     * @param ipnUrl      link máy chủ MoMo gọi về báo kết quả (cần domain public, vd ngrok)
     * @return payUrl để chuyển khách sang trang thanh toán MoMo
     */
    public static String createPayment(String orderId, long amount, String orderInfo,
            String redirectUrl, String ipnUrl) {
        String requestId = orderId;
        String extraData = "";

        // Chữ ký: ghép các tham số theo đúng thứ tự a-z mà MoMo quy định rồi ký HMAC-SHA256
        String rawSignature = "accessKey=" + ACCESS_KEY
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + PARTNER_CODE
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + REQUEST_TYPE;
        String signature = HashUtil.hmacSHA256(SECRET_KEY, rawSignature);

        // Nội dung gửi đi dạng JSON
        String body = "{"
                + "\"partnerCode\":\"" + PARTNER_CODE + "\","
                + "\"partnerName\":\"CineGo\","
                + "\"storeId\":\"CineGo\","
                + "\"requestId\":\"" + requestId + "\","
                + "\"amount\":" + amount + ","
                + "\"orderId\":\"" + orderId + "\","
                + "\"orderInfo\":\"" + orderInfo + "\","
                + "\"redirectUrl\":\"" + redirectUrl + "\","
                + "\"ipnUrl\":\"" + ipnUrl + "\","
                + "\"lang\":\"vi\","
                + "\"requestType\":\"" + REQUEST_TYPE + "\","
                + "\"autoCapture\":true,"
                + "\"extraData\":\"" + extraData + "\","
                + "\"signature\":\"" + signature + "\""
                + "}";

        String response = postJson(CREATE_URL, body);
        String resultCode = getJsonValue(response, "resultCode");
        if (!"0".equals(resultCode)) {
            throw new IllegalArgumentException("MoMo từ chối tạo giao dịch: " + getJsonValue(response, "message"));
        }
        return getJsonValue(response, "payUrl");
    }

    /**
     * Kiểm tra chữ ký kết quả MoMo gửi về (redirectUrl hoặc IPN).
     * Các tham số phải ghép đúng thứ tự a-z như tài liệu MoMo.
     */
    public static boolean verifySignature(Map<String, String> p) {
        String received = p.get("signature");
        if (received == null) {
            return false;
        }
        String rawSignature = "accessKey=" + ACCESS_KEY
                + "&amount=" + p.get("amount")
                + "&extraData=" + p.get("extraData")
                + "&message=" + p.get("message")
                + "&orderId=" + p.get("orderId")
                + "&orderInfo=" + p.get("orderInfo")
                + "&orderType=" + p.get("orderType")
                + "&partnerCode=" + p.get("partnerCode")
                + "&payType=" + p.get("payType")
                + "&requestId=" + p.get("requestId")
                + "&responseTime=" + p.get("responseTime")
                + "&resultCode=" + p.get("resultCode")
                + "&transId=" + p.get("transId");
        return HashUtil.hmacSHA256(SECRET_KEY, rawSignature).equalsIgnoreCase(received);
    }

    // Gửi POST JSON và đọc nội dung trả về (giống cách gọi API trong WeatherService của dự án Bus)
    private static String postJson(String urlString, String body) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    code < 400 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8));
            String response = "";
            String line;
            while ((line = in.readLine()) != null) {
                response += line;
            }
            in.close();
            return response;
        } catch (Exception e) {
            throw new IllegalArgumentException("Không kết nối được MoMo: " + e.getMessage());
        }
    }

    /**
     * Lấy giá trị 1 key trong chuỗi JSON đơn giản (không cần thư viện), vd
     * getJsonValue("{\"resultCode\":0,\"payUrl\":\"https://...\"}", "payUrl") -> "https://..."
     */
    private static String getJsonValue(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) {
            return null;
        }
        i = json.indexOf(":", i) + 1;
        while (json.charAt(i) == ' ') {
            i++;
        }
        if (json.charAt(i) == '"') {
            int end = json.indexOf("\"", i + 1);
            return json.substring(i + 1, end);
        }
        int end = i;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
            end++;
        }
        return json.substring(i, end).trim();
    }
}
