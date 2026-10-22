/* Ô Diễn viên trên form phim: mỗi tên nhập vào thành một card ngay dưới ô input.
   Danh sách được nối bằng ", " vào input ẩn castMembers để gửi lên server
   (server chuẩn hoá lại ở MovieService.cleanCast). */
(function () {
    var input = document.getElementById('castInput');
    var addBtn = document.getElementById('castAdd');
    var cards = document.getElementById('castCards');
    var hidden = document.getElementById('castMembers');
    var error = document.getElementById('castError');
    if (!input || !cards || !hidden) return;

    var MAX_TOTAL = 500;   // = độ dài cột cast_members
    var MAX_NAME = 100;    // = MovieService.MAX_ACTOR_NAME

    function names() {
        return Array.prototype.map.call(cards.querySelectorAll('.cast-card'), function (li) { return li.dataset.name; });
    }

    function sync() { hidden.value = names().join(', '); }

    function showError(msg) {
        error.textContent = msg || '';
        error.hidden = !msg;
        input.classList.toggle('is-invalid', !!msg);
    }

    function makeCard(name) {
        var li = document.createElement('li');
        li.className = 'cast-card';
        li.dataset.name = name;
        var span = document.createElement('span');
        span.className = 'cast-card-name';
        span.textContent = name;
        var rm = document.createElement('button');
        rm.type = 'button';
        rm.className = 'cast-card-remove';
        rm.setAttribute('aria-label', 'Xoá ' + name);
        rm.innerHTML = '&times;';
        li.appendChild(span);
        li.appendChild(rm);
        return li;
    }

    /* Thêm một hoặc nhiều tên (dán "A, B, C" cũng được). Trả về true nếu không có lỗi. */
    function add(raw) {
        var parts = raw.split(',').map(function (s) { return s.trim().replace(/\s+/g, ' '); })
                       .filter(function (s) { return s !== ''; });
        if (parts.length === 0) return true;
        var current = names();
        var lower = current.map(function (n) { return n.toLowerCase(); });
        for (var i = 0; i < parts.length; i++) {
            var name = parts[i];
            if (name.length > MAX_NAME) { showError('Tên diễn viên tối đa ' + MAX_NAME + ' ký tự.'); return false; }
            if (lower.indexOf(name.toLowerCase()) >= 0) { showError('"' + name + '" đã có trong danh sách.'); return false; }
            if (current.concat(name).join(', ').length > MAX_TOTAL) {
                showError('Danh sách diễn viên tối đa ' + MAX_TOTAL + ' ký tự.');
                return false;
            }
            cards.appendChild(makeCard(name));
            current.push(name);
            lower.push(name.toLowerCase());
        }
        sync();
        showError('');
        return true;
    }

    function commit() {
        if (add(input.value)) input.value = '';
        input.focus();
    }

    input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {          // không gửi form khi nhấn Enter trong ô này
            e.preventDefault();
            commit();
        } else if (e.key === 'Backspace' && input.value === '' && cards.lastElementChild) {
            cards.removeChild(cards.lastElementChild); // xoá nhanh card cuối
            sync();
        }
    });
    input.addEventListener('input', function () {
        if (input.value.indexOf(',') >= 0) commit();   // gõ dấu phẩy cũng tách thành card
        else showError('');
    });
    addBtn.addEventListener('click', commit);

    cards.addEventListener('click', function (e) {
        var rm = e.target.closest('.cast-card-remove');
        if (!rm) return;
        cards.removeChild(rm.closest('.cast-card'));
        sync();
        input.focus();
    });

    // Tên còn dở trong ô input khi bấm Lưu vẫn được thêm vào.
    input.form.addEventListener('submit', function (e) {
        if (input.value.trim() !== '' && !add(input.value)) { e.preventDefault(); input.focus(); return; }
        input.value = '';
        sync();
    });

    sync();
})();
