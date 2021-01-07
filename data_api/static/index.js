(function() {
    setupAutoRefresh();

    function setupAutoRefresh() {
        if (!window.needAutoRefresh) {
            return;
        }

        document.addEventListener('visibilitychange', function() {
            if (document.visibilityState == 'visible') {
                location.assign(location.toString());
            }
        });
    }
})();
