<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Manage Promotion · Cine Management</title>
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
                <h1 class="mb-4">Manage Promotion</h1>
                <jsp:include page="/WEB-INF/view/common/messages-khanh.jsp"/>
                <section class="panel">
                    <h2 class="h5">${empty editPromotion ? 'Tạo voucher' : 'Sửa voucher'}</h2>
                    <p class="text-muted">Mã CINE-... sinh tự động khi tạo và không thay đổi khi sửa.</p>
                    <form method="post">
                        <input type="hidden" name="csrfToken" value="${sessionScope.khanhCsrf}">
                        <input type="hidden" name="action" value="save">
                        <input type="hidden" name="promoId" value="${empty editPromotion ? 0 : editPromotion.promoId}">
                        <div class="form-grid mb-3">
                            <div>
                                <label class="form-label" for="discount">Số tiền giảm</label>
                                <input class="form-control" id="discount" name="discountAmount" type="number"
                                min="0.01" step="0.01" value="${editPromotion.discountAmount}" required>
                            </div>
                            <div>
                                <label class="form-label" for="minSpend">Đơn tối thiểu</label>
                                <input class="form-control" id="minSpend" name="minSpend" type="number"
                                min="0" step="0.01" value="${empty editPromotion ? 0 : editPromotion.minSpend}" required>
                            </div>
                            <div>
                                <label class="form-label" for="limit">Lượt dùng (Tối thiểu 1 lượt)</label>
                                <input class="form-control" id="limit" name="usageLimit" type="number"
                                min="1" step="1" value="${empty editPromotion ? 1 : editPromotion.usageLimit}" required>
                            </div>
                            <div>
                                <label class="form-label" for="start">Ngày bắt đầu</label>
                                <input class="form-control" id="start" name="startDate" type="date"
                                value="${editPromotion.startDate}" required>
                            </div>
                            <div>
                                <label class="form-label" for="end">Ngày kết thúc</label>
                                <input class="form-control" id="end" name="endDate" type="date"
                                value="${editPromotion.endDate}" required>
                            </div>
                            <div>
                                <label class="form-label" for="scope">Phạm vi</label>
                                <select class="form-select" id="scope" name="scope">
                                    <c:if test="${sessionScope.User.role eq 'Administrator'}">
                                        <option value="ALL" ${empty editPromotion.branchIds ? 'selected' : ''}>Toàn chuỗi</option>
                                    </c:if>
                                    <option value="SELECTED" ${not empty editPromotion.branchIds ? 'selected' : ''}>Chi nhánh được chọn</option>
                                </select>
                            </div>
                        </div>
                        <fieldset class="mb-3">
                            <legend class="h6">Chọn chi nhánh áp dụng</legend>
                            <c:forEach var="branch" items="${branchList}">
                                <label class="form-check">
                                    <input class="form-check-input" type="checkbox" name="branchIds"
                                    value="${branch.branchID}"
                                    ${not empty editPromotion and editPromotion.branchIds.contains(branch.branchID) ? 'checked' : ''}>
                                    <span class="form-check-label">
                                        <c:out value="${branch.name}"/>
                                    </span>
                                </label>
                            </c:forEach>
                        </fieldset>
                        <button class="btn btn-brand-pill" type="submit">Lưu voucher</button>
                    </form>
                </section>
                <section class="panel table-responsive">
                    <h2 class="h5">Danh sách voucher</h2>
                    <table class="table align-middle">
                        <thead>
                            <tr>
                                <th>Mã</th>
                                <th>Giảm</th>
                                <th>MinSpend</th>
                                <th>Hiệu lực</th>
                                <th>Đã dùng / giới hạn</th>
                                <th>Phạm vi</th>
                                <th>Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="p" items="${promotionList}">
                                <tr>
                                    <td>
                                        <c:out value="${p.promoCode}"/>
                                    </td>
                                    <td>
                                        <fmt:formatNumber value="${p.discountAmount}"/>
                                    </td>
                                    <td>
                                        <fmt:formatNumber value="${p.minSpend}"/>
                                    </td>
                                    <td>${p.startDate} → ${p.endDate}</td>
                                    <td>${p.usedCount} / ${p.usageLimit}</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${empty p.branchIds}">Toàn chuỗi</c:when>
                                            <c:otherwise>
                                                <c:forEach var="id" items="${p.branchIds}">CN-${id} </c:forEach>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:if test="${sessionScope.User.role eq 'Administrator'
                                        or (p.branchIds.size() eq 1 and p.branchIds.contains(sessionScope.User.branchID))}">
                                            <a class="btn btn-sm btn-outline-secondary mb-2"
                                            href="${pageContext.request.contextPath}/staff/promotions/manage?edit=${p.promoId}">
                                                Sửa
                                            </a>
                                            <form method="post">
                                                <input type="hidden" name="csrfToken" value="${sessionScope.khanhCsrf}">
                                                <input type="hidden" name="action" value="delete">
                                                <input type="hidden" name="promoId" value="${p.promoId}">
                                                <button class="btn btn-sm btn-outline-danger" type="submit">Xóa mã chưa dùng</button>
                                            </form>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                    <c:if test="${empty promotionList}">
                        <p class="text-muted">Chưa có voucher.</p>
                    </c:if>
                </section>
            </main>
        </div>
    </body>
</html>