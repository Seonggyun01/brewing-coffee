document.querySelectorAll('[data-app-fab]').forEach((fab) => {
    document.body.appendChild(fab);

    const path = window.location.pathname;
    const isFocusedFlow = path.endsWith('/new') || path.endsWith('/edit');

    if (isFocusedFlow) {
        fab.hidden = true;
        return;
    }

    document.addEventListener('click', (event) => {
        if (!fab.open || fab.contains(event.target)) {
            return;
        }
        fab.open = false;
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            fab.open = false;
        }
    });
});
