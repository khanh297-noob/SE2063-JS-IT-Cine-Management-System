<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
    <%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
        <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
            <fmt:setLocale value="vi_VN" />
            <!DOCTYPE html>
            <html lang="vi">

            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Phân Tích Doanh Thu Chi Tiết - CineGo Management</title>

                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">

                <%@ include file="/WEB-INF/view/report/fragments/manager-shell-style-tuyet.jsp" %>
                    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/revenue-dashboard.css">
            </head>

            <body>

                <!-- Thanh dieu huong tren cung (Navbar) -->
                <%@ include file="/WEB-INF/view/report/fragments/manager-navbar-tuyet.jsp" %>

                    <div class="layout">
                        <!-- Thanh menu dieu huong ben trai (Sidebar) -->
                        <%@ include file="/WEB-INF/view/report/fragments/manager-sidebar-tuyet.jsp" %>

                            <!-- Vung noi dung chinh ben phai -->
                            <main class="content">

                                <!-- Khoi tieu de trang va pham vi du lieu chi nhanh -->
                                <section class="card">
                                    <div class="head-row">
                                        <div>
                                            <h1 class="page-title"><i class="fa-solid fa-chart-column"></i> Phân Tích
                                                Doanh Thu Chi Tiết (Revenue Dashboard)</h1>
                                            <p class="page-subtitle">Biểu đồ và bảng doanh thu của chi nhánh, phân rã
                                                theo Phim, Loại ghế và Phương thức thanh toán (gồm F&amp;B đi kèm).</p>
                                        </div>

                                    </div>
                                    <hr class="divider">

                                    <c:if test="${not empty filterError}">
                                        <div class="alert-warn"><i class="fa-solid fa-triangle-exclamation"></i>
                                            <c:out value="${filterError}" />
                                        </div>
                                    </c:if>

                                    <div class="filter-row">
                                        <form method="get"
                                            action="${pageContext.request.contextPath}/report/revenue-dashboard"
                                            class="filter-left">
                                            <div class="field">
                                                <label>Từ ngày <span class="req">*</span></label>
                                                <input type="date" name="fromDate" value="<c:out value='${fromDate}'/>"
                                                    required>
                                            </div>
                                            <span class="sep">đến</span>
                                            <div class="field">
                                                <label>Đến ngày <span class="req">*</span></label>
                                                <input type="date" name="toDate" value="<c:out value='${toDate}'/>"
                                                    required>
                                            </div>
                                            <c:if test="${not empty selectedBranchId}"><input type="hidden"
                                                    name="branchId" value="${selectedBranchId}"></c:if>
                                            <button type="submit" class="btn-apply">Áp Dụng Lọc</button>
                                        </form>

                                        <div class="filter-right">
                                            <a class="btn-csv"
                                                href="${pageContext.request.contextPath}/report/export-revenue?fromDate=${fromDate}&toDate=${toDate}&branchId=${selectedBranchId}">
                                                <i class="fa-solid fa-file-csv"></i> Xuất Báo Cáo CSV
                                            </a>
                                            <button type="button" class="btn-pdf" onclick="window.print()">
                                                <i class="fa-solid fa-file-pdf"></i> Xuất Báo Cáo PDF
                                            </button>
                                        </div>
                                    </div>
                                </section>

                                <!-- Thong ke tang truong doanh thu ve va F&B theo tuan (CSS thuan) -->
                                <section class="card">
                                    <div class="chart-head">
                                        <div>
                                            <div class="chart-title">Biểu Đồ Tăng Trưởng Doanh Thu (Vé vs F&amp;B Combo)
                                            </div>
                                            <div class="chart-sub">So sánh doanh thu bán vé và dịch vụ bắp nước theo
                                                thời gian</div>
                                        </div>
                                        <div class="legend">
                                            <span><i class="dot dot-ticket"></i> Doanh thu Vé</span>
                                            <span><i class="dot dot-fnb"></i> Doanh thu F&amp;B</span>
                                        </div>
                                    </div>
                                    <div class="chart-box chart-box-compact">
                                        <div class="week-list">
                                            <c:forEach var="w" items="${dashboard.weekLabels}" varStatus="ws">
                                                <c:set var="tRev" value="${dashboard.weeklyTicketRevenue[ws.index]}" />
                                                <c:set var="fRev" value="${dashboard.weeklyFnbRevenue[ws.index]}" />
                                                <c:set var="sumRev" value="${tRev + fRev}" />
                                                <div class="week-row">
                                                    <span class="week-label">${w}</span>
                                                    <div class="week-bar">
                                                        <div class="week-fill-ticket"
                                                            style="width: ${sumRev > 0 ? (tRev / sumRev) * 100 : 0}%;"
                                                            title="Vé: <fmt:formatNumber value='${tRev}' pattern='#,##0'/> ₫">
                                                        </div>
                                                        <div class="week-fill-fnb"
                                                            style="width: ${sumRev > 0 ? (fRev / sumRev) * 100 : 0}%;"
                                                            title="F&B: <fmt:formatNumber value='${fRev}' pattern='#,##0'/> ₫">
                                                        </div>
                                                    </div>
                                                    <span class="week-total">
                                                        <fmt:formatNumber value="${sumRev}" pattern="#,##0" /> ₫
                                                    </span>
                                                </div>
                                            </c:forEach>
                                        </div>
                                    </div>
                                </section>

                                <!-- Bang phan ra doanh thu chi tiet - chon loai bang qua dropdown -->
                                <section class="card">
                                    <div class="table-head">
                                        <div class="chart-title">Bảng Phân Rã Chi Tiết (Breakdown Tables)</div>

                                        <!-- Form chon loai bang phan ra: gui lai trang voi tham so breakdown -->
                                        <form method="get"
                                            action="${pageContext.request.contextPath}/report/revenue-dashboard"
                                            class="breakdown-form">
                                            <!-- Giu lai cac tham so loc hien tai khi chuyen loai bang -->
                                            <input type="hidden" name="fromDate" value="<c:out value='${fromDate}'/>">
                                            <input type="hidden" name="toDate" value="<c:out value='${toDate}'/>">
                                            <c:if test="${not empty selectedBranchId}">
                                                <input type="hidden" name="branchId" value="${selectedBranchId}">
                                            </c:if>

                                            <div class="field">
                                                <label>Phân rã theo</label>
                                                <select name="breakdown" onchange="this.form.submit()">
                                                    <c:forEach var="tb" items="${dashboard.tables}">
                                                        <option value="${tb.key}" ${selectedBreakdown==tb.key
                                                            ? 'selected' : '' }>
                                                            <c:out value="${tb.title}" />
                                                        </option>
                                                    </c:forEach>
                                                </select>
                                            </div>
                                        </form>
                                    </div>

                                    <!-- Chi hien thi bang tuong ung voi loai da chon trong dropdown -->
                                    <c:forEach var="tb" items="${dashboard.tables}">
                                        <c:if test="${tb.key == selectedBreakdown}">
                                            <div class="tab-panel active" id="tab-${tb.key}">
                                                <div class="tab-title-print">
                                                    <c:out value="${tb.title}" />
                                                </div>
                                                <div class="table-wrap">
                                                    <table class="breakdown">
                                                        <thead>
                                                            <tr>
                                                                <th>
                                                                    <c:out value="${tb.nameHeader}" />
                                                                </th>
                                                                <th class="tickets-h">Tổng số vé bán</th>
                                                                <th>Doanh thu vé (VND)</th>
                                                                <th>Doanh thu F&amp;B đi kèm</th>
                                                                <th>
                                                                    <c:out value="${tb.totalHeader}" />
                                                                </th>
                                                                <th>Tỷ trọng (%)</th>
                                                            </tr>
                                                        </thead>
                                                        <tbody>
                                                            <c:choose>
                                                                <c:when test="${empty tb.rows}">
                                                                    <tr>
                                                                        <td colspan="6" class="empty">Không có dữ liệu
                                                                            trong khoảng thời gian này.</td>
                                                                    </tr>
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <c:forEach var="r" items="${tb.rows}">
                                                                        <tr>
                                                                            <td class="name">
                                                                                <c:out value="${r.name}" />
                                                                            </td>
                                                                            <td class="tickets">
                                                                                <fmt:formatNumber value="${r.tickets}"
                                                                                    pattern="#,##0" />
                                                                            </td>
                                                                            <td>
                                                                                <fmt:formatNumber
                                                                                    value="${r.ticketRevenue}"
                                                                                    pattern="#,##0" /> ₫
                                                                            </td>
                                                                            <td>
                                                                                <fmt:formatNumber
                                                                                    value="${r.fnbRevenue}"
                                                                                    pattern="#,##0" /> ₫
                                                                            </td>
                                                                            <td class="total">
                                                                                <fmt:formatNumber
                                                                                    value="${r.totalRevenue}"
                                                                                    pattern="#,##0" /> ₫
                                                                            </td>
                                                                            <td class="share">
                                                                                <fmt:formatNumber value="${r.share}"
                                                                                    pattern="0.0" />%
                                                                            </td>
                                                                        </tr>
                                                                    </c:forEach>
                                                                    <tr class="sum">
                                                                        <td>Tổng cộng</td>
                                                                        <td class="tickets">
                                                                            <fmt:formatNumber value="${tb.totalTickets}"
                                                                                pattern="#,##0" />
                                                                        </td>
                                                                        <td>
                                                                            <fmt:formatNumber
                                                                                value="${tb.totalTicketRevenue}"
                                                                                pattern="#,##0" /> ₫
                                                                        </td>
                                                                        <td>
                                                                            <fmt:formatNumber
                                                                                value="${tb.totalFnbRevenue}"
                                                                                pattern="#,##0" /> ₫
                                                                        </td>
                                                                        <td class="total">
                                                                            <fmt:formatNumber value="${tb.grandTotal}"
                                                                                pattern="#,##0" /> ₫
                                                                        </td>
                                                                        <td>100.0%</td>
                                                                    </tr>
                                                                </c:otherwise>
                                                            </c:choose>
                                                        </tbody>
                                                    </table>
                                                </div>
                                            </div>
                                        </c:if>
                                    </c:forEach>

                                    <p class="note">
                                        Chỉ tính vé còn hiệu lực của booking đã xác nhận và thanh toán thành công. Doanh
                                        thu F&amp;B của mỗi booking được
                                        chia đều cho các vé của booking đó và tính theo ngày chiếu; F&amp;B dùng đơn giá
                                        hiện tại trong danh mục F&amp;B.
                                    </p>
                                </section>
                            </main>
                    </div>

            </body>

            </html>