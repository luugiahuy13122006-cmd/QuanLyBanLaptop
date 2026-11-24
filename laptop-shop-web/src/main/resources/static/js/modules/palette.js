// Command Palette (Ctrl+K / Cmd+K) for quick search & navigation
document.addEventListener('DOMContentLoaded', function () {
  const modalEl = document.getElementById('nxCommandModal');
  if (!modalEl || typeof bootstrap === 'undefined') return;
  const modal = new bootstrap.Modal(modalEl);
  const input = document.getElementById('nxCommandInput');
  const resultsEl = document.getElementById('nxCommandResults');
  let timer = null;

  document.addEventListener('keydown', function (e) {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault();
      modal.show();
    }
  });

  document.querySelectorAll('[data-open-palette]').forEach(el => {
    el.addEventListener('click', function (e) {
      e.preventDefault();
      modal.show();
    });
  });

  modalEl.addEventListener('shown.bs.modal', function () {
    if (input) { input.focus(); input.select(); }
  });

  if (input && resultsEl) {
    input.addEventListener('input', function () {
      clearTimeout(timer);
      const q = input.value.trim();
      if (!q) {
        resultsEl.innerHTML = '';
        return;
      }
      timer = setTimeout(function () {
        fetch('/api/store/autocomplete?q=' + encodeURIComponent(q))
          .then(r => r.json())
          .then(items => {
            if (!Array.isArray(items) || items.length === 0) {
              resultsEl.innerHTML = '<div class="text-muted small p-3">Không tìm thấy laptop phù hợp.</div>';
              return;
            }
            resultsEl.innerHTML = items.map(it =>
              '<a href="/san-pham/' + it.id + '" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center">' +
              '<div><strong>' + (it.name || '') + '</strong><small class="text-muted ms-2">' + (it.sku || '') + '</small></div>' +
              '<span class="fw-bold text-primary">' + Number(it.price || 0).toLocaleString('vi-VN') + ' đ</span></a>'
            ).join('');
          })
          .catch(() => {});
      }, 200);
    });
  }
});
