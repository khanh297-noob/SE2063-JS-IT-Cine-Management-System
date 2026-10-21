<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<fmt:setLocale value="vi_VN"/>
<%-- Tạo / sửa suất chiếu. Giá suất KHÔNG nhập tay: tự tính theo phòng (phụ thu loại phòng) + ngày chiếu (T7/CN +20.000đ).
     Servlet: ShowtimeController (showtime, branch, rooms, movies, statuses, standardPrice, weekendSurcharge, defaultDate, errors). --%>
<c:set var="editing" value="${not empty showtime}"/>
<c:set var="pageTitle" value="${editing ? 'Sửa suất chiếu' : 'Tạo suất chiếu'}"/>
<c:set var="nav" value="showtimes"/>
<c:set var="extraCss" value="booking-hiep"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>
<c:set var="vMovie" value="${not empty param.movieId ? param.movieId : showtime.movieId}"/>
<c:set var="vRoom" value="${not empty param.roomId ? param.roomId : showtime.roomId}"/>
<c:set var="vDate" value="${not empty param.startTime ? param.date : (editing ? showtime.dateIso : (not empty defaultDate ? defaultDate : ''))}"/>
<c:set var="vStart" value="${not empty param.startTime ? param.startTime : showtime.startStr}"/>
<c:set var="vStatus" value="${not empty param.status ? param.status : (editing ? showtime.status : 'Open')}"/>
<c:set var="locked" value="${editing and showtime.soldCount > 0}"/>

<a class="back" href="${ctx}/manager/showtimes?branchId=${branch.branchId}&date=${vDate}">&larr; Danh sách suất chiếu</a>
<div class="page-head">
    <div>
        <h1 class="page-title">${pageTitle}</h1>
        <p class="page-sub"><c:out value="${branch.name}"/></p>
    </div>
</div>

<div class="bk-grid">
    <section class="form-card" style="max-width: none">
        <c:if test="${locked}">
            <div class="notice notice-info">Suất chiếu đã bán ${showtime.soldCount} vé: chỉ được đổi <b>trạng thái</b>. Muốn đổi lịch hãy Hủy suất (khách được hoàn tiền).</div>
        </c:if>
        <form method="post" action="${ctx}/manager/showtimes/save" id="stForm">
            <input type="hidden" name="id" value="${showtime.showtimeId}">
            <div class="field">
                <label for="movieId" class="required">Phim</label>
                <select name="movieId" id="movieId" required ${locked ? 'disabled' : ''}>
                    <option value="">-- Chọn phim --</option>
                    <c:forEach var="m" items="${movies}">
                        <option value="${m.movieId}" ${m.movieId == vMovie ? 'selected' : ''}><c:out value="${m.title}"/> · ${m.duration} phút</option>
                    </c:forEach>
                </select>
                <p class="hint">Chỉ hiện phim đang / sắp chiếu đã được phân cho chi nhánh này.</p>
            </div>
            <div class="field">
                <label for="roomId" class="required">Phòng chiếu</label>
                <select name="roomId" id="roomId" required ${locked ? 'disabled' : ''}>
                    <option value="">-- Chọn phòng --</option>
                    <c:forEach var="r" items="${rooms}">
                        <option value="${r.roomId}" data-surcharge="${r.roomSurcharge}" ${r.roomId == vRoom ? 'selected' : ''} ${r.active ? '' : 'disabled'}>
                            <c:out value="${r.roomName}"/> · <c:out value="${r.roomType}"/> · ${r.seatCount} ghế${r.active ? '' : ' · '}${r.statusLabel}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="field-row">
                <div class="field">
                    <label for="date" class="required">Ngày chiếu</label>
                    <input type="date" name="date" id="date" value="${vDate}" required ${locked ? 'readonly' : ''}>
                </div>
                <div class="field">
                    <label for="startTime" class="required">Giờ bắt đầu</label>
                    <input type="time" name="startTime" id="startTime" value="${vStart}" required ${locked ? 'readonly' : ''}
                           style="width: 100%; font: inherit; padding: 9px 12px; border: 1px solid var(--line); border-radius: var(--radius)">
                </div>
                <div class="field">
                    <label for="endTime">Giờ kết thúc</label>
                    <input type="text" id="endTime" value="${editing ? showtime.endStr : ''}" readonly placeholder="Tự tính">
                </div>
            </div>
            <c:if test="${locked}">
                <input type="hidden" name="movieId" value="${showtime.movieId}">
                <input type="hidden" name="roomId" value="${showtime.roomId}">
            </c:if>
            <div class="field-row">
                <div class="field">
                    <span class="label">Giá suất chiếu (tự tính)</span>
                    <div class="bk-price-box">
                        <b id="pricePreview"><c:choose><c:when test="${editing}"><fmt:formatNumber value="${showtime.basePrice}" pattern="#,##0"/> ₫</c:when><c:otherwise>--</c:otherwise></c:choose></b>
                        <div class="hint" id="priceNote">Chọn phòng và ngày chiếu để xem giá.</div>
                    </div>
                </div>
                <div class="field">
                    <label for="status">Trạng thái</label>
                    <select name="status" id="status">
                        <c:forEach var="stt" items="${statuses}">
                            <option value="${stt}" ${stt == vStatus ? 'selected' : ''}>
                                <c:choose><c:when test="${stt == 'Scheduled'}">Chưa mở bán</c:when><c:when test="${stt == 'Open'}">Mở bán</c:when><c:otherwise>Ngừng bán</c:otherwise></c:choose>
                            </option>
                        </c:forEach>
                    </select>
                </div>
            </div>
            <div id="overlapBox" class="bk-check" hidden></div>
            <div class="form-actions">
                <button class="btn btn-primary" type="submit">${editing ? 'Lưu thay đổi' : 'Tạo suất chiếu'}</button>
                <a class="btn" href="${ctx}/manager/showtimes?branchId=${branch.branchId}&date=${vDate}">Huỷ</a>
            </div>
        </form>
    </section>
    <aside class="bk-panel">
        <h3>Quy tắc xếp lịch</h3>
        <ul class="bk-rules">
            <li><b>Giá vé</b> = <fmt:formatNumber value="${standardPrice}" pattern="#,##0"/> ₫ + phụ thu loại phòng
                + <fmt:formatNumber value="${weekendSurcharge}" pattern="#,##0"/> ₫ nếu chiếu Thứ 7 / Chủ nhật. Ghế VIP / Sweetbox cộng thêm phụ thu loại ghế khi bán.</li>
            <li><b>BR-01</b> Phòng đang bảo trì không được xếp suất mới.</li>
            <li><b>BR-02</b> Hai suất liên tiếp cùng phòng cách nhau tối thiểu 15 phút dọn phòng.</li>
            <li><b>GB-01</b> Chỉ phim đang / sắp chiếu, ngày chiếu từ ngày khởi chiếu và trong thời gian phim được chiếu tại chi nhánh.</li>
            <li><b>GB-05</b> Quản lý chỉ xếp lịch phòng thuộc chi nhánh mình.</li>
            <li>Giờ kết thúc = giờ bắt đầu + thời lượng phim (hỗ trợ suất qua nửa đêm).</li>
            <li>Suất đã bán vé không đổi được phim / phòng / giờ, hãy <i>Hủy suất</i> (khách được hoàn tiền).</li>
        </ul>
    </aside>
