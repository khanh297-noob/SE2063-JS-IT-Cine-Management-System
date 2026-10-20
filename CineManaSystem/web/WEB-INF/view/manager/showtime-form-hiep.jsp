<c:set var="editing" value="${not empty showtime}"/>
<c:set var="pageTitle" value="${editing ? 'Sửa suất chiếu' : 'Tạo suất chiếu'}"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<c:set var="vMovie" value="${not empty param.movieId ? param.movieId : showtime.movieId}"/>
<c:set var="vRoom" value="${not empty param.roomId ? param.roomId : showtime.roomId}"/>
<c:set var="vDate" value="${not empty param.startTime ? param.date : (editing ? showtime.dateIso : (not empty defaultDate ? defaultDate : ''))}"/>
<c:set var="vStart" value="${not empty param.startTime ? param.startTime : showtime.startStr}"/>
<c:set var="vPrice" value="${not empty param.basePrice ? param.basePrice : (editing ? showtime.basePrice.longValue() : '')}"/>
<c:set var="vStatus" value="${not empty param.status ? param.status : (editing ? showtime.status : 'Open')}"/>
<c:set var="locked" value="${editing and showtime.soldCount > 0}"/>

<div class="page-head">
    <div>
        <h1 class="page-title">${pageTitle}</h1>
        <p class="muted">${fn:escapeXml(branch.name)}</p>
    </div>
    <a class="btn btn-ghost" href="${ctx}/manager/showtimes?branchId=${branch.branchId}&date=${vDate}">← Danh sách</a>
</div>

<div class="grid-2">
<section class="card">
    <c:if test="${not empty requestScope.errorMsg}"><div class="alert alert-error">${fn:escapeXml(requestScope.errorMsg)}</div></c:if>
    <c:if test="${locked}">
        <div class="alert alert-info">Suất chiếu đã bán ${showtime.soldCount} vé: chỉ được đổi <b>giá</b> (áp dụng cho vé bán sau) và <b>trạng thái</b>.</div>
    </c:if>
    <form method="post" action="${ctx}/manager/showtimes/save" class="form" id="stForm">
        <input type="hidden" name="id" value="${showtime.showtimeId}">
        <label>Phim *
            <select name="movieId" id="movieId" required ${locked ? 'disabled' : ''}>
                <option value="">-- Chọn phim --</option>
                <c:forEach var="m" items="${movies}">
                    <option value="${m.movieId}" data-duration="${m.duration}" ${m.movieId == vMovie ? 'selected' : ''}>
                        ${fn:escapeXml(m.title)} · ${m.duration}' · ${m.status}</option>
                </c:forEach>
            </select>
        </label>
        <label>Phòng chiếu *
            <select name="roomId" id="roomId" required ${locked ? 'disabled' : ''}>
                <option value="">-- Chọn phòng --</option>
                <c:forEach var="r" items="${rooms}">
                    <option value="${r.roomId}" ${r.roomId == vRoom ? 'selected' : ''} ${r.active ? '' : 'disabled'}>
                        ${fn:escapeXml(r.roomName)} · ${fn:escapeXml(r.roomType)} · ${r.seatCount} ghế${r.active ? '' : ' · '}${r.statusLabel}</option>
                </c:forEach>
            </select>
        </label>
        <div class="row">
            <label>Ngày chiếu *<input type="date" name="date" id="date" value="${vDate}" required ${locked ? 'readonly' : ''}></label>
            <label>Giờ bắt đầu *<input type="time" name="startTime" id="startTime" value="${vStart}" required ${locked ? 'readonly' : ''}></label>
            <label>Giờ kết thúc<input type="text" id="endTime" value="${editing ? showtime.endStr : ''}" readonly placeholder="Tự tính"></label>
        </div>
        <c:if test="${locked}">
            <input type="hidden" name="movieId" value="${showtime.movieId}">
            <input type="hidden" name="roomId" value="${showtime.roomId}">
        </c:if>
        <div class="row">
            <label>Giá vé cơ bản (VNĐ) *<input type="number" name="basePrice" min="1000" step="1000" value="${vPrice}" required></label>
            <label>Trạng thái
                <select name="status">
                    <c:forEach var="st" items="${statuses}">
                        <option value="${st}" ${st == vStatus ? 'selected' : ''}>
                            <c:choose><c:when test="${st == 'Scheduled'}">Chưa mở bán</c:when><c:when test="${st == 'Open'}">Mở bán</c:when><c:otherwise>Ngừng bán</c:otherwise></c:choose>
                        </option>
                    </c:forEach>
                </select>
            </label>
        </div>
        <div id="overlapBox" class="overlap hidden"></div>
        <button class="btn btn-primary" type="submit" id="saveBtn">${editing ? 'Lưu thay đổi' : 'Tạo suất chiếu'}</button>
    </form>
</section>
<aside class="card">
    <h3>Quy tắc xếp lịch</h3>
    <ul class="rules">
        <li><b>BR-01</b> Phòng đang bảo trì không được xếp suất mới.</li>
        <li><b>BR-02</b> Hai suất liên tiếp cùng phòng cách nhau tối thiểu thời gian dọn phòng (mặc định 15 phút).</li>
        <li><b>GB-01</b> Chỉ phim <i>Now Showing</i>/<i>Coming Soon</i>, ngày chiếu ≥ ngày khởi chiếu.</li>
        <li><b>GB-05</b> Quản lý chỉ xếp lịch phòng thuộc chi nhánh mình.</li>
        <li>Giờ kết thúc = giờ bắt đầu + thời lượng phim (hỗ trợ suất qua nửa đêm).</li>
        <li>Suất đã bán vé không đổi được phim/phòng/giờ – hãy <i>Hủy suất</i> (khách được hoàn tiền).</li>
        <li>Giá vé cuối = giá gốc + phụ thu loại ghế (SeatType) + phụ thu loại phòng (RoomType) – <b>BR-05</b>.</li>
    </ul>
</aside>
</div>

<script>
(function () {
    var ids = ['movieId', 'roomId', 'date', 'startTime'];
    var box = document.getElementById('overlapBox');
    var endEl = document.getElementById('endTime');
    var excludeId = '${editing ? showtime.showtimeId : ''}';
    var timer;
    function esc(s) { var d = document.createElement('div'); d.textContent = s; return d.innerHTML; }
    function check() {
        var p = new URLSearchParams();
        ids.forEach(function (id) { p.set(id, document.getElementById(id).value); });
        if (excludeId) p.set('excludeId', excludeId);
        fetch('${ctx}/manager/showtimes/check-overlap?' + p.toString(), {headers: {'X-Requested-With': 'XMLHttpRequest'}})
            .then(function (r) { return r.json(); })
            .then(function (d) {
                if (!d.ready) { box.className = 'overlap hidden'; if (d.error) { box.className = 'overlap bad'; box.textContent = d.error; } return; }
                endEl.value = d.end + (d.nextDay ? ' (+1 ngày)' : '');
                if (d.ok) {
                    box.className = 'overlap good';
                    box.textContent = '✓ Không trùng lịch (đã tính ' + d.buffer + ' phút dọn phòng).';
                } else {
                    var html = '✗ Trùng lịch với:<ul>';
                    d.conflicts.forEach(function (c) { html += '<li>' + esc(c.movie) + ' · ' + c.start + '–' + c.end + ' · ' + c.date + '</li>'; });
                    box.className = 'overlap bad';
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
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
