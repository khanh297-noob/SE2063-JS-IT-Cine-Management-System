<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<fmt:setLocale value="vi_VN"/>
<%-- Quản lý suất chiếu: danh sách theo ngày. Servlet: ShowtimeController
     (branches - chỉ Admin, branch, branchId, rooms, movies, date, prevDate, nextDate, dateLabel, roomId, showtimes, back). --%>
<c:set var="pageTitle" value="Suất chiếu"/>
<c:set var="nav" value="showtimes"/>
<c:set var="extraCss" value="booking-hiep"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<div class="page-head">
    <div>
        <h1 class="page-title">Suất chiếu</h1>
        <p class="page-sub"><c:out value="${branch.name}"/> · ${dateLabel}</p>
    </div>
    <div class="actions">
        <a class="btn" href="${ctx}/manager/schedule?branchId=${branchId}">Lịch tổng hợp</a>
        <a class="btn btn-primary" href="${ctx}/manager/showtimes/new?branchId=${branchId}&date=${date}">Tạo suất chiếu</a>
    </div>
</div>

<form method="get" action="${ctx}/manager/showtimes" class="bk-toolbar">
    <c:choose>
        <c:when test="${not empty branches}">
            <label>Chi nhánh
                <select name="branchId" onchange="this.form.submit()">
                    <c:forEach var="b" items="${branches}">
                        <option value="${b.branchId}" ${b.branchId == branchId ? 'selected' : ''}><c:out value="${b.name}"/></option>
                    </c:forEach>
                </select>
            </label>
        </c:when>
        <c:otherwise><input type="hidden" name="branchId" value="${branchId}"></c:otherwise>
    </c:choose>
    <a class="btn btn-sm" href="${ctx}/manager/showtimes?branchId=${branchId}&date=${prevDate}&roomId=${roomId}" aria-label="Ngày trước">&lsaquo;</a>
    <label>Ngày <input type="date" name="date" value="${date}" onchange="this.form.submit()"></label>
    <a class="btn btn-sm" href="${ctx}/manager/showtimes?branchId=${branchId}&date=${nextDate}&roomId=${roomId}" aria-label="Ngày sau">&rsaquo;</a>
    <label>Phòng
        <select name="roomId" onchange="this.form.submit()">
            <option value="">Tất cả phòng</option>
            <c:forEach var="r" items="${rooms}">
                <option value="${r.roomId}" ${r.roomId == roomId ? 'selected' : ''}><c:out value="${r.roomName}"/>${r.active ? '' : ' (bảo trì)'}</option>
            </c:forEach>
        </select>
    </label>
</form>

<c:choose>
    <c:when test="${empty showtimes}">
        <div class="table-wrap"><div class="empty">Chưa có suất chiếu nào trong ngày này.</div></div>
    </c:when>
    <c:otherwise>
        <div class="table-wrap">
            <table>
                <thead>
                <tr><th>Giờ</th><th>Phim</th><th>Phòng</th><th class="bk-num">Giá suất</th><th>Đã bán</th><th>Trạng thái</th><th></th></tr>
                </thead>
                <tbody>
                <c:forEach var="s" items="${showtimes}">
                    <tr class="${s.cancelled ? 'bk-row-muted' : ''}">
                        <td class="bk-mono"><b>${s.startStr}</b> - ${s.endStr}<c:if test="${s.overnight}"> <span class="tag tag-soon">+1 ngày</span></c:if></td>
                        <td><span class="movie-title"><c:out value="${s.movieTitle}"/></span> <span class="bk-age age-${s.ageRating}">${s.ageRating}</span>
                            <div class="meta">${s.movieDuration} phút</div></td>
                        <td><c:out value="${s.roomName}"/> <span class="tag tag-ended"><c:out value="${s.roomType}"/></span></td>
                        <td class="bk-num"><fmt:formatNumber value="${s.basePrice}" pattern="#,##0"/> ₫</td>
                        <td>
                            <div class="bk-bar"><span style="width:${s.occupancy}%"></span></div>
                            <span class="meta">${s.soldCount}/${s.seatCount} (${s.occupancy}%)</span>
                        </td>
                        <td><span class="bk-badge ${s.statusClass}">${s.displayStatus}</span></td>
                        <td>
                            <div class="bk-row-actions">
                                <c:if test="${not s.cancelled}">
                                    <a class="btn btn-sm" href="${ctx}/manager/showtimes/edit?id=${s.showtimeId}">Sửa</a>
                                    <c:if test="${s.soldCount == 0 and not s.started}">
                                        <details class="bk-pop">
                                            <summary class="btn btn-sm">Gán phòng</summary>
                                            <form method="post" action="${ctx}/manager/showtimes/assign-room" class="bk-pop-body">
                                                <input type="hidden" name="id" value="${s.showtimeId}">
                                                <input type="hidden" name="back" value="${fn:escapeXml(back)}">
                                                <select name="roomId" aria-label="Phòng mới">
                                                    <c:forEach var="r" items="${rooms}">
                                                        <c:if test="${r.active and r.roomId != s.roomId}">
                                                            <option value="${r.roomId}"><c:out value="${r.roomName}"/> (<c:out value="${r.roomType}"/>)</option>
                                                        </c:if>
                                                    </c:forEach>
                                                </select>
                                                <button class="btn btn-primary btn-sm">Gán</button>
                                            </form>
                                        </details>
                                        <details class="bk-pop">
                                            <summary class="btn btn-sm">Gán phim</summary>
                                            <form method="post" action="${ctx}/manager/showtimes/assign-movie" class="bk-pop-body">
                                                <input type="hidden" name="id" value="${s.showtimeId}">
                                                <input type="hidden" name="back" value="${fn:escapeXml(back)}">
                                                <select name="movieId" aria-label="Phim mới">
                                                    <c:forEach var="m" items="${movies}">
                                                        <c:if test="${m.movieId != s.movieId}">
                                                            <option value="${m.movieId}"><c:out value="${m.title}"/> (${m.duration}')</option>
                                                        </c:if>
                                                    </c:forEach>
                                                </select>
                                                <button class="btn btn-primary btn-sm">Gán</button>
                                            </form>
                                        </details>
                                    </c:if>
                                    <c:if test="${not s.started}">
                                        <form method="post" action="${ctx}/manager/showtimes/cancel"
                                              onsubmit="return confirm('Hủy suất chiếu này? ${s.soldCount} vé đã bán sẽ bị hủy và hoàn tiền.');">
                                            <input type="hidden" name="id" value="${s.showtimeId}">
                                            <input type="hidden" name="back" value="${fn:escapeXml(back)}">
                                            <button class="btn btn-sm btn-danger">Hủy suất</button>
                                        </form>
                                    </c:if>
                                </c:if>
                                <c:if test="${s.soldCount == 0}">
                                    <form method="post" action="${ctx}/manager/showtimes/delete" onsubmit="return confirm('Xóa vĩnh viễn suất chiếu này?');">
                                        <input type="hidden" name="id" value="${s.showtimeId}">
                                        <input type="hidden" name="back" value="${fn:escapeXml(back)}">
                                        <button class="btn btn-sm btn-quiet" title="Chỉ xóa được khi chưa phát sinh vé">Xóa</button>
                                    </form>
                                </c:if>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
