<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Update Order Status · Cine Management</title>
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
                <h1 class="mb-4">Update Order Status</h1>
                <jsp:include page="/WEB-INF/view/common/messages-khanh.jsp"/>
                <c:set var="pagePath" value="/staff/fnb/order-status" scope="request"/>
                <form method="get" action="${pageContext.request.contextPath}/staff/fnb/order-status" class="panel form-grid">
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
                    <jsp:include page="/WEB-INF/view/fnb/f_b-orders-khanh.jsp"/>
                </c:if>
                <jsp:include page="/WEB-INF/view/fnb/f_b-lines-khanh.jsp"/>
                <c:if test="${not empty selectedBooking}">
                    <section class="panel">
                        <c:if test="${selectedBooking.status eq 'Pending' and empty selectedBooking.paymentStatus}">
                            <form method="post">
                                <input type="hidden" name="csrfToken" value="${sessionScope.khanhCsrf}">
                                <input type="hidden" name="action" value="cancel">
                                <input type="hidden" name="bookingId" value="${selectedBooking.bookingId}">
                                <input type="hidden" name="branchId" value="${selectedBranchId}">
                                <button class="btn btn-outline-danger" type="submit">Hủy đơn chưa thanh toán</button>
                            </form>
                        </c:if>
                        <c:if test="${selectedBooking.status eq 'Confirmed' and selectedBooking.paymentStatus eq 'Success' and not empty orderLines}">
                            <c:set var="current" value="${orderLines[0].orderStatus}"/>
                            <c:set var="next" value="${current eq 'Confirmed' ? 'Preparing' : (current eq 'Preparing' ? 'Ready' : (current eq 'Ready' ? 'Delivered' : ''))}"/>
                            <c:if test="${not empty next}">
                                <form method="post">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.khanhCsrf}">
                                    <input type="hidden" name="action" value="progress">
                                    <input type="hidden" name="bookingId" value="${selectedBooking.bookingId}">
                                    <input type="hidden" name="branchId" value="${selectedBranchId}">
                                    <input type="hidden" name="nextStatus" value="${next}">
                                    <button class="btn btn-brand-pill" type="submit">
                                        ${next eq 'Preparing' ? 'Bắt đầu chế biến' : (next eq 'Ready' ? 'Sẵn sàng giao' : 'Đã giao')}
                                    </button>
                                </form>
                            </c:if>
                        </c:if>
                        <p class="text-muted mt-3">Thanh toán và hoàn tiền thuộc luồng Payment của Nam.</p>
                    </section>
                </c:if>
            </main>
        </div>
    </body>
</html>
