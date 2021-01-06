(function() {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
        document.addEventListener('load', init);
    } else {
        init();
    }

    function init() {
        document.removeEventListener('DOMContentLoaded', init);
        document.removeEventListener('load', init);

        document.querySelectorAll('.file-input').forEach(function(form) {
            if (form.tagName != 'FORM') {
                console.warn('File input block is not a form');
                return;
            }
            form.addEventListener('change', onFileChange);
        });
    }

    function onFileChange(e) {
        var input = e.target;
        if (input.tagName != 'INPUT' && input.type != 'file') {
            return;
        }

        var form = e.currentTarget;
        if (input.files.length) {
            form.submit();
        }
    }
})();
