<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Promotion &amp; Pricing - CineGo Management</title>
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
        <%@ include file="/WEB-INF/view/report/fragments/manager-shell-style-tuyet.jsp" %>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/promotion-pricing.css">
    </head>
    <body>
        <%@ include file="/WEB-INF/view/report/fragments/manager-navbar-tuyet.jsp" %>
        <div class="layout">
            <%@ include file="/WEB-INF/view/report/fragments/manager-sidebar-tuyet.jsp" %>
            <main class="content main-content">

                <%-- Trang tổng hợp feature Promotion & Pricing: mỗi chức năng chi tiết là một trang riêng --%>
                <a class="back-link" href="${manageHomeUrl}"><i class="fa-solid fa-arrow-left"></i> Về trang chủ quản trị</a>
                <div class="page-header">
                    <div>
                        <h1 class="page-title"><i class="fa-solid fa-tags"></i> Promotion &amp; Pricing</h1>
                        <p class="page-subtitle">Chọn một chức năng để làm việc. Bạn chỉ thấy các chức năng thuộc vai trò của mình (${roleLabel}).</p>
                    </div>
                </div>

                <div class="func-grid">
                    <c:if test="${canManagePricing}">
                        <a class="func-card" href="${pageContext.request.contextPath}/promotion-price-manage/ticket-pricing">
                            <span class="func-icon"><i class="fa-solid fa-ticket"></i></span>
                            <h2>Quản lý giá vé</h2>
                            <p>Đặt giá gốc cho từng suất chiếu hoặc hàng loạt, xem phụ thu theo loại phòng và loại ghế.</p>
                            <span class="func-roles">Admin, Quản lý chi nhánh</span>
                            <span class="func-cta">Mở chức năng <i class="fa-solid fa-arrow-right"></i></span>
                        </a>
                        <a class="func-card" href="${pageContext.request.contextPath}/promotion-price-manage/fnb-pricing">
                            <span class="func-icon"><i class="fa-solid fa-burger"></i></span>
                            <h2>Quản lý giá F&amp;B</h2>
                            <p>Đặt giá riêng và trạng thái bán của từng món, combo tại chi nhánh.</p>
                            <span class="func-roles">Admin, Quản lý chi nhánh</span>
                            <span class="func-cta">Mở chức năng <i class="fa-solid fa-arrow-right"></i></span>
                        </a>
                    </c:if>
                    <c:if test="${canValidatePromotion}">
                        <a class="func-card" href="${pageContext.request.contextPath}/promotion-price-manage/validate-promotion">
                            <span class="func-icon"><i class="fa-solid fa-circle-check"></i></span>
                            <h2>Kiểm tra mã khuyến mãi</h2>
                            <p>Kiểm tra mã có áp dụng được cho đơn hàng không và số tiền được giảm.</p>
                            <span class="func-roles">Thu ngân POS, Quản lý chi nhánh</span>
                            <span class="func-cta">Mở chức năng <i class="fa-solid fa-arrow-right"></i></span>
                        </a>
                    </c:if>
                </div>

            </main>
        </div>
    </body>
</html>
