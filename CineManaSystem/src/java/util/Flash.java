package util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Thông báo hiển thị một lần sau khi redirect (mẫu Post-Redirect-Get).
 * Servlet POST gọi success() hoặc error() rồi redirect; servlet GET gọi moveToRequest() để JSP đọc
 * flashMessage, flashError, sau đó thông báo bị xóa khỏi session.
 */
public class Flash {

    private static final String MESSAGE = "flashMessage";
    private static final String ERROR = "flashError";

    public static void success(HttpServletRequest request, String message) {
        request.getSession().setAttribute(MESSAGE, message);
    }

    public static void error(HttpServletRequest request, String message) {
        request.getSession().setAttribute(ERROR, message);
    }

    /** Chuyển thông báo từ session sang request (và xóa khỏi session). */
    public static void moveToRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        for (String key : new String[]{MESSAGE, ERROR}) {
            Object value = session.getAttribute(key);
            if (value != null) {
                request.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }
}
