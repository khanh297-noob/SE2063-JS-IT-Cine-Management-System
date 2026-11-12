// ==========================================================================
// CÁC TÍNH NĂNG XỬ LÝ TRANG GIÁ VÉ & CẤU HÌNH HỆ THỐNG
// ==========================================================================

// 1. Lọc bảng giá vé ở trang chủ (#pricing) theo chi nhánh.
// Mỗi nút có data-branch (id chi nhánh hoặc "all"), mỗi bảng giá (.price-board) có data-branch tương ứng.
(function () {
    'use strict';

    var filter = document.getElementById('branchFilter');
    var container = document.getElementById('priceBoards');
    if (!filter || !container) {
        return;
    }

    var chips = filter.querySelectorAll('.branch-chip');
    var boards = container.querySelectorAll('.price-board');
    var empty = document.getElementById('priceEmpty');

    function apply(branch) {
        var shown = 0;
        boards.forEach(function (board) {
            var visible = branch === 'all' || board.getAttribute('data-branch') === branch;
            board.hidden = !visible;
            if (visible) {
                shown++;
            }
        });
        chips.forEach(function (chip) {
            chip.classList.toggle('active', chip.getAttribute('data-branch') === branch);
        });
        if (empty) {
            empty.hidden = shown !== 0;
        }
    }

    filter.addEventListener('click', function (event) {
        var chip = event.target.closest('.branch-chip');
        if (chip) {
            apply(chip.getAttribute('data-branch'));
        }
    });

    // Tự động kích hoạt chip ban đầu
    var activeChip = filter.querySelector('.branch-chip.active') || chips[0];
    if (activeChip) {
        apply(activeChip.getAttribute('data-branch'));
    }
})();

// 2. Xử lý Chế độ Xem / Sửa các mục cấu hình giá vé (Trang ticket-pricing)
document.addEventListener('DOMContentLoaded', function () {
    'use strict';

    // Bắt sự kiện click nút "Sửa"
    document.querySelectorAll('.pp-page [data-edit]').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            var section = btn.closest('.cfg-section');
            if (section) {
                section.classList.add('is-editing');
            }
        });
    });

    // Bắt sự kiện click nút "Hủy"
    document.querySelectorAll('.pp-page [data-cancel]').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            var section = btn.closest('.cfg-section');
            if (section) {
                section.classList.remove('is-editing');
            }
        });
    });
});