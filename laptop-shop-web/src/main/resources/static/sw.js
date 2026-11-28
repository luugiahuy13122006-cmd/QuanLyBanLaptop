const CACHE_NAME = 'nexora-static-v2';
const OFFLINE_ASSETS = [
  '/',
  '/vendor/bootstrap/bootstrap.min.css',
  '/vendor/bootstrap/bootstrap.bundle.min.js',
  '/vendor/bootstrap-icons/bootstrap-icons.min.css',
  '/vendor/bootstrap-icons/fonts/bootstrap-icons.woff2?v=1.11.3',
  '/css/base.css',
  '/css/components.css',
  '/images/laptop-default.svg'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(OFFLINE_ASSETS).catch(() => {}))
  );
  self.skipWaiting();
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
    )
  );
  self.clients.claim();
});

self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') return;
  const url = new URL(event.request.url);
  if (url.pathname.startsWith('/vendor/') || url.pathname.startsWith('/css/')
      || url.pathname.startsWith('/js/') || url.pathname.startsWith('/images/')) {
    event.respondWith(
      caches.match(event.request).then(cached => cached || fetch(event.request))
    );
  }
});
