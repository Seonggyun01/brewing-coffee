const cafeListElement = document.querySelector('[data-cafe-list]');
const selectedCafeElement = document.querySelector('[data-selected-cafe]');
const cafeCountElement = document.querySelector('[data-cafe-count]');
const cafeMapCanvas = document.querySelector('[data-kakao-cafe-map]');
const cafeMapStatus = document.querySelector('[data-cafe-map-status]');
const mapResetButton = document.querySelector('[data-map-reset]');
const visibleCafeSearchButton = document.querySelector('[data-visible-cafe-search]');
const visibleCafeClearButton = document.querySelector('[data-visible-cafe-clear]');
const visibleCafeStatusElement = document.querySelector('[data-visible-cafe-status]');

const DEFAULT_CENTER = {
    latitude: 36.2683,
    longitude: 127.6358
};
const DEFAULT_LEVEL = 13;
const MAX_VISIBLE_SEARCH_RESULTS = 15;

let cafes = [];
let map = null;
let placesService = null;
let activeInfoWindow = null;
let activeVisibleInfoOverlay = null;
let markersByCafeId = new Map();
let visibleCafeOverlays = [];

const escapeHtml = (value) => String(value || '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');

const hasCoordinate = (cafe) => {
    return Number.isFinite(Number(cafe.latitude)) && Number.isFinite(Number(cafe.longitude));
};

const setStatus = (title, message, isHidden = false) => {
    cafeMapStatus.classList.toggle('is-hidden', isHidden);
    cafeMapStatus.innerHTML = `
        <strong>${escapeHtml(title)}</strong>
        <p>${escapeHtml(message)}</p>
    `;
};

const setVisibleCafeStatus = (message) => {
    visibleCafeStatusElement.hidden = !message;
    visibleCafeStatusElement.textContent = message || '';
};

const selectedCafeHtml = (cafe) => {
    const address = cafe.address || '주소 미기록';
    const visitCount = cafe.visitCount || 1;
    const latestVisit = cafe.latestVisitDate ? `<small>최근 방문 ${escapeHtml(cafe.latestVisitDate)}</small>` : '';

    return `
        <span>선택 카페</span>
        <strong>${escapeHtml(cafe.cafeName)}</strong>
        <p>${escapeHtml(address)}</p>
        <small>방문 ${visitCount}회</small>
        ${latestVisit}
    `;
};

const selectedVisibleCafeHtml = (place) => {
    const address = place.road_address_name || place.address_name || '주소 미기록';
    const link = place.place_url
        ? `<a href="${escapeHtml(place.place_url)}" target="_blank" rel="noopener noreferrer">카카오맵에서 보기</a>`
        : '';

    return `
        <span>현재 화면 검색 결과</span>
        <strong>${escapeHtml(place.place_name)}</strong>
        <p>${escapeHtml(address)}</p>
        ${link}
    `;
};

const visitedInfoHtml = (cafe) => {
    const address = cafe.address || '주소 미기록';
    const visitCount = cafe.visitCount || 1;

    return `
        <div class="cafe-map-info">
            <strong>${escapeHtml(cafe.cafeName)}</strong>
            <p>${escapeHtml(address)}</p>
            <small>방문 ${visitCount}회</small>
        </div>
    `;
};

const visibleCafeInfoHtml = (place) => {
    const address = place.road_address_name || place.address_name || '주소 미기록';
    const link = place.place_url
        ? `<a href="${escapeHtml(place.place_url)}" target="_blank" rel="noopener noreferrer">카카오맵</a>`
        : '';

    return `
        <div class="cafe-map-info cafe-map-info--visible">
            <strong>${escapeHtml(place.place_name)}</strong>
            <p>${escapeHtml(address)}</p>
            ${link}
        </div>
    `;
};

