package util;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public final class FormUtil {
    private FormUtil() {}

    public static void csrf(HttpServletRequest r) {
        if (r.getSession().getAttribute("khanhCsrf") == null)
            r.getSession().setAttribute("khanhCsrf", UUID.randomUUID().toString());
    }

    public static void checkCsrf(HttpServletRequest r) {
        Object token = r.getSession().getAttribute("khanhCsrf");
        if (token == null || !token.equals(r.getParameter("csrfToken")))
            throw new SecurityException("Yêu cầu không hợp lệ, hãy tải lại trang.");
    }

    public static int integer(HttpServletRequest r, String name) {
        try { return Integer.parseInt(r.getParameter(name)); }
        catch (RuntimeException e) {
            throw new IllegalArgumentException(name + " phải là số nguyên.");
        }
    }

    public static BigDecimal money(HttpServletRequest r, String name) {
        try { return new BigDecimal(r.getParameter(name)); }
        catch (RuntimeException e) {
            throw new IllegalArgumentException(name + " phải là số tiền.");
        }
    }

    public static BigDecimal amount(BigDecimal value, boolean allowZero) {
        if (value == null || value.signum() < 0
                || (!allowZero && value.signum() == 0))
            throw new IllegalArgumentException("Số tiền không hợp lệ.");
        try {
            BigDecimal n = value.setScale(2, RoundingMode.UNNECESSARY);
            if (n.precision() > 12)
                throw new IllegalArgumentException("Số tiền vượt DECIMAL(12,2).");
            return n;
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Tối đa 2 chữ số thập phân.");
        }
    }
}