package filter;

import model.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter(filterName = "AdminAuthFilter", urlPatterns = {"/admin/*"})
public class AdminAuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession();

        if (session.getAttribute("currentUser") == null) {
            User mockAdmin = new User();
            mockAdmin.setUserId(1);
            mockAdmin.setFullName("Nguyễn Văn Admin");
            mockAdmin.setRole("Administrator");
            mockAdmin.setStatus("Active");
            session.setAttribute("currentUser", mockAdmin);
        }

        User currentUser = (User) session.getAttribute("currentUser");

        if (currentUser == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        if (!"Active".equalsIgnoreCase(currentUser.getStatus())) {
            session.invalidate();
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        if (!"Administrator".equalsIgnoreCase(currentUser.getRole())
                && !"Admin".equalsIgnoreCase(currentUser.getRole())) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập khu vực Admin!");
            return;
        }

        chain.doFilter(request, response);
    }
}