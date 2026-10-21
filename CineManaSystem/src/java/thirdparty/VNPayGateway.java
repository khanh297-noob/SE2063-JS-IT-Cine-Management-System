package thirdparty;

import util.HashUtil;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.net.URL;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
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
        SimpleDateFormat fmt = vnTimeFormat();
        // Ngày tạo lấy từ phần thời gian trong txnRef -> lúc tra cứu (queryPayment) tính lại được đúng ngày này
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(getCreateMillis(txnRef));

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

    // ===================== Tra cứu giao dịch (querydr) =====================
    public static final String QUERY_URL = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

    /**
     * Hỏi VNPay trạng thái 1 giao dịch. Dùng cho VNPay QR tại quầy: khách trả trên trang VNPay,
     * máy chủ VNPay không gọi về được localhost nên màn hình quầy tự hỏi VNPay.
     * @return Map: responseCode ("00" tra cứu được, "91" chưa có giao dịch),
     *              transactionStatus ("00" đã thanh toán, "01" chưa xong, "02" lỗi), amount (đã chia 100), transactionNo
     */
    public static Map<String, String> queryPayment(String txnRef, String clientIp) {
        SimpleDateFormat fmt = vnTimeFormat();
        String requestId = "Q" + System.currentTimeMillis();
        String createDate = fmt.format(Calendar.getInstance().getTime());
        String transactionDate = fmt.format(getCreateMillis(txnRef));   // ngày tạo giao dịch gốc
        String orderInfo = "Tra cuu giao dich " + txnRef;

        // Chuỗi ký của querydr: các giá trị nối bằng dấu | theo đúng thứ tự VNPay quy định
        String rawHash = requestId + "|2.1.0|querydr|" + TMN_CODE + "|" + txnRef + "|" + transactionDate
                + "|" + createDate + "|" + clientIp + "|" + orderInfo;
        String body = "{"
                + "\"vnp_RequestId\":\"" + requestId + "\","
                + "\"vnp_Version\":\"2.1.0\","
                + "\"vnp_Command\":\"querydr\","
                + "\"vnp_TmnCode\":\"" + TMN_CODE + "\","
                + "\"vnp_TxnRef\":\"" + txnRef + "\","
                + "\"vnp_OrderInfo\":\"" + orderInfo + "\","
                + "\"vnp_TransactionDate\":\"" + transactionDate + "\","
                + "\"vnp_CreateDate\":\"" + createDate + "\","
                + "\"vnp_IpAddr\":\"" + clientIp + "\","
                + "\"vnp_SecureHash\":\"" + HashUtil.hmacSHA512(HASH_SECRET, rawHash) + "\""
                + "}";
        String json = postJson(QUERY_URL, body);

        Map<String, String> result = new HashMap<>();
        result.put("responseCode", getJsonValue(json, "vnp_ResponseCode"));
        result.put("transactionStatus", getJsonValue(json, "vnp_TransactionStatus"));
        result.put("transactionNo", getJsonValue(json, "vnp_TransactionNo"));
        result.put("message", getJsonValue(json, "vnp_Message"));
        String amount = getJsonValue(json, "vnp_Amount");
        if (amount != null && !amount.isEmpty()) {
            result.put("amount", new BigDecimal(amount).divide(BigDecimal.valueOf(100)).toPlainString());
        }
        return result;
    }

    // Định dạng ngày giờ VNPay: yyyyMMddHHmmss, giờ Việt Nam
    private static SimpleDateFormat vnTimeFormat() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
        fmt.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        return fmt;
    }

    // txnRef dạng "{bookingId}_{millis}" -> lấy millis (thời điểm tạo giao dịch)
    private static long getCreateMillis(String txnRef) {
        try {
            return Long.parseLong(txnRef.substring(txnRef.indexOf('_') + 1));
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    // Gửi POST JSON và đọc nội dung trả về (giống cách gọi API trong WeatherService của dự án Bus)
    private static String postJson(String urlString, String body) {
        try {
            HttpsURLConnection conn = (HttpsURLConnection) new URL(urlString).openConnection();
            conn.setSSLSocketFactory(getSslFactory());
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            String response = "";
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    code < 400 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    response += line;
                }
            }
            return response;
        } catch (Exception e) {
            throw new IllegalArgumentException("Không kết nối được VNPay: " + e.getMessage());
        }
    }

    private static SSLSocketFactory sslFactory;

    /**
     * VNPay sandbox dùng chứng chỉ gốc "Sectigo Public Server Authentication Root R46" mà JDK 17 chưa có sẵn
     * (trình duyệt thì có) -> gọi API bị lỗi "PKIX path building failed".
     * Tin các chứng chỉ mặc định của Java + thêm chứng chỉ gốc trong file WEB-INF/vnpay-root-ca.pem.
     */
    private static synchronized SSLSocketFactory getSslFactory() throws Exception {
        if (sslFactory == null) {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream in = new FileInputStream(System.getProperty("java.home") + "/lib/security/cacerts")) {
                ks.load(in, "changeit".toCharArray());
            }
            try (InputStream in = VNPayGateway.class.getClassLoader().getResourceAsStream("../vnpay-root-ca.pem")) {
                if (in != null) {
                    ks.setCertificateEntry("sectigo-r46", CertificateFactory.getInstance("X.509").generateCertificate(in));
                }
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), null);
            sslFactory = ctx.getSocketFactory();
        }
        return sslFactory;
    }

    // Lấy giá trị 1 key trong chuỗi JSON đơn giản (không cần thư viện)
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
            return json.substring(i + 1, json.indexOf("\"", i + 1));
        }
        int end = i;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
            end++;
        }
        return json.substring(i, end).trim();
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
