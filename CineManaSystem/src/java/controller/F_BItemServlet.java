package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import model.User;
import service.F_BItemService;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.UUID;

@WebServlet(name = "F_BItemServlet", urlPatterns = {"/admin/fnb-items"})
public class F_BItemServlet extends HttpServlet {

}