package util;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Tạo chữ ký HMAC-SHA512 dùng cho cổng thanh toán VNPay.
 */
public class HashUtil {

    public static String hmacSHA512(String key, String data) {
        return hmac("HmacSHA512", key, data);
    }

    // Tính HMAC rồi đổi mảng byte sang chuỗi hex (mỗi byte = 2 ký tự)
    private static String hmac(String algorithm, String key, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String hex = "";
            for (byte b : bytes) {
                hex += String.format("%02x", b & 0xff);
            }
            return hex;
        } catch (Exception e) {
            throw new RuntimeException("Không tạo được chữ ký " + algorithm, e);
        }
    }
}
