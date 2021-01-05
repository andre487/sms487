document.addEventListener('visibilitychange', function() {
    if (document.visibilityState != 'visible') {
        return;
    }

    var url = location.toString();
    if (url.indexOf('no_reload=1') == -1) {
        location.assign(url);
    }
});

if ('serviceWorker' in navigator) {
    navigator.serviceWorker.addEventListener('message', e => {
        if (e.data.type == 'offlineMode') {
            var isOffline = e.data.val;

            requestAnimationFrame(function() {
                document.querySelectorAll('.offline').forEach(function(elem) {
                    var classList = elem.classList;
                    if (isOffline) {
                        classList.add('offline_visible');
                    } else {
                        classList.remove('offline_visible');
                    }
                });
            });
        }
    });

    navigator.serviceWorker
        .register(window.swUrl, { scope: window.indexUrl })
        .then(() => {
            return navigator.serviceWorker.ready;
        })
        .then(reg => {
            reg.active.postMessage({ type: 'requestOfflineMode' });
        })
        .catch(function(error) {
            console.error('SW registration failed:', error);
        });
}
