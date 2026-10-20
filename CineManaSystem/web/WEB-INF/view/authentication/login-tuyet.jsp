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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/login.css">
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
            <div class="error-alert success-alert">
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
                        <div class="spacing-top-sm">
                            <a href="verify-email" class="link-accent">
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
            <a href="${pageContext.request.contextPath}/index-tuyet.jsp" class="guest-link">Tiếp tục với tư cách Khách (Continue as Guest)</a>
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