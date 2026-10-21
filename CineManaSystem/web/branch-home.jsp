<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<c:set var="pageTitle" value="Trang quản trị"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>


<style>
    /* Styling cho giao diện quản lý phân tầng nghiệp vụ */
    .manage-hero {
        margin: 12px 0 32px;
        padding-bottom: 20px;
        border-bottom: 1px solid var(--line, #e5e7eb);
    }
    .manage-hero h1 {
        font-size: 30px;
        font-weight: 700;
        color: var(--ink, #111827);
        margin: 0 0 8px;
        line-height: 1.2;
    }
    .manage-hero p {
        color: var(--muted, #6b7280);
        font-size: 15px;
        margin: 0;
    }
    .feature-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
        gap: 24px;
        margin-top: 16px;
    }
    .feature-card {
        background: var(--paper, #ffffff);
        border: 1px solid var(--line, #e5e7eb);
        border-radius: 12px;
        padding: 28px 24px;
        text-decoration: none;
        color: var(--ink, #111827);
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
        box-shadow: 0 2px 4px rgba(0,0,0,0.04);
        position: relative;
        overflow: hidden;
    }
    .feature-card::before {
        content: "";
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        height: 4px;
        background: var(--velvet, #b91c1c);
        opacity: 0;
        transition: opacity 0.2s ease;
    }
    .feature-card:hover {
        transform: translateY(-4px);
        box-shadow: 0 10px 20px -3px rgba(0,0,0,0.08);
        border-color: var(--velvet, #b91c1c);
        color: var(--ink, #111827);
    }
    .feature-card:hover::before {
        opacity: 1;
    }
    .feature-header {
        display: flex;
        align-items: center;
        gap: 16px;
        margin-bottom: 14px;
    }
    .feature-icon {
        width: 52px;
        height: 52px;
        border-radius: 10px;
        background: var(--velvet-soft, #fef2f2);
        color: var(--velvet, #b91c1c);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 26px;
        flex-shrink: 0;
    }
    .feature-title-wrap h2 {
        font-size: 20px;
        font-weight: 700;
        margin: 0 0 4px;
        color: var(--ink, #111827);
    }
    .feature-badge {
        display: inline-block;
        font-size: 11.5px;
        font-weight: 600;
        padding: 2px 8px;
        border-radius: 999px;
        background: #e0f2fe;
        color: #0369a1;
    }
    .feature-desc {
        color: var(--muted, #6b7280);
        font-size: 14px;
        line-height: 1.5;
        margin: 0 0 20px;
        flex-grow: 1;
    }
    .feature-action {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding-top: 14px;
        border-top: 1px dashed var(--line, #e5e7eb);
        font-size: 14px;
        font-weight: 600;
        color: var(--velvet, #b91c1c);
    }
    .feature-arrow {
        font-size: 18px;
        transition: transform 0.2s ease;
    }
    .feature-card:hover .feature-arrow {
        transform: translateX(4px);
    }
    .note-box {
        margin-top: 36px;
        padding: 16px 20px;
        background: var(--surface, #f9fafb);
        border: 1px dashed var(--line, #e5e7eb);
        border-radius: 8px;
        font-size: 13.5px;
        color: var(--muted, #6b7280);
    }
    .note-box strong {
        color: var(--ink, #111827);
    }
</style>
<%
    // Lấy đối tượng User từ Session (kiểm tra cả "User" hoa và "user" thường)
    Object sessionUser = session.getAttribute("User");
    if (sessionUser == null) {
        sessionUser = session.getAttribute("user");
    }

    // Khai báo biến hiển thị mặc định
    String fullName = "Nguyễn Văn A";
    String email = "user@example.com";

    // Đọc thông tin từ Session qua Reflection để tránh lỗi ClassCastException
    if (sessionUser != null) {
        try {
            java.lang.reflect.Method getFullNameMethod = sessionUser.getClass().getMethod("getFullName");
            Object nameObj = getFullNameMethod.invoke(sessionUser);
            if (nameObj != null && !nameObj.toString().trim().isEmpty()) {
                fullName = nameObj.toString();
            }
        } catch (Exception e) {
            try {
                java.lang.reflect.Method getNameMethod = sessionUser.getClass().getMethod("getName");
                Object nameObj = getNameMethod.invoke(sessionUser);
                if (nameObj != null) fullName = nameObj.toString();
            } catch (Exception ex) {}
        }

        try {
            java.lang.reflect.Method getEmailMethod = sessionUser.getClass().getMethod("getEmail");
            Object emailObj = getEmailMethod.invoke(sessionUser);
            if (emailObj != null) email = emailObj.toString();
        } catch (Exception e) {}
    }
%>
<section class="manage-hero">
    <h1>Giao diện quản trị chi nhánh</h1>
    <p>Chọn phân hệ nghiệp vụ để xem các chức năng quản lý chi tiết.</p>
    <div class="auth-group">
        <% if (sessionUser != null) { %>
        <a href="<%= request.getContextPath() %>/logout" class="dropdown-item logout-item">
            <div class="dropdown-item-left">
                <i class="fa-solid fa-right-from-bracket"></i>
                <span>Đăng xuất</span>
            </div>
        </a>
        <% } else { %>
        <a href="<%= request.getContextPath() %>/register" class="btn-register">Đăng ký</a>
        <a href="<%= request.getContextPath() %>/login" class="btn-login-pill">Đăng nhập</a>
        <% } %>
    </div>
</section>

<div class="feature-grid">
    <!-- Feature 1: Quản lý Chi nhánh -->
    <a class="feature-card" href="${ctx}/manage/branch">
        <div>
            <div class="feature-header">
                <div class="feature-icon" aria-hidden="true">&#127970;</div>
                <div class="feature-title-wrap">
                    <h2>Chi nhánh</h2>
                    <span class="feature-badge">3 chức năng chi tiết</span>
                </div>
            </div>
            <p class="feature-desc">
                Quản lý hệ thống cụm rạp, thông tin chi nhánh, cơ sở phòng chiếu, định dạng loại phòng (2D, 3D, IMAX) và sơ đồ ghế.
            </p>
        </div>
        <div class="feature-action">
            <span>Truy cập quản lý Branch</span>
            <span class="feature-arrow">&rarr;</span>
        </div>
    </a>

    <!-- Feature 2: Quản lý Phim -->
    <a class="feature-card" href="${ctx}/manage/movie">
        <div>
            <div class="feature-header">
                <div class="feature-icon" aria-hidden="true">&#127916;</div>
                <div class="feature-title-wrap">
                    <h2>Phim</h2>
                    <span class="feature-badge">3 chức năng chi tiết</span>
                </div>
            </div>
            <p class="feature-desc">
                Quản lý kho phim, cập nhật thông tin chi tiết, trailer, poster, thêm phim mới vào hệ thống và danh mục thể loại phim.
            </p>
        </div>
        <div class="feature-action">
            <span>Truy cập quản lý Movie</span>
            <span class="feature-arrow">&rarr;</span>
        </div>
    </a>

    <!-- Feature: Suất chiếu (Showtime Management) -->
    <a class="feature-card" href="${ctx}/manager/showtimes">
        <div>
            <div class="feature-header">
                <div class="feature-icon" aria-hidden="true">&#128197;</div>
                <div class="feature-title-wrap">
                    <h2>Suất chiếu</h2>
                    <span class="feature-badge">Lịch chiếu &amp; giá vé</span>
                </div>
            </div>
            <p class="feature-desc">
                Tạo / sửa / hủy suất chiếu, gán phòng, gán phim, kiểm tra trùng lịch. Giá vé tự tính theo phòng và ngày (Thứ 7, Chủ nhật +20.000đ). Xem lịch chiếu tổng hợp theo tuần.
            </p>
        </div>
        <div class="feature-action">
            <span>Truy cập quản lý Suất chiếu</span>
            <span class="feature-arrow">&rarr;</span>
        </div>
    </a>

    <!-- Feature 3: Báo cáo & Thống kê -->
    <a class="feature-card" href="${ctx}/report/branch-dashboard">
        <div>
            <div class="feature-header">
                <div class="feature-icon" aria-hidden="true">&#128200;</div>
                <div class="feature-title-wrap">
                    <h2>Báo cáo &amp; Thống kê</h2>
                    <span class="feature-badge">4 chức năng chi tiết</span>
                </div>
            </div>
            <p class="feature-desc">
                Xem tổng quan hệ thống, báo cáo doanh thu chi nhánh, thống kê vé bán chi tiết và quản lý thông tin nhân sự.
            </p>
        </div>
        <div class="feature-action">
            <span>Truy cập Báo cáo &amp; Thống kê</span>
            <span class="feature-arrow">&rarr;</span>
        </div>
    </a>
    <a class="feature-card" href="${ctx}/promotion-price-manage">Promotion & Pricing Management
    </a>
    <!-- Feature: Quản lý thanh toán (phần 6 - Nam) -->
    <a class="feature-card" href="${ctx}/admin/payments">
        <div>
            <div class="feature-header">
                <div class="feature-icon" aria-hidden="true">&#128179;</div>
                <div class="feature-title-wrap">
                    <h2>Quản lý thanh toán</h2>
                    <span class="feature-badge">VNPay / Tại quầy</span>
                </div>
            </div>
            <p class="feature-desc">
                Tra cứu giao dịch theo ngày, chi nhánh, phương thức; xem chi tiết và đối soát giao dịch đang chờ.
            </p>
        </div>
        <div class="feature-action">
            <span>Truy cập Quản lý thanh toán</span>
            <span class="feature-arrow">&rarr;</span>
        </div>
    </a>

    <!-- Feature: Giờ làm nhân viên (phần 7 - Nam) -->
    <a class="feature-card" href="${ctx}/manager/working-hours">
        <div>
            <div class="feature-header">
                <div class="feature-icon" aria-hidden="true">&#9201;</div>
                <div class="feature-title-wrap">
                    <h2>Giờ làm nhân viên</h2>
                    <span class="feature-badge">Ca thu ngân &amp; soát vé</span>
                </div>
            </div>
            <p class="feature-desc">
                Xem các ca làm việc, thời gian vào/ra ca và tổng giờ làm của nhân viên quầy và soát vé.
            </p>
        </div>
        <div class="feature-action">
            <span>Xem giờ làm</span>
            <span class="feature-arrow">&rarr;</span>
        </div>
    </a>
</div>

<div class="note-box">
    <strong>Ghi chú phân tầng nghiệp vụ:</strong> Mỗi ô đại diện cho một Feature lớn. Bấm vào từng ô để mở trang quản lý chi tiết riêng của nghiệp vụ đó trước khi thực hiện thao tác chuyên sâu.
</div>

