<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Create &amp; Manage Order · Cine Management</title>
        <link rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
        <link rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
        <link rel="stylesheet"
        href="${pageContext.request.contextPath}/assets/css/admin-style.css">
    </head>
    <body>
        <jsp:include page="/WEB-INF/view/common/sidebar-khanh.jsp"/>
        <div class="main-wrapper">
            <jsp:include page="/WEB-INF/view/common/header-khanh.jsp"/>
            <main class="content-body">
                <h1 class="mb-4">Create &amp; Manage Order</h1>
                <jsp:include page="/WEB-INF/view/common/messages-khanh.jsp"/>
                <c:set var="pagePath" value="/staff/fnb/orders" scope="request"/>
                <form method="get" action="${pageContext.request.contextPath}/staff/fnb/orders" class="panel form-grid">
                    <div>
                        <label for="branchFilter" class="form-label">Chi nhánh</label>
                        <select id="branchFilter" name="branchId" class="form-select">
                            <option value="">Chọn chi nhánh</option>
                            <c:forEach var="branch" items="${branchList}">
                                <option value="${branch.branchID}"
                                ${selectedBranchId eq branch.branchID ? 'selected' : ''}>
                                    CN-${branch.branchID} · <c:out value="${branch.name}"/>
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <button class="btn btn-brand-pill" type="submit">Xem</button>
                </form>
                <c:if test="${not empty selectedBranchId}">
                    <c:if test="${not empty saleMenu}">
                        <section class="panel">
                            <h2 class="h5">Tạo đơn mới</h2>
                            <form method="post">
                                <input type="hidden" name="csrfToken" value="${sessionScope.khanhCsrf}">
                                <input type="hidden" name="action" value="create">
                                <input type="hidden" name="branchId" value="${selectedBranchId}">
                                <label class="form-label" for="customer">Khách hàng</label>
                                <select id="customer" name="customerId" class="form-select mb-3" required>
                                    <c:forEach var="customer" items="${customers}">
                                        <option value="${customer.userID}">
                                            <c:out value="${customer.fullName}"/> · <c:out value="${customer.email}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                                <table class="table align-middle">
                                    <thead>
                                        <tr>
                                            <th>Món</th>
                                            <th>Giá bán</th>
                                            <th>Còn</th>
                                            <th>Số lượng chọn</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="item" items="${saleMenu}">
                                            <tr>
                                                <td>
                                                    <c:out value="${item.itemName}"/>
                                                </td>
                                                <td>
                                                    <fmt:formatNumber value="${item.effectivePrice}"/>
                                                </td>
                                                <td>${item.stockQuantity}</td>
                                                <td>
                                                    <input type="hidden" name="itemId" value="${item.itemId}">
                                                    <label class="visually-hidden" for="qty-${item.itemId}">Số lượng</label>
                                                    <input class="form-control" id="qty-${item.itemId}" name="quantity"
                                                    type="number" min="0" max="${item.stockQuantity}" step="1" value="0" required>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                                <p class="text-muted">Đơn mới Pending, chưa trừ kho.</p>
                                <button class="btn btn-brand-pill" type="submit">Tạo đơn</button>
                            </form>
                        </section>
                    </c:if>
                    <c:if test="${empty saleMenu}">
                        <p class="alert alert-info">Chưa có món đang bán và còn tồn tại chi nhánh.</p>
                    </c:if>
                    <jsp:include page="/WEB-INF/view/fnb/f_b-orders-khanh.jsp"/>
                </c:if>
                <jsp:include page="/WEB-INF/view/fnb/f_b-lines-khanh.jsp"/>
                <c:if test="${canPayCash}">
                    <section class="panel mt-3">
                        <h2 class="h5 mb-3">Thanh toán tiền mặt</h2>

                        <p>
                            Đơn #${selectedBooking.bookingId} — Phải thu:

                            <strong class="text-danger">
                                <fmt:formatNumber
                                    value="${selectedBooking.totalAmount}"
                                    pattern="#,##0.##"/>
                                đ
                            </strong>
                        </p>

                        <form
                            method="post"
                            action="${pageContext.request.contextPath}/staff/fnb/orders"
                            onsubmit="return confirm('Xác nhận bạn đã nhận tiền mặt của khách?');">

                            <input
                                type="hidden"
                                name="csrfToken"
                                value="${sessionScope.khanhCsrf}">

                            <input
                                type="hidden"
                                name="action"
                                value="payCash">

                            <input
                                type="hidden"
                                name="bookingId"
                                value="${selectedBooking.bookingId}">

                            <input
                                type="hidden"
                                name="branchId"
                                value="${selectedBranchId}">

                            <label for="cashReceived" class="form-label">
                                Tiền khách đưa
                            </label>

                            <input
                                id="cashReceived"
                                name="cashReceived"
                                type="number"
                                class="form-control mb-3"
                                min="${selectedBooking.totalAmount}"
                                max="9999999999.99"
                                step="0.01"
                                required>

                            <button type="submit" class="btn btn-brand-pill">
                                Xác nhận thu tiền
                            </button>
                        </form>

                        <p class="text-muted small mt-3 mb-0">
                            Chỉ xác nhận sau khi nhận tiền.
                            Thu ngân cần vào ca tại đúng chi nhánh.
                        </p>
                    </section>
                </c:if>
            </main>
        </div>
    </body>
</html>
