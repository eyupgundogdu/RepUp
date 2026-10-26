// Header theme toggle. The inline script in layout.html has already set the
// "dark" class on <html> before the page was drawn; this file only handles clicks.
// An explicit choice is saved in localStorage; without one, the OS setting wins.
(function () {
    const root = document.documentElement;
    const button = document.getElementById('theme-toggle');
    const systemDark = window.matchMedia('(prefers-color-scheme: dark)');

    function savedTheme() {
        try {
            return localStorage.getItem('theme');
        } catch (e) {
            return null;
        }
    }

    function updateLabel() {
        const dark = root.classList.contains('dark');
        button.setAttribute('aria-label', dark ? 'Switch to light mode' : 'Switch to dark mode');
    }

    button.addEventListener('click', function () {
        const dark = !root.classList.contains('dark');
        root.classList.toggle('dark', dark);
        try {
            localStorage.setItem('theme', dark ? 'dark' : 'light');
        } catch (e) {
            // Storage blocked (private mode): the choice lasts until the page is reloaded.
        }
        updateLabel();
    });

    // Follow OS changes live, but only while the user has not picked a theme.
    systemDark.addEventListener('change', function (event) {
        if (!savedTheme()) {
            root.classList.toggle('dark', event.matches);
            updateLabel();
        }
    });

    updateLabel();
})();
