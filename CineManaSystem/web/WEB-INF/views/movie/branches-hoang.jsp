<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="pageTitle" value="Chi nhánh chiếu"/>
<c:set var="nav" value="movies"/>
<c:set var="movieTab" value="branches"/>
<c:set var="readOnly" value="${movie.ended}"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<a class="back" href="${ctx}/manage/movie">&larr; Danh sách phim</a>
<h1 class="page-title"><c:out value="${movie.title}"/></h1>
<%@ include file="/WEB-INF/views/common/movie-tabs-hoang.jspf" %>

<c:if test="${readOnly}">
    <div class="notice notice-info">Phim đã ngưng chiếu. Danh sách chi nhánh chỉ để xem; chiếu lại phim ở tab Trạng thái để sửa.</div>
</c:if>

<section class="screen-strip" aria-label="Độ phủ chi nhánh">
    <div class="screen"></div>
    <div class="seats">
        <c:forEach var="row" items="${rows}">
            <span class="seat ${row.assigned ? 'on' : ''} ${row.branch.active ? '' : 'inactive'}"
                  data-seat="${row.branch.branchId}" title="${fn:escapeXml(row.branch.name)}"></span>
        </c:forEach>
    </div>
    <p>Chiếu tại <strong id="onCount">0</strong> trên ${fn:length(rows)} chi nhánh</p>
</section>

<form method="post" action="${ctx}/manage/movie/branches" id="branchForm">
    <input type="hidden" name="movieId" value="${movie.movieId}">

    <c:if test="${not readOnly and fn:length(rows) > 1}">
        <div class="toolbar">
            <input type="search" id="branchFilter" placeholder="Lọc theo tên chi nhánh hoặc thành phố" aria-label="Lọc chi nhánh">
            <button type="button" class="btn" id="selectAll">Chọn mọi chi nhánh đang hoạt động</button>
            <button type="button" class="btn btn-quiet" id="clearAll">Bỏ chọn</button>
        </div>
    </c:if>

    <div class="table-wrap">
        <c:choose>
            <c:when test="${empty rows}">
                <div class="empty">Chưa có chi nhánh nào. <a href="${ctx}/manage/branch?action=create">Tạo chi nhánh</a> trước.</div>
            </c:when>
            <c:otherwise>
                <table>
                    <thead><tr><th><span class="sr-only">Chọn</span></th><th>Chi nhánh</th>
                        <th class="hide-sm">Suất sắp tới</th><th>Thời gian chiếu tại chi nhánh</th></tr></thead>
                    <tbody>
                    <c:forEach var="row" items="${rows}">
                        <c:set var="b" value="${row.branch}"/>
                        <tr class="branch-row ${row.assigned ? 'is-on' : ''}" data-id="${b.branchId}" data-initial="${row.persisted}"
                            data-search="${fn:escapeXml(fn:toLowerCase(b.name))} ${fn:escapeXml(fn:toLowerCase(b.city))}">
                            <td>
                                <input type="hidden" name="shown" value="${b.branchId}">
                                <c:choose>
                                    <c:when test="${row.locked}">
                                        <%-- checkbox disabled không được gửi đi nên giữ giá trị bằng hidden input --%>
                                        <input type="checkbox" id="br-${b.branchId}" class="branch-toggle" checked disabled aria-describedby="lock-${b.branchId}">
                                        <input type="hidden" name="branch" value="${b.branchId}">
                                    </c:when>
                                    <c:otherwise>
                                        <input type="checkbox" id="br-${b.branchId}" class="branch-toggle" name="branch" value="${b.branchId}"
                                            ${row.assigned ? 'checked' : ''} ${(readOnly or not row.selectable) ? 'disabled' : ''}>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <div class="branch-name">
                                    <label for="br-${b.branchId}"><c:out value="${b.name}"/></label>
                                    <c:if test="${not b.active}"><span class="tag tag-inactive">Ngưng hoạt động</span></c:if>
                                </div>
                                <div class="meta"><c:out value="${b.address}"/>, <c:out value="${b.city}"/></div>
                                <c:if test="${row.locked}">
                                    <div class="lock-note" id="lock-${b.branchId}">Còn ${row.upcomingShowtimes} suất sắp tới nên không thể gỡ.</div>
                                </c:if>
                            </td>
                            <td class="hide-sm">${row.upcomingShowtimes}</td>
                            <td>
                                <div class="dates">
                                    <input type="date" name="from_${b.branchId}" value="${row.availableFrom}" aria-label="Từ ngày"
                                        ${(readOnly or not row.assigned) ? 'disabled' : ''} ${readOnly ? 'data-readonly' : ''}>
                                    <input type="date" name="to_${b.branchId}" value="${row.availableTo}" aria-label="Đến ngày"
                                        ${(readOnly or not row.assigned) ? 'disabled' : ''} ${readOnly ? 'data-readonly' : ''}>
                                </div>
                                <div class="meta">Để trống nếu không giới hạn.</div>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </div>

    <c:if test="${not readOnly and not empty rows}">
        <div class="savebar">
            <div class="savebar-inner">
                <span class="pending" id="pending" aria-live="polite">Chưa có thay đổi</span>
                <a class="btn btn-quiet" href="${ctx}/manage/movie/branches?movieId=${movie.movieId}">Huỷ thay đổi</a>
                <button type="submit" class="btn btn-primary">Lưu thay đổi</button>
            </div>
        </div>
    </c:if>
</form>
<script src="${ctx}/assets/js/movie-branches-hoang.js?v=${assetVer}"></script>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
