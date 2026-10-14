<c:set var="pageTitle" value="Lịch chiếu"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<section class="hero">
    <h1>Lịch chiếu</h1>
    <form method="get" action="${ctx}/" class="inline-form">
        <label class="select-label">Rạp
            <select name="branchId" onchange="this.form.submit()">
                <c:forEach var="b" items="${branches}">
                    <option value="${b.branchId}" ${b.branchId == branchId ? 'selected' : ''}>${fn:escapeXml(b.name)}</option>
                </c:forEach>
            </select>
        </label>
        <input type="hidden" name="date" value="${selectedDate}">
    </form>
</section>

<div class="day-tabs">
    <c:forEach var="d" items="${days}">
        <a class="day-tab ${d.iso == selectedDate ? 'active' : ''}" href="${ctx}/?branchId=${branchId}&date=${d.iso}">
            ${d.today ? 'Hôm nay' : d.label}
        </a>
    </c:forEach>
</div>

<c:choose>
    <c:when test="${empty movies}">
        <div class="empty">Không có suất chiếu nào trong ngày này. (MSG01)</div>
    </c:when>
    <c:otherwise>
        <div class="movie-list">
            <c:forEach var="m" items="${movies}">
                <article class="movie-card">
                    <div class="poster" aria-hidden="true">${fn:substring(m.title, 0, 1)}</div>
                    <div class="movie-body">
                        <h2>${fn:escapeXml(m.title)} <span class="age age-${m.ageRating}">${m.ageRating}</span></h2>
                        <p class="muted small">${m.duration} phút</p>
                        <div class="chips">
                            <c:forEach var="s" items="${m.showtimes}">
                                <c:choose>
                                    <c:when test="${s.bookable}">
                                        <a class="chip" href="${ctx}/booking/seats?showtimeId=${s.showtimeId}">
                                            <b>${s.startStr}</b><span>${s.roomType} · còn ${s.availableCount}</span>
                                        </a>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="chip disabled" title="${s.displayStatus}">
                                            <b>${s.startStr}</b><span>${s.roomType} · ${s.displayStatus}</span>
                                        </span>
                                    </c:otherwise>
                                </c:choose>
                            </c:forEach>
                        </div>
                    </div>
                </article>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
