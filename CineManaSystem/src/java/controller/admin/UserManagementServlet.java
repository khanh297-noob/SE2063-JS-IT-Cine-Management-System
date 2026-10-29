package controller.admin;

import service.admin.BranchService;
import service.admin.UserService;
import util.FormUtil;
import util.SessionUtil;
import model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name="UserManagementServlet",urlPatterns="/admin/users")
public class UserManagementServlet extends HttpServlet {
    private final UserService userService=new UserService();
    private final BranchService branchService=new BranchService();
    @Override
    protected void doGet(HttpServletRequest request,HttpServletResponse response)
            throws ServletException,IOException {
        try {
            User actor=SessionUtil.getLoggedInUser(request);
            String keyword = request.getParameter("q");
            request.setAttribute("userList", userService.searchUsers(actor, keyword));
            request.setAttribute("q", keyword == null ? "" : keyword.trim());
            request.setAttribute("branchList",branchService.getAllBranches(actor));
            FormUtil.csrf(request);
            String view = "create".equals(request.getParameter("action")) ? "/WEB-INF/view/admin/employee-create-khanh.jsp" : "/WEB-INF/view/admin/user-management-khanh.jsp";
            request.getRequestDispatcher(view)
                    .forward(request,response);
        } catch(SecurityException e) {
            response.sendRedirect(request.getContextPath()+"/access-denied");
        } catch (IllegalArgumentException e) {
            request.getSession().setAttribute("toastError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/admin/users");
        } catch(SQLException e) {
            getServletContext().log("Không đọc được user",e);response.sendError(500);
        }
    }
    @Override
    protected void doPost(HttpServletRequest request,HttpServletResponse response)
            throws IOException {
        try {
            FormUtil.checkCsrf(request);
            User actor=SessionUtil.getLoggedInUser(request);
            String action = request.getParameter("action");
            if ("createEmployee".equals(action)) {
                int id = userService.createEmployee(
                        actor, request.getParameter("fullName"),
                        request.getParameter("email"), request.getParameter("phone"),
                        request.getParameter("password"), request.getParameter("role"),
                        FormUtil.integer(request, "branchId"), request.getRemoteAddr());
                request.getSession().setAttribute("toastSuccess", "Đã tạo nhân viên #" + id + ".");
                response.sendRedirect(request.getContextPath() + "/admin/users");
                return;
            }
            int id = FormUtil.integer(request, "id");
            if("toggleStatus".equals(action)) {
                userService.changeUserStatus(actor,id,request.getParameter("targetStatus"), request.getRemoteAddr());
            } else if("updateRole".equals(action)) {
                String raw=request.getParameter("branchId");
                Integer branch=raw==null || raw.isBlank() ? null : FormUtil.integer(request,"branchId");
                userService.changeUserRole(actor,id,request.getParameter("newRole"), branch,request.getRemoteAddr());
            } else throw new IllegalArgumentException("Thao tác không hợp lệ.");

            request.getSession().setAttribute("toastSuccess","Đã cập nhật tài khoản.");
        } catch(SecurityException e) {
            response.sendRedirect(request.getContextPath()+"/access-denied");return;
        } catch(IllegalArgumentException e) {
            request.getSession().setAttribute("toastError",e.getMessage());
        } catch(SQLException e) {
            getServletContext().log("Không cập nhật được user",e);
            request.getSession().setAttribute("toastError","Không lưu được dữ liệu.");
        }
        response.sendRedirect(request.getContextPath()+"/admin/users");
    }
}