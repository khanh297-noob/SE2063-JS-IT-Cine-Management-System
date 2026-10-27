<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Configure Usage Limits · Cine Management</title>
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
                <h1 class="mb-4">Configure Usage Limits</h1>
                <jsp:include page="/WEB-INF/view/common/messages-khanh.jsp"/>
                <section class="panel table-responsive">
                    <p class="text-muted">Quản lý chi nhánh chỉ sửa voucher riêng của chi nhánh mình.</p>
                    <table class="table align-middle">
                        <thead>
                            <tr>
                                <th>Mã voucher</th>
                                <th>Đã dùng</th>
                                <th>Hiện tại</th>
                                <th>Cấu hình mới</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="p" items="${promotionList}">
                                <tr>
                                    <td>
                                        <c:out value="${p.promoCode}"/>
                                    </td>
                                    <td>${p.usedCount}</td>
                                    <td>${p.usageLimit}</td>
                                    <td>
                                        <c:if test="${sessionScope.User.role eq 'Administrator'
                                        or (p.branchIds.size() eq 1 and p.branchIds.contains(sessionScope.User.branchID))}">
                                            <form class="form-grid" method="post">
                                                <input type="hidden" name="csrfToken" value="${sessionScope.khanhCsrf}">
                                                <input type="hidden" name="action" value="limit">
                                                <input type="hidden" name="promoId" value="${p.promoId}">
                                                <div>
                                                    <label class="form-label" for="value-${p.promoId}">Lượt dùng (Tối thiểu 1 lượt)</label>
                                                    <input class="form-control" id="value-${p.promoId}" name="usageLimit"
                                                    type="number" min="1" step="1"
                                                    value="${p.usageLimit}" required>
                                                </div>
                                                <button class="btn btn-brand-pill" type="submit">Lưu</button>
                                            </form>
                                        </c:if>
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
