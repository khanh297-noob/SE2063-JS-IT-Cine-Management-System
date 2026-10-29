package controller.admin;

import service.admin.BranchService;
import util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name="BranchManagementServlet",urlPatterns="/admin/branches")
public class BranchManagementServlet extends HttpServlet {
    private final BranchService branchService=new BranchService();
    @Override
    protected void doGet(HttpServletRequest request,HttpServletResponse response)
            throws ServletException,IOException {
        try {
            request.setAttribute("branchList",
                    branchService.getAllBranches(SessionUtil.getLoggedInUser(request)));
            request.getRequestDispatcher("/WEB-INF/view/admin/branch-management-khanh.jsp")
                    .forward(request,response);
        } catch(SecurityException e) {
            response.sendRedirect(request.getContextPath()+"/access-denied");
        } catch(SQLException e) {
            getServletContext().log("Không đọc được chi nhánh",e);
            response.sendError(500);
        }
    }
}
