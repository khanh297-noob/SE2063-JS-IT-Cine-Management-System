<c:set var="pageTitle" value="Trang quản lý"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<div class="page-head">
    <div>
        <h1 class="page-title">Trang quản lý chi nhánh</h1>
        <p class="muted">${fn:escapeXml(branch.name)} · Hôm nay có <b>${todayCount}</b> suất chiếu</p>
    </div>
    <c:if test="${not empty branches}">
        <form method="get" action="${ctx}/manager" class="inline-form">
            <label class="select-label">Chi nhánh
                <select name="branchId" onchange="this.form.submit()">
                    <c:forEach var="b" items="${branches}">
                        <option value="${b.branchId}" ${b.branchId == branchId ? 'selected' : ''}>${fn:escapeXml(b.name)}</option>
                    </c:forEach>
                </select>
            </label>
        </form>
    </c:if>
</div>

<div class="landing-grid">
    <a class="landing-card" href="${ctx}/manager/showtimes?branchId=${branchId}&date=${today}">
        <span class="landing-icon" aria-hidden="true">🎬</span>
        <h2>Quản lý suất chiếu</h2>
        <p class="muted">Xem danh sách suất chiếu theo ngày / phòng; sửa, gán phòng, gán phim, hủy hoặc xóa suất chiếu.</p>
    </a>
    <a class="landing-card" href="${ctx}/manager/showtimes/new?branchId=${branchId}&date=${today}">
        <span class="landing-icon" aria-hidden="true">➕</span>
        <h2>Tạo suất chiếu</h2>
        <p class="muted">Xếp suất chiếu mới, hệ thống tự kiểm tra trùng lịch phòng và tính giờ kết thúc.</p>
    </a>
    <a class="landing-card" href="${ctx}/manager/schedule?branchId=${branchId}">
        <span class="landing-icon" aria-hidden="true">🗓</span>
        <h2>Lịch tổng hợp</h2>
        <p class="muted">Lịch tuần theo Phòng × Ngày cùng thống kê tỉ lệ lấp đầy từng phim.</p>
    </a>
</div>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
