package cinemanagement.controller.home;

import cinemanagement.controller.BaseServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Trang chủ quản trị. Trang "/" (chọn giao diện) là web/index.html dùng chung của nhóm.
 * GET /manage  trang chủ quản trị: lối vào tất cả chức năng quản lý (chưa có Login nên vào tự do)
 */
@WebServlet("/manage")
public class HomeServlet extends BaseServlet {

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        render(req, resp, "home-hoang");
    }
}
