package cinemanagement.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cinemanagement.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public final class Web {
    public static final String SESSION_USER = "user";

    private Web() {
    }

    public static User currentUser(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (User) s.getAttribute(SESSION_USER);
    }

    public static Integer intParam(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static int intParam(HttpServletRequest req, String name, int def) {
        Integer v = intParam(req, name);
        return v == null ? def : v;
    }

    public static List<Integer> intParams(HttpServletRequest req, String name) {
        String[] values = req.getParameterValues(name);
        Set<Integer> result = new LinkedHashSet<>();
        if (values != null) {
            for (String v : values) {
                try {
                    result.add(Integer.valueOf(v.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return new ArrayList<>(result);
    }

    public static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    public static void flash(HttpServletRequest req, String type, String message) {
        req.getSession().setAttribute("error".equals(type) ? "errorMsg" : "successMsg", message);
    }

    public static void forward(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/" + view + "-hiep.jsp").forward(req, resp);
    }

    public static void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    public static String clientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        String ip = (xf != null && !xf.isBlank()) ? xf.split(",")[0].trim() : req.getRemoteAddr();
        return ip.length() > 50 ? ip.substring(0, 50) : ip;
    }

    public static void json(HttpServletResponse resp, String json) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(json);
    }

    public static String js(String s) {
        if (s == null) {
            return "null";
        }
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
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
