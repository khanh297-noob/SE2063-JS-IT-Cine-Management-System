<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hồ sơ & Xác minh tài khoản - CineGo</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        }

        body {
            background-color: #0f172a;
            color: #f8fafc;
            min-height: 100vh;
            padding-bottom: 40px;
        }

        /* Navbar */
        .navbar {
            background-color: #1e293b;
            padding: 16px 32px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            border-bottom: 1px solid #334155;
        }

        .brand-logo {
            display: flex;
            align-items: center;
            gap: 10px;
            text-decoration: none;
        }

        .logo-badge {
            background-color: #e50914;
            color: #ffffff;
            font-weight: 800;
            font-size: 16px;
            padding: 4px 8px;
            border-radius: 6px;
        }

        .logo-text {
            color: #ffffff;
            font-size: 22px;
            font-weight: 700;
        }

        .nav-actions {
            display: flex;
            align-items: center;
            gap: 16px;
        }

        .btn-nav {
            color: #cbd5e1;
            text-decoration: none;
            font-size: 14px;
            font-weight: 600;
            padding: 8px 14px;
            border-radius: 8px;
            transition: all 0.2s;
        }

        .btn-nav:hover {
            background-color: #334155;
            color: #ffffff;
        }

        .btn-logout {
            background-color: rgba(239, 68, 68, 0.15);
            color: #f87171;
        }

        .btn-logout:hover {
            background-color: #ef4444;
            color: #ffffff;
        }

        /* Container */
        .container {
            max-width: 1000px;
            margin: 36px auto;
            padding: 0 20px;
        }

        .page-header {
            margin-bottom: 28px;
        }

        .page-title {
            font-size: 26px;
            font-weight: 700;
            color: #ffffff;
            margin-bottom: 6px;
        }

        .page-subtitle {
            font-size: 14px;
            color: #94a3b8;
        }

        .grid-layout {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 24px;
        }

        @media (max-width: 768px) {
            .grid-layout {
                grid-template-columns: 1fr;
            }
        }

        .card {
            background-color: #1e293b;
            border-radius: 14px;
            padding: 28px;
            border: 1px solid #334155;
            box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
        }

        .card-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 24px;
            padding-bottom: 16px;
            border-bottom: 1px solid #334155;
        }

        .card-title {
            font-size: 18px;
            font-weight: 700;
            color: #ffffff;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .card-title i {
            color: #e50914;
        }

        /* Status Badge */
        .badge {
            font-size: 12px;
            font-weight: 700;
            padding: 4px 10px;
            border-radius: 20px;
            display: inline-flex;
            align-items: center;
            gap: 6px;
        }

        .badge-verified {
            background-color: rgba(34, 197, 94, 0.15);
            color: #4ade80;
            border: 1px solid rgba(34, 197, 94, 0.3);
        }

        .badge-unverified {
            background-color: rgba(239, 68, 68, 0.15);
            color: #f87171;
            border: 1px solid rgba(239, 68, 68, 0.3);
        }

        /* Form Controls */
        .form-group {
            margin-bottom: 18px;
        }

        label {
            display: block;
            font-size: 12px;
            font-weight: 600;
            color: #94a3b8;
            margin-bottom: 6px;
            letter-spacing: 0.5px;
        }

        input[type="text"],
        input[type="email"],
        input[type="password"] {
            width: 100%;
            background-color: #0f172a;
            border: 1px solid #334155;
            color: #ffffff;
            padding: 12px 14px;
            border-radius: 8px;
            font-size: 14px;
            outline: none;
            transition: all 0.2s;
        }

        input[type="text"]:focus,
        input[type="email"]:focus,
        input[type="password"]:focus {
            border-color: #e50914;
            box-shadow: 0 0 0 2px rgba(229, 9, 20, 0.2);
        }

        input:disabled {
            background-color: #1e293b;
            color: #64748b;
            cursor: not-allowed;
        }

        .btn-primary {
            background-color: #e50914;
            color: #ffffff;
            border: none;
            padding: 12px 20px;
            font-size: 14px;
            font-weight: 700;
            border-radius: 8px;
            cursor: pointer;
            width: 100%;
            transition: all 0.2s;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
        }

        .btn-primary:hover {
            background-color: #c10712;
        }

        /* Alerts */
        .alert {
            padding: 12px 16px;
            border-radius: 8px;
            font-size: 13px;
            margin-bottom: 18px;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .alert-success {
            background-color: rgba(34, 197, 94, 0.15);
            border: 1px solid rgba(34, 197, 94, 0.3);
            color: #4ade80;
        }

        .alert-error {
            background-color: rgba(239, 68, 68, 0.15);
            border: 1px solid rgba(239, 68, 68, 0.3);
            color: #f87171;
        }

        /* Verification Card Banner */
        .verification-banner {
            background: linear-gradient(135deg, rgba(229, 9, 20, 0.15), rgba(30, 41, 59, 0.8));
            border: 1px solid rgba(229, 9, 20, 0.3);
            border-radius: 12px;
            padding: 18px 22px;
            margin-bottom: 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .verification-info h4 {
            font-size: 15px;
            font-weight: 700;
            color: #ffffff;
            margin-bottom: 4px;
        }

        .verification-info p {
            font-size: 13px;
            color: #cbd5e1;
        }
    </style>
</head>
<body>

    <!-- Navbar -->
    <header class="navbar">
        <a href="${pageContext.request.contextPath}/index.jsp" class="brand-logo">
            <div class="logo-badge">CG</div>
            <div class="logo-text">CineGo</div>
        </a>
        <div class="nav-actions">
            <c:if test="${sessionScope.User.role eq 'Administrator' or sessionScope.User.role eq 'Admin' or sessionScope.User.role eq 'Branch Management' or sessionScope.User.role eq 'BranchManager'}">
                <a href="${pageContext.request.contextPath}/report/branch-dashboard" class="btn-nav">
                    <i class="fa-solid fa-chart-line"></i> Dashboard quản lý
                </a>
            </c:if>
            <a href="${pageContext.request.contextPath}/index.jsp" class="btn-nav">
                <i class="fa-solid fa-house"></i> Trang chủ
            </a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-nav btn-logout">
                <i class="fa-solid fa-right-from-bracket"></i> Đăng xuất
            </a>
        </div>
    </header>

    <div class="container">
        <div class="page-header">
            <h1 class="page-title">Thông tin & Xác minh tài khoản</h1>
            <p class="page-subtitle">Xem, cập nhật và bảo mật thông tin tài khoản của bạn</p>
        </div>

        <!-- Trạng thái xác thực Email Banner -->
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
                <a href="${pageContext.request.contextPath}/verify-email?email=${user.email}" class="btn-primary" style="width: auto; padding: 10px 18px;">
                    <i class="fa-solid fa-shield-halved"></i> Xác thực ngay
                </a>
            </c:if>
        </div>

        <div class="grid-layout">
            <!-- Cột 1: Thông tin cá nhân -->
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title"><i class="fa-solid fa-user-pen"></i> Thông tin cá nhân</h2>
                </div>

                <c:if test="${not empty successMessage}">
                    <div class="alert alert-success">
                        <i class="fa-solid fa-circle-check"></i>
                        <span>${successMessage}</span>
                    </div>
                </c:if>

                <c:if test="${not empty errorMessage}">
                    <div class="alert alert-error">
                        <i class="fa-solid fa-circle-exclamation"></i>
                        <span>${errorMessage}</span>
                    </div>
                </c:if>

                <form action="profile" method="post">
                    <div class="form-group">
                        <label for="fullName">HỌ VÀ TÊN</label>
                        <input type="text" id="fullName" name="fullName" value="${user.fullName}" required>
                    </div>

                    <div class="form-group">
                        <label for="email">ĐỊA CHỈ EMAIL (KHÔNG ĐỔI)</label>
                        <input type="email" id="email" value="${user.email}" disabled>
                    </div>

                    <div class="form-group">
                        <label for="phone">SỐ ĐIỆN THOẠI</label>
                        <input type="text" id="phone" name="phone" value="${user.phone}" required pattern="^(0|\+84)[0-9]{9}$">
                    </div>

                    <button type="submit" class="btn-primary">
                        <i class="fa-solid fa-floppy-disk"></i> Lưu thay đổi
                    </button>
                </form>
            </div>

            <!-- Cột 2: Đổi mật khẩu -->
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title"><i class="fa-solid fa-key"></i> Đổi mật khẩu</h2>
                </div>

                <c:if test="${not empty passwordSuccess}">
                    <div class="alert alert-success">
                        <i class="fa-solid fa-circle-check"></i>
                        <span>${passwordSuccess}</span>
                    </div>
                </c:if>

                <c:if test="${not empty passwordError}">
                    <div class="alert alert-error">
                        <i class="fa-solid fa-circle-exclamation"></i>
                        <span>${passwordError}</span>
                    </div>
                </c:if>

                <form action="change-password" method="post">
                    <div class="form-group">
                        <label for="oldPassword">MẬT KHẨU HIỆN TẠI</label>
                        <input type="password" id="oldPassword" name="oldPassword" placeholder="••••••••" required>
                    </div>

                    <div class="form-group">
                        <label for="newPassword">MẬT KHẨU MỚI</label>
                        <input type="password" id="newPassword" name="newPassword" placeholder="Ít nhất 6 ký tự" required minlength="6">
                    </div>

                    <div class="form-group">
                        <label for="confirmPassword">XÁC NHẬN MẬT KHẨU MỚI</label>
                        <input type="password" id="confirmPassword" name="confirmPassword" placeholder="Nhập lại mật khẩu mới" required>
                    </div>

                    <button type="submit" class="btn-primary">
                        <i class="fa-solid fa-lock"></i> Cập nhật mật khẩu
                    </button>
                </form>
            </div>
        </div>
    </div>

</body>
</html>
