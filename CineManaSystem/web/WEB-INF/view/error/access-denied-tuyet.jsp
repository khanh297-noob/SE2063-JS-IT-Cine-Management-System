<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Không có quyền truy cập - CineGo</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/access-denied.css">
</head>
<body>

<div class="error-card">

    <div class="error-icon-wrapper">
        <i class="fas fa-shield-halved"></i>
    </div>

    <div class="error-code">403</div>
    <h1 class="error-title">Không có quyền truy cập</h1>
    <p class="error-description">
        Tài khoản của bạn không có đủ quyền để xem trang này.<br>
        Vui lòng liên hệ quản trị viên nếu bạn cho rằng đây là nhầm lẫn.
    </p>

    <!-- Hiển thị thông tin user đang đăng nhập -->
    <c:if test="${not empty currentUser}">
        <div class="user-badge">
            <div class="user-badge-avatar">
                ${fn:substring(currentUser.fullName, 0, 1)}
            </div>
            <div class="user-badge-info">
                <div class="user-badge-name">${currentUser.fullName}</div>
                <div class="user-badge-role">${currentUser.role}</div>
            </div>
        </div>
    </c:if>

    <div class="btn-group">
        <a href="${pageContext.request.contextPath}/" class="btn btn-primary" id="btnGoHome">
            <i class="fas fa-house"></i>
            Về trang chủ
        </a>
        <a href="javascript:history.back()" class="btn btn-secondary" id="btnGoBack">
            <i class="fas fa-arrow-left"></i>
            Quay lại
        </a>
    </div>

    <hr class="divider">

    <a href="${pageContext.request.contextPath}/logout" class="logout-link" id="linkLogout">
        <i class="fas fa-right-from-bracket"></i>
        Đăng xuất và đăng nhập tài khoản khác
    </a>

</div>

</body>
</html>
