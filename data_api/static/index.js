document.addEventListener('visibilitychange', function() {
    if (document.visibilityState != 'visible') {
        return;
    }

    var url = location.toString();
    if (url.indexOf('no-reload=1') == -1) {
        location.assign(url);
    }
});
