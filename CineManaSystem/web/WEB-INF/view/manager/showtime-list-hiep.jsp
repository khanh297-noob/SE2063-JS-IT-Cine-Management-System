<c:set var="pageTitle" value="Quản lý suất chiếu"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<div class="page-head">
    <div>
        <h1 class="page-title">Quản lý suất chiếu</h1>
        <p class="muted">${fn:escapeXml(branch.name)} · ${dateLabel}</p>
    </div>
    <div class="actions">
        <a class="btn btn-ghost" href="${ctx}/manager/schedule?branchId=${branchId}">Lịch tổng hợp</a>
        <a class="btn btn-primary" href="${ctx}/manager/showtimes/new?branchId=${branchId}&date=${date}">+ Tạo suất chiếu</a>
    </div>
</div>

<form method="get" action="${ctx}/manager/showtimes" class="filter-bar">
    <c:if test="${not empty branches}">
        <label>Chi nhánh
            <select name="branchId" onchange="this.form.submit()">
                <c:forEach var="b" items="${branches}">
                    <option value="${b.branchId}" ${b.branchId == branchId ? 'selected' : ''}>${fn:escapeXml(b.name)}</option>
                </c:forEach>
            </select>
        </label>
    </c:if>
    <c:if test="${empty branches}"><input type="hidden" name="branchId" value="${branchId}"></c:if>
    <a class="btn btn-ghost btn-sm" href="${ctx}/manager/showtimes?branchId=${branchId}&date=${prevDate}&roomId=${roomId}">‹</a>
    <label>Ngày <input type="date" name="date" value="${date}" onchange="this.form.submit()"></label>
    <a class="btn btn-ghost btn-sm" href="${ctx}/manager/showtimes?branchId=${branchId}&date=${nextDate}&roomId=${roomId}">›</a>
    <label>Phòng
        <select name="roomId" onchange="this.form.submit()">
            <option value="">Tất cả phòng</option>
            <c:forEach var="r" items="${rooms}">
                <option value="${r.roomId}" ${r.roomId == roomId ? 'selected' : ''}>${fn:escapeXml(r.roomName)}${r.active ? '' : ' (bảo trì)'}</option>
            </c:forEach>
        </select>
    </label>
</form>

<c:choose>
    <c:when test="${empty showtimes}">
        <div class="empty">Chưa có suất chiếu nào. (MSG01)</div>
    </c:when>
    <c:otherwise>
        <div class="table-wrap">
        <table class="table">
            <thead>
            <tr><th>Giờ</th><th>Phim</th><th>Phòng</th><th class="num">Giá gốc</th><th>Đã bán</th><th>Trạng thái</th><th></th></tr>
            </thead>
            <tbody>
            <c:forEach var="s" items="${showtimes}">
                <tr class="${s.cancelled ? 'row-muted' : ''}">
                    <td class="mono"><b>${s.startStr}</b> – ${s.endStr}<c:if test="${s.overnight}"> <span class="tag">+1 ngày</span></c:if></td>
                    <td>${fn:escapeXml(s.movieTitle)} <span class="age age-${s.ageRating}">${s.ageRating}</span><div class="muted small">${s.movieDuration} phút</div></td>
                    <td>${fn:escapeXml(s.roomName)} <span class="tag">${fn:escapeXml(s.roomType)}</span></td>
                    <td class="num"><fmt:formatNumber value="${s.basePrice}" pattern="#,##0"/> ₫</td>
                    <td>
                        <div class="bar"><span style="width:${s.occupancy}%"></span></div>
                        <span class="small">${s.soldCount}/${s.seatCount} (${s.occupancy}%)</span>
                    </td>
                    <td><span class="badge badge-${s.statusClass}">${s.displayStatus}</span></td>
                    <td class="row-actions">
                        <c:if test="${not s.cancelled}">
                            <a class="btn btn-ghost btn-sm" href="${ctx}/manager/showtimes/edit?id=${s.showtimeId}">Sửa</a>
                            <c:if test="${s.soldCount == 0 and not s.started}">
                                <details class="pop">
                                    <summary class="btn btn-ghost btn-sm">Gán phòng</summary>
                                    <form method="post" action="${ctx}/manager/showtimes/assign-room" class="pop-body">
                                        <input type="hidden" name="id" value="${s.showtimeId}">
                                        <input type="hidden" name="back" value="${fn:escapeXml(back)}">
                                        <select name="roomId">
                                            <c:forEach var="r" items="${rooms}">
                                                <c:if test="${r.active and r.roomId != s.roomId}">
                                                    <option value="${r.roomId}">${fn:escapeXml(r.roomName)} (${fn:escapeXml(r.roomType)})</option>
                                                </c:if>
                                            </c:forEach>
                                        </select>
                                        <button class="btn btn-primary btn-sm">Gán</button>
                                    </form>
                                </details>
                                <details class="pop">
                                    <summary class="btn btn-ghost btn-sm">Gán phim</summary>
                                    <form method="post" action="${ctx}/manager/showtimes/assign-movie" class="pop-body">
                                        <input type="hidden" name="id" value="${s.showtimeId}">
                                        <input type="hidden" name="back" value="${fn:escapeXml(back)}">
                                        <select name="movieId">
                                            <c:forEach var="m" items="${movies}">
                                                <c:if test="${m.movieId != s.movieId}">
                                                    <option value="${m.movieId}">${fn:escapeXml(m.title)} (${m.duration}')</option>
                                                </c:if>
                                            </c:forEach>
                                        </select>
                                        <button class="btn btn-primary btn-sm">Gán</button>
                                    </form>
                                </details>
                            </c:if>
                            <c:if test="${not s.started}">
                                <form method="post" action="${ctx}/manager/showtimes/cancel" class="inline"
                                      onsubmit="return confirm('Hủy suất chiếu này? ${s.soldCount} vé đã bán sẽ bị hủy và hoàn tiền.');">
                                    <input type="hidden" name="id" value="${s.showtimeId}">
                                    <input type="hidden" name="back" value="${fn:escapeXml(back)}">
                                    <button class="btn btn-danger btn-sm">Hủy suất</button>
                                </form>
                            </c:if>
                        </c:if>
                        <c:if test="${s.soldCount == 0}">
                            <form method="post" action="${ctx}/manager/showtimes/delete" class="inline"
                                  onsubmit="return confirm('Xóa vĩnh viễn suất chiếu này?');">
                                <input type="hidden" name="id" value="${s.showtimeId}">
                                <input type="hidden" name="back" value="${fn:escapeXml(back)}">
                                <button class="btn btn-ghost btn-sm" title="Chỉ xóa được khi chưa phát sinh vé">Xóa</button>
                            </form>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
        </div>
    </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
