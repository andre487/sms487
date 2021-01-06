(function() {
    setupAutoRefresh();

    function setupAutoRefresh() {
        if (location.toString().indexOf('refresh=1') == -1) {
            return;
        }

        document.addEventListener('visibilitychange', function() {
            if (document.visibilityState == 'visible') {
                location.assign(location.toString());
            }
        });
    }
})();
