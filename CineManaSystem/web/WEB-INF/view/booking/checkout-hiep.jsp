<c:set var="pageTitle" value="Giữ ghế & thanh toán"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<c:set var="st" value="${hold.showtime}"/>
<div class="steps"><span class="done">1. Chọn ghế</span><span class="active">2. Giữ ghế &amp; thanh toán</span><span>3. Nhận vé QR</span></div>
<c:if test="${not empty requestScope.errorMsg}"><div class="alert alert-error">${fn:escapeXml(requestScope.errorMsg)}</div></c:if>

<div class="grid-2">
<section class="card">
    <div class="timer-box" id="timerBox">
        <span>Thời gian giữ ghế còn</span>
        <b id="timer" class="mono">--:--</b>
    </div>
    <h2>${fn:escapeXml(st.movieTitle)} <span class="age age-${st.ageRating}">${st.ageRating}</span></h2>
    <p class="muted">${fn:escapeXml(st.branchName)} · ${fn:escapeXml(st.roomName)} (${fn:escapeXml(st.roomType)})<br>
        <b>${st.startStr} – ${st.endStr}</b> · ${st.weekday} ${st.dateStr}</p>
    <table class="table compact">
        <thead><tr><th>Ghế</th><th>Loại</th><th class="num">Giá</th></tr></thead>
        <tbody>
        <c:forEach var="s" items="${hold.seats}">
            <tr><td><b>${s.code}</b></td><td>${fn:escapeXml(s.seatType)}</td><td class="num"><fmt:formatNumber value="${s.price}" pattern="#,##0"/> ₫</td></tr>
        </c:forEach>
        </tbody>
        <tfoot><tr><th colspan="2">Tổng cộng</th><th class="num"><fmt:formatNumber value="${hold.total}" pattern="#,##0"/> ₫</th></tr></tfoot>
    </table>
    <form method="post" action="${ctx}/booking/checkout">
        <input type="hidden" name="showtimeId" value="${st.showtimeId}">
        <input type="hidden" name="action" value="release">
        <button class="btn btn-ghost">Hủy giữ ghế &amp; chọn lại</button>
    </form>
</section>

<section class="card">
    <h3>Thanh toán</h3>
    <form method="post" action="${ctx}/booking/checkout" class="form" id="payForm">
        <input type="hidden" name="showtimeId" value="${st.showtimeId}">
        <input type="hidden" name="action" value="confirm">
        <fieldset class="methods">
            <legend>Phương thức</legend>
            <c:forEach var="m" items="${methods}" varStatus="vs">
                <label class="method"><input type="radio" name="method" value="${m}" ${vs.first ? 'checked' : ''}>
                    <span>${m}</span></label>
            </c:forEach>
        </fieldset>
        <p class="muted small">Môi trường demo: cổng VNPay đang được <b>giả lập</b> (luôn thành công). Phần tích hợp VNPay thật thuộc module Payment.</p>
        <button class="btn btn-primary btn-block" type="submit" id="payBtn">
            Thanh toán qua VNPay · <fmt:formatNumber value="${hold.total}" pattern="#,##0"/> ₫
        </button>
    </form>
</section>
</div>

<script>
(function () {
    var left = ${hold.remainingSeconds};
    var el = document.getElementById('timer'), box = document.getElementById('timerBox'), btn = document.getElementById('payBtn');
    (function tick() {
        el.textContent = Math.floor(left / 60) + ':' + ('0' + (left % 60)).slice(-2);
        if (left <= 60) box.classList.add('urgent');
        if (left <= 0) {
            btn.disabled = true;
            box.innerHTML = '<span>Hết thời gian giữ ghế – ghế đã được nhả.</span>';
            setTimeout(function () { location.href = '${ctx}/booking/seats?showtimeId=${st.showtimeId}'; }, 2500);
            return;
        }
        left--; setTimeout(tick, 1000);
    })();
    document.getElementById('payForm').addEventListener('submit', function () { btn.disabled = true; });
})();
</script>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
