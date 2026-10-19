package controller;

import model.AuditLog;
import service.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "AuditLogServlet", urlPatterns = {"/admin/audit-logs"})
public class AuditLogServlet extends HttpServlet {
    private final AuditLogService auditLogService = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        List<AuditLog> logList = auditLogService.getAllLogs();
        request.setAttribute("logList", logList);

        request.getRequestDispatcher("/WEB-INF/views/admin/audit-log-khanh.jsp").forward(request, response);
    }
}