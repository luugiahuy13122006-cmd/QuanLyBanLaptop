// Toast & Delete/Action Confirmation Modal helper
window.NexoraToast = function (title, message, variant, targetUrl) {
  const container = document.getElementById('nxToastContainer');
  if (!container || typeof bootstrap === 'undefined') return;
  const tone = variant || 'primary';
  const toastEl = document.createElement('div');
  toastEl.className = 'toast align-items-center border-0 shadow-lg text-bg-' + tone;
  if (targetUrl) toastEl.style.cursor = 'pointer';
  toastEl.setAttribute('role', 'alert');
  toastEl.innerHTML =
    '<div class="d-flex"><div class="toast-body">' +
    '<strong class="d-block mb-1">' + escapeHtml(title) + '</strong>' +
    '<span>' + escapeHtml(message) + '</span>' +
    (targetUrl ? '<div class="small mt-1 text-decoration-underline opacity-75">Nhấn để mở ngay</div>' : '') +
    '</div>' +
    '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>';
  if (targetUrl) {
    toastEl.addEventListener('click', function (ev) {
      if (!ev.target.closest('.btn-close')) {
        window.location.href = targetUrl;
      }
    });
  }
  container.appendChild(toastEl);
  const t = new bootstrap.Toast(toastEl, { delay: 6500 });
  t.show();
  toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
};

function escapeHtml(str) {
  return String(str || '').replace(/[&<>"']/g, function (c) {
    return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c];
  });
}

document.addEventListener('DOMContentLoaded', function () {
  let pendingForm = null;
  const modalEl = document.getElementById('nxConfirmModal');
  const msgEl = document.getElementById('nxConfirmMessage');
  const okBtn = document.getElementById('nxConfirmAcceptBtn');
  const modal = modalEl && typeof bootstrap !== 'undefined' ? new bootstrap.Modal(modalEl) : null;

  document.addEventListener('submit', function (e) {
    const form = e.target;
    const msg = form && form.getAttribute && form.getAttribute('data-confirm');
    if (msg && !form.dataset.confirmed) {
      e.preventDefault();
      if (modal && msgEl) {
        pendingForm = form;
        msgEl.textContent = msg;
        modal.show();
      } else if (window.confirm(msg)) {
        form.dataset.confirmed = 'true';
        form.submit();
      }
    }
  });

  if (okBtn) {
    okBtn.addEventListener('click', function () {
      if (pendingForm) {
        pendingForm.dataset.confirmed = 'true';
        const toSubmit = pendingForm;
        pendingForm = null;
        if (modal) modal.hide();
        toSubmit.submit();
      }
    });
  }
});
