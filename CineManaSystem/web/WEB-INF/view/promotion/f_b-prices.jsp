<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Manage F&amp;B Pricing · Cine Management</title>
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
                <h1 class="mb-4">Manage F&amp;B Pricing</h1>
                <jsp:include page="/WEB-INF/view/common/messages-khanh.jsp"/>
                <form method="get" action="${pageContext.request.contextPath}/staff/promotions/fnb-prices" class="panel form-grid">
                    <div>
                        <label for="branchFilter" class="form-label">Chi nhánh</label>
                        <select id="branchFilter" name="branchId" class="form-select">
                            <option value="">Giá chung (Admin)</option>
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
                <section class="panel">
                    <p class="text-muted">Admin không chọn chi nhánh: sửa giá chung. Có chi nhánh: sửa giá riêng.</p>
                    <c:if test="${empty selectedBranchId and sessionScope.User.role eq 'Administrator'}">
                        <c:forEach var="item" items="${itemList}">
                            <form class="form-grid mb-3" method="post">
                                <input type="hidden" name="csrfToken" value="${sessionScope.khanhCsrf}">
                                <input type="hidden" name="action" value="fnb-price">
                                <input type="hidden" name="itemId" value="${item.itemId}">
                                <div>
                                    <label class="form-label" for="base-${item.itemId}">
                                        <c:out value="${item.itemName}"/>
                                    </label>
                                    <input class="form-control" id="base-${item.itemId}" name="price" type="number"
                                    min="0.01" step="0.01" value="${item.price}" required>
                                </div>
                                <button class="btn btn-brand-pill" type="submit">Lưu giá chung</button>
                            </form>
                        </c:forEach>
                    </c:if>
                    <c:if test="${not empty selectedBranchId}">
                        <p>Giá riêng trống nghĩa là quay về giá chung.</p>
                        <c:forEach var="item" items="${stockList}">
                            <form class="form-grid mb-3" method="post">
                                <input type="hidden" name="csrfToken" value="${sessionScope.khanhCsrf}">
                                <input type="hidden" name="action" value="fnb-price">
                                <input type="hidden" name="branchId" value="${selectedBranchId}">
                                <input type="hidden" name="itemId" value="${item.itemId}">
                                <div>
                                    <label class="form-label" for="override-${item.itemId}">
                                        <c:out value="${item.itemName}"/>
                                    </label>
                                    <input class="form-control" id="override-${item.itemId}" name="price"
                                    type="number" min="0.01" step="0.01" value="${item.overridePrice}">
                                </div>
                                <button class="btn btn-brand-pill" type="submit">Lưu giá riêng</button>
                            </form>
                        </c:forEach>
                    </c:if>
                </section>
            </main>
        </div>
    </body>
</html>
