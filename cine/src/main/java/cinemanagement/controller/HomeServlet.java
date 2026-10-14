package cinemanagement.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Trang chủ quản trị: lối vào tất cả chức năng (chưa có Login nên vào tự do). */
@WebServlet("")
public class HomeServlet extends BaseServlet {

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        render(req, resp, "home");
    }
}
