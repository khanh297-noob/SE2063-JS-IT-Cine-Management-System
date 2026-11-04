<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<fmt:setLocale value="vi_VN"/>

<c:set var="st" value="${showtime}"/>
<c:set var="pageTitle" value="Chọn ghế - ${st.movieTitle}"/>
<c:set var="extraCss" value="booking-hiep"/>
<%@ include file="/WEB-INF/views/common/customer-header-hoang.jspf" %>

<div class="bk-page">
    <a class="back" href="${ctx}/booking?movieId=${st.movieId}&date=${st.dateIso}">&larr; Chọn suất khác</a>
    <ol class="bk-steps">
        <li class="done">1. Chọn phim &amp; suất chiếu</li>
        <li class="active">2. Chọn ghế</li>
        <li>3. Đồ ăn &amp; nước</li>
        <li>4. Thanh toán VNPay</li>
        <li>5. Nhận vé QR</li>
    </ol>

    <section class="bk-panel">
        <h1 class="page-title"><c:out value="${st.movieTitle}"/> <span class="bk-age age-${st.ageRating}">${st.ageRating}</span></h1>
        <p class="bk-muted"><c:out value="${st.branchName}"/> · <c:out value="${st.roomName}"/> (<c:out value="${st.roomType}"/>)
            · <b>${st.startStr}</b> ${st.weekday} ${st.dateStr} · ${st.movieDuration} phút</p>
        <div class="bk-price-legend">
            <c:forEach var="p" items="${prices}">
                <span><i class="bk-dot" style="box-shadow: inset 0 -3px 0 ${fn:escapeXml(p.color)}"></i>
                    <c:out value="${p.seatType}"/>: <b><fmt:formatNumber value="${p.price}" pattern="#,##0"/> ₫</b></span>
            </c:forEach>
        </div>
    </section>

    <c:if test="${not empty pendingBookingId}">
        <div class="notice notice-info">Bạn đang có đơn chờ thanh toán cho suất này.
            <a href="${ctx}/payment/checkout?bookingId=${pendingBookingId}">Tiếp tục thanh toán &rarr;</a></div>
    </c:if>
    <c:if test="${not empty conflicts}">
        <div class="notice notice-info">
            <b>Lưu ý trùng giờ:</b> bạn đã có vé cho
            <c:forEach var="o" items="${conflicts}" varStatus="vs">${vs.index > 0 ? '; ' : ''}<i><c:out value="${o.movieTitle}"/></i> ${o.startStr} - ${o.endStr} (<c:out value="${o.branchName}"/>)</c:forEach>.
            Bạn vẫn có thể tiếp tục nếu mua cho người khác.
        </div>
    </c:if>
    <c:if test="${not empty hold}">
        <div class="notice notice-ok">Bạn đang giữ ghế <b>${hold.seatCodes}</b> (còn <span class="bk-mono" data-countdown="${hold.remainingSeconds}"></span>).
            <a href="${ctx}/booking/fnb?showtimeId=${st.showtimeId}">Tiếp tục chọn đồ ăn &rarr;</a></div>
    </c:if>

    <form method="post" action="${ctx}/booking/hold" id="seatForm">
        <input type="hidden" name="showtimeId" value="${st.showtimeId}">
        <div class="bk-grid">
            <section class="bk-panel bk-seat-panel">
                <div class="bk-screen">MÀN HÌNH</div>
                <div class="bk-seat-map">
                    <c:forEach var="row" items="${rows}">
                        <div class="bk-seat-row">
                            <span class="bk-row-label">${row.label}</span>
                            <div class="bk-seat-cols" style="grid-template-columns: repeat(${seatCols}, 34px)">
                                <c:forEach var="sv" items="${row.seats}">
                                    <c:choose>
                                        <c:when test="${sv.coupleRight}"><%-- đã vẽ chung với ô bên trái của ghế đôi --%></c:when>
                                        <c:when test="${sv.coupleLeft}">
                                            <%-- Ghế đôi: 1 ghế rộng 2 chỗ liền nhau, chọn là giữ cả 2 chỗ (2 vé) --%>
                                            <c:set var="mate" value="${sv.pair}"/>
                                            <label class="bk-seat bk-couple state-${sv.unitState}" style="grid-column: ${sv.colNum} / span 2; border-bottom-color: ${fn:escapeXml(sv.color)}"
                                                   title="Ghế đôi ${sv.unitCode} · ${fn:escapeXml(sv.seatType)} · 2 chỗ${sv.broken || mate.broken ? ' · Ghế hỏng' : ''}" ${sv.broken || mate.broken ? 'data-broken="1"' : ''}>
                                                <input type="checkbox" name="seatIds" value="${sv.seatId}" data-code="${sv.unitCode}" data-price="${sv.unitPrice}"
                                                       data-mate="${mate.seatId}" data-seats="2"
                                                       ${sv.mine && mate.mine ? 'checked' : ''} ${sv.selectable ? '' : 'disabled'}>
                                                <input type="checkbox" name="seatIds" value="${mate.seatId}" class="bk-mate" hidden tabindex="-1" aria-hidden="true"
                                                       ${sv.mine && mate.mine ? 'checked' : ''}>
                                                <span>${sv.seatNumber}-${mate.seatNumber}</span>
                                            </label>
                                        </c:when>
                                        <c:otherwise>
                                            <label class="bk-seat state-${sv.state}" style="grid-column: ${sv.colNum}; border-bottom-color: ${fn:escapeXml(sv.color)}"
                                                   title="${sv.code} · ${fn:escapeXml(sv.seatType)}${sv.broken ? ' · Ghế hỏng' : ''}" ${sv.broken ? 'data-broken="1"' : ''}>
                                                <input type="checkbox" name="seatIds" value="${sv.seatId}" data-code="${sv.code}" data-price="${sv.price}"
                                                       ${sv.mine ? 'checked' : ''} ${sv.selectable ? '' : 'disabled'}>
                                                <span>${sv.seatNumber}</span>
                                            </label>
                                        </c:otherwise>
                                    </c:choose>
                                </c:forEach>
                            </div>
                            <span class="bk-row-label">${row.label}</span>
                        </div>
                    </c:forEach>
                </div>
                <div class="bk-legend">
                    <span><i class="bk-dot"></i>Trống</span>
                    <span><i class="bk-dot state-selected"></i>Đang chọn</span>
                    <span><i class="bk-dot state-held"></i>Người khác đang giữ</span>
                    <span><i class="bk-dot state-sold"></i>Đã bán / hỏng</span>
                    <span><i class="bk-dot bk-dot-couple"></i>Ghế đôi (2 chỗ liền, tính 2 vé)</span>
                </div>
            </section>
            <aside class="bk-panel bk-summary">
                <h3>Ghế đã chọn</h3>
                <p id="selCodes" class="bk-sel">Chưa chọn ghế</p>
                <div class="bk-line"><span>Số ghế</span><b><span id="selCount">0</span> / ${maxSeats}</b></div>
                <div class="bk-line total"><span>Tạm tính</span><b id="selTotal">0 ₫</b></div>
                <p class="bk-muted bk-small">Ghế được giữ ${holdMinutes} phút để bạn chọn đồ ăn và thanh toán.</p>
                <button class="btn btn-primary bk-block-btn" id="continueBtn" type="submit" disabled>Giữ ghế &amp; tiếp tục</button>
            </aside>
        </div>
    </form>
