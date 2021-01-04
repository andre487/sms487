const currentCache = 'v1';

addEventListener('activate', event => {
    const clean = caches.keys().then(keyList => {
        return Promise.all(keyList.map(key => {
            if (key != currentCache) {
                return caches.delete(key);
            }
        }));
    });

    event.waitUntil(clean);
});

addEventListener('fetch', e => {
    const request = e.request;
    const url = request.url;

    if (request.method != 'GET' || url.includes('/sw.js')) {
        return;
    }

    const result = fetch(e.request);

    const cacheUrl = new URL(url);
    cacheUrl.search = '';
    const cacheKey = cacheUrl.toString();

    e.respondWith(result.catch(err => {
        console.error(err);
        return caches.match(cacheKey);
    }));

    e.waitUntil(caches.open(currentCache).then(cache => {
        result.then(res => {
            if (res.status == 200) {
                return cache.put(cacheKey, res.clone());
            }
        }).catch(err => {
            console.error(err);
        });
    }));
});
