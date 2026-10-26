<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thông tin cá nhân - CineGo</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="${ctx}/assets/css/profile.css">
</head>
<body>

    <!-- Navbar -->
    <header class="navbar">
        <a href="${ctx}/home" class="brand-logo">
            <div class="logo-badge">CG</div>
            <div class="logo-text">CineGo</div>
        </a>
        <div class="nav-actions">
            <c:if test="${sessionScope.User.role eq 'Administrator' or sessionScope.User.role eq 'Admin' or sessionScope.User.role eq 'Branch Management' or sessionScope.User.role eq 'BranchManager'}">
                <a href="${ctx}/report/branch-dashboard" class="btn-nav">
                    <i class="fa-solid fa-chart-line"></i> Dashboard quản lý
                </a>
            </c:if>
            <a href="${ctx}/home" class="btn-nav">
                <i class="fa-solid fa-house"></i> Trang chủ
            </a>
            <a href="${ctx}/customer/change-password" class="btn-nav">
                <i class="fa-solid fa-key"></i> Đổi mật khẩu
            </a>
            <c:if test="${sessionScope.User.role eq 'Customer'}">
                <a href="${ctx}/customer/bookings" class="btn-nav">
                    <i class="fa-solid fa-ticket"></i> Vé của tôi
                </a>
                <a href="${ctx}/customer/deactivate" class="btn-nav">
                    <i class="fa-solid fa-user-slash"></i> Vô hiệu hóa tài khoản
                </a>
            </c:if>
            <a href="${ctx}/logout" class="btn-nav btn-logout">
                <i class="fa-solid fa-right-from-bracket"></i> Đăng xuất
            </a>
        </div>
    </header>

    <div class="container">
        <div class="page-header">
            <h1 class="page-title">Thông tin cá nhân</h1>
            <p class="page-subtitle">Xem và cập nhật thông tin tài khoản của bạn</p>
        </div>

        <!-- Trạng thái tài khoản -->
        <div class="verification-banner">
            <div class="verification-info">
                <h4>Trạng thái tài khoản:
                    <c:choose>
                        <c:when test="${user.status eq 'Active'}">
                            <span class="badge badge-verified"><i class="fa-solid fa-circle-check"></i> Đã kích hoạt & Xác thực</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge badge-unverified"><i class="fa-solid fa-circle-xmark"></i> Chưa kích hoạt</span>
                        </c:otherwise>
                    </c:choose>
                </h4>
                <p>Vai trò hệ thống: <strong>${user.role}</strong> | Mã định danh User: <strong>#${user.userID}</strong></p>
            </div>
            <c:if test="${user.status ne 'Active'}">
                <a href="${ctx}/verify-email?email=${fn:escapeXml(user.email)}" class="btn-primary btn-auto">
                    <i class="fa-solid fa-shield-halved"></i> Xác thực ngay
                </a>
            </c:if>
        </div>

        <div class="card">
            <div class="card-header">
                <h2 class="card-title"><i class="fa-solid fa-user-pen"></i> Thông tin cá nhân</h2>
            </div>

            <c:if test="${not empty successMessage}">
                <div class="alert alert-success">
                    <i class="fa-solid fa-circle-check"></i>
                    <span><c:out value="${successMessage}"/></span>
                </div>
            </c:if>

            <c:if test="${not empty errorMessage}">
                <div class="alert alert-error">
                    <i class="fa-solid fa-circle-exclamation"></i>
                    <span><c:out value="${errorMessage}"/></span>
                </div>
            </c:if>

            <form action="${ctx}/customer/profile" method="post">
                <div class="form-group">
                    <label for="fullName">HỌ VÀ TÊN</label>
                    <input type="text" id="fullName" name="fullName" value="${fn:escapeXml(user.fullName)}" maxlength="100" required>
                </div>

                <div class="form-group">
                    <label for="email">ĐỊA CHỈ EMAIL (KHÔNG ĐỔI)</label>
                    <input type="email" id="email" value="${fn:escapeXml(user.email)}" disabled>
                </div>

                <div class="form-group">
                    <label for="phone">SỐ ĐIỆN THOẠI</label>
                    <input type="text" id="phone" name="phone" value="${fn:escapeXml(user.phone)}" required pattern="^(0|\+84)[0-9]{9}$" title="Số điện thoại bắt đầu bằng 0 hoặc +84, gồm 10 chữ số">
                </div>

                <button type="submit" class="btn-primary">
                    <i class="fa-solid fa-floppy-disk"></i> Lưu thay đổi
                </button>
            </form>
        </div>
    </div>

</body>
</html>
