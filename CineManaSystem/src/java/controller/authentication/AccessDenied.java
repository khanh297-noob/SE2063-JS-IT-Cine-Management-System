package controller.authentication;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import util.PermissionUtil;

/**
 * Trang thông báo "không đủ quyền truy cập".
 */
@WebServlet(name = "accessDenied", urlPatterns = {"/access-denied"})
public class AccessDenied extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("currentUser", PermissionUtil.getCurrentUser(request));
        request.getRequestDispatcher("/WEB-INF/view/error/access-denied-tuyet.jsp").forward(request, response);
    }
}
