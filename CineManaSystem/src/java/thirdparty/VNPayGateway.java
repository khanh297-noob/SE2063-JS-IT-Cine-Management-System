package thirdparty;

import util.HashUtil;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * Cổng thanh toán VNPay (môi trường Sandbox).
 * Đăng ký tài khoản test tại https://sandbox.vnpayment.vn/devreg/ rồi điền TMN_CODE và HASH_SECRET.
 * Thẻ test (ngân hàng NCB): 9704198526191432198 - NGUYEN VAN A - 07/15 - OTP 123456
 */
public class VNPayGateway {

    public static final String PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    // Key SANDBOX đọc từ file WEB-INF/vnpay.properties (không đẩy lên GitHub, xem vnpay.properties.example)
    public static final String TMN_CODE;
    public static final String HASH_SECRET;

    static {
        Properties p = new Properties();
        try (InputStream in = VNPayGateway.class.getClassLoader().getResourceAsStream("../vnpay.properties")) {
            if (in != null) {
                p.load(in);
            } else {
                System.out.println("Chưa có file WEB-INF/vnpay.properties - thanh toán VNPay sẽ bị từ chối");
            }
        } catch (IOException e) {
            System.out.println("Không đọc được vnpay.properties: " + e.getMessage());
        }
        TMN_CODE = p.getProperty("vnp_TmnCode", "");
        HASH_SECRET = p.getProperty("vnp_HashSecret", "");
    }

    /**
     * Tạo link chuyển khách sang trang thanh toán VNPay.
     * @param txnRef    mã giao dịch, dạng "{bookingId}_{thời gian}"
     * @param returnUrl link tuyệt đối VNPay chuyển trình duyệt về sau khi thanh toán
     */
    public static String createPaymentUrl(String txnRef, BigDecimal amount, String orderInfo,
            String returnUrl, String clientIp) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
        fmt.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        Calendar cal = Calendar.getInstance();

        // TreeMap tự sắp xếp key theo a-z (VNPay yêu cầu)
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", TMN_CODE);
        params.put("vnp_Amount", amount.multiply(BigDecimal.valueOf(100)).toBigInteger().toString()); // VNPay yêu cầu x100
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", fmt.format(cal.getTime()));
        cal.add(Calendar.MINUTE, 10);
        params.put("vnp_ExpireDate", fmt.format(cal.getTime()));

        String query = buildQuery(params);
        return PAY_URL + "?" + query + "&vnp_SecureHash=" + HashUtil.hmacSHA512(HASH_SECRET, query);
    }

    // Kiểm tra chữ ký các tham số VNPay gửi về (chống giả mạo kết quả)
    public static boolean verifySignature(Map<String, String> params) {
        String received = params.get("vnp_SecureHash");
        if (received == null) {
            return false;
        }
        Map<String, String> sorted = new TreeMap<>();
        for (String key : params.keySet()) {
            if (key.startsWith("vnp_") && !key.equals("vnp_SecureHash") && !key.equals("vnp_SecureHashType")) {
                sorted.put(key, params.get(key));
            }
        }
        return HashUtil.hmacSHA512(HASH_SECRET, buildQuery(sorted)).equalsIgnoreCase(received);
    }

    // key1=value1&key2=value2... (value được URL-encode)
    private static String buildQuery(Map<String, String> sorted) {
        String query = "";
        for (String key : sorted.keySet()) {
            String value = sorted.get(key);
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (!query.isEmpty()) {
                query += "&";
            }
            query += URLEncoder.encode(key, StandardCharsets.US_ASCII) + "="
                    + URLEncoder.encode(value, StandardCharsets.US_ASCII);
        }
        return query;
    }
}
