<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<fmt:setLocale value="vi_VN"/>

<c:set var="st" value="${hold.showtime}"/>
<c:set var="pageTitle" value="Đồ ăn & nước"/>
<c:set var="extraCss" value="booking-hiep"/>
<%@ include file="/WEB-INF/views/common/customer-header-hoang.jspf" %>

<div class="bk-page">
    <ol class="bk-steps">
        <li class="done">1. Chọn phim &amp; suất chiếu</li>
        <li class="done">2. Chọn ghế</li>
        <li class="active">3. Đồ ăn &amp; nước</li>
        <li>4. Thanh toán VNPay</li>
        <li>5. Nhận vé QR</li>
    </ol>

    <form method="post" action="${ctx}/booking/fnb" id="fnbForm">
        <input type="hidden" name="showtimeId" value="${st.showtimeId}">
        <div class="bk-grid">
            <section class="bk-panel">
                <h2>Thực đơn tại <c:out value="${st.branchName}"/></h2>
                <p class="bk-muted bk-small">Giá theo chi nhánh. Tối đa ${maxQty} phần mỗi món. Nhận đồ tại quầy khi xuất trình vé.</p>
                <c:choose>
                    <c:when test="${empty fnbMenu}">
                        <div class="empty">Chi nhánh này hiện chưa mở bán đồ ăn &amp; nước online.</div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-wrap">
                            <table class="bk-fnb-table">
                                <thead><tr><th>Món</th><th class="bk-num">Đơn giá</th><th class="bk-num">Số lượng</th></tr></thead>
                                <tbody>
                                <c:forEach var="f" items="${fnbMenu}">
                                    <tr>
                                        <td><b><c:out value="${f.itemName}"/></b><br><span class="bk-fnb-cat"><c:out value="${f.category}"/></span></td>
                                        <td class="bk-num"><fmt:formatNumber value="${f.price}" pattern="#,##0"/> ₫</td>
                                        <td class="bk-num">
                                            <span class="bk-qty">
                                                <button type="button" class="btn btn-sm" data-step="-1" aria-label="Bớt">&minus;</button>
                                                <input type="number" name="qty_${f.itemId}" value="${f.quantity}" min="0" max="${maxQty}"
                                                       data-price="${f.price}" aria-label="Số lượng ${fn:escapeXml(f.itemName)}">
                                                <button type="button" class="btn btn-sm" data-step="1" aria-label="Thêm">+</button>
                                            </span>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>

            <aside class="bk-panel bk-summary">
                <div class="bk-timer" id="timerBox"><span>Giữ ghế còn</span><b id="timer">--:--</b></div>
                <h3><c:out value="${st.movieTitle}"/></h3>
                <p class="bk-muted bk-small"><c:out value="${st.branchName}"/> · <c:out value="${st.roomName}"/><br><b>${st.startStr}</b> ${st.weekday} ${st.dateStr}</p>
                <div class="bk-line"><span>Ghế ${hold.seatCodes}</span><b><fmt:formatNumber value="${hold.total}" pattern="#,##0"/> ₫</b></div>
                <div class="bk-line"><span>Đồ ăn &amp; nước</span><b><span id="fnbTotal">0</span> ₫</b></div>
                <div class="bk-line total"><span>Tạm tính</span><b><span id="grandTotal">0</span> ₫</b></div>
                <p class="bk-muted bk-small">Mã giảm giá được áp ở bước thanh toán.</p>
                <button class="btn btn-primary bk-block-btn" type="submit" name="action" value="pay" id="nextBtn">Thanh toán &rarr;</button>
                <button class="btn bk-block-btn" type="submit" name="action" value="skip" formnovalidate>Không mua đồ ăn, thanh toán luôn</button>
                <button class="btn btn-quiet bk-block-btn" type="submit" name="action" value="release" formnovalidate>Nhả ghế, chọn lại</button>
            </aside>
        </div>
    </form>
</div>

<script>
(function () {
    var ticket = ${hold.total};
    var inputs = document.querySelectorAll('#fnbForm input[type=number]');
    function fmt(n) { return Math.round(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.'); }
    function recalc() {
        var sum = 0;
        inputs.forEach(function (i) { sum += (parseInt(i.value, 10) || 0) * parseFloat(i.dataset.price); });
        document.getElementById('fnbTotal').textContent = fmt(sum);
        document.getElementById('grandTotal').textContent = fmt(ticket + sum);
    }
    document.querySelectorAll('.bk-qty').forEach(function (q) {
        var input = q.querySelector('input');
        q.querySelectorAll('button').forEach(function (b) {
            b.addEventListener('click', function () {
                var v = (parseInt(input.value, 10) || 0) + parseInt(b.dataset.step, 10);
                input.value = Math.max(0, Math.min(parseInt(input.max, 10), v));
                recalc();
            });
        });
        input.addEventListener('input', recalc);
    });
    recalc();

    var left = ${hold.remainingSeconds};
    var el = document.getElementById('timer'), box = document.getElementById('timerBox'), btn = document.getElementById('nextBtn');
    (function tick() {
        el.textContent = Math.floor(left / 60) + ':' + ('0' + (left % 60)).slice(-2);
        if (left <= 60) box.classList.add('urgent');
        if (left <= 0) {
            btn.disabled = true;
            box.innerHTML = '<span>Hết thời gian giữ ghế, ghế đã được nhả.</span>';
            setTimeout(function () { location.href = '${ctx}/booking/seats?showtimeId=${st.showtimeId}'; }, 2500);
            return;
        }
        left--; setTimeout(tick, 1000);
    })();
})();
</script>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
