
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quên mật khẩu - CineGo</title>

    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: Arial, sans-serif;
        }

        body {
            background: #f4f6f9;
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
        }

        .forgot-card {
            background: white;
            width: 100%;
            max-width: 420px;
            padding: 35px;
            border-radius: 12px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
        }

        h1 {
            color: #e50914;
            text-align: center;
            margin-bottom: 12px;
        }

        p {
            color: #666;
            text-align: center;
            font-size: 14px;
            line-height: 1.6;
            margin-bottom: 24px;
        }

        label {
            display: block;
            font-size: 13px;
            font-weight: bold;
            margin-bottom: 8px;
        }

        input {
            width: 100%;
            padding: 12px;
            border: 1px solid #ddd;
            border-radius: 8px;
            margin-bottom: 18px;
        }

        button {
            width: 100%;
            padding: 13px;
            background: #e50914;
            color: white;
            border: none;
            border-radius: 25px;
            font-weight: bold;
            cursor: pointer;
        }

        button:hover {
            background: #c10712;
        }

        .message {
            margin-bottom: 15px;
            font-size: 14px;
            text-align: center;
        }

        .error {
            color: #e50914;
        }

        .success {
            color: #198754;
        }

        .back {
            display: block;
            margin-top: 20px;
            text-align: center;
            color: #e50914;
            text-decoration: none;
            font-size: 14px;
        }
    </style>
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
