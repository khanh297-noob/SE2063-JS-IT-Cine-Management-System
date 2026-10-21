package cinemanagement.controller;

import cinemanagement.util.BusinessException;
import cinemanagement.util.NotFoundException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Lớp cha cho mọi servlet: đọc tham số, forward/redirect, flash message
 * và xử lý lỗi chung.
 *
 * Lớp con override handleGet / handlePost.
 * - NotFoundException       -> 404
 * - BusinessException chưa bắt -> flash lỗi, quay lại trang trước
 * - SQLException            -> 500
 */
public abstract class BaseServlet extends HttpServlet {

    private static final String VIEW_ROOT = "/WEB-INF/views/";

    protected void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException, SQLException, BusinessException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req, resp, true);
    }

    @Override
    protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req, resp, false);
    }

    private void dispatch(HttpServletRequest req, HttpServletResponse resp, boolean get)
            throws ServletException, IOException {
        try {
            if (get) handleGet(req, resp); else handlePost(req, resp);
        } catch (NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (BusinessException e) {
            flashError(req, String.join(" ", e.getErrors()));
            String back = req.getHeader("Referer");
            resp.sendRedirect(back != null ? back : req.getContextPath() + "/");
        } catch (SQLException e) {
            throw new ServletException("Lỗi cơ sở dữ liệu: " + e.getMessage(), e);
        }
    }

    // ---------------- view ----------------

    /** view: đường dẫn tương đối trong /WEB-INF/views, không có ".jsp". Vd "branch/list-hoang". */
    protected void render(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        req.getRequestDispatcher(VIEW_ROOT + view + ".jsp").forward(req, resp);
    }

    /** path bắt đầu bằng "/", không gồm context path. Vd "/manage/branch". */
    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    protected void flashSuccess(HttpServletRequest req, String message) {
        req.getSession().setAttribute("flashSuccess", message);
    }

    protected void flashError(HttpServletRequest req, String message) {
        req.getSession().setAttribute("flashError", message);
    }

    // ---------------- parameters ----------------

    protected String action(HttpServletRequest req, String defaultAction) {
        String a = str(req, "action");
        return a == null ? defaultAction : a;
    }

    /** Chuỗi đã trim, null nếu rỗng. */
    protected String str(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    /** Chuỗi giữ nguyên xuống dòng (cho textarea). */
    protected String text(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null || v.isBlank() ? null : v.strip();
    }

    protected Integer intParam(HttpServletRequest req, String name) {
        String v = str(req, name);
        if (v == null) return null;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Tham số id bắt buộc; thiếu hoặc sai thì 404. */
    protected int requiredId(HttpServletRequest req, String name) {
        Integer id = intParam(req, name);
        if (id == null) throw new NotFoundException("Thiếu hoặc sai tham số " + name);
        return id;
    }

    protected BigDecimal decimalParam(HttpServletRequest req, String name) {
        String v = str(req, name);
        if (v == null) return null;
        try {
            return new BigDecimal(v.replace(",", "").replace(" ", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected LocalDate dateParam(HttpServletRequest req, String name) {
        String v = str(req, name);
        if (v == null) return null;
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    protected LocalTime timeParam(HttpServletRequest req, String name) {
        String v = str(req, name);
        if (v == null) return null;
        try {
            return LocalTime.parse(v.length() > 5 ? v.substring(0, 5) : v);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
