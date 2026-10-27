package controller.navigation;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import model.User;
import service.common.AccessService;
import util.SessionUtil;

@WebServlet(urlPatterns={"/admin/system","/staff/fnb","/staff/promotions"})
public class FeatureMenuServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request,HttpServletResponse response) throws ServletException,IOException {
        try {
            User actor=SessionUtil.getLoggedInUser(request);
            String view;
            switch(request.getServletPath()) {
                case "/admin/system": AccessService.admin(actor);
                    view="/WEB-INF/view/admin/system-menu-khanh.jsp";
                    break;
                case "/staff/fnb": AccessService.fnb(actor);
                    view="/WEB-INF/view/fnb/fnb-menu-khanh.jsp";
                    break;
                case "/staff/promotions": AccessService.promotion(actor);
                    view="/WEB-INF/view/promotion/promotion-menu-khanh.jsp";
                    break;
                default:response.sendError(404);return;
            }
            request.getRequestDispatcher(view).forward(request,response);
        } catch(SecurityException e) {
            response.sendRedirect(request.getContextPath()+"/access-denied");
        }
    }
}