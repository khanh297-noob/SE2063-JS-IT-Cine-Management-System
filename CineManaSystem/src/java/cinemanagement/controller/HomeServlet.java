package cinemanagement.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import cinemanagement.model.Branch;
import cinemanagement.service.HomeService;
import cinemanagement.util.DateTimeHelper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {
    private HomeService homeService;

    @Override
    public void init() throws ServletException {
        homeService = new HomeService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String branchIdRaw = request.getParameter("branchId");
            String dateRaw = request.getParameter("date");
            List<Branch> branches = homeService.getAllBranches();
            Integer requested = (branchIdRaw == null || branchIdRaw.isEmpty()) ? null : Integer.parseInt(branchIdRaw);
            Integer branchId = homeService.resolveBranchId(requested, branches);
            LocalDate date = homeService.resolveDate(DateTimeHelper.parseDate(dateRaw), 6);
            request.setAttribute("branches", branches);
            request.setAttribute("branchId", branchId);
            request.setAttribute("days", homeService.buildDayTabs(7));
            request.setAttribute("selectedDate", date.toString());
            request.setAttribute("movies", homeService.getMovieSchedules(branchId, date));
            request.getRequestDispatcher("/WEB-INF/views/home-hiep.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            request.getSession().setAttribute("errorMsg", "Mã rạp trên đường dẫn không hợp lệ.");
            response.sendRedirect(request.getContextPath() + "/home");
        } catch (Exception e) {
            request.setAttribute("errorMsg", "Hệ thống gặp sự cố khi tải lịch chiếu: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/error-hiep.jsp").forward(request, response);
        }
    }
}
