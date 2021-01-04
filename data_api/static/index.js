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
        if (e.data.type != 'offlineMode' || window.isOffline) {
            return;
        }

        window.isOffline = true;

        document.querySelectorAll('.offline').forEach(function(elem) {
            elem.classList.add('offline_visible');
        });
    });

    navigator.serviceWorker
        .register(window.swUrl, { scope: window.indexUrl })
        .catch(function(error) {
            console.error('SW registration failed:', error);
        });
}
