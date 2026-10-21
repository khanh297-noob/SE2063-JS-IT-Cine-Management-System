package util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Tiện ích web cho phần Đặt vé online và Quản lý suất chiếu: tham số nhiều giá trị, IP, trả JSON. */
public final class WebUtil {

    private WebUtil() {
    }

    /** Các giá trị số của tham số lặp lại (vd seatIds=1&seatIds=2), bỏ giá trị sai và trùng. */
    public static List<Integer> intParams(HttpServletRequest req, String name) {
        String[] values = req.getParameterValues(name);
        Set<Integer> result = new LinkedHashSet<>();
        if (values != null) {
            for (String v : values) {
                Integer i = ParamUtil.parseIntOrNull(v == null ? null : v.trim());
                if (i != null) result.add(i);
            }
        }
        return new ArrayList<>(result);
    }

    /** IP khách (ghi AuditLog). IPv6 localhost đổi về 127.0.0.1. */
    public static String clientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        String ip = (xf != null && !xf.isBlank()) ? xf.split(",")[0].trim() : req.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ip)) ip = "127.0.0.1";
        return ip.length() > 50 ? ip.substring(0, 50) : ip;
    }

    public static void json(HttpServletResponse resp, String json) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(json);
    }

    /** Chuỗi JSON đã escape (kể cả dấu < để nhúng an toàn trong HTML). */
    public static String js(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '<' -> sb.append("\\u003c");
                default -> {
                    if (ch < 0x20) sb.append(String.format("\\u%04x", (int) ch));
                    else sb.append(ch);
                }
            }
        }
        return sb.append('"').toString();
    }
}
