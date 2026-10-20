package cinemanagement.controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class BaseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        run(true, req, resp);
    }

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        run(false, req, resp);
    }

    private void run(boolean isGet, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (isGet) {
                get(req, resp);
            } else {
                post(req, resp);
            }
        } catch (ServletException | IOException e) {
            throw e;
        } catch (Exception e) {
            getServletContext().log("Unhandled error at " + req.getRequestURI(), e);
            if (!resp.isCommitted()) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                req.setAttribute("errorMsg", "Đã có lỗi xảy ra. Vui lòng thử lại sau. (MSG53)");
                req.setAttribute("errorDetail", e.getClass().getSimpleName() + ": " + e.getMessage());
                req.getRequestDispatcher("/WEB-INF/views/error-hiep.jsp").forward(req, resp);
            }
        }
    }

    protected static String pathInfo(HttpServletRequest req) {
        String p = req.getPathInfo();
        return p == null ? "" : p;
    }
}
