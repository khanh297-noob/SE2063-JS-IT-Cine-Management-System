<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập - CineGo</title>
    <!-- FontAwesome hỗ trợ icon mắt và icon Facebook -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
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
            padding: 20px;
        }

        .login-card {
            background-color: #ffffff;
            width: 100%;
            max-width: 440px;
            padding: 40px 32px;
            border-radius: 12px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
            text-align: center;
        }

        /* Header / Logo */
        .brand-logo {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            margin-bottom: 24px;
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
            margin-bottom: 3px;
        }

        .subtitle {
            font-size: 13px;
            color: #8c8c8c;
            margin-bottom: 5px;
        }

        /* Form Controls */
        .form-group {
            text-align: left;
            margin-bottom: 18px;
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

        .forgot-password {
            font-size: 12px;
            color: #e50914;
            text-decoration: none;
            font-weight: 600;
        }

        .forgot-password:hover {
            text-decoration: underline;
        }

        .input-wrapper {
            position: relative;
            display: flex;
            align-items: center;
        }

        input[type="email"],
        input[type="password"],
        input[type="text"] {
            width: 100%;
            padding: 12px 14px;
            font-size: 14px;
            border: 1px solid #d9d9d9;
            border-radius: 8px;
            outline: none;
            transition: border-color 0.2s;
            color: #333;
        }

        input::placeholder {
            color: #bfbfbf;
        }

        input:focus {
            border-color: #e50914;
        }

        .toggle-password {
            position: absolute;
            right: 14px;
            color: #8c8c8c;
            cursor: pointer;
            font-size: 14px;
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
            margin-top: 10px;
            transition: background-color 0.2s;
            letter-spacing: 0.5px;
        }

        .btn-submit:hover {
            background-color: #c10712;
        }

        /* Divider */
        .divider {
            display: flex;
            align-items: center;
            margin: 24px 0;
        }

        .divider::before,
        .divider::after {
            content: "";
            flex: 1;
            border-bottom: 1px solid #e8e8e8;
        }

        .divider span {
            padding: 0 12px;
            color: #8c8c8c;
            font-size: 12px;
        }

        /* Social Login Buttons */
        .btn-social {
            width: 100%;
            background-color: #ffffff;
            border: 1px solid #d9d9d9;
            padding: 11px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 600;
            color: #4a4a4a;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
            cursor: pointer;
            margin-bottom: 12px;
            transition: background-color 0.2s;
        }

        .btn-social:hover {
            background-color: #f9f9f9;
        }

        .google-icon {
            width: 18px;
            height: 18px;
        }

        .facebook-icon {
            color: #1877f2;
            font-size: 18px;
        }

        /* Guest Option & Register Footer */
        .guest-link {
            display: inline-block;
            margin-top: 12px;
            font-size: 13px;
            font-weight: 600;
            color: #4a4a4a;
            text-decoration: none;
        }

        .guest-link:hover {
            text-decoration: underline;
        }

        .footer-line {
            margin-top: 32px;
            padding-top: 20px;
            border-top: 1px solid #f0f0f0;
            font-size: 13px;
            color: #595959;
        }

        .register-link {
            color: #e50914;
            font-weight: 700;
            text-decoration: none;
        }

        .register-link:hover {
            text-decoration: underline;
        }
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
        .message-success {
            font-size: 14px;
            color: green;
            margin-bottom: 10px;
        }
    </style>
</head>
<body>

    <div class="login-card">
        <!-- Logo Header -->
        <div class="brand-logo">
            <div class="logo-badge">CG</div>
            <div class="logo-text">CineGo</div>
        </div>

        <h1 class="title">Đăng nhập tài khoản</h1>
        <p class="subtitle">Nhập thông tin tài khoản của bạn để tiếp tục</p>
        <c:if test="${not empty sessionScope.verifySuccess}">
            <div class="error-alert" style="background-color: #f6ffed; border-color: #b7eb8f; color: #52c41a;">
                <i class="fa-solid fa-circle-check"></i>
                <span>${sessionScope.verifySuccess}</span>
            </div>
            <c:remove var="verifySuccess" scope="session"/>
        </c:if>

        <c:if test="${not empty sessionScope.loginNotice}">
    <div class="error-alert">
        <i class="fa-solid fa-circle-exclamation"></i>
        <span><c:out value="${sessionScope.loginNotice}"/></span>
    </div>
    <c:remove var="loginNotice" scope="session"/>
</c:if>

<c:if test="${not empty sessionScope.message}">
    <div class="message-success">
        <c:out value="${sessionScope.message}"/>
    </div>
    <c:remove var="message" scope="session"/>
</c:if>

        <c:if test="${not empty error}">
            <div class="error-alert">
                <i class="fa-solid fa-circle-exclamation"></i>
                <div>
                    <span><c:out value="${error}"/></span>
                    <c:if test="${not empty sessionScope.verifyEmail}">
                        <div style="margin-top: 6px;">
                            <a href="verify-email" style="color: #e50914; font-weight: 700; text-decoration: underline;">
                                Nhấp vào đây để xác thực Email ngay &rarr;
                            </a>
                        </div>
                    </c:if>
                </div>
            </div>
        </c:if>
        
        <!-- Form đăng nhập kết nối Servlet -->
        <form action="login" method="post">
            <div class="form-group">
                <div class="label-row">
                    <label for="email">EMAIL <span class="required">*</span></label>
                </div>
                <div class="input-wrapper">
                    <input type="email" id="email" name="email" placeholder="example@domain.com" required value="${requestScope.email}">
                </div>
            </div>

            <div class="form-group">
                <div class="label-row">
                    <label for="password">MẬT KHẨU <span class="required">*</span></label>
                    <a href="${pageContext.request.contextPath}/forgot-password" class="forgot-password">Quên mật khẩu?</a>
                </div>
                <div class="input-wrapper">
                    <input type="password" id="password" name="password" placeholder="••••••••" required>
                    <i class="fa-regular fa-eye toggle-password" id="togglePassword"></i>
                </div>
            </div>

            <button type="submit" class="btn-submit">ĐĂNG NHẬP</button>
        </form>

        <!-- Hoặc -->
        <div class="divider">
            <span>hoặc</span>
        </div>



        <div>
            <a href="${pageContext.request.contextPath}/index.jsp" class="guest-link">Tiếp tục với tư cách Khách (Continue as Guest)</a>
        </div>

        <div class="footer-line">
            Chưa có tài khoản? <a href="register" class="register-link">Đăng ký ngay</a>
        </div>
    </div>

    <!-- JS bật/tắt ẩn hiện mật khẩu -->
    <script>
        const togglePassword = document.querySelector('#togglePassword');
        const password = document.querySelector('#password');

        togglePassword.addEventListener('click', function () {
            const type = password.getAttribute('type') === 'password' ? 'text' : 'password';
            password.setAttribute('type', type);
            this.classList.toggle('fa-eye-slash');
        });
    </script>
</body>
</html>