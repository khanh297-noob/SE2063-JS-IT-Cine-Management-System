<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%-- Trang chủ đã chuyển sang servlet /home (lấy phim từ database).
     Giữ file này để các link cũ tới /index-tuyet.jsp vẫn vào đúng trang chủ. --%>
<% response.sendRedirect(request.getContextPath() + "/home"); %>
