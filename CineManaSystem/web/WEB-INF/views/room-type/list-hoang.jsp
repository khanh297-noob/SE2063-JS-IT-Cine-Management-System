<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<c:set var="pageTitle" value="Loại phòng"/>
<c:set var="nav" value="room-types"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<div class="page-head">
    <div>
        <h1 class="page-title">Loại phòng</h1>
        <p class="page-sub">Phụ thu được cộng vào giá vé cơ bản của suất chiếu.
            <c:if test="${not isAdmin}">Loại phòng dùng chung cho cả chuỗi rạp, chỉ Admin được thay đổi.</c:if></p>
    </div>
    <c:if test="${isAdmin}"><a class="btn btn-primary" href="${ctx}/manage/room-types?action=create">Thêm loại phòng</a></c:if>
</div>

<div class="table-wrap">
    <c:choose>
        <c:when test="${empty roomTypes}">
            <div class="empty">Chưa có loại phòng.
                <c:if test="${isAdmin}"><a href="${ctx}/manage/room-types?action=create">Thêm loại đầu tiên</a></c:if></div>
        </c:when>
        <c:otherwise>
            <table>
                <thead><tr><th>Loại phòng</th><th>Phụ thu</th><th>Số phòng</th><th>Trạng thái</th><c:if test="${isAdmin}"><th></th></c:if></tr></thead>
                <tbody>
                <c:forEach var="t" items="${roomTypes}">
                    <c:set var="used" value="${roomCounts[t.roomTypeId]}"/>
                    <tr>
                        <td><div class="movie-title"><c:out value="${t.name}"/></div>
                            <div class="meta"><c:out value="${t.description}"/></div></td>
                        <td><fmt:formatNumber value="${t.surcharge}" maxFractionDigits="0"/> đ</td>
                        <td>${empty used ? 0 : used}</td>
                        <td>
                            <c:choose>
                                <c:when test="${t.active}"><span class="tag tag-now">Đang dùng</span></c:when>
                                <c:otherwise><span class="tag tag-inactive">Ngưng dùng</span></c:otherwise>
                            </c:choose>
                        </td>
                        <c:if test="${isAdmin}">
                        <td>
                            <div class="actions">
                                <a class="btn btn-sm" href="${ctx}/manage/room-types?action=edit&id=${t.roomTypeId}">Sửa</a>
                                <form method="post" action="${ctx}/manage/room-types">
                                    <input type="hidden" name="action" value="status">
                                    <input type="hidden" name="id" value="${t.roomTypeId}">
                                    <input type="hidden" name="status" value="${t.active ? 'Inactive' : 'Active'}">
                                    <button class="btn btn-sm" type="submit">${t.active ? 'Ngưng dùng' : 'Dùng lại'}</button>
                                </form>
                                <c:if test="${empty used or used == 0}">
                                    <form method="post" action="${ctx}/manage/room-types">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="id" value="${t.roomTypeId}">
                                        <button class="btn btn-sm btn-danger" type="submit"
                                                data-confirm="Xoá loại phòng ${fn:escapeXml(t.name)}?">Xoá</button>
                                    </form>
                                </c:if>
                            </div>
                        </td>
                        </c:if>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>
<script src="${ctx}/assets/js/confirm-hoang.js?v=${assetVer}"></script>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
