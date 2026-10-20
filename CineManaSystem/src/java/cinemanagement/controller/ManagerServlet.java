package cinemanagement.controller;

import java.io.IOException;
import java.time.LocalDate;

import cinemanagement.model.User;
import cinemanagement.service.ShowtimeService;
import cinemanagement.util.BusinessException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/manager")
public class ManagerServlet extends HttpServlet {
    private ShowtimeService showtimeService;

    @Override
    public void init() throws ServletException {
        showtimeService = new ShowtimeService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        try {
            String branchIdRaw = request.getParameter("branchId");
            Integer requested = (branchIdRaw == null || branchIdRaw.isEmpty()) ? null : Integer.parseInt(branchIdRaw);
            int branchId = showtimeService.resolveBranchId(user, requested);
            LocalDate today = LocalDate.now();
            request.setAttribute("branches", user.isAdmin() ? showtimeService.getAllBranches() : null);
            request.setAttribute("branch", showtimeService.getBranch(branchId));
            request.setAttribute("branchId", branchId);
            request.setAttribute("today", today.toString());
            request.setAttribute("todayCount", showtimeService.list(branchId, today, null).size());
            request.getRequestDispatcher("/WEB-INF/views/manager/manager-hiep.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            session.setAttribute("errorMsg", "Mã chi nhánh trên đường dẫn không hợp lệ.");
            response.sendRedirect(request.getContextPath() + "/manager");
        } catch (BusinessException e) {
            request.setAttribute("errorMsg", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error-hiep.jsp").forward(request, response);
        } catch (Exception e) {
            request.setAttribute("errorMsg", "Hệ thống gặp sự cố khi tải chức năng quản lý: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error-hiep.jsp").forward(request, response);
        }
    }
}
