<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Manage Ticket Pricing · Cine Management</title>
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
                <h1 class="mb-4">Manage Ticket Pricing</h1>
                <jsp:include page="/WEB-INF/view/common/messages-khanh.jsp"/>
                <form method="get" action="${pageContext.request.contextPath}/staff/promotions/ticket-prices" class="panel form-grid">
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
                <section class="panel table-responsive">
                    <p class="text-muted">Chọn chi nhánh. Vé đã bán giữ nguyên Ticket.FinalPrice.</p>
                    <table class="table align-middle">
                        <thead>
                            <tr>
                                <th>Suất</th>
                                <th>Phim / phòng</th>
                                <th>Ngày</th>
                                <th>Giá cơ bản</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="show" items="${showtimes}">
                                <tr>
                                    <td>#${show.showtimeId}</td>
                                    <td>
                                        <c:out value="${show.movieTitle}"/> · <c:out value="${show.roomName}"/>
                                    </td>
                                    <td>${show.showDate}</td>
                                    <td>
                                        <form class="form-grid" method="post">
                                            <input type="hidden" name="csrfToken" value="${sessionScope.khanhCsrf}">
                                            <input type="hidden" name="action" value="ticket-price">
                                            <input type="hidden" name="branchId" value="${selectedBranchId}">
                                            <input type="hidden" name="showtimeId" value="${show.showtimeId}">
                                            <div>
                                                <label class="visually-hidden" for="price-${show.showtimeId}">Giá</label>
                                                <input class="form-control" id="price-${show.showtimeId}" name="basePrice"
                                                type="number" min="0.01" step="0.01" value="${show.basePrice}" required>
                                            </div>
                                            <button class="btn btn-brand-pill" type="submit">Lưu giá</button>
                                        </form>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </section>
            </main>
        </div>
    </body>
</html>
