<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<c:set var="pageTitle" value="Trang quản trị"/>
<%@ include file="/WEB-INF/views/common/header-hoang.jspf" %>

<section class="home-hero">
    <h1>Quản lý chuỗi rạp</h1>
    <p class="page-sub">Chi nhánh, phòng chiếu và danh mục phim của CineGo.</p>
</section>

<div class="home-groups">
    <section class="home-group">
        <h2>Chi nhánh và phòng chiếu</h2>
        <a class="home-link" href="${ctx}/manage/branches">
            <strong>Chi nhánh</strong><span>Thông tin, trạng thái và giờ mở cửa của từng rạp</span>
        </a>
        <a class="home-link" href="${ctx}/manage/rooms">
            <strong>Phòng chiếu</strong><span>Thêm phòng, đổi trạng thái, sắp xếp sơ đồ ghế</span>
        </a>
        <a class="home-link" href="${ctx}/manage/room-types">
            <strong>Loại phòng</strong><span>2D, 3D, IMAX và phụ thu đi kèm</span>
        </a>
    </section>
    <section class="home-group">
        <h2>Phim</h2>
        <a class="home-link" href="${ctx}/manage/movies">
            <strong>Phim</strong><span>Thông tin phim, poster, trailer, trạng thái và chi nhánh chiếu</span>
        </a>
        <a class="home-link" href="${ctx}/manage/movies?action=create">
            <strong>Thêm phim mới</strong><span>Bắt đầu từ thông tin cơ bản, sau đó thêm poster và chọn chi nhánh</span>
        </a>
        <a class="home-link" href="${ctx}/manage/genres">
            <strong>Thể loại</strong><span>Danh mục thể loại dùng để phân loại và lọc phim</span>
        </a>
    </section>
</div>

<%@ include file="/WEB-INF/views/common/footer-hoang.jspf" %>
