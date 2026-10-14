<c:set var="pageTitle" value="Đăng nhập"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="card narrow">
    <h1 class="page-title">Đăng nhập</h1>
    <p class="muted small">Màn hình đăng nhập tối giản để kiểm thử – module Authentication thuộc phần của thành viên khác.</p>
    <c:if test="${not empty error}"><div class="alert alert-error">${fn:escapeXml(error)}</div></c:if>
    <form method="post" action="${ctx}/login" class="form">
        <input type="hidden" name="next" value="${fn:escapeXml(param.next)}">
        <label>Email<input type="email" name="email" required value="${fn:escapeXml(email)}" autofocus></label>
        <label>Mật khẩu<input type="password" name="password" required></label>
        <button class="btn btn-primary btn-block" type="submit">Đăng nhập</button>
    </form>
    <details class="hint">
        <summary>Tài khoản mẫu (mật khẩu 123456)</summary>
        <ul class="small">
            <li><b>customer2@gmail.com</b> – Khách hàng</li>
            <li><b>manager@cine.vn</b> – Quản lý Hà Đông · <b>manager2@cine.vn</b> – Quản lý Cầu Giấy</li>
            <li><b>cashier@cine.vn</b> – Thu ngân Hà Đông · <b>cashier2@cine.vn</b> – Thu ngân Cầu Giấy</li>
            <li><b>admin@cine.vn</b> – Quản trị · <b>customer@gmail.com</b> – Khách bị khóa (test MSG09)</li>
        </ul>
    </details>
</section>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
