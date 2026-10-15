<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Xác thực Email tài khoản - CineGo</title>
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
            padding: 20px;
        }

        .verify-card {
            background-color: #ffffff;
            width: 100%;
            max-width: 460px;
            padding: 40px 32px;
            border-radius: 14px;
            box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
            text-align: center;
        }

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

        .icon-circle {
            width: 64px;
            height: 64px;
            background-color: #fff1f0;
            color: #e50914;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 28px;
            margin: 0 auto 16px;
        }

        .title {
            font-size: 22px;
            font-weight: 700;
            color: #1a1a1a;
            margin-bottom: 8px;
        }

        .subtitle {
            font-size: 13px;
            color: #595959;
            line-height: 1.5;
            margin-bottom: 24px;
        }

        .email-highlight {
            font-weight: 700;
            color: #e50914;
            background: #fff1f0;
            padding: 2px 8px;
            border-radius: 4px;
            display: inline-block;
            word-break: break-all;
        }

        .demo-otp-banner {
            background-color: #f6ffed;
            border: 1px solid #b7eb8f;
            color: #389e0d;
            padding: 10px 14px;
            border-radius: 8px;
            font-size: 13px;
            margin-bottom: 20px;
            text-align: left;
            display: flex;
            align-items: center;
            gap: 8px;
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

        .success-alert {
            background-color: #f6ffed;
            border: 1px solid #b7eb8f;
            color: #52c41a;
            padding: 10px 14px;
            border-radius: 8px;
            font-size: 13px;
            margin-bottom: 18px;
            text-align: left;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .form-group {
            text-align: left;
            margin-bottom: 20px;
        }

        label {
            display: block;
            font-size: 11px;
            font-weight: 700;
            color: #4a4a4a;
            letter-spacing: 0.5px;
            margin-bottom: 8px;
        }

        .otp-input {
            width: 100%;
            padding: 14px;
            font-size: 24px;
            font-weight: 700;
            letter-spacing: 8px;
            text-align: center;
            border: 2px solid #d9d9d9;
            border-radius: 10px;
            outline: none;
            transition: all 0.2s;
            color: #1a1a1a;
        }

        .otp-input:focus {
            border-color: #e50914;
            box-shadow: 0 0 0 3px rgba(229, 9, 20, 0.1);
        }

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
            transition: background-color 0.2s;
            letter-spacing: 0.5px;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
        }

        .btn-submit:hover {
            background-color: #c10712;
        }

        .resend-section {
            margin-top: 24px;
            font-size: 13px;
            color: #595959;
        }

        .btn-link {
            background: none;
            border: none;
            color: #e50914;
            font-weight: 700;
            cursor: pointer;
            padding: 0;
            font-size: 13px;
            text-decoration: underline;
        }

        .btn-link:hover {
            color: #c10712;
        }

        .footer-links {
            margin-top: 24px;
            padding-top: 18px;
            border-top: 1px solid #f0f0f0;
            font-size: 13px;
        }

        .footer-links a {
            color: #595959;
            text-decoration: none;
        }

        .footer-links a:hover {
            color: #e50914;
            text-decoration: underline;
        }
    </style>
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
            <form action="resend-otp" method="post" style="display: inline;">
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
