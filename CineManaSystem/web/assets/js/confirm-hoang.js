/* Nút submit có data-confirm sẽ hỏi lại trước khi gửi form. */
document.addEventListener('click', function (e) {
    var btn = e.target.closest('[data-confirm]');
    if (btn && !window.confirm(btn.dataset.confirm)) e.preventDefault();
});
