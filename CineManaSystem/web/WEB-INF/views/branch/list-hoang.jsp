<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="pageTitle" value="Chi nhánh"/>
<c:set var="nav" value="branches"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<div class="page-head">
    <div>
        <h1 class="page-title">Chi nhánh</h1>
        <%-- Quản lý chi nhánh chỉ thấy chi nhánh của mình (BranchServlet đã lọc), không tạo / ngưng chi nhánh --%>
        <p class="page-sub"><c:choose><c:when test="${isAdmin}">${fn:length(branches)} chi nhánh</c:when><c:otherwise>Chi nhánh bạn đang quản lý</c:otherwise></c:choose></p>
    </div>
    <c:if test="${isAdmin}"><a class="btn btn-primary" href="${ctx}/manage/branch?action=create">Thêm chi nhánh</a></c:if>
</div>

<c:if test="${isAdmin}">
<form class="filters" method="get" action="${ctx}/manage/branch">
    <input type="search" name="q" value="${fn:escapeXml(q)}" placeholder="Tìm theo tên, thành phố, địa chỉ" aria-label="Tìm chi nhánh">
    <select name="status" aria-label="Trạng thái">
        <option value="">Mọi trạng thái</option>
        <option value="Active" ${status == 'Active' ? 'selected' : ''}>Đang hoạt động</option>
        <option value="Inactive" ${status == 'Inactive' ? 'selected' : ''}>Ngưng hoạt động</option>
    </select>
    <button class="btn" type="submit">Lọc</button>
</form>
</c:if>

<div class="table-wrap">
    <c:choose>
        <c:when test="${empty branches}">
            <div class="empty">Không có chi nhánh nào khớp bộ lọc.
                <c:if test="${isAdmin}"><a href="${ctx}/manage/branch?action=create">Thêm chi nhánh</a></c:if></div>
        </c:when>
        <c:otherwise>
            <table>
                <thead><tr><th>Chi nhánh</th><th class="hide-sm">Hotline</th><th>Trạng thái</th><th></th></tr></thead>
                <tbody>
                <c:forEach var="b" items="${branches}">
                    <tr>
                        <td>
                            <div class="movie-title"><c:out value="${b.name}"/></div>
                            <div class="meta"><c:out value="${b.address}"/>, <c:out value="${b.city}"/></div>
                        </td>
                        <td class="hide-sm"><c:out value="${b.hotline}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${b.active}"><span class="tag tag-now">Đang hoạt động</span></c:when>
                                <c:otherwise><span class="tag tag-inactive">Ngưng hoạt động</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <div class="actions">
                                <a class="btn btn-sm" href="${ctx}/manage/branch?action=edit&id=${b.branchId}">Sửa</a>
                                <a class="btn btn-sm" href="${ctx}/manage/branch/hours?branchId=${b.branchId}">Giờ mở cửa</a>
                                <a class="btn btn-sm" href="${ctx}/manage/rooms?branchId=${b.branchId}">Phòng</a>
                                <c:if test="${b.active}">
                                    <a class="btn btn-sm btn-quiet" href="${ctx}/branch/details?id=${b.branchId}">Trang khách</a>
                                </c:if>
                                <c:if test="${isAdmin}">
                                <form method="post" action="${ctx}/manage/branch">
                                    <input type="hidden" name="action" value="status">
                                    <input type="hidden" name="id" value="${b.branchId}">
                                    <c:choose>
                                        <c:when test="${b.active}">
                                            <input type="hidden" name="status" value="Inactive">
                                            <button class="btn btn-sm btn-danger" type="submit"
                                                    data-confirm="Ngưng hoạt động ${fn:escapeXml(b.name)}? Khách sẽ không thấy chi nhánh này nữa.">Ngưng</button>
                                        </c:when>
                                        <c:otherwise>
                                            <input type="hidden" name="status" value="Active">
                                            <button class="btn btn-sm" type="submit">Mở lại</button>
                                        </c:otherwise>
                                    </c:choose>
                                </form>
                                </c:if>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>
<script src="${ctx}/assets/js/confirm-hoang.js?v=${assetVer}"></script>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
