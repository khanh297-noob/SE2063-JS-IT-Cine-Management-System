<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- Co dinh ngon ngu Viet Nam de so tien luon co dang 250.000 du trinh duyet dung ngon ngu nao --%>
<fmt:setLocale value="vi_VN" />
<c:set var="pageUrl" value="${pageContext.request.contextPath}/promotion-price-manage/ticket-pricing" />
<!DOCTYPE html>
<html lang="vi">

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Quản lý giá vé - CineGo Management</title>
        <link rel="stylesheet"
              href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
        <link rel="stylesheet"
              href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css">
        <link rel="stylesheet"
              href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin-style.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/promotion-pricing.css">
    </head>

    <body>
        <%-- Khung chung của phân hệ: menu bên trái chọn theo khanhFeature/khanhActive --%>
        <c:set var="khanhFeature" value="promotion" scope="request" />
        <c:set var="khanhActive" value="ticket-prices" scope="request" />
        <jsp:include page="/WEB-INF/view/common/sidebar-khanh.jsp" />
        <div class="main-wrapper">
            <jsp:include page="/WEB-INF/view/common/header-khanh.jsp" />
            <main class="pp-page">
                <link rel="stylesheet"
                      href="${pageContext.request.contextPath}/assets/css/pricing-config-tuyet.css?v=2">

                <%-- Chọn rạp quản lý: trước đây nằm trên thanh điều hướng, nay ở đầu phần nội dung --%>
                <c:if test="${not empty scopeWarning}">
                    <div class="alert alert-warn"><i class="fa-solid fa-triangle-exclamation"></i>
                        <c:out value="${scopeWarning}" />
                    </div>
                </c:if>
                <div class="branch-bar">
                    <div>
                        <c:choose>
                            <c:when test="${branches.size() > 1}">
                                <form method="get"
                                      action="${pageContext.request.contextPath}${shellAction}"
                                      class="branch-bar-form">
                                    <div class="branch-pill">
                                        <i class="fa-solid fa-location-dot"></i> Rạp:
                                        <select name="branchId" onchange="this.form.submit()">
                                            <option value="">Tất cả chi nhánh</option>
                                            <c:forEach var="b" items="${branches}">
                                                <option value="${b.branchID}"
                                                        ${selectedBranchId==b.branchID ? 'selected' : ''
                                                        }>
                                                    <c:out value="${b.name}" /> (#${b.branchID < 10
                                                                    ? '0' : '' }${b.branchID}) </option>
                                                </c:forEach>
                                        </select>
                                    </div>
                                </form>
                            </c:when>
                            <c:otherwise>
                                <div class="branch-pill">
                                    <i class="fa-solid fa-location-dot"></i> Rạp: <b>
                                        <c:out value="${branchPill}" />
                                    </b>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <div class="page-header">
                    <div>
                        <h1 class="page-title"><i class="fa-solid fa-ticket"></i> Quản lý giá vé</h1>
                        <p class="page-subtitle">Cấu hình giá vé theo ngày, phụ thu định dạng phòng
                            và loại ghế. Giá mới áp dụng cho các vé bán sau khi lưu.</p>
                    </div>
                </div>

                <c:if test="${not empty flashMessage}">
                    <div class="alert alert-success"><i class="fa-solid fa-circle-check"></i>
                        <c:out value="${flashMessage}" />
                    </div>
                </c:if>
                <c:if test="${not empty flashError}">
                    <div class="alert alert-error"><i class="fa-solid fa-circle-exclamation"></i>
                        <c:out value="${flashError}" />
                    </div>
                </c:if>
                <c:if test="${false and not empty filterError}">
                    <div class="alert alert-warn"><i class="fa-solid fa-triangle-exclamation"></i>
                        <c:out value="${filterError}" />
                    </div>
                </c:if>

                <%-- Cấu hình giá hệ thống: Showtime.BasePrice=giá cơ bản theo ngày + phụ thu loại
                    phòng. Mỗi mục là một form riêng: bình thường chỉ hiển thị giá trị; Admin
                    bấm "Sửa" mới hiện ô nhập cùng nút Lưu / Hủy của mục đó (bật tắt bằng class
                    is-editing, xem script bên dưới). Quản lý chi nhánh chỉ xem. --%>
                <div class="card cfg-card">
                    <h2 class="card-title"><i class="fa-solid fa-gear"></i> Cấu hình giá vé
                        &amp; phụ thu hệ thống</h2>


                    <%-- 1. Giá cơ bản theo loại ngày --%>
                    <form method="post" action="${pageUrl}" class="cfg-section"
                          data-section="day">
                        <c:if test="${canEditDayPrices}">
                            <input type="hidden" name="action" value="saveConfig">
                            <input type="hidden" name="section" value="day">
                            <%@ include
                                file="/WEB-INF/view/promotion-pricing/fragments/filter-hidden-tuyet.jsp"
                                %>
                        </c:if>
                        <div class="cfg-head">
                            <h3 class="cfg-title">
                                <i class="fa-solid fa-calendar-day"></i> 1. Giá cơ bản theo
                                loại ngày
                                <c:if test="${not empty targetBranchId}">
                                    <span class="badge"
                                          style="font-size: 0.8rem; font-weight: normal; margin-left: 8px; background: #e0e7ff; color: #3730a3; padding: 2px 8px; border-radius: 4px;">
                                        <c:out value="${branchPill}" />
                                    </span>
                                </c:if>
                            </h3>
                            <c:if test="${canEditDayPrices}"><button type="button"
                                    class="btn-edit cfg-view" data-edit><i
                                        class="fa-solid fa-pen"></i> Sửa</button></c:if>
                            </div>
                            <table class="cfg-table">
                                <tbody>
                                    <tr>
                                        <td class="cell-strong">Ngày thường (T2 - T5)</td>
                                        <td class="cfg-value">
                                            <span class="cfg-view"><b>
                                                <fmt:formatNumber
                                                    value="${dayPrices.weekday}"
                                                    pattern="#,##0" /> ₫
                                            </b></span>
                                            <c:if test="${canEditDayPrices}">
                                            <span class="cfg-edit"><input
                                                    class="price-input" type="text"
                                                    inputmode="numeric" name="weekdayPrice"
                                                    autocomplete="off"
                                                    value="<fmt:formatNumber value='${dayPrices.weekday}' maxFractionDigits='0' groupingUsed='false'/>"
                                                    aria-label="Ngày thường (T2 - T5)">
                                                ₫</span>
                                            </c:if>
                                    </td>
                                    <td class="cfg-hint">Áp dụng Thứ 2, Thứ 3, Thứ 4, Thứ 5
                                    </td>
                                </tr>
                                <tr>
                                    <td class="cell-strong">Cuối tuần (T6, T7, CN)</td>
                                    <td class="cfg-value">
                                        <span class="cfg-view"><b>
                                                <fmt:formatNumber
                                                    value="${dayPrices.weekend}"
                                                    pattern="#,##0" /> ₫
                                            </b></span>
                                            <c:if test="${canEditDayPrices}">
                                            <span class="cfg-edit"><input
                                                    class="price-input" type="text"
                                                    inputmode="numeric" name="weekendPrice"
                                                    autocomplete="off"
                                                    value="<fmt:formatNumber value='${dayPrices.weekend}' maxFractionDigits='0' groupingUsed='false'/>"
                                                    aria-label="Cuối tuần (T6, T7, CN)">
                                                ₫</span>
                                            </c:if>
                                    </td>
                                    <td class="cfg-hint">Áp dụng Thứ 6, Thứ 7, Chủ nhật</td>
                                </tr>
                            </tbody>
                        </table>
                        <c:if test="${canEditDayPrices}">
                            <div class="cfg-edit cfg-form-actions">
                                <button type="button" class="btn-reset"
                                        data-cancel>Hủy</button>
                                <button type="submit" class="btn-save btn-save-lg"><i
                                        class="fa-solid fa-floppy-disk"></i> Lưu</button>
                            </div>
                        </c:if>
                    </form>

                    <%-- 2. Phụ thu loại ghế --%>
                    <div class="cfg-section" data-section="seat">
                        <div class="cfg-head">
                            <h3 class="cfg-title">
                                <i class="fa-solid fa-couch"></i> 2. Phụ thu loại ghế
                                <c:if test="${not empty targetBranchId}">
                                    <span class="badge"
                                          style="font-size: 0.8rem; font-weight: normal; margin-left: 8px; background: #e0e7ff; color: #3730a3; padding: 2px 8px; border-radius: 4px;">
                                        <c:out value="${branchPill}" />
                                    </span>
                                </c:if>
                            </h3>
                            <c:if test="${isAdminUser}"><button type="button"
                                    class="btn-edit cfg-view" data-edit><i
                                        class="fa-solid fa-pen"></i> Sửa</button></c:if>
                            </div>
                        <c:if test="${isAdminUser}">
                            <form method="post" action="${pageUrl}">
                                <input type="hidden" name="action" value="saveConfig">
                                <input type="hidden" name="section" value="seat">
                                <%@ include
                                    file="/WEB-INF/view/promotion-pricing/fragments/filter-hidden-tuyet.jsp"
                                    %>
                                    <table class="cfg-table">
                                        <tbody>
                                            <c:forEach var="t" items="${seatTypes}">
                                                <tr>
                                                    <td class="cell-strong">
                                                        <c:out value="${t.name}" />
                                                    </td>
                                                    <td class="cfg-value">
                                                        <span class="cfg-view">
                                                            <b>
                                                                <fmt:formatNumber
                                                                    value="${t.effectiveSurcharge}"
                                                                    pattern="#,##0" /> ₫
                                                            </b>
                                                        </span>
                                                        <span class="cfg-edit"><input
                                                                class="price-input"
                                                                type="text"
                                                                inputmode="numeric"
                                                                name="seat_${t.id}"
                                                                autocomplete="off"
                                                                value="<fmt:formatNumber value='${t.effectiveSurcharge}' maxFractionDigits='0' groupingUsed='false'/>"
                                                                aria-label="Phụ thu loại ghế ${t.name}">
                                                            ₫</span>
                                                    </td>
                                                    <td class="cfg-hint">
                                                        <c:choose>
                                                            <c:when
                                                                test="${t.branchSurcharge != null}">
                                                                <span
                                                                    class="branch-badge">
                                                                    <i
                                                                        class="fa-solid fa-circle-exclamation"></i>
                                                                    Riêng chi nhánh
                                                                    (Chung:
                                                                    <fmt:formatNumber
                                                                        value="${t.surcharge}"
                                                                        pattern="#,##0" />
                                                                    ₫)
                                                                </span>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <span
                                                                    style="color: #64748b;">Đang
                                                                    áp dụng phụ thu
                                                                    chung toàn
                                                                    chuỗi</span>
                                                                </c:otherwise>
                                                            </c:choose>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>
                                    <div class="cfg-edit cfg-form-actions">
                                        <button type="button" class="btn-reset"
                                                data-cancel>Hủy</button>
                                        <button type="submit"
                                                class="btn-save btn-save-lg"><i
                                                class="fa-solid fa-floppy-disk"></i>
                                            Lưu</button>
                                    </div>
                                </form>
                        </c:if>
                        <c:if test="${not isAdminUser}">
                            <table class="cfg-table">
                                <thead>
                                    <tr>
                                        <th>Loại ghế</th>
                                        <th>Phụ thu chung</th>
                                        <th>Phụ thu tại
                                            <c:out value="${branchPill}" />
                                        </th>
                                        <th>Trạng thái</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="t" items="${seatTypes}">
                                        <tr>
                                            <td class="cell-strong">
                                                <c:out value="${t.name}" />
                                            </td>
                                            <td>
                                                <fmt:formatNumber value="${t.surcharge}"
                                                                  pattern="#,##0" /> ₫
                                            </td>
                                            <td>
                                                <form method="post" action="${pageUrl}"
                                                      class="inline-form">
                                                    <input type="hidden" name="action"
                                                           value="seatTypeBranch">
                                                    <input type="hidden" name="id"
                                                           value="${t.id}">
                                                    <%@ include
                                                        file="/WEB-INF/view/promotion-pricing/fragments/filter-hidden-tuyet.jsp"
                                                        %>
                                                        <input class="price-input"
                                                               type="text"
                                                               inputmode="numeric"
                                                               name="price"
                                                               autocomplete="off"
                                                               value="<fmt:formatNumber value='${t.effectiveSurcharge}' maxFractionDigits='0' groupingUsed='false'/>"
                                                               aria-label="Phụ thu loại ghế ${t.name} tại chi nhánh bạn">
                                                        <button type="submit"
                                                                class="btn-save">Lưu</button>
                                                    </form>
                                                    <c:if
                                                        test="${t.branchSurcharge != null}">
                                                        <form method="post"
                                                              action="${pageUrl}"
                                                              class="inline-form ms-1"
                                                              onsubmit="return confirm('Bỏ phụ thu riêng và dùng lại phụ thu chung của loại ghế này?');">
                                                            <input type="hidden"
                                                                   name="action"
                                                                   value="seatTypeBranchReset">
                                                            <input type="hidden" name="id"
                                                                   value="${t.id}">
                                                            <%@ include
                                                                file="/WEB-INF/view/promotion-pricing/fragments/filter-hidden-tuyet.jsp"
                                                                %>
                                                                <button type="submit"
                                                                        class="btn-reset"
                                                                        style="padding: 4px 8px; font-size: 0.85rem;">Dùng
                                                                    giá chung</button>
                                                            </form>
                                                    </c:if>
                                                </td>
                                                <td class="cfg-hint">
                                                    <c:choose>
                                                        <c:when
                                                            test="${t.branchSurcharge != null}">
                                                            <span class="branch-badge">
                                                                <i
                                                                    class="fa-solid fa-circle-exclamation"></i>
                                                                Đang dùng phụ thu riêng
                                                                (Chung:
                                                                <fmt:formatNumber
                                                                    value="${t.surcharge}"
                                                                    pattern="#,##0" /> ₫)
                                                            </span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span
                                                                style="color: #64748b;">Đang
                                                                áp dụng phụ thu chung toàn
                                                                chuỗi</span>
                                                            </c:otherwise>
                                                        </c:choose>
                                                </td>
                                            </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </c:if>
                    </div>

                    <%-- 3. Phụ thu định dạng phòng --%>
                    <div class="cfg-section" data-section="room">
                        <div class="cfg-head">
                            <h3 class="cfg-title">
                                <i class="fa-solid fa-film"></i> 3. Phụ thu định
                                dạng phòng
                                <c:if test="${not empty targetBranchId}">
                                    <span class="badge"
                                          style="font-size: 0.8rem; font-weight: normal; margin-left: 8px; background: #e0e7ff; color: #3730a3; padding: 2px 8px; border-radius: 4px;">
                                        <c:out value="${branchPill}" />
                                    </span>
                                </c:if>
                            </h3>
                            <c:if test="${isAdminUser}"><button type="button"
                                    class="btn-edit cfg-view" data-edit><i
                                        class="fa-solid fa-pen"></i> Sửa</button>
                                </c:if>
                        </div>
                        <c:if test="${isAdminUser}">
                            <form method="post" action="${pageUrl}">
                                <input type="hidden" name="action"
                                       value="saveConfig">
                                <input type="hidden" name="section" value="room">
                                <%@ include
                                    file="/WEB-INF/view/promotion-pricing/fragments/filter-hidden-tuyet.jsp"
                                    %>
                                    <table class="cfg-table">
                                        <tbody>
                                            <c:forEach var="t" items="${roomTypes}">
                                                <tr>
                                                    <td class="cell-strong">
                                                        <c:out value="${t.name}" />
                                                    </td>
                                                    <td class="cfg-value">
                                                        <span class="cfg-view"><b>
                                                                <fmt:formatNumber
                                                                    value="${t.effectiveSurcharge}"
                                                                    pattern="#,##0" />
                                                                ₫
                                                            </b></span>
                                                        <span
                                                            class="cfg-edit"><input
                                                                class="price-input"
                                                                type="text"
                                                                inputmode="numeric"
                                                                name="room_${t.id}"
                                                                autocomplete="off"
                                                                value="<fmt:formatNumber value='${t.effectiveSurcharge}' maxFractionDigits='0' groupingUsed='false'/>"
                                                                aria-label="Phụ thu loại phòng ${t.name}">
                                                            ₫</span>
                                                    </td>
                                                    <td class="cfg-hint">
                                                        <c:choose>
                                                            <c:when
                                                                test="${t.branchSurcharge != null}">
                                                                <span
                                                                    class="branch-badge">
                                                                    <i
                                                                        class="fa-solid fa-circle-exclamation"></i>
                                                                    Riêng chi nhánh
                                                                    (Chung:
                                                                    <fmt:formatNumber
                                                                        value="${t.surcharge}"
                                                                        pattern="#,##0" />
                                                                    ₫)
                                                                </span>
                                                            </c:when>
                                                            <c:when
                                                                test="${not empty targetBranchId}">
                                                                <span
                                                                    style="color: #64748b;">Đang
                                                                    áp dụng phụ thu
                                                                    chung toàn
                                                                    chuỗi</span>
                                                                </c:when>
                                                                <c:otherwise>Đổi phụ thu
                                                                thì giá các suất sắp
                                                                chiếu của loại phòng
                                                                này đổi theo
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>
                                    <div class="cfg-edit cfg-form-actions">
                                        <button type="button" class="btn-reset"
                                                data-cancel>Hủy</button>
                                        <button type="submit"
                                                class="btn-save btn-save-lg"><i
                                                class="fa-solid fa-floppy-disk"></i>
                                            Lưu</button>
                                    </div>
                                </form>
                        </c:if>
                        <c:if test="${not isAdminUser}">
                            <p class="note">Phụ thu chung do Admin đặt. Bạn có thể lưu
                                một mức phụ thu cố định riêng cho chi nhánh (áp dụng
                                cho suất sắp chiếu và suất mới). Bằng phụ thu chung
                                nghĩa là dùng lại giá chung.</p>
                            <table class="cfg-table">
                                <thead>
                                    <tr>
                                        <th>Loại phòng</th>
                                        <th>Phụ thu chung</th>
                                        <th>Phụ thu tại
                                            <c:out value="${branchPill}" />
                                        </th>
                                        <th>Trạng thái</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="t" items="${roomTypes}">
                                        <tr>
                                            <td class="cell-strong">
                                                <c:out value="${t.name}" />
                                            </td>
                                            <td class="cfg-value">
                                                <fmt:formatNumber value="${t.surcharge}"
                                                                  pattern="#,##0" /> ₫
                                            </td>
                                            <td>
                                                <form method="post" action="${pageUrl}"
                                                      style="display: inline-flex; gap: 6px; align-items: center; flex-wrap: wrap;">
                                                    <input type="hidden" name="action"
                                                           value="roomTypeBranch">
                                                    <input type="hidden" name="id"
                                                           value="${t.id}">
                                                    <%@ include
                                                        file="/WEB-INF/view/promotion-pricing/fragments/filter-hidden-tuyet.jsp"
                                                        %>
                                                        <input class="price-input"
                                                               type="text"
                                                               inputmode="numeric"
                                                               name="price"
                                                               autocomplete="off"
                                                               value="<fmt:formatNumber value='${t.effectiveSurcharge}' maxFractionDigits='0' groupingUsed='false'/>"
                                                               aria-label="Phụ thu ${t.name} tại chi nhánh">
                                                        <button type="submit"
                                                                class="btn-save">Lưu</button>
                                                </form>
                                                <c:if test="${t.branchSurcharge != null}">
                                                    <form method="post" action="${pageUrl}"
                                                          class="inline-form ms-1"
                                                          style="display: inline-block; margin-left: 6px;"
                                                          onsubmit="return confirm('Bỏ phụ thu riêng và dùng lại phụ thu chung của loại phòng này?');">
                                                        <input type="hidden" name="action"
                                                               value="roomTypeBranchReset">
                                                        <input type="hidden" name="id"
                                                               value="${t.id}">
                                                        <%@ include
                                                            file="/WEB-INF/view/promotion-pricing/fragments/filter-hidden-tuyet.jsp"
                                                            %>
                                                            <button type="submit"
                                                                    class="btn-reset"
                                                                    style="padding: 4px 8px; font-size: 0.85rem;">Dùng
                                                                giá chung</button>
                                                    </form>
                                                </c:if>
                                            </td>
                                            <td class="cfg-hint">
                                                <c:choose>
                                                    <c:when test="${t.branchSurcharge != null}">
                                                        <span class="branch-badge">
                                                            <i class="fa-solid fa-circle-exclamation"></i>
                                                            Đang dùng phụ thu riêng
                                                            (Chung:
                                                            <fmt:formatNumber
                                                                value="${t.surcharge}"
                                                                pattern="#,##0" /> ₫)
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span style="color: #64748b;">Đang
                                                            áp dụng phụ thu chung toàn
                                                            chuỗi</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </c:if>   
                    </div>
                </div>
            </main>
        </div>
        <script src="${pageContext.request.contextPath}/assets/js/pricing-tuyet.js?v=2"></script>
        <style>
            .pp-page .cfg-table {
                font-size: 16px !important;
            }

            .pp-page .cfg-table td {
                padding: 14px 18px !important;
            }

            .pp-page .cfg-table td:first-child,
            .pp-page .cfg-table td.cell-strong {
                font-size: 16px !important;
                font-weight: 700 !important;
                color: #0f172a !important;
            }

            .pp-page .cfg-table .cfg-value,
            .pp-page .cfg-table .cfg-value b,
            .pp-page .cfg-table .cfg-view b {
                font-size: 18px !important;
                color: #0f172a !important;
                font-weight: 800 !important;
            }

            .pp-page .cfg-table .cfg-hint {
                font-size: 14px !important;
                color: #475569 !important;
            }
        </style>
    </body>