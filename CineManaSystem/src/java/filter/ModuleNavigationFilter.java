package filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Chuẩn bị trạng thái hiển thị menu cho các trang của Khánh.
 * Không kiểm tra/thay thế phân quyền và không lưu trạng thái vào session.
 */
@WebFilter(
        filterName = "khanhModuleNavigation",
        urlPatterns = {
                "/admin/users", "/admin/branches", "/admin/audit-logs",
                "/staff/fnb/*", "/admin/fnb-items", "/staff/promotions/*"
        }
)
public class ModuleNavigationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest input, ServletResponse output, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) input;
        String path = request.getServletPath();
        String feature = null;
        String active = null;

        if (path.startsWith("/staff/fnb/") || "/admin/fnb-items".equals(path)) {
            feature = "fnb";
            active = switch (path) {
                case "/staff/fnb/orders" -> "orders";
                case "/staff/fnb/order-status" -> "status";
                case "/staff/fnb/stock" -> "stock";
                case "/staff/fnb/quantity" -> "quantity";
                default -> "items";
            };
        } else if (path.startsWith("/staff/promotions/")) {
            feature = "promotion";
            active = switch (path) {
                case "/staff/promotions/discounts" -> "discounts";
                case "/staff/promotions/limits" -> "limits";
                case "/staff/promotions/ticket-prices" -> "ticket-prices";
                case "/staff/promotions/fnb-prices" -> "fnb-prices";
                case "/staff/promotions/validate" -> "validate";
                default -> "manage";
            };
        } else {
            switch (path) {
                case "/admin/users" -> {
                    feature = "system";
                    active = "users";
                }
                case "/admin/branches" -> {
                    feature = "system";
                    active = "branches";
                }
                case "/admin/audit-logs" -> {
                    feature = "system";
                    active = "audit";
                }
                default -> {
                    // URL khác không thuộc menu của Khánh.
                }
            }
        }

        if (feature != null) {
            request.setAttribute("khanhFeature", feature);
            request.setAttribute("khanhActive", active);
        }
        chain.doFilter(input, output);
    }
}
