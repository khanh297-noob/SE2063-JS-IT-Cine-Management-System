<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng ký tài khoản - CineGo</title>
    <!-- FontAwesome hỗ trợ icon mắt và icon alert -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/register.css">
</head>
<body>

    <div class="register-card">
        <!-- Logo Header -->
        <div class="brand-logo">
            <div class="logo-badge">CG</div>
            <div class="logo-text">CineGo</div>
        </div>

        <h1 class="title">Tạo tài khoản thành viên</h1>
        <p class="subtitle">Đăng ký ngay để nhận nhiều ưu đãi và mua vé xem phim nhanh chóng</p>

        <!-- Thông báo lỗi (nếu có) -->
        <c:if test="${not empty error}">
            <div class="error-alert">
                <i class="fa-solid fa-circle-exclamation"></i>
                <span>${error}</span>
            </div>
        </c:if>

        <!-- Form đăng ký -->
        <form action="register" method="post" autocomplete="off">
            <!-- Họ và tên -->
            <div class="form-group">
                <div class="label-row">
                    <label for="fullName">HỌ VÀ TÊN <span class="required">*</span></label>
                </div>
                <div class="input-wrapper">
                    <input type="text" id="fullName" name="fullName" placeholder="Nguyễn Văn A" required
                           value="${requestScope.fullName}" autocomplete="off">
                </div>
            </div>

            <!-- Email -->
            <div class="form-group">
                <div class="label-row">
                    <label for="email">EMAIL <span class="required">*</span></label>
                </div>
                <div class="input-wrapper">
                    <input type="email" id="email" name="email" placeholder="example@domain.com" required
                           value="${requestScope.email}" autocomplete="off">
                </div>
            </div>

            <!-- Số điện thoại -->
            <div class="form-group">
                <div class="label-row">
                    <label for="phone">SỐ ĐIỆN THOẠI <span class="required">*</span></label>
                </div>
                <div class="input-wrapper">
                    <input type="tel" id="phone" name="phone" placeholder="0912345678" required
                           value="${requestScope.phone}" autocomplete="off">
                </div>
            </div>

            <!-- Mật khẩu -->
            <div class="form-group">
                <div class="label-row">
                    <label for="password">MẬT KHẨU <span class="required">*</span></label>
                </div>
                <div class="input-wrapper">
                    <input type="password" id="password" name="password" placeholder="••••••••" required autocomplete="new-password">
                    <i class="fa-regular fa-eye toggle-password" id="togglePassword"></i>
                </div>
            </div>

            <!-- Xác nhận mật khẩu -->
            <div class="form-group">
                <div class="label-row">
                    <label for="confirmPassword">XÁC NHẬN MẬT KHẨU <span class="required">*</span></label>
                </div>
                <div class="input-wrapper">
                    <input type="password" id="confirmPassword" name="confirmPassword" placeholder="••••••••" required autocomplete="new-password">
                    <i class="fa-regular fa-eye toggle-password" id="toggleConfirmPassword"></i>
                </div>
            </div>

            <!-- Nút submit -->
            <button type="submit" class="btn-submit">ĐĂNG KÝ TÀI KHOẢN</button>
        </form>

        <!-- Footer link đăng nhập -->
        <div class="footer-line">
            Đã có tài khoản? <a href="login" class="login-link">Đăng nhập ngay</a>
        </div>
    </div>

    <!-- JS bật/tắt hiển thị mật khẩu -->
    <script>
        function setupToggle(toggleId, inputId) {
            const toggleIcon = document.getElementById(toggleId);
            const inputField = document.getElementById(inputId);

            if (toggleIcon && inputField) {
                toggleIcon.addEventListener('click', function () {
                    const isPassword = inputField.getAttribute('type') === 'password';
                    inputField.setAttribute('type', isPassword ? 'text' : 'password');
                    this.classList.toggle('fa-eye-slash', isPassword);
                    this.classList.toggle('fa-eye', !isPassword);
                });
            }
        }

        setupToggle('togglePassword', 'password');
        setupToggle('toggleConfirmPassword', 'confirmPassword');
    </script>
</body>
</html>
