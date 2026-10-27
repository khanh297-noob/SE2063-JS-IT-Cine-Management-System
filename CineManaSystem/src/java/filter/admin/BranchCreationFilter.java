package filter.admin;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import service.admin.UserService;
import util.SessionUtil;

@WebFilter(filterName = "khanhBranchCreationFilter", urlPatterns = "/manage/branch")
public class BranchCreationFilter implements Filter {
    private final UserService users = new UserService();

    @Override
    public void doFilter(ServletRequest input, ServletResponse output, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) input;
        HttpServletResponse response = (HttpServletResponse) output;
        request.setCharacterEncoding("UTF-8");

        if (isCreateRequest(request)) {
            if (SessionUtil.getLoggedInUser(request) == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
            try {
                // Đọc lại quyền ở DB: không phụ thuộc thứ tự chạy giữa hai Filter.
                users.requireCurrentAdmin(SessionUtil.getLoggedInUser(request));
            } catch (SecurityException exception) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Chỉ Administrator được tạo chi nhánh.");
                return;
            } catch (SQLException exception) {
                request.getServletContext().log("Không kiểm tra được quyền tạo chi nhánh", exception);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isCreateRequest(HttpServletRequest request) {
        String rawAction = request.getParameter("action");
        String action = rawAction == null ? "" : rawAction.trim();
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return "create".equals(action);
        }
        if (!"POST".equalsIgnoreCase(request.getMethod()) || "status".equals(action)) {
            return false;
        }
        String rawId = request.getParameter("id");
        try {
            return rawId == null || rawId.isBlank() || Integer.parseInt(rawId.trim()) <= 0;
        } catch (NumberFormatException exception) {
            // BaseServlet hiện coi id sai định dạng là null, dẫn đến nhánh tạo mới.
            return true;
        }
    }
}
