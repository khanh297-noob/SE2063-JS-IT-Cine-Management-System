<c:set var="pageTitle" value="Chọn ghế"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<c:set var="st" value="${showtime}"/>
<div class="steps"><span class="active">1. Chọn ghế</span><span>2. Giữ ghế &amp; thanh toán</span><span>3. Nhận vé QR</span></div>

<div class="booking-head card">
    <div>
        <h1 class="page-title">${fn:escapeXml(st.movieTitle)} <span class="age age-${st.ageRating}">${st.ageRating}</span></h1>
        <p class="muted">${fn:escapeXml(st.branchName)} · ${fn:escapeXml(st.roomName)} (${st.roomType}) · <b>${st.startStr}</b> ${st.weekday} ${st.dateStr} · ${st.movieDuration} phút</p>
    </div>
    <div class="price-legend">
        <c:forEach var="p" items="${prices}">
            <span class="legend-item"><i class="seat-dot type-${p.key}"></i>${p.key}: <b><fmt:formatNumber value="${p.value}" pattern="#,##0"/> ₫</b></span>
        </c:forEach>
    </div>
</div>

<c:if test="${not empty conflicts}">
    <div class="alert alert-warn">
        <b>Lưu ý trùng giờ:</b> bạn đã có vé cho
        <c:forEach var="o" items="${conflicts}" varStatus="vs">${vs.index > 0 ? '; ' : ''}<i>${fn:escapeXml(o.movieTitle)}</i> ${o.startStr}–${o.endStr} (${fn:escapeXml(o.branchName)})</c:forEach>.
        Bạn vẫn có thể tiếp tục nếu mua cho người khác.
    </div>
</c:if>
<c:if test="${not empty hold}">
    <div class="alert alert-info">Bạn đang giữ ghế <b>${hold.seatCodes}</b> (còn <span class="mono" data-countdown="${hold.remainingSeconds}"></span>).
        <a href="${ctx}/booking/checkout?showtimeId=${st.showtimeId}">Tiếp tục thanh toán →</a></div>
</c:if>

<form method="post" action="${ctx}/booking/hold" id="seatForm">
    <input type="hidden" name="showtimeId" value="${st.showtimeId}">
    <div class="grid-seat">
        <section class="card seat-card">
            <div class="screen">MÀN HÌNH</div>
            <div class="seat-map">
                <c:forEach var="row" items="${rows}">
                    <div class="seat-row">
                        <span class="row-label">${row.label}</span>
                        <c:forEach var="sv" items="${row.seats}">
                            <label class="seat type-${sv.seatType} state-${sv.state}" title="${sv.code} · ${sv.seatType}" data-id="${sv.seatId}">
                                <input type="checkbox" name="seatIds" value="${sv.seatId}" data-code="${sv.code}" data-price="${sv.price}"
                                       ${sv.mine ? 'checked' : ''} ${sv.selectable ? '' : 'disabled'}>
                                <span>${sv.seat.seatNumber}</span>
                            </label>
                        </c:forEach>
                        <span class="row-label">${row.label}</span>
                    </div>
                </c:forEach>
            </div>
            <div class="legend">
                <span><i class="seat-dot state-available"></i>Trống</span>
                <span><i class="seat-dot state-selected"></i>Đang chọn</span>
                <span><i class="seat-dot state-held"></i>Người khác đang giữ</span>
                <span><i class="seat-dot state-sold"></i>Đã bán</span>
                <span><i class="seat-dot type-VIP"></i>VIP</span>
                <span><i class="seat-dot type-Sweetbox"></i>Sweetbox</span>
            </div>
        </section>
        <aside class="card summary">
            <h3>Ghế đã chọn</h3>
            <p id="selCodes" class="sel-codes muted">Chưa chọn ghế</p>
            <div class="sum-line"><span>Số ghế</span><b id="selCount">0</b> / ${maxSeats}</div>
            <div class="sum-line total"><span>Tạm tính</span><b id="selTotal">0 ₫</b></div>
            <p class="muted small">Ghế sẽ được giữ trong ${holdMinutes} phút để bạn thanh toán.</p>
            <button class="btn btn-primary btn-block" id="continueBtn" type="submit" disabled>Giữ ghế &amp; tiếp tục</button>
        </aside>
    </div>
</form>

<script>
(function () {
    var max = ${maxSeats};
    var boxes = Array.prototype.slice.call(document.querySelectorAll('#seatForm input[name=seatIds]'));
    var fmt = new Intl.NumberFormat('vi-VN');
    function refresh() {
        var codes = [], total = 0;
        boxes.forEach(function (b) {
            b.parentNode.classList.toggle('state-selected', b.checked);
            if (b.checked) { codes.push(b.dataset.code); total += parseFloat(b.dataset.price); }
        });
        document.getElementById('selCodes').textContent = codes.length ? codes.join(', ') : 'Chưa chọn ghế';
        document.getElementById('selCount').textContent = codes.length;
        document.getElementById('selTotal').textContent = fmt.format(total) + ' ₫';
        document.getElementById('continueBtn').disabled = codes.length === 0;
    }
    boxes.forEach(function (b) {
        b.addEventListener('change', function () {
            var n = boxes.filter(function (x) { return x.checked; }).length;
            if (n > max) { b.checked = false; alert('Mỗi đơn chỉ được chọn tối đa ' + max + ' ghế.'); }
            refresh();
        });
    });
    refresh();

    // AJAX: cap nhat ghe vua bi nguoi khac giu/ban moi 10 giay (SDS - AJAX Calls)
    function poll() {
        fetch('${ctx}/booking/seat-status?showtimeId=${st.showtimeId}', {headers: {'X-Requested-With': 'XMLHttpRequest'}})
            .then(function (r) { return r.json(); })
            .then(function (states) {
                boxes.forEach(function (b) {
                    if (b.checked) return;
                    var s = states[b.value] || 'available';
                    var label = b.parentNode;
                    ['available', 'held', 'sold', 'mine'].forEach(function (k) { label.classList.remove('state-' + k); });
                    label.classList.add('state-' + s);
                    b.disabled = (s === 'held' || s === 'sold');
                });
            }).catch(function () {});
    }
    setInterval(poll, 10000);

    document.querySelectorAll('[data-countdown]').forEach(function (el) {
        var left = parseInt(el.dataset.countdown, 10);
        (function tick() {
            el.textContent = Math.floor(left / 60) + ':' + ('0' + (left % 60)).slice(-2);
            if (left-- > 0) setTimeout(tick, 1000); else location.reload();
        })();
    });
})();
</script>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
