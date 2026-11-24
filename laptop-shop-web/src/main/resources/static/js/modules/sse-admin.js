// Admin / Staff Real-time SSE notifications & live unread chat badge
document.addEventListener('DOMContentLoaded', function () {
  if (!document.querySelector('.pro-sidebar')) return;
  if (typeof EventSource === 'undefined') return;

  function syncUnreadBadges() {
    fetch('/api/chat/unread-count')
      .then(r => r.ok ? r.json() : null)
      .then(res => {
        if (!res) return;
        const count = Number(res.unreadCount || 0);
        document.querySelectorAll('.nx-unread-chat-badge').forEach(el => {
          el.textContent = count;
          el.style.display = count > 0 ? '' : 'none';
        });
      }).catch(() => {});
  }

  try {
    const es = new EventSource('/admin/sse/notifications');
    es.addEventListener('notification', function (ev) {
      try {
        const data = JSON.parse(ev.data);
        const isChat = data.type === 'NEW_CHAT';
        if (window.NexoraToast) {
          window.NexoraToast(
            data.title || 'Thông báo mới',
            data.message || '',
            isChat ? 'danger' : 'info',
            data.url || ''
          );
        }
        if (isChat) {
          syncUnreadBadges();
        }
      } catch (e) {}
    });
  } catch (e) {}
});