const renderCafeList = () => {
    cafeCountElement.textContent = cafes.length;

    if (cafes.length === 0) {
        cafeListElement.innerHTML = '<p>표시할 카페가 없습니다.</p>';
        selectedCafeElement.innerHTML = '<p>카페 필터 기록을 남기면 이곳에 카페가 표시됩니다.</p>';
        return;
    }

    cafeListElement.innerHTML = cafes.map((cafe) => `
        <button class="cafe-list-card" type="button" data-cafe-id="${cafe.id}">
            <strong>${escapeHtml(cafe.cafeName)}</strong>
            <span>${escapeHtml(cafe.address || '주소 미기록')}</span>
            <small>방문 ${cafe.visitCount || 1}회</small>
        </button>
    `).join('');

    cafeListElement.querySelectorAll('[data-cafe-id]').forEach((card) => {
        card.addEventListener('click', () => {
            const cafe = cafes.find((item) => String(item.id) === card.dataset.cafeId);
            if (cafe) {
                selectCafe(cafe);
            }
        });
    });
};

const markActiveCard = (cafe) => {
    document.querySelectorAll('[data-cafe-id]').forEach((element) => {
        element.classList.toggle('is-active', cafe && String(cafe.id) === element.dataset.cafeId);
    });
};

const closeActiveInfo = () => {
    if (activeInfoWindow) {
        activeInfoWindow.close();
        activeInfoWindow = null;
    }

    if (activeVisibleInfoOverlay) {
        activeVisibleInfoOverlay.setMap(null);
        activeVisibleInfoOverlay = null;
    }
};

const setNationwideView = () => {
    if (!map) {
        return;
    }

    map.setCenter(new kakao.maps.LatLng(DEFAULT_CENTER.latitude, DEFAULT_CENTER.longitude));
    map.setLevel(DEFAULT_LEVEL);
};

function selectCafe(cafe) {
    const marker = markersByCafeId.get(String(cafe.id));
    if (!map || !marker) {
        return;
    }

    const position = marker.getPosition();

    closeActiveInfo();
    selectedCafeElement.innerHTML = selectedCafeHtml(cafe);
    markActiveCard(cafe);
    map.panTo(position);

    if (map.getLevel() > 5) {
        map.setLevel(5);
    }

    activeInfoWindow = new kakao.maps.InfoWindow({
        content: visitedInfoHtml(cafe),
        removable: true
    });
    activeInfoWindow.open(map, marker);
}

function selectVisibleCafe(place, position) {
    closeActiveInfo();
    markActiveCard(null);
    selectedCafeElement.innerHTML = selectedVisibleCafeHtml(place);

    activeVisibleInfoOverlay = new kakao.maps.CustomOverlay({
        position,
        content: visibleCafeInfoHtml(place),
        xAnchor: 0.5,
        yAnchor: 1.42,
        zIndex: 8
    });
    activeVisibleInfoOverlay.setMap(map);
}

const createVisitedMarkers = () => {
    markersByCafeId = new Map();

    cafes.filter(hasCoordinate).forEach((cafe) => {
        const position = new kakao.maps.LatLng(Number(cafe.latitude), Number(cafe.longitude));
        const marker = new kakao.maps.Marker({
            map,
            position,
            title: cafe.cafeName
        });

        markersByCafeId.set(String(cafe.id), marker);
        kakao.maps.event.addListener(marker, 'click', () => selectCafe(cafe));
    });
};

const createVisibleCafePinElement = (place) => {
    const markerElement = document.createElement('button');
    markerElement.type = 'button';
    markerElement.className = 'cafe-map-visible-pin';
    markerElement.setAttribute('aria-label', `${place.place_name} 위치 보기`);
    return markerElement;
};

const clearVisibleCafeSearch = () => {
    closeActiveInfo();

    visibleCafeOverlays.forEach((overlay) => overlay.setMap(null));
    visibleCafeOverlays = [];

    visibleCafeClearButton.hidden = true;
    setVisibleCafeStatus('');
};

