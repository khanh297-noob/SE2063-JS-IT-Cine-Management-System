<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Xác thực Email tài khoản - CineGo</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/verify-email.css">
</head>
<body>

    <div class="verify-card">
        <!-- Logo -->
        <div class="brand-logo">
            <div class="logo-badge">CG</div>
            <div class="logo-text">CineGo</div>
        </div>

        <div class="icon-circle">
            <i class="fa-regular fa-envelope-open"></i>
        </div>

        <c:set var="displayEmail" value="${not empty email ? email : sessionScope.verifyEmail}"/>

        <h1 class="title">Xác thực tài khoản</h1>
        <p class="subtitle">
            Hệ thống đã gửi mã OTP xác thực (6 số) đến email: <br>
            <span class="email-highlight"><c:out value="${not empty displayEmail ? displayEmail : 'email của bạn'}"/></span>
        </p>

        <c:if test="${not empty sessionScope.registerSuccess}">
            <div class="success-alert">
                <i class="fa-solid fa-circle-check"></i>
                <span><c:out value="${sessionScope.registerSuccess}"/></span>
            </div>
            <c:remove var="registerSuccess" scope="session"/>
        </c:if>

        <c:if test="${not empty error}">
            <div class="error-alert">
                <i class="fa-solid fa-circle-exclamation"></i>
                <span><c:out value="${error}"/></span>
            </div>
        </c:if>

        <c:if test="${not empty sessionScope.resendMessage}">
            <div class="success-alert">
                <i class="fa-solid fa-circle-check"></i>
                <span><c:out value="${sessionScope.resendMessage}"/></span>
            </div>
            <c:remove var="resendMessage" scope="session"/>
        </c:if>

        <c:if test="${not empty sessionScope.resendError}">
            <div class="error-alert">
                <i class="fa-solid fa-circle-exclamation"></i>
                <span><c:out value="${sessionScope.resendError}"/></span>
            </div>
            <c:remove var="resendError" scope="session"/>
        </c:if>

        <!-- Form xác thực OTP -->
        <form action="verify-email" method="post">
            <input type="hidden" name="email" value="<c:out value="${displayEmail}"/>">

            <div class="form-group">
                <label for="otpCode">NHẬP MÃ OTP (6 CHỮ SỐ) *</label>
                <input type="text" id="otpCode" name="otpCode" class="otp-input" maxlength="6" inputmode="numeric" pattern="[0-9]{6}" placeholder="••••••" autofocus required autocomplete="one-time-code">
            </div>

            <button type="submit" class="btn-submit">
                <i class="fa-solid fa-shield-check"></i> XÁC THỰC NGAY
            </button>
        </form>

        <!-- Nút gửi lại mã OTP -->
        <div class="resend-section">
            Chưa nhận được mã? 
            <form action="resend-otp" method="post" class="inline-form">
                <input type="hidden" name="email" value="<c:out value="${displayEmail}"/>">
                <button type="submit" class="btn-link">Gửi lại mã OTP</button>
            </form>
        </div>

        <div class="footer-links">
            <a href="login"><i class="fa-solid fa-arrow-left"></i> Quay lại Đăng nhập</a>
        </div>
    </div>

</body>
</html>
