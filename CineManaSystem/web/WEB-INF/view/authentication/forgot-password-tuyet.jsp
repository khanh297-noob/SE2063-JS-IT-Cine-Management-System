
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quên mật khẩu - CineGo</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/forgot-password.css">
</head>

<body>
    <div class="forgot-card">
        <h1>CineGo</h1>

        <p>
            Quên mật khẩu?<br>
            Nhập email đã đăng ký để nhận hướng dẫn đặt lại mật khẩu.
        </p>

        <c:if test="${not empty error}">
            <div class="message error">
                <c:out value="${error}"/>
            </div>
        </c:if>

        <c:if test="${not empty message}">
            <div class="message success">
                <c:out value="${message}"/>
            </div>
        </c:if>

        <form action="${pageContext.request.contextPath}/forgot-password"
              method="post">

            <label for="email">Email đăng ký</label>

            <input type="email"
                   id="email"
                   name="email"
                   placeholder="example@gmail.com"
                   value="<c:out value='${email}'/>"
                   required>

            <button type="submit">GỬI YÊU CẦU</button>
        </form>

        <a href="${pageContext.request.contextPath}/login" class="back">
            Quay lại đăng nhập
        </a>
    </div>
</body>
</html>
