<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%-- Co dinh ngon ngu Viet Nam de so tien luon co dang 250.000 du trinh duyet dung ngon ngu nao --%>
<fmt:setLocale value="vi_VN"/>
<c:set var="pageUrl" value="${pageContext.request.contextPath}/promotion-price-manage/fnb-pricing"/>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Quản lý giá F&amp;B - CineGo Management</title>
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
        <%@ include file="/WEB-INF/view/report/fragments/manager-shell-style-tuyet.jsp" %>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/promotion-pricing.css">
    </head>
    <body>
        <%@ include file="/WEB-INF/view/report/fragments/manager-navbar-tuyet.jsp" %>
        <div class="layout">
            <%@ include file="/WEB-INF/view/report/fragments/manager-sidebar-tuyet.jsp" %>
            <main class="content main-content">

                <a class="back-link" href="${pageContext.request.contextPath}/promotion-price-manage"><i class="fa-solid fa-arrow-left"></i> Promotion &amp; Pricing</a>
                <div class="page-header">
                    <div>
                        <h1 class="page-title"><i class="fa-solid fa-burger"></i> Quản lý giá F&amp;B</h1>
                        <p class="page-subtitle">
                            <c:choose>
                                <c:when test="${not empty branch}">Thực đơn của chi nhánh <b><c:out value="${branch.name}"/></b>. Để trống giá riêng nghĩa là dùng giá gốc của chuỗi.</c:when>
                                <c:otherwise>Giá và trạng thái bán của từng món theo chi nhánh.</c:otherwise>
                            </c:choose>
                        </p>
                    </div>
                </div>

                <c:if test="${not empty flashMessage}"><div class="alert alert-success"><i class="fa-solid fa-circle-check"></i> <c:out value="${flashMessage}"/></div></c:if>
                <c:if test="${not empty flashError}"><div class="alert alert-error"><i class="fa-solid fa-circle-exclamation"></i> <c:out value="${flashError}"/></div></c:if>

                <c:choose>
                    <c:when test="${empty branch}">
                        <div class="alert alert-warn"><i class="fa-solid fa-triangle-exclamation"></i> Tài khoản của bạn chưa được gán chi nhánh nên chưa có thực đơn để quản lý. Hãy liên hệ Admin.</div>
                    </c:when>
                    <c:otherwise>
                        <div class="card">
                            <div class="table-container">
                                <table>
                                    <thead>
                                        <tr>
                                            <th>Danh mục</th><th>Món</th>
                                            <th class="align-right">Giá gốc</th>
                                            <th class="align-right">Đang áp dụng</th>
                                            <th>Giá riêng của chi nhánh (đồng)</th>
                                            <th>Trạng thái</th>
                                            <th></th>
                                            <c:if test="${isAdminUser}"><th>Giá gốc toàn chuỗi (đồng)</th></c:if>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:choose>
                                            <c:when test="${not empty menu}">
                                                <c:forEach var="item" items="${menu}">
                                                    <c:set var="current" value="${item.inMenu ? item.status : 'Available'}"/>
                                                    <tr>
                                                        <td><span class="tag"><c:out value="${item.category}"/></span></td>
                                                        <td class="cell-strong"><c:out value="${item.name}"/></td>
                                                        <td class="align-right"><fmt:formatNumber value="${item.basePrice}" pattern="#,##0"/> ₫</td>
                                                        <td class="align-right">
                                                            <c:choose>
                                                                <c:when test="${item.inMenu}"><b><fmt:formatNumber value="${item.effectivePrice}" pattern="#,##0"/> ₫</b></c:when>
                                                                <c:otherwise><span class="badge badge-gray">Chưa bán</span></c:otherwise>
                                                            </c:choose>
                                                        </td>
                                                        <td colspan="3">
                                                            <form method="post" action="${pageUrl}" class="inline-form">
                                                                <input type="hidden" name="action" value="saveItem">
                                                                <input type="hidden" name="branchId" value="${branch.branchID}">
                                                                <input type="hidden" name="itemId" value="${item.itemId}">
                                                                <input class="price-input" type="text" inputmode="numeric" name="price" autocomplete="off"
                                                                       placeholder="Dùng giá gốc"
                                                                       value="<c:if test='${item.branchPrice != null}'><fmt:formatNumber value='${item.branchPrice}' maxFractionDigits='0' groupingUsed='false'/></c:if>"
                                                                       aria-label="Giá riêng của ${item.name}">
                                                                <select name="status" class="status-select" aria-label="Trạng thái ${item.name}">
                                                                    <option value="Available" ${current == 'Available' ? 'selected' : ''}>Đang bán</option>
                                                                    <option value="SoldOut" ${current == 'SoldOut' ? 'selected' : ''}>Hết hàng</option>
                                                                    <option value="Inactive" ${current == 'Inactive' ? 'selected' : ''}>Ngừng bán</option>
                                                                </select>
                                                                <button type="submit" class="btn-save">${item.inMenu ? 'Lưu' : 'Thêm vào thực đơn'}</button>
                                                            </form>
                                                        </td>
                                                        <c:if test="${isAdminUser}">
                                                            <td>
                                                                <form method="post" action="${pageUrl}" class="inline-form">
                                                                    <input type="hidden" name="action" value="basePrice">
                                                                    <input type="hidden" name="branchId" value="${branch.branchID}">
                                                                    <input type="hidden" name="itemId" value="${item.itemId}">
                                                                    <input class="price-input" type="text" inputmode="numeric" name="price" autocomplete="off"
                                                                           value="<fmt:formatNumber value='${item.basePrice}' maxFractionDigits='0' groupingUsed='false'/>"
                                                                           aria-label="Giá gốc của ${item.name}">
                                                                    <button type="submit" class="btn-save btn-save-alt">Lưu giá gốc</button>
                                                                </form>
                                                            </td>
                                                        </c:if>
                                                    </tr>
                                                </c:forEach>
                                            </c:when>
                                            <c:otherwise>
                                                <tr><td colspan="8" class="empty-cell"><i class="fa-solid fa-folder-open empty-icon"></i>Chưa có món nào trong danh mục F&amp;B của chuỗi.</td></tr>
                                            </c:otherwise>
                                        </c:choose>
                                    </tbody>
                                </table>
                            </div>
                            <p class="note">Món hiện &quot;Chưa bán&quot; sẽ xuất hiện trong thực đơn chi nhánh sau khi bấm &quot;Thêm vào thực đơn&quot;.
                                <c:if test="${isAdminUser}">Giá gốc áp dụng cho mọi chi nhánh chưa đặt giá riêng.</c:if>
                                <c:if test="${not isAdminUser}">Giá gốc do Admin quản lý.</c:if>
                            </p>
                        </div>
                    </c:otherwise>
                </c:choose>

            </main>
        </div>
    </body>
</html>
