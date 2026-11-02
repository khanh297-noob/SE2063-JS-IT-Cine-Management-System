/* Chuông thông báo (notification-bell.jspf): số chưa đọc, popup danh sách, đánh dấu đã đọc, xem thêm.
   Dữ liệu từ NotificationServlet (/notifications/*). Không tải lại trang; tự kiểm tra số chưa đọc mỗi 60 giây. */
(function () {
    var root = document.querySelector('[data-notif-root]');
    if (!root) return;

    var ctx = root.getAttribute('data-ctx') || '';
    var bell = root.querySelector('.notif-bell');
    var badge = root.querySelector('.notif-badge');
    var panel = root.querySelector('.notif-panel');
    var list = root.querySelector('.notif-list');
    var more = root.querySelector('.notif-more');
    var readAll = root.querySelector('.notif-readall');
    var oldestId = null;
    var POLL_MS = 60000;

    function api(path, options) {
        var opts = options || {};
        opts.credentials = 'same-origin';
        opts.headers = Object.assign({ 'Accept': 'application/json' }, opts.headers || {});
        return fetch(ctx + '/notifications/' + path, opts).then(function (r) {
            var type = r.headers.get('content-type') || '';
            if (!r.ok || type.indexOf('application/json') < 0) throw new Error('Notification API ' + r.status);
            return r.json();
        });
    }
    function post(path, data) {
        return api(path, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams(data || {}).toString()
        });
    }

    function setBadge(n) {
        if (n > 0) {
            badge.textContent = n > 99 ? '99+' : String(n);
            badge.hidden = false;
            bell.setAttribute('aria-label', 'Thông báo, ' + n + ' chưa đọc');
        } else {
            badge.hidden = true;
            bell.setAttribute('aria-label', 'Thông báo');
        }
    }

    function ago(iso) {
        if (!iso) return '';
        var t = new Date(iso);
        var s = Math.floor((Date.now() - t.getTime()) / 1000);
        if (s < 60) return 'Vừa xong';
        if (s < 3600) return Math.floor(s / 60) + ' phút trước';
        if (s < 86400) return Math.floor(s / 3600) + ' giờ trước';
        if (s < 7 * 86400) return Math.floor(s / 86400) + ' ngày trước';
        return ('0' + t.getDate()).slice(-2) + '/' + ('0' + (t.getMonth() + 1)).slice(-2) + '/' + t.getFullYear();
    }

    function internalLink(link) {
        return typeof link === 'string' && link.charAt(0) === '/' && link.charAt(1) !== '/' ? ctx + link : null;
    }

    function itemNode(n) {
        var li = document.createElement('li');
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'notif-item' + (n.read ? '' : ' unread');
        var body = document.createElement('span');
        body.className = 'notif-body';
        var title = document.createElement('span');
        title.className = 'notif-item-title';
        title.textContent = n.title;
        body.appendChild(title);
        if (n.content) {
            var content = document.createElement('span');
            content.className = 'notif-item-content';
            content.textContent = n.content;
            body.appendChild(content);
        }
        var time = document.createElement('span');
        time.className = 'notif-item-time';
        time.textContent = ago(n.createdAt);
        body.appendChild(time);
        btn.appendChild(body);
        if (!n.read) {
            var dot = document.createElement('span');
            dot.className = 'notif-dot';
            dot.setAttribute('aria-label', 'Chưa đọc');
            btn.appendChild(dot);
        }
        btn.addEventListener('click', function () { openItem(n, btn); });
        li.appendChild(btn);
        return li;
    }

    function openItem(n, btn) {
        var go = internalLink(n.link);
        var done = function () { if (go) window.location.href = go; };
        if (n.read) { done(); return; }
        post('read', { id: n.id }).then(function (res) {
            n.read = true;
            btn.classList.remove('unread');
            var dot = btn.querySelector('.notif-dot');
            if (dot) dot.remove();
            if (typeof res.unread === 'number') setBadge(res.unread);
        }).catch(function () { }).then(done);
    }

    function render(data, append) {
        if (!append) list.innerHTML = '';
        if (!append && data.items.length === 0) {
            var empty = document.createElement('li');
            empty.className = 'notif-empty';
            empty.textContent = data.available === false ? 'Chưa tải được thông báo.' : 'Chưa có thông báo nào.';
            list.appendChild(empty);
        }
        data.items.forEach(function (n) { list.appendChild(itemNode(n)); });
        if (data.items.length) oldestId = data.items[data.items.length - 1].id;
        more.hidden = !data.hasMore;
        setBadge(data.unread || 0);
    }

    function loadFirstPage() {
        oldestId = null;
        return api('list?limit=10').then(function (d) { render(d, false); }).catch(function () {
            render({ items: [], hasMore: false, unread: 0, available: false }, false);
        });
    }

    function refreshCount() {
        api('count').then(function (d) { setBadge(d.unread || 0); }).catch(function () { });
    }

    function open() {
        panel.hidden = false;
        bell.setAttribute('aria-expanded', 'true');
        loadFirstPage();
    }
    function close(focusBell) {
        if (panel.hidden) return;
        panel.hidden = true;
        bell.setAttribute('aria-expanded', 'false');
        if (focusBell) bell.focus();
    }

    bell.addEventListener('click', function (e) {
        e.stopPropagation();
        if (panel.hidden) open(); else close(false);
    });
    panel.addEventListener('click', function (e) { e.stopPropagation(); });
    document.addEventListener('click', function () { close(false); });
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape') close(true); });

    more.addEventListener('click', function () {
        if (oldestId == null) return;
        api('list?limit=10&before=' + oldestId).then(function (d) { render(d, true); }).catch(function () { });
    });
    readAll.addEventListener('click', function () {
        post('read-all').then(function () {
            list.querySelectorAll('.notif-item.unread').forEach(function (b) {
                b.classList.remove('unread');
                var dot = b.querySelector('.notif-dot');
                if (dot) dot.remove();
            });
            setBadge(0);
        }).catch(function () { });
    });

    refreshCount();
    setInterval(function () { if (!document.hidden) refreshCount(); }, POLL_MS);
    document.addEventListener('visibilitychange', function () { if (!document.hidden) refreshCount(); });
})();
