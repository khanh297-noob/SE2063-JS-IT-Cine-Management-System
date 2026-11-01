<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Promotion &amp; Pricing - CineGo Management</title>
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin-style.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/promotion-pricing.css">
    </head>
    <body>
        <%-- Khung chung của phân hệ: menu bên trái chọn theo khanhFeature/khanhActive --%>
        <c:set var="khanhFeature" value="promotion" scope="request"/>
        <jsp:include page="/WEB-INF/view/common/sidebar-khanh.jsp"/>
        <div class="main-wrapper">
            <jsp:include page="/WEB-INF/view/common/header-khanh.jsp"/>
            <main class="pp-page">

                <%-- Chọn rạp quản lý: trước đây nằm trên thanh điều hướng, nay ở đầu phần nội dung --%>
                <c:if test="${not empty scopeWarning}">
                    <div class="alert alert-warn"><i class="fa-solid fa-triangle-exclamation"></i> <c:out value="${scopeWarning}"/></div>
                </c:if>
                <div class="branch-bar">
                    <c:choose>
                        <c:when test="${branches.size() > 1}">
                            <form method="get" action="${pageContext.request.contextPath}${shellAction}" class="branch-pill">
                                <c:if test="${not empty param.fromDate}"><input type="hidden" name="fromDate" value="<c:out value='${param.fromDate}'/>"></c:if>
                                <c:if test="${not empty param.toDate}"><input type="hidden" name="toDate" value="<c:out value='${param.toDate}'/>"></c:if>
                                <c:if test="${not empty param.type}"><input type="hidden" name="type" value="<c:out value='${param.type}'/>"></c:if>
                                <c:if test="${not empty param.movieId}"><input type="hidden" name="movieId" value="<c:out value='${param.movieId}'/>"></c:if>
                                <i class="fa-solid fa-bookmark"></i> Rạp quản lý:
                                <select name="branchId" onchange="this.form.submit()">
                                    <option value="">Tất cả chi nhánh</option>
                                    <c:forEach var="b" items="${branches}">
                                        <option value="${b.branchID}" ${selectedBranchId == b.branchID ? 'selected' : ''}><c:out value="${b.name}"/> (Branch #${b.branchID < 10 ? '0' : ''}${b.branchID})</option>
                                    </c:forEach>
                                </select>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <span class="branch-pill"><i class="fa-solid fa-bookmark"></i> Rạp quản lý: <b><c:out value="${branchPill}"/></b></span>
                        </c:otherwise>
                    </c:choose>
                </div>


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
