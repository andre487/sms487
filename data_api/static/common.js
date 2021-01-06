(function(window, serviceWorker) {
    if (!serviceWorker) {
        return;
    }

    serviceWorker.addEventListener('message', e => {
        switch (e.data.type) {
            case 'offlineMode':
                handleOfflineMode(e);
                break;
        }
    });

    serviceWorker
        .register(window.swUrl, { scope: window.indexUrl })
        .then(() => {
            return serviceWorker.ready;
        })
        .then(reg => {
            reg.active.postMessage({ type: 'requestOfflineMode' });
        })
        .catch(function(err) {
            console.error('SW registration failed:', err);
        });

    function handleOfflineMode(e) {
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
})(window, navigator.serviceWorker);
