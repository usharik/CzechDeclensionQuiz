// Auto-redirect to user's preferred language on first visit
window.addEventListener('DOMContentLoaded', () => {
    // Only redirect from index.html (English page)
    if (!window.location.pathname.endsWith('index.html') &&
        !window.location.pathname.endsWith('/') &&
        !window.location.pathname.endsWith('/docs/')) {
        return; // Don't redirect if already on a language-specific page
    }

    // Check if user has already been redirected (to avoid redirect loop)
    const hasVisited = sessionStorage.getItem('languageRedirected');
    if (hasVisited) {
        return;
    }

    // Get browser language
    const browserLang = navigator.language.split('-')[0];
    const langMap = {
        'cs': 'cs.html',
        'ru': 'ru.html',
        'uk': 'ua.html',  // Ukrainian browser language
        'ua': 'ua.html'
    };

    // Redirect if browser language is supported
    if (langMap[browserLang]) {
        sessionStorage.setItem('languageRedirected', 'true');
        window.location.href = langMap[browserLang];
    }
});