</div>

<script>
(function () {
    var ids = ['movieId', 'roomId', 'date', 'startTime'];
    var box = document.getElementById('overlapBox');
    var endEl = document.getElementById('endTime');
    var priceEl = document.getElementById('pricePreview');
    var noteEl = document.getElementById('priceNote');
    var excludeId = '${editing ? showtime.showtimeId : ''}';
    var fmt = new Intl.NumberFormat('vi-VN');
    var timer;
    function esc(s) { var d = document.createElement('div'); d.textContent = s; return d.innerHTML; }
    function check() {
        var p = new URLSearchParams();
        ids.forEach(function (id) { p.set(id, document.getElementById(id).value); });
        if (excludeId) p.set('excludeId', excludeId);
        fetch('${ctx}/manager/showtimes/check-overlap?' + p.toString(), {headers: {'X-Requested-With': 'XMLHttpRequest'}})
            .then(function (r) { return r.json(); })
            .then(function (d) {
                if (d.price !== undefined) {
                    priceEl.textContent = fmt.format(d.price) + ' ₫';
                    noteEl.textContent = d.weekend ? 'Đã cộng phụ thu cuối tuần.' : 'Ngày thường.';
                }
                if (!d.ready) {
                    box.hidden = !d.error;
                    if (d.error) { box.className = 'bk-check bad'; box.textContent = d.error; }
                    return;
                }
                endEl.value = d.end + (d.nextDay ? ' (+1 ngày)' : '');
                box.hidden = false;
                if (d.ok) {
                    box.className = 'bk-check good';
                    box.textContent = 'Không trùng lịch (đã tính ' + d.buffer + ' phút dọn phòng).';
                } else {
                    var html = 'Trùng lịch với:<ul>';
                    d.conflicts.forEach(function (c) { html += '<li>' + esc(c.movie) + ' · ' + c.start + ' - ' + c.end + ' · ' + c.date + '</li>'; });
                    box.className = 'bk-check bad';
                    box.innerHTML = html + '</ul>Cần cách nhau tối thiểu ' + d.buffer + ' phút.';
                }
            }).catch(function () {});
    }
    ids.forEach(function (id) {
        document.getElementById(id).addEventListener('change', function () { clearTimeout(timer); timer = setTimeout(check, 200); });
    });
    check();
})();
</script>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
