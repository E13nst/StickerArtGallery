/**
 * Тема админ-панели: по умолчанию тёмная, переключение в localStorage.
 */
(function () {
    var STORAGE_KEY = 'admin-theme';

    function getStored() {
        try {
            return localStorage.getItem(STORAGE_KEY);
        } catch (e) {
            return null;
        }
    }

    function apply(name) {
        var root = document.documentElement;
        if (name === 'light') {
            root.classList.remove('dark');
        } else {
            root.classList.add('dark');
        }
    }

    function get() {
        var v = getStored();
        return v === 'light' || v === 'dark' ? v : 'dark';
    }

    function set(name) {
        if (name !== 'light' && name !== 'dark') name = 'dark';
        try {
            localStorage.setItem(STORAGE_KEY, name);
        } catch (e) { /* ignore */ }
        apply(name);
        try {
            window.dispatchEvent(new CustomEvent('admin-theme-change', { detail: name }));
        } catch (e2) { /* ignore */ }
    }

    function init() {
        apply(get());
    }

    function toggle() {
        set(get() === 'dark' ? 'light' : 'dark');
    }

    function isDark() {
        return get() === 'dark';
    }

    window.AdminTheme = {
        init: init,
        get: get,
        set: set,
        toggle: toggle,
        isDark: isDark
    };

    init();
})();
