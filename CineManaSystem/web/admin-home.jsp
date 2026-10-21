<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<c:set var="pageTitle" value="Admin Dashboard"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>



<style>
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
        grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
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
        transition: transform 0.2s ease,
            box-shadow 0.2s ease,
            border-color 0.2s ease;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
        position: relative;
        overflow: hidden;
        min-height: 280px;
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
        box-shadow: 0 10px 20px -3px rgba(0, 0, 0, 0.08);
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
        font-size: 19px;
        font-weight: 700;
        margin: 0 0 6px;
        color: var(--ink, #111827);
        line-height: 1.4;
    }

    .feature-badge {
        display: inline-block;
        font-size: 11.5px;
        font-weight: 600;
        padding: 3px 9px;
        border-radius: 999px;
        background: #e0f2fe;
        color: #0369a1;
    }

    .feature-desc {
        color: var(--muted, #6b7280);
        font-size: 14px;
        line-height: 1.7;
        margin: 0 0 24px;
        flex-grow: 1;
    }

    .feature-action {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        padding-top: 14px;
        border-top: 1px dashed var(--line, #e5e7eb);
        font-size: 13px;
        font-weight: 600;
        color: var(--velvet, #b91c1c);
    }

    .feature-arrow {
        font-size: 20px;
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
        line-height: 1.7;
        color: var(--muted, #6b7280);
    }

    .note-box strong {
        color: var(--ink, #111827);
    }

    @media (max-width: 600px) {
        .manage-hero h1 {
            font-size: 25px;
        }

        .feature-grid {
            grid-template-columns: 1fr;
            gap: 16px;
        }

        .feature-card {
            padding: 24px 20px;
        }
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
    <h1>Admin Dashboard</h1>
    <p>
        Manage branches, promotions, pricing rules and system settings
        from one place.
    </p>
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

    <!-- Feature 1: Branch Management -->
    <a class="feature-card" href="${ctx}/manage/branch">
        <div>
            <div class="feature-header">
                <div class="feature-icon" aria-hidden="true">
                    &#127970;
                </div>

                <div class="feature-title-wrap">
                    <h2>Branch Management</h2>
                    <span class="feature-badge">
                        Branch Operations
                    </span>
                </div>
            </div>

            <p class="feature-desc">
                Manage cinema branches, screening rooms, seating layouts
                and room formats such as 2D, 3D and IMAX.
                Maintain branch information and cinema facilities
                across the system.
            </p>
        </div>

        <div class="feature-action">
            <span>Access Branch Management</span>
            <span class="feature-arrow">&rarr;</span>
        </div>
    </a>

    <!-- Feature 2: Movie Management -->
    <a class="feature-card" href="${ctx}/manage/movie">
        <div>
            <div class="feature-header">
                <div class="feature-icon" aria-hidden="true">
                    &#127916;
                </div>

                <div class="feature-title-wrap">
                    <h2>Movie Management</h2>
                    <span class="feature-badge">
                        Movie Catalog
                    </span>
                </div>
            </div>

            <p class="feature-desc">
                Manage the movie catalog, movie details, posters and
                trailers, screening status and the branches showing
                each movie. Maintain genres used to classify and
                filter movies.
            </p>
        </div>

        <div class="feature-action">
            <span>Access Movie Management</span>
            <span class="feature-arrow">&rarr;</span>
        </div>
    </a>

    <!-- Feature 3: Promotion & Pricing Management -->
    <a class="feature-card" href="${ctx}/manage/promotion-pricing">
        <div>
            <div class="feature-header">
                <div class="feature-icon" aria-hidden="true">
                    &#127873;
                </div>

                <div class="feature-title-wrap">
                    <h2>Promotion &amp; Pricing Management</h2>
                    <span class="feature-badge">
                        Pricing &amp; Discounts
                    </span>
                </div>
            </div>

            <p class="feature-desc">
                Manage movie ticket prices, pricing rules, food and
                beverage prices, discount codes and promotional campaigns.
                Configure promotion conditions and validate discounts
                for eligible orders.
            </p>
        </div>

        <div class="feature-action">
            <span>Access Promotion &amp; Pricing</span>
            <span class="feature-arrow">&rarr;</span>
        </div>
    </a>

    <!-- Feature 4: System Administration -->
    <a class="feature-card" href="${ctx}/admin/system">
        <div>
            <div class="feature-header">
                <div class="feature-icon" aria-hidden="true">
                    &#9881;
                </div>

                <div class="feature-title-wrap">
                    <h2>System Administration</h2>
                    <span class="feature-badge">
                        System Control
                    </span>
                </div>
            </div>

            <p class="feature-desc">
                Manage user accounts, employee information, roles
                and permissions. Control system access and maintain
                administrative settings to ensure secure and
                consistent system operations.
            </p>
        </div>

        <div class="feature-action">
            <span>Access System Administration</span>
            <span class="feature-arrow">&rarr;</span>
        </div>
    </a>

</div>

<div class="note-box">
    <strong>Ghi chú phân tầng nghiệp vụ:</strong> Mỗi ô đại diện cho một Feature lớn. Bấm vào từng ô để mở trang quản lý chi tiết riêng của nghiệp vụ đó trước khi thực hiện thao tác chuyên sâu.
</div>


