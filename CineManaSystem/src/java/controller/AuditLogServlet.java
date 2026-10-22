package controller;
import service.AuditLogService;
import util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name="AuditLogServlet",urlPatterns="/admin/audit-logs")
public class AuditLogServlet extends HttpServlet {
    private final AuditLogService auditLogService=new AuditLogService();
    @Override
    protected void doGet(HttpServletRequest request,HttpServletResponse response)
            throws ServletException,IOException {
        try {
            request.setAttribute("logList",
                    auditLogService.getAllLogs(SessionUtil.getLoggedInUser(request)));
            request.getRequestDispatcher("/WEB-INF/view/admin/audit-log-khanh.jsp")
                    .forward(request,response);
        } catch(SecurityException e) {
            response.sendRedirect(request.getContextPath()+"/access-denied");
        } catch(SQLException e) {
            getServletContext().log("Không đọc được audit log",e);
            response.sendError(500);
        }
    }
}