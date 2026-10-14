<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<c:set var="b" value="${detail.branch}"/>
<c:set var="pageTitle" value="${b.name}"/>
<c:set var="publicPage" value="${true}"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<section class="public-hero">
    <h1><c:out value="${b.name}"/></h1>
    <p><c:out value="${b.address}"/>, <c:out value="${b.city}"/></p>
    <p>Hotline <a href="tel:${fn:escapeXml(b.hotline)}"><c:out value="${b.hotline}"/></a>
        <c:if test="${not empty b.email}">, email <a href="mailto:${fn:escapeXml(b.email)}"><c:out value="${b.email}"/></a></c:if>
    </p>
    <span class="open-badge ${detail.openNow ? 'open' : ''}">${detail.openNow ? 'Đang mở cửa' : 'Đang đóng cửa'}</span>
</section>

<div class="detail-grid">
    <div>
        <h2 class="section-title">Phim đang chiếu</h2>
        <c:choose>
            <c:when test="${empty detail.nowShowing}">
                <p class="page-sub">Chi nhánh chưa có phim đang chiếu. Xem các chi nhánh khác bên cạnh.</p>
            </c:when>
            <c:otherwise>
                <div class="movie-grid">
                    <c:forEach var="ms" items="${detail.nowShowing}">
                        <div class="movie-card">
                            <c:choose>
                                <c:when test="${not empty ms.posterUrl}">
                                    <img class="poster" src="${fn:escapeXml(ms.posterUrl)}" alt="Poster ${fn:escapeXml(ms.movie.title)}" loading="lazy">
                                </c:when>
                                <c:otherwise><span class="poster" aria-hidden="true"></span></c:otherwise>
                            </c:choose>
                            <strong><c:out value="${ms.movie.title}"/></strong>
                            <span class="meta"><c:out value="${ms.movie.ageRating}"/>, ${ms.movie.duration} phút</span>
                            <div class="meta"><c:out value="${ms.genreText}"/></div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <aside>
        <section class="side-block">
            <h2>Giờ mở cửa</h2>
            <c:choose>
                <c:when test="${empty detail.hours}"><p class="meta">Chưa cập nhật.</p></c:when>
                <c:otherwise>
                    <ul>
                        <c:forEach var="h" items="${detail.hours}">
                            <li style="${h.dayOfWeek == detail.todayDayOfWeek ? 'font-weight:600' : ''}">
                                <span>${h.dayName}</span>
                                <span>
                                    <c:choose>
                                        <c:when test="${h.closed}">Nghỉ</c:when>
                                        <c:otherwise>${h.openTime} - ${h.closeTime}<c:if test="${h.overnight}"> (hôm sau)</c:if></c:otherwise>
                                    </c:choose>
                                </span>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </section>

        <section class="side-block">
            <h2>Phòng chiếu</h2>
            <c:choose>
                <c:when test="${empty detail.rooms}"><p class="meta">Chưa có phòng chiếu.</p></c:when>
                <c:otherwise>
                    <ul>
                        <c:forEach var="r" items="${detail.rooms}">
                            <li><span><c:out value="${r.room.roomName}"/> (<c:out value="${r.roomTypeName}"/>)</span>
                                <span class="meta">${r.seatCount} ghế</span></li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </section>

        <c:if test="${not empty otherBranches}">
            <section class="side-block">
                <h2>Chi nhánh khác</h2>
                <ul>
                    <c:forEach var="o" items="${otherBranches}">
                        <li><a href="${ctx}/branch?id=${o.branchId}"><c:out value="${o.name}"/></a>
                            <span class="meta"><c:out value="${o.city}"/></span></li>
                    </c:forEach>
                </ul>
            </section>
        </c:if>
    </aside>
</div>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
