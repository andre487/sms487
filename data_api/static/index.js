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
    navigator.serviceWorker
        .register(window.swUrl, { scope: window.indexUrl })
        .catch(function(error) {
            console.error('SW registration failed:', error);
        });
}
