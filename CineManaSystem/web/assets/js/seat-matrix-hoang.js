/* Trình vẽ sơ đồ ghế cho màn Configure Seat Matrix.
   Định dạng layout: hàng nối bằng "/", ô nối bằng ",";
   ""=lối đi, "3"=ghế loại 3, "3x"=ghế loại 3 bị hỏng.
   Kiểm tra lối đi (BFS từ 4 góc) và Sweetbox theo cặp giống SeatLayoutValidator phía server,
   để báo lỗi ngay khi tô; server vẫn kiểm tra lại khi lưu. */
(function () {
    var root = document.getElementById('matrixEditor');
    if (!root) return;

    var gridEl = document.getElementById('seatGrid');
    var locked = root.dataset.locked === 'true';
    var maxRows = +root.dataset.maxRows, maxCols = +root.dataset.maxCols;
    var rows = +root.dataset.rows, cols = +root.dataset.cols;
    var initialLayout = root.dataset.layout || '';

    var colors = {}, sweetbox = {}, firstType = null;
    document.querySelectorAll('.brush[data-brush="type"]').forEach(function (b) {
        colors[b.dataset.type] = b.dataset.color;
        if (b.dataset.sweetbox === 'true') sweetbox[b.dataset.type] = true;
        if (firstType === null) firstType = b.dataset.type;
    });

    var brush = { kind: 'type', type: firstType };
    var grid = parse(initialLayout, rows, cols);
    var original = parse(initialLayout, rows, cols); // để biết ô nào vốn có ghế khi bị khoá
    var painting = false;
    var issues = { messages: [], cells: {} };

    function parse(layout, r, c) {
        var lines = layout.split('/'), g = [];
        for (var i = 0; i < r; i++) {
            var cells = (lines[i] || '').split(','), row = [];
            for (var j = 0; j < c; j++) {
                var tok = (cells[j] || '').trim();
                if (tok === '') row.push(null);
                else row.push({ t: tok.replace('x', ''), b: tok.charAt(tok.length - 1) === 'x' });
            }
            g.push(row);
        }
        return g;
    }

    function serialize() {
        return grid.map(function (row) {
            return row.map(function (s) { return s ? s.t + (s.b ? 'x' : '') : ''; }).join(',');
        }).join('/');
    }

    function label(i) { return String.fromCharCode(65 + i); }
    function cellName(r, c) { return label(r) + (c + 1); }

    function canEdit(r, c) {
        if (!locked) return true;
        return !!(original[r] && original[r][c]); // khoá: chỉ sửa ô vốn có ghế
    }

    function apply(r, c) {
        if (!canEdit(r, c)) return;
        var cur = grid[r][c];
        if (brush.kind === 'aisle') {
            if (locked) return;
            grid[r][c] = null;
        } else if (brush.kind === 'broken') {
            if (cur) cur.b = true;
        } else {
            grid[r][c] = { t: brush.type, b: false };
        }
        paintCell(r, c);
    }

    function cellEl(r, c) {
        return gridEl.querySelector('[data-r="' + r + '"][data-c="' + c + '"]');
    }

    function paintCell(r, c, skipStats) {
        var el = cellEl(r, c);
        if (!el) return;
        var s = grid[r][c];
        el.className = 'cell' + (s ? '' : ' aisle') + (s && s.b ? ' broken' : '') + (canEdit(r, c) ? '' : ' locked')
                + (issues.cells[r + ',' + c] ? ' invalid' : '');
        el.style.backgroundColor = s ? (colors[s.t] || '#7d8299') : '';
        el.setAttribute('aria-label', cellName(r, c) + (s ? (s.b ? ' hỏng' : '') : ' lối đi'));
        if (!skipStats) updateStats();
    }

    function render() {
        gridEl.innerHTML = '';
        gridEl.style.gridTemplateColumns = '26px repeat(' + cols + ', 26px)';
        var corner = document.createElement('span');
        corner.className = 'grid-corner';
        gridEl.appendChild(corner);
        for (var c = 0; c < cols; c++) {
            var h = document.createElement('button');
            h.type = 'button'; h.className = 'grid-head'; h.textContent = c + 1;
            h.dataset.col = c; h.title = 'Tô cả cột ' + (c + 1);
            gridEl.appendChild(h);
        }
        for (var r = 0; r < rows; r++) {
            var rl = document.createElement('button');
            rl.type = 'button'; rl.className = 'grid-row-label'; rl.textContent = label(r);
            rl.dataset.row = r; rl.title = 'Tô cả hàng ' + label(r);
            gridEl.appendChild(rl);
            for (var cc = 0; cc < cols; cc++) {
                var cell = document.createElement('button');
                cell.type = 'button';
                cell.dataset.r = r; cell.dataset.c = cc;
                cell.textContent = cc + 1;
                gridEl.appendChild(cell);
                paintCell(r, cc, true);
            }
        }
        fillRegionInputs();
        updateStats();
    }

    // ---- kiểm tra sơ đồ ----

    /* Lối đi: BFS nhiều nguồn từ các góc là lối đi, loang theo 4 hướng qua các ô lối đi.
       Ô lối đi không được thăm = không thông ra góc nào. */
    function checkAisles(result) {
        var visited = [], queue = [], r, c;
        for (r = 0; r < rows; r++) { visited.push([]); for (c = 0; c < cols; c++) visited[r].push(false); }
        var corners = [[0, 0], [0, cols - 1], [rows - 1, 0], [rows - 1, cols - 1]];
        corners.forEach(function (k) {
            if (!grid[k[0]][k[1]] && !visited[k[0]][k[1]]) { visited[k[0]][k[1]] = true; queue.push(k); }
        });
        if (queue.length === 0) {
            var names = [];
            corners.forEach(function (k) { var n = cellName(k[0], k[1]); if (names.indexOf(n) < 0) names.push(n); });
            result.messages.push('Lối đi phải xuất phát từ góc phòng: đặt ít nhất một trong các góc '
                    + names.join(', ') + ' là lối đi.');
            corners.forEach(function (k) { result.cells[k[0] + ',' + k[1]] = true; });
            return;
        }
        var dirs = [[-1, 0], [1, 0], [0, -1], [0, 1]];
        for (var head = 0; head < queue.length; head++) {
            var cur = queue[head];
            dirs.forEach(function (d) {
                var nr = cur[0] + d[0], nc = cur[1] + d[1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) return;
                if (visited[nr][nc] || grid[nr][nc]) return;
                visited[nr][nc] = true;
                queue.push([nr, nc]);
            });
        }
        var isolated = [];
        for (r = 0; r < rows; r++) {
            for (c = 0; c < cols; c++) {
                if (!grid[r][c] && !visited[r][c]) { isolated.push(cellName(r, c)); result.cells[r + ',' + c] = true; }
            }
        }
        if (isolated.length) {
            result.messages.push('Lối đi tại ' + isolated.slice(0, 8).join(', ') + (isolated.length > 8 ? '...' : '')
                    + ' không nối liền với lối đi ở góc phòng.');
        }
    }

    /* Sweetbox: mỗi dãy Sweetbox liền nhau trong một hàng phải có số ghế chẵn. */
    function checkSweetbox(result) {
        for (var r = 0; r < rows; r++) {
            var c = 0;
            while (c < cols) {
                if (!grid[r][c] || !sweetbox[grid[r][c].t]) { c++; continue; }
                var start = c;
                while (c < cols && grid[r][c] && sweetbox[grid[r][c].t]) c++;
                var len = c - start;
                if (len % 2 !== 0) {
                    result.messages.push((len === 1 ? 'Ghế ' + cellName(r, start)
                            : 'Dãy ' + cellName(r, start) + '-' + cellName(r, c - 1) + ' (' + len + ' ghế)')
                            + ': Sweetbox phải đi theo cặp.');
                    for (var k = start; k < c; k++) result.cells[r + ',' + k] = true;
                }
            }
        }
    }

    function validate() {
        var result = { messages: [], cells: {} };
        if (!locked) checkAisles(result); // khi khoá không đổi được lối đi, server cũng bỏ qua
        checkSweetbox(result);
        var changed = Object.keys(result.cells).join('|') !== Object.keys(issues.cells).join('|');
        var old = issues;
        issues = result;
        if (changed) {
            Object.keys(old.cells).concat(Object.keys(result.cells)).forEach(function (key) {
                var el = cellEl(+key.split(',')[0], +key.split(',')[1]);
                if (el) el.classList.toggle('invalid', !!result.cells[key]);
            });
        }
        var box = document.getElementById('layoutIssues');
        var list = document.getElementById('layoutIssueList');
        list.innerHTML = '';
        result.messages.forEach(function (m) {
            var li = document.createElement('li');
            li.textContent = m;
            list.appendChild(li);
        });
        box.hidden = result.messages.length === 0;
    }

    function updateStats() {
        var counts = {}, broken = 0, total = 0;
        grid.forEach(function (row) {
            row.forEach(function (s) {
                if (!s) return;
                if (s.b) broken++; else { total++; counts[s.t] = (counts[s.t] || 0) + 1; }
            });
        });
        document.querySelectorAll('[data-count]').forEach(function (el) {
            el.textContent = el.dataset.count === 'broken' ? broken : (counts[el.dataset.count] || 0);
        });
        document.getElementById('totalSeats').textContent = total;
        var pending = document.getElementById('pending');
        var dirty = serialize() !== initialLayout || rows !== +root.dataset.rows || cols !== +root.dataset.cols;
        pending.textContent = dirty ? 'Có thay đổi chưa lưu' : 'Chưa có thay đổi';
        pending.classList.toggle('has-changes', dirty);
        validate();
    }

    // ---- tô bằng chuột / chạm ----
    gridEl.addEventListener('pointerdown', function (e) {
        var t = e.target;
        if (t.dataset.r !== undefined) {
            painting = true;
            apply(+t.dataset.r, +t.dataset.c);
            e.preventDefault();
        }
    });
    gridEl.addEventListener('pointerover', function (e) {
        var t = e.target;
        if (painting && t.dataset.r !== undefined) apply(+t.dataset.r, +t.dataset.c);
    });
    window.addEventListener('pointerup', function () { painting = false; });

    // ---- bàn phím (Enter / Space trên ô), tô cả hàng / cột ----
    gridEl.addEventListener('click', function (e) {
        var t = e.target;
        if (t.dataset.r !== undefined && e.detail === 0) apply(+t.dataset.r, +t.dataset.c);
        if (t.dataset.row !== undefined) for (var c = 0; c < cols; c++) apply(+t.dataset.row, c);
        if (t.dataset.col !== undefined) for (var r = 0; r < rows; r++) apply(r, +t.dataset.col);
    });

    // ---- chọn cọ ----
    document.querySelectorAll('.brush').forEach(function (b) {
        if (locked && b.dataset.brush === 'aisle') b.disabled = true;
        b.addEventListener('click', function () {
            document.querySelectorAll('.brush').forEach(function (x) { x.setAttribute('aria-pressed', 'false'); });
            b.setAttribute('aria-pressed', 'true');
            brush = { kind: b.dataset.brush, type: b.dataset.type };
        });
    });

    // ---- tô theo vùng: hàng a-b, cột a-b ----
    var rowFrom = document.getElementById('rowFrom'), rowTo = document.getElementById('rowTo');
    var colFrom = document.getElementById('colFrom'), colTo = document.getElementById('colTo');
    var regionBrush = document.getElementById('regionBrush');
    var regionMsg = document.getElementById('regionMsg');

    function fillRegionInputs() {
        [rowFrom, rowTo].forEach(function (sel, i) {
            var keep = sel.value;
            sel.innerHTML = '';
            for (var r = 0; r < rows; r++) {
                var o = document.createElement('option');
                o.value = r; o.textContent = label(r);
                sel.appendChild(o);
            }
            sel.value = keep !== '' && +keep < rows ? keep : (i === 0 ? 0 : rows - 1);
        });
        colFrom.max = colTo.max = cols;
        if (!(+colFrom.value >= 1 && +colFrom.value <= cols)) colFrom.value = 1;
        if (!(+colTo.value >= 1 && +colTo.value <= cols)) colTo.value = cols;
    }

    document.getElementById('regionApply').addEventListener('click', function () {
        var r1 = +rowFrom.value, r2 = +rowTo.value;
        var c1 = parseInt(colFrom.value, 10), c2 = parseInt(colTo.value, 10);
        if (!(c1 >= 1 && c1 <= cols && c2 >= 1 && c2 <= cols)) {
            regionMsg.textContent = 'Cột phải từ 1 đến ' + cols + '.';
            return;
        }
        if (r1 > r2) { var tr = r1; r1 = r2; r2 = tr; }
        if (c1 > c2) { var tc = c1; c1 = c2; c2 = tc; }
        var v = regionBrush.value;
        var saved = brush;
        brush = v.indexOf('type:') === 0 ? { kind: 'type', type: v.substring(5) } : { kind: v };
        var cellsCount = 0;
        for (var r = r1; r <= r2; r++) {
            for (var c = c1 - 1; c <= c2 - 1; c++) {
                if (canEdit(r, c)) cellsCount++;
                apply(r, c);
            }
        }
        brush = saved;
        regionMsg.textContent = 'Đã áp dụng cho ' + cellsCount + ' ô, vùng ' + label(r1) + c1 + ' đến ' + label(r2) + c2 + '.';
    });

    // ---- đổi kích thước ----
    var rowsInput = document.getElementById('rowsInput');
    var colsInput = document.getElementById('colsInput');
    var resizeBtn = document.getElementById('resizeBtn');
    if (locked) { rowsInput.disabled = colsInput.disabled = resizeBtn.disabled = true; }
    resizeBtn.addEventListener('click', function () {
        var nr = Math.max(1, Math.min(maxRows, parseInt(rowsInput.value, 10) || rows));
        var nc = Math.max(1, Math.min(maxCols, parseInt(colsInput.value, 10) || cols));
        rowsInput.value = nr; colsInput.value = nc;
        var g = [];
        for (var r = 0; r < nr; r++) {
            var row = [];
            for (var c = 0; c < nc; c++) {
                row.push(grid[r] && c < cols ? grid[r][c] : (firstType ? { t: firstType, b: false } : null));
            }
            g.push(row);
        }
        grid = g; rows = nr; cols = nc;
        original = parse('', rows, cols);
        issues = { messages: [], cells: {} };
        render();
    });

    // ---- gửi form ----
    document.getElementById('matrixForm').addEventListener('submit', function (e) {
        validate();
        if (issues.messages.length) {
            e.preventDefault();
            document.getElementById('layoutIssues').scrollIntoView({ behavior: 'smooth', block: 'center' });
            return;
        }
        document.getElementById('rowsField').value = rows;
        document.getElementById('colsField').value = cols;
        document.getElementById('layoutField').value = serialize();
    });

    render();
})();
