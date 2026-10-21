<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%-- Khách tự vô hiệu hoá tài khoản. Servlet: MyBookingServlet (POST /customer/deactivate). --%>
<c:set var="pageTitle" value="Vô hiệu hóa tài khoản"/>
<c:set var="extraCss" value="booking-hiep"/>
<%@ include file="/WEB-INF/views/common/customer-header-hoang.jspf" %>

<div class="bk-page">
    <a class="back" href="${ctx}/customer/profile">&larr; Hồ sơ cá nhân</a>
    <section class="form-card">
        <h1 class="page-title" style="color: var(--velvet)">Vô hiệu hóa tài khoản</h1>
        <ul class="bk-rules" style="margin: 14px 0 20px">
            <li>Bạn sẽ bị đăng xuất và không thể đăng nhập lại.</li>
            <li>Lịch sử đặt vé và thanh toán vẫn được lưu, không bị xóa.</li>
            <li>Nếu còn đơn vé sắp chiếu, hãy hủy hoặc sử dụng trước, hệ thống sẽ không cho vô hiệu hóa.</li>
            <li>Muốn kích hoạt lại, vui lòng liên hệ quản trị viên.</li>
        </ul>
        <form method="post" action="${ctx}/customer/deactivate">
            <div class="field">
                <label for="password" class="required">Nhập mật khẩu để xác nhận</label>
                <input type="password" id="password" name="password" required autocomplete="current-password"
                       style="width: 100%; font: inherit; padding: 9px 12px; border: 1px solid var(--line); border-radius: var(--radius)">
            </div>
            <div class="field">
                <label class="check-pill"><input type="checkbox" name="confirm" required> Tôi hiểu và muốn vô hiệu hóa tài khoản</label>
            </div>
            <div class="form-actions">
                <a class="btn" href="${ctx}/customer/bookings">Quay lại</a>
                <button class="btn btn-danger" type="submit">Vô hiệu hóa</button>
            </div>
        </form>
    </section>
</div>
<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
