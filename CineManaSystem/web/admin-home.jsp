<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Trang quản trị · Cine Management</title>
        <link rel="stylesheet" href="${ctx}/assets/css/admin-portal-khanh.css">
        <script src="${ctx}/assets/js/admin-portal-khanh.js" defer></script>
    </head>
    <body class="kp-portal">
        <svg class="kp-sprite" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
            <symbol id="kp-film" viewBox="0 0 24 24"><rect x="3" y="3" width="18" height="18" rx="3"/><path d="M7 3v18M17 3v18M3 8h4M3 16h4M17 8h4M17 16h4"/></symbol>
            <symbol id="kp-users" viewBox="0 0 24 24"><circle cx="9" cy="8" r="3"/><path d="M3 21v-2a6 6 0 0 1 12 0v2M16 5a3 3 0 0 1 0 6M21 21v-2a6 6 0 0 0-4-5"/></symbol>
            <symbol id="kp-fnb" viewBox="0 0 24 24"><path d="m4 8 2 13h12l2-13H4ZM7 8V5a2 2 0 0 1 4 0M11 8V4a2 2 0 0 1 4 0v4M15 8V6a2 2 0 0 1 4 0v2M9 12v5M15 12v5"/></symbol>
            <symbol id="kp-gift" viewBox="0 0 24 24"><rect x="3" y="8" width="18" height="4" rx="1"/><path d="M5 12v9h14v-9M12 8v13M12 8H8a3 3 0 1 1 3-3l1 3ZM12 8h4a3 3 0 1 0-3-3l-1 3Z"/></symbol>
            <symbol id="kp-branch" viewBox="0 0 24 24"><path d="M4 21V5l8-2v18M12 8h8v13M2 21h20M7 8h2M7 12h2M7 16h2M15 12h2M15 16h2"/></symbol>
            <symbol id="kp-movie" viewBox="0 0 24 24"><rect x="3" y="7" width="18" height="14" rx="2"/><path d="m3 7 1-4 17 1v3M8 3l2 4M15 4l2 3M10 12l5 3-5 3v-6Z"/></symbol>
            <symbol id="kp-calendar" viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="16" rx="3"/><path d="M7 3v4M17 3v4M3 11h18M8 15h1M15 15h1M8 18h1"/></symbol>
            <symbol id="kp-payment" viewBox="0 0 24 24"><rect x="2" y="5" width="20" height="14" rx="3"/><path d="M2 10h20M6 15h4M15 15h3"/></symbol>
            <symbol id="kp-chart" viewBox="0 0 24 24"><path d="M4 3v18h17M8 16v-4M13 16V8M18 16V5"/></symbol>
            <symbol id="kp-arrow" viewBox="0 0 24 24"><path d="M5 12h14M13 6l6 6-6 6"/></symbol>
            <symbol id="kp-search" viewBox="0 0 24 24"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></symbol>
            <symbol id="kp-close" viewBox="0 0 24 24"><path d="m6 6 12 12M18 6 6 18"/></symbol>
            <symbol id="kp-logout" viewBox="0 0 24 24"><path d="M9 4H4v16h5M13 8l4 4-4 4M8 12h13"/></symbol>
            <symbol id="kp-profile" viewBox="0 0 24 24"><circle cx="12" cy="8" r="4"/><path d="M4 22v-2a8 8 0 0 1 16 0v2"/></symbol>
        </svg>
        <a class="kp-skip" href="#portal-main">Bỏ qua điều hướng</a>
        <header class="kp-topbar">
            <div class="kp-topbar-inner">
                <a class="kp-brand" href="${ctx}/admin-home.jsp" aria-label="Cine Management, trang quản trị">
                    <span class="kp-brand-icon">
                        <svg class="kp-icon" aria-hidden="true"><use href="#kp-film" /></svg>
                    </span>
                    <span>CINE <strong>MANAGEMENT</strong></span>
                </a>
                <nav class="kp-account-nav" aria-label="Tài khoản">
                    <a class="kp-user" href="${ctx}/admin/profile">
                        <svg class="kp-icon" aria-hidden="true"><use href="#kp-profile" /></svg>
                        <span><c:out value="${sessionScope.User.fullName}" default="Tài khoản" /></span>
                    </a>
                    <a class="kp-logout" href="${ctx}/logout">
                        <svg class="kp-icon" aria-hidden="true"><use href="#kp-logout" /></svg>
                        <span>Đăng xuất</span>
                    </a>
                </nav>
            </div>
        </header>
        <main id="portal-main" class="kp-main">
            <section class="kp-heading" aria-labelledby="portal-title">
                <div>
                    <p class="kp-eyebrow">Không gian quản trị</p>
                    <h1 id="portal-title">Quản lý chuỗi rạp</h1>
                    <p class="kp-intro">Chọn phân hệ để bắt đầu công việc.</p>
                </div>
                <span class="kp-role">
                    <span class="kp-role-dot" aria-hidden="true"></span>
                    <c:out value="${sessionScope.User.role}" default="Administrator" />
                </span>
            </section>
           <section class="kp-modules" aria-labelledby="module-title">
                <div class="kp-toolbar">
                    <div>
                        <h2 id="module-title">Các phân hệ</h2>
                        <p id="module-count" class="kp-count" aria-live="polite">8 phân hệ</p>
                    </div>
                    <div class="kp-search">
                        <label class="kp-sr-only" for="module-search">Tìm phân hệ</label>
                        <svg class="kp-icon kp-search-icon" aria-hidden="true"><use href="#kp-search" /></svg>
                        <input id="module-search" type="search"
                               placeholder="Tìm phân hệ, ví dụ: kho, voucher..."
                               autocomplete="off" aria-controls="module-grid">
                        <button id="module-clear" class="kp-search-clear"
                                type="button" aria-label="Xóa tìm kiếm" hidden>
                            <svg class="kp-icon" aria-hidden="true"><use href="#kp-close" /></svg>
                        </button>
                    </div>
                </div>
                <div id="module-grid" class="kp-grid">
                <a class="kp-card kp-card-featured"
                   href="${ctx}/admin/system"
                   data-module-card
                   data-keywords="user role account người dùng nhân viên tài khoản phân quyền audit admin">
                    <div class="kp-card-top">
                        <span class="kp-card-icon" aria-hidden="true">
                            <svg class="kp-icon"><use href="#kp-users" /></svg>
                        </span>
                        <span class="kp-card-category">Quản trị</span>
                    </div>
                    <div>
                        <p class="kp-card-kicker">Tài khoản & quyền</p>
                        <h2>System Administration</h2>
                        <p class="kp-card-description">Quản lý người dùng, vai trò, chi nhánh và lịch sử hoạt động.</p>
                    </div>
                    <span class="kp-card-action">
                        <span>Mở phân hệ</span>
                        <svg class="kp-icon" aria-hidden="true">
                            <use href="#kp-arrow" />
                        </svg>
                    </span>
                </a>
                <a class="kp-card kp-card-featured"
                   href="${ctx}/staff/fnb"
                   data-module-card
                   data-keywords="fnb f&b food đồ ăn bắp nước kho stock đơn hàng menu sản phẩm">
                    <div class="kp-card-top">
                        <span class="kp-card-icon" aria-hidden="true">
                            <svg class="kp-icon"><use href="#kp-fnb" /></svg>
                        </span>
                        <span class="kp-card-category">F&B</span>
                    </div>
                    <div>
                        <p class="kp-card-kicker">Sản phẩm & đơn hàng</p>
                        <h2>F&amp;B Booking</h2>
                        <p class="kp-card-description">Quản lý bắp nước, đơn hàng và tồn kho theo từng cơ sở.</p>
                    </div>
                    <span class="kp-card-action">
                        <span>Mở phân hệ</span>
                        <svg class="kp-icon" aria-hidden="true">
                            <use href="#kp-arrow" />
                        </svg>
                    </span>
                </a>
                <a class="kp-card kp-card-featured"
                   href="${ctx}/staff/promotions"
                   data-module-card
                   data-keywords="promotion pricing voucher ưu đãi khuyến mãi giảm giá giá vé">
                    <div class="kp-card-top">
                        <span class="kp-card-icon" aria-hidden="true">
                            <svg class="kp-icon"><use href="#kp-gift" /></svg>
                        </span>
                        <span class="kp-card-category">Ưu đãi</span>
                    </div>
                    <div>
                        <p class="kp-card-kicker">Ưu đãi & giá bán</p>
                        <h2>Promotion &amp; Pricing</h2>
                        <p class="kp-card-description">Thiết lập voucher, mức giảm, lượt dùng và giá bán.</p>
                    </div>
                    <span class="kp-card-action">
                        <span>Mở phân hệ</span>
                        <svg class="kp-icon" aria-hidden="true">
                            <use href="#kp-arrow" />
                        </svg>
                    </span>
                </a>
                <a class="kp-card"
                   href="${ctx}/manage/branch"
                   data-module-card
                   data-keywords="branch chi nhánh cơ sở rạp phòng ghế room seats">
                    <div class="kp-card-top">
                        <span class="kp-card-icon" aria-hidden="true">
                            <svg class="kp-icon"><use href="#kp-branch" /></svg>
                        </span>
                        <span class="kp-card-category">Vận hành</span>
                    </div>
                    <div>
                        <p class="kp-card-kicker">Cơ sở & phòng chiếu</p>
                        <h2>Branch Management</h2>
                        <p class="kp-card-description">Quản lý thông tin chi nhánh, phòng chiếu và sơ đồ ghế.</p>
                    </div>
                    <span class="kp-card-action">
                        <span>Mở phân hệ</span>
                        <svg class="kp-icon" aria-hidden="true">
                            <use href="#kp-arrow" />
                        </svg>
                    </span>
                </a>
                <a class="kp-card"
                   href="${ctx}/manage/movie"
                   data-module-card
                   data-keywords="movie phim thể loại poster trailer danh mục genre">
                    <div class="kp-card-top">
                        <span class="kp-card-icon" aria-hidden="true">
                            <svg class="kp-icon"><use href="#kp-movie" /></svg>
                        </span>
                        <span class="kp-card-category">Nội dung</span>
                    </div>
                    <div>
                        <p class="kp-card-kicker">Danh mục phim</p>
                        <h2>Movie Management</h2>
                        <p class="kp-card-description">Quản lý phim, thể loại, hình ảnh và cơ sở đang chiếu.</p>
                    </div>
                    <span class="kp-card-action">
                        <span>Mở phân hệ</span>
                        <svg class="kp-icon" aria-hidden="true">
                            <use href="#kp-arrow" />
                        </svg>
                    </span>
                </a>
                <a class="kp-card"
                   href="${ctx}/manager/showtimes"
                   data-module-card
                   data-keywords="showtime suất chiếu lịch chiếu ngày giờ phòng schedule">
                    <div class="kp-card-top">
                        <span class="kp-card-icon" aria-hidden="true">
                            <svg class="kp-icon"><use href="#kp-calendar" /></svg>
                        </span>
                        <span class="kp-card-category">Vận hành</span>
                    </div>
                    <div>
                        <p class="kp-card-kicker">Lịch chiếu</p>
                        <h2>Showtime Management</h2>
                        <p class="kp-card-description">Sắp xếp suất chiếu, phân phòng và theo dõi lịch.</p>
                    </div>
                    <span class="kp-card-action">
                        <span>Mở phân hệ</span>
                        <svg class="kp-icon" aria-hidden="true">
                            <use href="#kp-arrow" />
                        </svg>
                    </span>
                </a>
                <a class="kp-card"
                   href="${ctx}/admin/payments"
                   data-module-card
                   data-keywords="payment thanh toán tiền giao dịch pos vnpay quầy đối soát">
                    <div class="kp-card-top">
                        <span class="kp-card-icon" aria-hidden="true">
                            <svg class="kp-icon"><use href="#kp-payment" /></svg>
                        </span>
                        <span class="kp-card-category">Tài chính</span>
                    </div>
                    <div>
                        <p class="kp-card-kicker">Giao dịch & đối soát</p>
                        <h2>Payment Management</h2>
                        <p class="kp-card-description">Tra cứu thanh toán và quản lý giao dịch tại các cơ sở.</p>
                    </div>
                    <span class="kp-card-action">
                        <span>Mở phân hệ</span>
                        <svg class="kp-icon" aria-hidden="true">
                            <use href="#kp-arrow" />
                        </svg>
                    </span>
                </a>
                <a class="kp-card"
                   href="${ctx}/report/revenue-dashboard"
                   data-module-card
                   data-keywords="report analytics báo cáo doanh thu dashboard thống kê revenue">
                    <div class="kp-card-top">
                        <span class="kp-card-icon" aria-hidden="true">
                            <svg class="kp-icon"><use href="#kp-chart" /></svg>
                        </span>
                        <span class="kp-card-category">Báo cáo</span>
                    </div>
                    <div>
                        <p class="kp-card-kicker">Báo cáo</p>
                        <h2>Reports &amp; Analytics</h2>
                        <p class="kp-card-description">Mở dashboard doanh thu và các báo cáo đã tích hợp.</p>
                    </div>
                    <span class="kp-card-action">
                        <span>Mở phân hệ</span>
                        <svg class="kp-icon" aria-hidden="true">
                            <use href="#kp-arrow" />
                        </svg>
                    </span>
                </a>
                </div>
                <div id="module-empty" class="kp-empty" hidden>
                    <svg class="kp-icon" aria-hidden="true"><use href="#kp-search" /></svg>
                    <h3>Không tìm thấy phân hệ</h3>
                    <p>Thử từ khóa khác hoặc xóa nội dung tìm kiếm.</p>
                    <button id="module-reset" class="kp-reset" type="button">Hiện tất cả</button>
                </div>
            </section>
            <p id="portal-preview-note" class="kp-preview-note" hidden></p>
            <footer class="kp-footer">
                <span>Cine Management System</span>
                <span>Chọn phân hệ · Thao tác theo quyền được cấp</span>
            </footer>
        </main>
    </body>
</html>
