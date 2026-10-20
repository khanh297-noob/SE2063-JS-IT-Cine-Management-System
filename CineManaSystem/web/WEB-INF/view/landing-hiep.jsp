<c:set var="pageTitle" value="Chào mừng"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="landing-hero">
    <h1>Chào mừng đến với Cine<b>Star</b></h1>
    <p class="lead muted">Bạn muốn làm gì hôm nay?</p>
</section>

<div class="landing-grid">
    <a class="landing-card" href="${ctx}/home">
        <span class="landing-icon" aria-hidden="true">🎟</span>
        <h2>Đặt vé xem phim</h2>
        <p class="muted">Xem lịch chiếu theo rạp và ngày, chọn ghế, giữ ghế và thanh toán để nhận vé QR.</p>
        <span class="btn btn-primary">Đặt vé ngay →</span>
    </a>
    <a class="landing-card" href="${ctx}/manager">
        <span class="landing-icon" aria-hidden="true">🗓</span>
        <h2>Branch Manager</h2>
        <p class="muted">Tạo, sửa, gán phòng, gán phim, hủy suất chiếu và xem lịch tổng hợp của chi nhánh.</p>
        <span class="btn btn-ghost">
            <c:choose>
                <c:when test="${me.manager or me.admin}">Vào trang quản lý →</c:when>
                <c:otherwise>Đăng nhập quản lý →</c:otherwise>
            </c:choose>
        </span>
    </a>
</div>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
