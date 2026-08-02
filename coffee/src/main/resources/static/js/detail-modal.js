(function () {
    const DETAIL_PATH_PATTERN = /^\/(?:coffee-beans|brew-records|cafe-filter-coffees)\/\d+\/?$/;
    let modal;
    let panel;
    let content;
    let title;
    let closeButton;
    let lastFocusedElement;
    let abortController;

    function isDetailUrl(url) {
        return url.origin === window.location.origin && DETAIL_PATH_PATTERN.test(url.pathname);
    }

    function ensureModal() {
        if (modal) {
            return;
        }

        modal = document.createElement('div');
        modal.className = 'detail-modal';
        modal.hidden = true;
        modal.innerHTML = `
            <div class="detail-modal__backdrop" data-detail-modal-close></div>
            <section class="detail-modal__panel" role="dialog" aria-modal="true" aria-labelledby="detail-modal-title" tabindex="-1">
                <header class="detail-modal__header">
                    <strong id="detail-modal-title">상세 정보</strong>
                    <button class="detail-modal__close" type="button" data-detail-modal-close aria-label="상세 창 닫기">×</button>
                </header>
                <div class="detail-modal__body" data-detail-modal-body></div>
            </section>
        `;
        document.body.appendChild(modal);

        panel = modal.querySelector('.detail-modal__panel');
        content = modal.querySelector('[data-detail-modal-body]');
        title = modal.querySelector('#detail-modal-title');
        closeButton = modal.querySelector('.detail-modal__close');

        modal.addEventListener('click', (event) => {
            if (event.target.closest('[data-detail-modal-close]')) {
                closeModal();
            }
        });
    }

    function setLoading() {
        ensureModal();
        title.textContent = '상세 정보';
        content.innerHTML = `
            <div class="detail-modal__loading" role="status" aria-live="polite">
                <strong>상세 정보를 불러오는 중입니다</strong>
                <span>잠시만 기다려주세요.</span>
            </div>
        `;
    }

    function openModal() {
        ensureModal();
        lastFocusedElement = document.activeElement;
        modal.hidden = false;
        document.body.classList.add('is-detail-modal-open');
        panel.focus({preventScroll: true});
    }

    function closeModal() {
        if (!modal || modal.hidden) {
            return;
        }

        if (abortController) {
            abortController.abort();
            abortController = null;
        }

        modal.hidden = true;
        content.innerHTML = '';
        document.body.classList.remove('is-detail-modal-open');
        if (lastFocusedElement && document.contains(lastFocusedElement)) {
            lastFocusedElement.focus({preventScroll: true});
        }
    }

    function shouldSkipScript(sourceScript) {
        const source = sourceScript.getAttribute('src') || '';
        return source.includes('/js/app-actions.js') || source.includes('/js/detail-modal.js');
    }

    function executeScripts(sourceDocument) {
        sourceDocument.querySelectorAll('script').forEach((sourceScript) => {
            if (shouldSkipScript(sourceScript)) {
                return;
            }

            const script = document.createElement('script');
            Array.from(sourceScript.attributes).forEach((attribute) => {
                script.setAttribute(attribute.name, attribute.value);
            });
            script.textContent = sourceScript.textContent;
            document.body.appendChild(script);
            if (!script.src) {
                script.remove();
            }
        });
    }

    function prepareDetailMain(sourceDocument, url) {
        const main = sourceDocument.querySelector('main.page');
        if (!main) {
            return null;
        }

        const clonedMain = main.cloneNode(true);
        clonedMain.classList.add('detail-modal__content');
        clonedMain.querySelectorAll('.page__header .text-link').forEach((link) => {
            link.remove();
        });

        clonedMain.querySelectorAll('a[href]').forEach((link) => {
            const targetUrl = new URL(link.getAttribute('href'), url);
            if (isDetailUrl(targetUrl)) {
                link.setAttribute('data-detail-modal-link', 'true');
            }
        });

        return clonedMain;
    }

    async function loadDetail(url) {
        ensureModal();
        if (abortController) {
            abortController.abort();
        }
        abortController = new AbortController();

        setLoading();
        openModal();

        const response = await fetch(url.href, {
            headers: {'X-Requested-With': 'fetch'},
            signal: abortController.signal
        });

        if (!response.ok) {
            throw new Error(`상세 정보를 불러오지 못했습니다. (${response.status})`);
        }

        const html = await response.text();
        const sourceDocument = new DOMParser().parseFromString(html, 'text/html');
        const nextContent = prepareDetailMain(sourceDocument, url);

        if (!nextContent) {
            window.location.href = url.href;
            return;
        }

        const heading = nextContent.querySelector('.page__header h1');
        title.textContent = heading?.textContent?.trim() || sourceDocument.title || '상세 정보';
        content.replaceChildren(nextContent);
        executeScripts(sourceDocument);
        closeButton.focus({preventScroll: true});
    }

    function shouldIgnoreClick(event, anchor) {
        return event.defaultPrevented
            || event.metaKey
            || event.ctrlKey
            || event.shiftKey
            || event.altKey
            || anchor.target === '_blank'
            || anchor.hasAttribute('download');
    }

    document.addEventListener('click', (event) => {
        const anchor = event.target.closest('a[href]');
        if (!anchor || shouldIgnoreClick(event, anchor)) {
            return;
        }

        const url = new URL(anchor.getAttribute('href'), window.location.href);
        const isModalCandidate = isDetailUrl(url)
            && (anchor.matches('.record-card__link, .cafe-filter-card__link, .table-link')
                || anchor.hasAttribute('data-detail-modal-link')
                || anchor.closest('.detail-modal'));

        if (!isModalCandidate) {
            return;
        }

        event.preventDefault();
        loadDetail(url).catch((error) => {
            if (error.name === 'AbortError') {
                return;
            }
            window.location.href = url.href;
        });
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            closeModal();
        }
    });
})();