const renderVisibleCafes = (places) => {
    clearVisibleCafeSearch();

    places.slice(0, MAX_VISIBLE_SEARCH_RESULTS).forEach((place) => {
        const position = new kakao.maps.LatLng(Number(place.y), Number(place.x));
        const markerElement = createVisibleCafePinElement(place);
        const overlay = new kakao.maps.CustomOverlay({
            position,
            content: markerElement,
            xAnchor: 0.5,
            yAnchor: 1,
            zIndex: 6,
            clickable: true
        });

        markerElement.addEventListener('click', () => selectVisibleCafe(place, position));
        overlay.setMap(map);
        visibleCafeOverlays.push(overlay);
    });

    visibleCafeClearButton.hidden = false;
    setVisibleCafeStatus(`현재 지도 화면에서 카페 ${places.length}개를 찾았어요.`);
    selectedCafeElement.innerHTML = '<p>초록색 핀을 선택하면 현재 화면의 카페 정보를 볼 수 있어요.</p>';
};

const filterPlacesInCurrentBounds = (places) => {
    const bounds = map.getBounds();

    return places.filter((place) => {
        const position = new kakao.maps.LatLng(Number(place.y), Number(place.x));
        return bounds.contain(position);
    });
};

const searchVisibleCafes = () => {
    if (!map || !placesService || !window.kakao?.maps?.services?.Status) {
        setVisibleCafeStatus('카카오맵 장소 검색을 사용할 수 없어요.');
        return;
    }

    visibleCafeSearchButton.disabled = true;
    setVisibleCafeStatus('현재 지도 화면에서 카페를 검색하고 있어요.');

    placesService.categorySearch('CE7', (data, status) => {
        visibleCafeSearchButton.disabled = false;

        const visiblePlaces = status === kakao.maps.services.Status.OK
            ? filterPlacesInCurrentBounds(data)
            : [];

        if (visiblePlaces.length === 0) {
            clearVisibleCafeSearch();
            setVisibleCafeStatus('현재 지도 화면에서 표시할 카페를 찾지 못했어요.');
            return;
        }

        renderVisibleCafes(visiblePlaces);
    }, {
        useMapBounds: true
    });
};

const initializeMap = () => {
    if (!window.kakao?.maps?.Map) {
        setStatus('카카오맵을 불러오지 못했어요.', '키 설정과 JavaScript SDK 도메인 등록을 확인해주세요.');
        return;
    }

    map = new kakao.maps.Map(cafeMapCanvas, {
        center: new kakao.maps.LatLng(DEFAULT_CENTER.latitude, DEFAULT_CENTER.longitude),
        level: DEFAULT_LEVEL
    });
    placesService = window.kakao?.maps?.services?.Places
        ? new kakao.maps.services.Places(map)
        : null;

    createVisitedMarkers();
    setStatus('', '', true);
    setNationwideView();

    selectedCafeElement.innerHTML = '<p>지도 마커나 목록에서 카페를 선택하세요.</p>';

    window.setTimeout(() => {
        map.relayout();
        setNationwideView();
    }, 0);
};

const initialize = () => {
    fetch('/api/maps/cafes')
        .then((response) => {
            if (!response.ok) {
                throw new Error('Failed to load cafes');
            }
            return response.json();
        })
        .then((data) => {
            cafes = data.filter(hasCoordinate);
            renderCafeList();

            if (cafes.length === 0) {
                setStatus('표시할 카페가 없어요.', '카페 필터 기록에서 좌표가 있는 카페를 등록해보세요.');
            }

            if (window.kakao?.maps?.load) {
                kakao.maps.load(initializeMap);
            } else {
                initializeMap();
            }
        })
        .catch(() => {
            cafeListElement.innerHTML = '<p>카페 데이터를 불러오지 못했습니다.</p>';
            selectedCafeElement.innerHTML = '<p>잠시 후 다시 시도해주세요.</p>';
            setStatus('카페 데이터를 불러오지 못했어요.', '서버 상태를 확인한 뒤 다시 열어주세요.');
        });
};

mapResetButton.addEventListener('click', () => {
    closeActiveInfo();
    markActiveCard(null);
    selectedCafeElement.innerHTML = '<p>지도 마커나 목록에서 카페를 선택하세요.</p>';
    setNationwideView();
});

visibleCafeSearchButton.addEventListener('click', searchVisibleCafes);
visibleCafeClearButton.addEventListener('click', () => {
    clearVisibleCafeSearch();
    selectedCafeElement.innerHTML = '<p>지도 마커나 목록에서 카페를 선택하세요.</p>';
    setNationwideView();
});

initialize();
