<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Update Item Quantity · Cine Management</title>
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
                <h1 class="mb-4">Update Item Quantity</h1>
                <jsp:include page="/WEB-INF/view/common/messages-khanh.jsp"/>
                <c:set var="pagePath" value="/staff/fnb/quantity" scope="request"/>
                <form method="get" action="${pageContext.request.contextPath}/staff/fnb/quantity" class="panel form-grid">
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
                    <jsp:include page="/WEB-INF/view/common/fnb-orders-khanh.jsp"/>
                </c:if>
                <jsp:include page="/WEB-INF/view/common/fnb-lines-khanh.jsp"/>
                <c:if test="${not empty selectedBooking}">
                    <section class="panel">
                        <p class="text-muted">Chỉ sửa Pending, chưa có Payment và đã gỡ voucher.</p>
                        <c:if test="${selectedBooking.status eq 'Pending' and empty selectedBooking.paymentStatus and empty selectedBooking.promoId}">
                            <c:forEach var="line" items="${orderLines}">
                                <form class="form-grid mb-3" method="post">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.khanhCsrf}">
                                    <input type="hidden" name="action" value="quantity">
                                    <input type="hidden" name="bookingId" value="${selectedBooking.bookingId}">
                                    <input type="hidden" name="branchId" value="${selectedBranchId}">
                                    <input type="hidden" name="itemId" value="${line.itemId}">
                                    <div>
                                        <label class="form-label" for="edit-${line.itemId}">
                                            <c:out value="${line.itemName}"/>
                                        </label>
                                        <input class="form-control" id="edit-${line.itemId}" name="quantity"
                                        type="number" min="1" step="1" value="${line.quantity}" required>
                                    </div>
                                    <button class="btn btn-brand-pill" type="submit">Lưu số lượng</button>
                                </form>
                            </c:forEach>
                        </c:if>
                    </section>
                </c:if>
            </main>
        </div>
    </body>
</html>
