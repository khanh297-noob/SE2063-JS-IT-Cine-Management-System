<c:set var="pageTitle" value="Vô hiệu hóa tài khoản"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="card narrow">
    <h1 class="page-title danger-text">Vô hiệu hóa tài khoản</h1>
<c:if test="${not empty requestScope.errorMsg}"><div class="alert alert-error">${fn:escapeXml(requestScope.errorMsg)}</div></c:if>
    <ul class="rules">
        <li>Bạn sẽ bị đăng xuất và không thể đăng nhập lại.</li>
        <li>Lịch sử đặt vé và thanh toán vẫn được lưu (GB-16), không bị xóa.</li>
        <li>Nếu còn đơn vé sắp chiếu, hãy hủy hoặc sử dụng trước – hệ thống sẽ không cho vô hiệu hóa.</li>
        <li>Muốn kích hoạt lại, vui lòng liên hệ quản trị viên.</li>
    </ul>
    <form method="post" action="${ctx}/profile/deactivate" class="form">
        <label>Nhập mật khẩu để xác nhận<input type="password" name="password" required autocomplete="current-password"></label>
        <label class="check"><input type="checkbox" name="confirm" required> Tôi hiểu và muốn vô hiệu hóa tài khoản</label>
        <div class="actions">
            <a class="btn btn-ghost" href="${ctx}/profile">Quay lại</a>
            <button class="btn btn-danger">Vô hiệu hóa</button>
        </div>
    </form>
</section>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