</div>

<script>
(function () {
    var max = ${maxSeats};
    // Mỗi ô hiển thị là 1 checkbox; ghế đôi có thêm checkbox ẩn (.bk-mate) cho chỗ thứ 2, luôn đi cùng ô chính.
    var boxes = Array.prototype.slice.call(document.querySelectorAll('#seatForm input[name=seatIds]:not(.bk-mate)'));
    function mateOf(b) { return b.dataset.mate ? b.parentNode.querySelector('.bk-mate') : null; }
    function seatsOf(b) { return parseInt(b.dataset.seats || '1', 10); }
    var fmt = new Intl.NumberFormat('vi-VN');
    function refresh() {
        var codes = [], total = 0, count = 0;
        boxes.forEach(function (b) {
            var mate = mateOf(b);
            if (mate) mate.checked = b.checked;
            b.parentNode.classList.toggle('state-selected', b.checked);
            if (b.checked) { codes.push(b.dataset.code); total += parseFloat(b.dataset.price); count += seatsOf(b); }
        });
        document.getElementById('selCodes').textContent = codes.length ? codes.join(', ') : 'Chưa chọn ghế';
        document.getElementById('selCount').textContent = count;
        document.getElementById('selTotal').textContent = fmt.format(total) + ' ₫';
        document.getElementById('continueBtn').disabled = count === 0;
    }
    boxes.forEach(function (b) {
        b.addEventListener('change', function () {
            var n = boxes.reduce(function (sum, x) { return sum + (x.checked ? seatsOf(x) : 0); }, 0);
            if (n > max) {
                b.checked = false;
                alert('Mỗi đơn chỉ được chọn tối đa ' + max + ' ghế' + (seatsOf(b) > 1 ? ' (ghế đôi tính 2 ghế).' : '.'));
            }
            refresh();
        });
    });
    refresh();

    setInterval(function () {
        fetch('${ctx}/booking/seat-status?showtimeId=${st.showtimeId}', {headers: {'X-Requested-With': 'XMLHttpRequest'}})
            .then(function (r) { return r.json(); })
            .then(function (states) {
                boxes.forEach(function (b) {
                    var label = b.parentNode;
                    if (b.checked || label.dataset.broken) return;
                    var s = states[b.value] || 'available';
                    var mate = mateOf(b);
                    // Ghế đôi: chỉ cần 1 trong 2 chỗ bận là cả ghế bận
                    if (mate && s === 'available') s = states[mate.value] || 'available';
                    ['available', 'held', 'sold', 'mine'].forEach(function (k) { label.classList.remove('state-' + k); });
                    label.classList.add('state-' + s);
                    b.disabled = (s === 'held' || s === 'sold');
                });
            }).catch(function () {});
    }, 10000);

    document.querySelectorAll('[data-countdown]').forEach(function (el) {
        var left = parseInt(el.dataset.countdown, 10);
        (function tick() {
            el.textContent = Math.floor(left / 60) + ':' + ('0' + (left % 60)).slice(-2);
            if (left-- > 0) setTimeout(tick, 1000); else location.reload();
        })();
    });
})();
</script>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
