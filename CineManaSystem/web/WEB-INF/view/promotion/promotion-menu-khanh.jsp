<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Promotion & Pricing</title>
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin-style.css">
    </head>
    <body>
        <jsp:include page="/WEB-INF/view/common/sidebar-khanh.jsp"/>
        <div class="main-wrapper">
            <jsp:include page="/WEB-INF/view/common/header-khanh.jsp"/>
            <main class="content-body">
                <h1 class="mb-3">Promotion & Pricing</h1>
                <p class="text-muted mb-4">Chọn chức năng cần thực hiện.</p>
                <div class="khanh-feature-grid">
                    <c:if test="${sessionScope.User.role eq 'Administrator' or sessionScope.User.role eq 'Branch Management'}">
                        <a class="khanh-feature-card" href="${pageContext.request.contextPath}/staff/promotions/manage">
                            <h2>Manage Promotion</h2>
                            <p>Voucher chung hoặc riêng chi nhánh</p>
                            <span>Mở chức năng →</span>
                        </a>
                    </c:if>
                    <c:if test="${sessionScope.User.role eq 'Administrator' or sessionScope.User.role eq 'Branch Management'}">
                        <a class="khanh-feature-card" href="${pageContext.request.contextPath}/staff/promotions/discounts">
                            <h2>Configure Discount Amount</h2>
                            <p>Cấu hình số tiền giảm</p>
                            <span>Mở chức năng →</span>
                        </a>
                    </c:if>
                    <c:if test="${sessionScope.User.role eq 'Administrator' or sessionScope.User.role eq 'Branch Management'}">
                        <a class="khanh-feature-card" href="${pageContext.request.contextPath}/staff/promotions/limits">
                            <h2>Configure Usage Limits</h2>
                            <p>Cấu hình lượt sử dụng</p>
                            <span>Mở chức năng →</span>
                        </a>
                    </c:if>
                    <c:if test="${sessionScope.User.role eq 'Administrator' or sessionScope.User.role eq 'Branch Management'}">
                        <a class="khanh-feature-card" href="${pageContext.request.contextPath}/staff/promotions/ticket-prices">
                            <h2>Manage Ticket Pricing</h2>
                            <p>Giá cơ bản theo suất chiếu</p>
                            <span>Mở chức năng →</span>
                        </a>
                    </c:if>
                    <c:if test="${sessionScope.User.role eq 'Administrator' or sessionScope.User.role eq 'Branch Management'}">
                        <a class="khanh-feature-card" href="${pageContext.request.contextPath}/staff/promotions/fnb-prices">
                            <h2>Manage F&amp;B Pricing</h2>
                            <p>Giá chung và giá chi nhánh</p>
                            <span>Mở chức năng →</span>
                        </a>
                    </c:if>
                    <a class="khanh-feature-card" href="${pageContext.request.contextPath}/staff/promotions/validate">
                        <h2>Validate Promotion</h2>
                        <p>Kiểm tra điều kiện áp dụng</p>
                        <span>Mở chức năng →</span>
                    </a>
                </div>
            </main>
        </div>
    </body>
</html>
