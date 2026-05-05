(function () {
    const countries = Array.from(document.querySelectorAll('.map-country'));
    const worldMap = document.querySelector('.world-map');
    const zoomInButton = document.querySelector('[data-origin-zoom-in]');
    const zoomOutButton = document.querySelector('[data-origin-zoom-out]');
    const zoomResetButton = document.querySelector('[data-origin-zoom-reset]');
    const zoomLabel = document.querySelector('[data-origin-zoom-label]');
    const countryPanel = document.querySelector('[data-country-panel]');
    const countryName = countryPanel.querySelector('[data-country-name]');
    const countrySummary = countryPanel.querySelector('[data-country-summary]');
    const countryLink = countryPanel.querySelector('[data-country-link]');
    const countryMore = countryPanel.querySelector('[data-country-more]');
    const countryPanelClose = document.querySelector('[data-country-panel-close]');
    const recentBeans = document.querySelector('[data-recent-beans]');
    const statCountries = document.querySelector('[data-stat="countries"]');
    const statBeans = document.querySelector('[data-stat="beans"]');
    const statBrews = document.querySelector('[data-stat="brews"]');
    const countryMap = new Map();
    const MIN_ZOOM = 1;
    const MAX_ZOOM = 4;
    const ZOOM_BUTTON_STEP = 0.24;
    const ZOOM_WHEEL_STEP = 0.08;
    let zoomLevel = 1;
    let zoomOrigin = { x: 50, y: 50 };
    let panOffset = { x: 0, y: 0 };
    let dragStart = null;
    let didDragMap = false;

    function clamp(value, min, max) {
        return Math.min(Math.max(value, min), max);
    }

    function applyZoom() {
        worldMap.style.transformOrigin = `${zoomOrigin.x}% ${zoomOrigin.y}%`;
        worldMap.style.transform = `translate(${panOffset.x}px, ${panOffset.y}px) scale(${zoomLevel})`;
        worldMap.classList.toggle('is-zoomed', zoomLevel > MIN_ZOOM);
        zoomLabel.textContent = `${Math.round(zoomLevel * 100)}%`;
    }

    function setZoomOriginFromEvent(event) {
        const rect = worldMap.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) {
            return;
        }

        zoomOrigin = {
            x: clamp(((event.clientX - rect.left) / rect.width) * 100, 0, 100),
            y: clamp(((event.clientY - rect.top) / rect.height) * 100, 0, 100),
        };
    }

    function zoomMap(delta, event) {
        if (event) {
            setZoomOriginFromEvent(event);
        }

        zoomLevel = clamp(zoomLevel + delta, MIN_ZOOM, MAX_ZOOM);
        if (zoomLevel === MIN_ZOOM) {
            panOffset = { x: 0, y: 0 };
        }
        applyZoom();
    }

    function resetZoom() {
        zoomLevel = MIN_ZOOM;
        zoomOrigin = { x: 50, y: 50 };
        panOffset = { x: 0, y: 0 };
        applyZoom();
    }

    function startDrag(event) {
        if (zoomLevel === MIN_ZOOM || event.button !== 0) {
            return;
        }

        dragStart = {
            pointerId: event.pointerId,
            x: event.clientX,
            y: event.clientY,
            panX: panOffset.x,
            panY: panOffset.y,
        };
        didDragMap = false;
        worldMap.classList.add('is-dragging');
        worldMap.setPointerCapture(event.pointerId);
    }

    function dragMap(event) {
        if (!dragStart || dragStart.pointerId !== event.pointerId) {
            return;
        }

        const deltaX = event.clientX - dragStart.x;
        const deltaY = event.clientY - dragStart.y;
        if (Math.abs(deltaX) > 3 || Math.abs(deltaY) > 3) {
            didDragMap = true;
        }

        panOffset = {
            x: dragStart.panX + deltaX,
            y: dragStart.panY + deltaY,
        };
        applyZoom();
    }

    function endDrag(event) {
        if (!dragStart || dragStart.pointerId !== event.pointerId) {
            return;
        }

        dragStart = null;
        worldMap.classList.remove('is-dragging');
        if (worldMap.hasPointerCapture(event.pointerId)) {
            worldMap.releasePointerCapture(event.pointerId);
        }
    }

    function setPanel(country) {
        document.body.classList.add('has-country-panel');
        countryPanel.hidden = false;
        countries.forEach((countryShape) => countryShape.classList.toggle('is-selected', countryShape.id === country.countryCode));
        countryName.textContent = `${country.koreanCountryName} (${country.countryName})`;
        countrySummary.textContent = `경험한 원두 ${country.beanCount}개, 브루잉 기록 ${country.brewRecordCount}개`;
        countryLink.textContent = `${country.koreanCountryName} 원두 보기`;
        countryLink.href = `/coffee-beans?countryCode=${country.countryCode}`;
        countryMore.href = `/coffee-beans?countryCode=${country.countryCode}`;
        renderRecentBeans(country.recentBeans);
    }

    function renderRecentBeans(beans) {
        recentBeans.innerHTML = '';
        if (!beans || beans.length === 0) {
            const empty = document.createElement('p');
            empty.textContent = '아직 등록된 원두가 없습니다.';
            recentBeans.appendChild(empty);
            return;
        }

        beans.forEach((bean) => {
            const link = document.createElement('a');
            link.className = 'origin-bean-card';
            link.href = `/coffee-beans/${bean.id}`;

            const flavorGradient = document.createElement('span');
            flavorGradient.className = 'flavor-gradient origin-bean-card__flavor-gradient';
            flavorGradient.setAttribute('aria-hidden', 'true');
            flavorGradient.setAttribute('style', bean.flavorGradientStyle || 'background: linear-gradient(90deg, #d8c8b5, #efe4d3);');

            const name = document.createElement('strong');
            name.textContent = bean.name;

            const roastery = document.createElement('span');
            roastery.textContent = bean.roastery || '로스터리 미기록';

            const meta = document.createElement('small');
            meta.textContent = `${bean.region || '지역 미기록'} · ${bean.processType || '가공 미기록'}`;

            const notes = document.createElement('small');
            notes.className = 'origin-bean-card__notes';
            notes.textContent = bean.flavorNoteSummary || '향미 미기록';

            link.append(flavorGradient, name, roastery, meta, notes);
            recentBeans.appendChild(link);
        });
    }

    fetch('/api/origin-map/countries')
        .then((response) => response.json())
        .then((data) => {
            statCountries.textContent = data.experiencedCountryCount;
            statBeans.textContent = data.totalBeanCount;
            statBrews.textContent = data.totalBrewRecordCount;

            data.countries.forEach((country) => {
                countryMap.set(country.countryCode, country);
                const shape = document.getElementById(country.countryCode);
                if (shape) {
                    shape.style.fill = country.fillColor;
                    shape.dataset.beanCount = country.beanCount;
                    shape.classList.add('has-records');
                }
            });
        });

    countries.forEach((countryShape) => {
        countryShape.addEventListener('click', () => {
            if (didDragMap) {
                return;
            }

            const country = countryMap.get(countryShape.id);
            if (!country) {
                return;
            }
            setPanel(country);
        });
    });

    worldMap.addEventListener('wheel', (event) => {
        event.preventDefault();
        zoomMap(event.deltaY < 0 ? ZOOM_WHEEL_STEP : -ZOOM_WHEEL_STEP, event);
    }, { passive: false });

    worldMap.addEventListener('click', (event) => {
        if (!didDragMap) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();
        setTimeout(() => {
            didDragMap = false;
        }, 0);
    }, true);
    worldMap.addEventListener('pointerdown', startDrag);
    worldMap.addEventListener('pointermove', dragMap);
    worldMap.addEventListener('pointerup', endDrag);
    worldMap.addEventListener('pointercancel', endDrag);
    worldMap.addEventListener('lostpointercapture', () => {
        dragStart = null;
        worldMap.classList.remove('is-dragging');
    });

    zoomInButton.addEventListener('click', () => zoomMap(ZOOM_BUTTON_STEP));
    zoomOutButton.addEventListener('click', () => zoomMap(-ZOOM_BUTTON_STEP));
    zoomResetButton.addEventListener('click', resetZoom);
    applyZoom();

    countryPanelClose.addEventListener('click', () => {
        document.body.classList.remove('has-country-panel');
        countryPanel.hidden = true;
        countries.forEach((countryShape) => countryShape.classList.remove('is-selected'));
    });
}());
