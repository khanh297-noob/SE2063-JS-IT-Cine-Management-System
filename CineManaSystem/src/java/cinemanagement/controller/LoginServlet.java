package cinemanagement.controller;

import java.io.IOException;

import cinemanagement.model.User;
import cinemanagement.service.AuthService;
import cinemanagement.util.BusinessException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private AuthService authService;

    @Override
    public void init() throws ServletException {
        authService = new AuthService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");
            response.sendRedirect(request.getContextPath() + authService.homePathFor(user));
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/login-hiep.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String email = request.getParameter("email") == null ? "" : request.getParameter("email").trim();
        String password = request.getParameter("password");
        String next = request.getParameter("next");
        try {
            User user = authService.login(email, password);
            HttpSession old = request.getSession(false);
            if (old != null) {
                old.invalidate();
            }
            HttpSession session = request.getSession(true);
            session.setAttribute("user", user.sessionCopy());
            session.setAttribute("successMsg", "Đăng nhập thành công. (MSG10)");
            if (next != null && next.startsWith("/") && !next.startsWith("//")) {
                response.sendRedirect(request.getContextPath() + next);
            } else {
                response.sendRedirect(request.getContextPath() + authService.homePathFor(user));
            }
        } catch (BusinessException e) {
            request.setAttribute("errorMsg", e.getMessage());
            request.setAttribute("email", email);
            request.getRequestDispatcher("/WEB-INF/views/login-hiep.jsp").forward(request, response);
        } catch (Exception e) {
            request.setAttribute("errorMsg", "Lỗi hệ thống: " + e.getMessage());
            request.setAttribute("email", email);
            request.getRequestDispatcher("/WEB-INF/views/login-hiep.jsp").forward(request, response);
        }
    }
}
