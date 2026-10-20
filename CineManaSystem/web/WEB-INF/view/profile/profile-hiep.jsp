<c:set var="pageTitle" value="Tài khoản"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<c:set var="u" value="${profile}"/>
<div class="page-head">
    <h1 class="page-title">Tài khoản của tôi</h1>
    <a class="btn btn-ghost" href="${ctx}/profile/history">Lịch sử đặt vé →</a>
</div>
<c:if test="${not empty requestScope.errorMsg}"><div class="alert alert-error">${fn:escapeXml(requestScope.errorMsg)}</div></c:if>
<div class="grid-2">
<section class="card">
    <h3>Thông tin cá nhân</h3>
    <form method="post" action="${ctx}/profile/update" class="form">
        <label>Họ tên *<input type="text" name="fullName" value="${fn:escapeXml(u.fullName)}" maxlength="100" required></label>
        <label>Email <span class="muted small">(dùng để đăng nhập, không thể đổi)</span>
            <input type="email" value="${fn:escapeXml(u.email)}" readonly></label>
        <label>Số điện thoại *<input type="tel" name="phone" value="${fn:escapeXml(u.phone)}" pattern="\d{9,11}" title="9-11 chữ số" required></label>
        <label>Trạng thái tài khoản<input type="text" value="${u.status}" readonly></label>
        <button class="btn btn-primary">Lưu thay đổi</button>
    </form>
</section>
<section class="card">
    <h3>Đổi mật khẩu</h3>
    <form method="post" action="${ctx}/profile/password" class="form">
        <label>Mật khẩu hiện tại<input type="password" name="currentPassword" required autocomplete="current-password"></label>
        <label>Mật khẩu mới <span class="muted small">(≥ 8 ký tự, có chữ và số)</span>
            <input type="password" name="newPassword" minlength="8" required autocomplete="new-password"></label>
        <label>Nhập lại mật khẩu mới<input type="password" name="confirmPassword" minlength="8" required autocomplete="new-password"></label>
        <button class="btn btn-primary">Đổi mật khẩu</button>
    </form>
    <hr>
    <h3 class="danger-text">Vùng nguy hiểm</h3>
    <p class="muted small">Vô hiệu hóa tài khoản: bạn sẽ không thể đăng nhập và đặt vé nữa.</p>
    <a class="btn btn-danger" href="${ctx}/profile/deactivate">Vô hiệu hóa tài khoản</a>
</section>
</div>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
