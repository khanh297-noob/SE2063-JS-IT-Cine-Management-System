<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@taglib prefix="fmt" uri="jakarta.tags.fmt"%>
<c:set var="pageTitle" value="Phòng chiếu"/>
<c:set var="nav" value="rooms"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<div class="page-head">
    <div>
        <h1 class="page-title">Phòng chiếu</h1>
        <p class="page-sub">${fn:length(rooms)} phòng</p>
    </div>
    <a class="btn btn-primary" href="${ctx}/manage/rooms?action=create${empty branchId ? '' : '&branchId='.concat(branchId)}">Thêm phòng</a>
</div>

<form class="filters" method="get" action="${ctx}/manage/rooms">
    <select name="branchId" aria-label="Chi nhánh" onchange="this.form.submit()">
        <option value="">Mọi chi nhánh</option>
        <c:forEach var="b" items="${branches}">
            <option value="${b.branchId}" ${b.branchId == branchId ? 'selected' : ''}><c:out value="${b.name}"/></option>
        </c:forEach>
    </select>
    <noscript><button class="btn" type="submit">Lọc</button></noscript>
</form>

<div class="table-wrap">
    <c:choose>
        <c:when test="${empty rooms}">
            <div class="empty">Chưa có phòng nào. <a href="${ctx}/manage/rooms?action=create">Thêm phòng</a></div>
        </c:when>
        <c:otherwise>
            <table>
                <thead><tr><th>Phòng</th><th>Loại</th><th>Ghế</th><th>Trạng thái</th><th></th></tr></thead>
                <tbody>
                <c:forEach var="rs" items="${rooms}">
                    <c:set var="r" value="${rs.room}"/>
                    <tr>
                        <td><div class="movie-title"><c:out value="${r.roomName}"/></div>
                            <div class="meta"><c:out value="${rs.branchName}"/></div></td>
                        <td><c:out value="${rs.roomTypeName}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${rs.seatCount == 0}"><a href="${ctx}/manage/rooms/seats?roomId=${r.roomId}">Chưa xếp ghế</a></c:when>
                                <c:otherwise>${rs.seatCount} <span class="meta">(${r.totalRows} x ${r.totalColumns})</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <form method="post" action="${ctx}/manage/rooms" class="status-form">
                                <input type="hidden" name="action" value="status">
                                <input type="hidden" name="id" value="${r.roomId}">
                                <c:if test="${not empty branchId}"><input type="hidden" name="branchId" value="${branchId}"></c:if>
                                <select name="status" aria-label="Trạng thái ${fn:escapeXml(r.roomName)}" onchange="this.form.submit()">
                                    <option value="Active" ${r.status == 'Active' ? 'selected' : ''}>Hoạt động</option>
                                    <option value="Maintenance" ${r.status == 'Maintenance' ? 'selected' : ''}>Bảo trì</option>
                                    <option value="Inactive" ${r.status == 'Inactive' ? 'selected' : ''}>Ngưng</option>
                                </select>
                            </form>
                        </td>
                        <td>
                            <div class="actions">
                                <a class="btn btn-sm" href="${ctx}/manage/rooms?action=edit&id=${r.roomId}">Sửa</a>
                                <a class="btn btn-sm" href="${ctx}/manage/rooms/seats?roomId=${r.roomId}">Sơ đồ ghế</a>
                                <form method="post" action="${ctx}/manage/rooms">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="id" value="${r.roomId}">
                                    <c:if test="${not empty branchId}"><input type="hidden" name="branchId" value="${branchId}"></c:if>
                                    <button class="btn btn-sm btn-danger" type="submit"
                                            data-confirm="Xoá ${fn:escapeXml(r.roomName)} và toàn bộ ghế của phòng?">Xoá</button>
                                </form>
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
