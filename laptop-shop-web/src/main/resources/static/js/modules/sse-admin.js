// Admin / Staff Real-time SSE notifications
document.addEventListener('DOMContentLoaded', function () {
  if (!document.querySelector('.pro-sidebar')) return;
  if (typeof EventSource === 'undefined') return;
  try {
    const es = new EventSource('/admin/sse/notifications');
    es.addEventListener('notification', function (ev) {
      try {
        const data = JSON.parse(ev.data);
        if (window.NexoraToast) {
          window.NexoraToast(data.title || 'Thông báo mới', data.message || '', 'info');
        }
      } catch (e) {}
    });
  } catch (e) {}
});
