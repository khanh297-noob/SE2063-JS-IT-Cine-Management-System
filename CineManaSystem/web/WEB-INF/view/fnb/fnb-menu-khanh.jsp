<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>F&B Booking</title>
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin-style.css">
    </head>
    <body>
        <jsp:include page="/WEB-INF/view/common/sidebar-khanh.jsp"/>
        <div class="main-wrapper">
            <jsp:include page="/WEB-INF/view/common/header-khanh.jsp"/>
            <main class="content-body">
                <h1 class="mb-3">F&B Booking</h1>
                <p class="text-muted mb-4">Chọn chức năng cần thực hiện.</p>
                <div class="khanh-feature-grid">
                    <c:if test="${sessionScope.User.role eq 'Administrator'}">
                        <a class="khanh-feature-card" href="${pageContext.request.contextPath}/staff/fnb/items">
                            <h2>Manage F&amp;B Item</h2>
                            <p>Danh mục chung do Admin quản lý</p>
                            <span>Mở chức năng →</span>
                        </a>
                    </c:if>
                    <a class="khanh-feature-card" href="${pageContext.request.contextPath}/staff/fnb/orders">
                        <h2>Create &amp; Manage Order</h2>
                        <p>Tạo và xem đơn F&B</p>
                        <span>Mở chức năng →</span>
                    </a>
                    <a class="khanh-feature-card" href="${pageContext.request.contextPath}/staff/fnb/order-status">
                        <h2>Update Order Status</h2>
                        <p>Theo dõi tiến độ sau thanh toán</p>
                        <span>Mở chức năng →</span>
                    </a>
                    <a class="khanh-feature-card" href="${pageContext.request.contextPath}/staff/fnb/stock">
                        <h2>Monitor Stock / Thực đơn chi nhánh</h2>
                        <p>Xem tồn, cấu hình món theo quyền</p>
                        <span>Mở chức năng →</span>
                    </a>
                    <a class="khanh-feature-card" href="${pageContext.request.contextPath}/staff/fnb/quantity">
                        <h2>Update Item Quantity</h2>
                        <p>Sửa số lượng trong đơn đang chờ</p>
                        <span>Mở chức năng →</span>
                    </a>
                </div>
            </main>
        </div>
    </body>
</html>
