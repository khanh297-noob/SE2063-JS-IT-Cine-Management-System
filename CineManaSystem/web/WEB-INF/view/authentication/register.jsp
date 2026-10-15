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
    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        }

        body {
            background-color: #f4f6f9;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            padding: 30px 20px;
        }

        .register-card {
            background-color: #ffffff;
            width: 100%;
            max-width: 480px;
            padding: 40px 36px;
            border-radius: 14px;
            box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
            text-align: center;
        }

        /* Header / Logo */
        .brand-logo {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            margin-bottom: 20px;
        }

        .logo-badge {
            background-color: #e50914;
            color: #ffffff;
            font-weight: 800;
            font-size: 16px;
            padding: 4px 8px;
            border-radius: 6px;
            letter-spacing: 0.5px;
        }

        .logo-text {
            color: #e50914;
            font-size: 24px;
            font-weight: 700;
        }

        .title {
            font-size: 22px;
            font-weight: 700;
            color: #1a1a1a;
            margin-bottom: 6px;
        }

        .subtitle {
            font-size: 13px;
            color: #8c8c8c;
            margin-bottom: 24px;
            line-height: 1.4;
        }

        /* Form Controls */
        .form-group {
            text-align: left;
            margin-bottom: 16px;
        }

        .label-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 6px;
        }

        label {
            font-size: 11px;
            font-weight: 700;
            color: #4a4a4a;
            letter-spacing: 0.5px;
        }

        .required {
            color: #e50914;
        }

        .input-wrapper {
            position: relative;
            display: flex;
            align-items: center;
        }

        input[type="email"],
        input[type="password"],
        input[type="text"],
        input[type="tel"] {
            width: 100%;
            padding: 12px 14px;
            font-size: 14px;
            border: 1px solid #d9d9d9;
            border-radius: 8px;
            outline: none;
            transition: border-color 0.2s, box-shadow 0.2s;
            color: #333;
        }

        input::placeholder {
            color: #bfbfbf;
        }

        input:focus {
            border-color: #e50914;
            box-shadow: 0 0 0 2px rgba(229, 9, 20, 0.1);
        }

        .toggle-password {
            position: absolute;
            right: 14px;
            color: #8c8c8c;
            cursor: pointer;
            font-size: 14px;
            user-select: none;
        }

        .toggle-password:hover {
            color: #333;
        }

        /* Main Submit Button */
        .btn-submit {
            width: 100%;
            background-color: #e50914;
            color: #ffffff;
            border: none;
            padding: 14px;
            font-size: 14px;
            font-weight: 700;
            border-radius: 25px;
            cursor: pointer;
            margin-top: 14px;
            transition: background-color 0.2s, transform 0.1s;
            letter-spacing: 0.5px;
        }

        .btn-submit:hover {
            background-color: #c10712;
        }

        .btn-submit:active {
            transform: scale(0.99);
        }

        /* Footer */
        .footer-line {
            margin-top: 24px;
            font-size: 13px;
            color: #595959;
        }

        .login-link {
            color: #e50914;
            font-weight: 700;
            text-decoration: none;
        }

        .login-link:hover {
            text-decoration: underline;
        }

        /* Alert Box */
        .error-alert {
            background-color: #fff2f0;
            border: 1px solid #ffccc7;
            color: #ff4d4f;
            padding: 10px 14px;
            border-radius: 8px;
            font-size: 13px;
            margin-bottom: 18px;
            text-align: left;
            display: flex;
            align-items: center;
            gap: 8px;
        }
    </style>
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
