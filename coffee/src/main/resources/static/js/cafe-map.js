const cafeListElement = document.querySelector('[data-cafe-list]');
const selectedCafeElement = document.querySelector('[data-selected-cafe]');
const cafeCountElement = document.querySelector('[data-cafe-count]');
const markerLayerElement = document.querySelector('[data-cafe-marker-layer]');
const koreaMapElement = document.querySelector('[data-korea-map]');
const mapBackButton = document.querySelector('[data-map-back]');
const pageElement = document.body;

const VIEW_BOX = {
    width: 560,
    height: 720,
    value: '0 0 560 720'
};

const KOREA_BOUNDS = {
    minLat: 33.0,
    maxLat: 38.7,
    minLng: 125.5,
    maxLng: 130.0
};

const KOREA_LONGITUDE_SCALE = 0.82;

const clamp = (value, min, max) => Math.min(Math.max(value, min), max);
const SVG_NS = 'http://www.w3.org/2000/svg';
const EMPTY_REGION_COLOR = '#ddd2c4';
const LIGHT_REGION_COLOR = { r: 219, g: 181, b: 122 };
const DARK_REGION_COLOR = { r: 92, g: 46, b: 31 };

let currentCafes = [];
let currentTopology = null;
let municipalityTopology = null;
let currentRegionName = null;

const REGION_LABELS = {
    Busan: '부산',
    'Chungcheongbuk-do': '충북',
    'Chungcheongnam-do': '충남',
    Daegu: '대구',
    Daejeon: '대전',
    'Gangwon-do': '강원',
    Gwangju: '광주',
    'Gyeonggi-do': '경기',
    'Gyeongsangbuk-do': '경북',
    'Gyeongsangnam-do': '경남',
    Incheon: '인천',
    Jeju: '제주',
    'Jeollabuk-do': '전북',
    'Jeollanam-do': '전남',
    Seoul: '서울',
    Ulsan: '울산'
};

const ADDRESS_REGION_RULES = [
    { keyword: '서울', region: 'Seoul' },
    { keyword: '부산', region: 'Busan' },
    { keyword: '대구', region: 'Daegu' },
    { keyword: '인천', region: 'Incheon' },
    { keyword: '광주', region: 'Gwangju' },
    { keyword: '대전', region: 'Daejeon' },
    { keyword: '울산', region: 'Ulsan' },
    { keyword: '세종', region: 'Chungcheongnam-do' },
    { keyword: '경기', region: 'Gyeonggi-do' },
    { keyword: '강원', region: 'Gangwon-do' },
    { keyword: '충북', region: 'Chungcheongbuk-do' },
    { keyword: '충청북', region: 'Chungcheongbuk-do' },
    { keyword: '충남', region: 'Chungcheongnam-do' },
    { keyword: '충청남', region: 'Chungcheongnam-do' },
    { keyword: '전북', region: 'Jeollabuk-do' },
    { keyword: '전라북', region: 'Jeollabuk-do' },
    { keyword: '전남', region: 'Jeollanam-do' },
    { keyword: '전라남', region: 'Jeollanam-do' },
    { keyword: '경북', region: 'Gyeongsangbuk-do' },
    { keyword: '경상북', region: 'Gyeongsangbuk-do' },
    { keyword: '경남', region: 'Gyeongsangnam-do' },
    { keyword: '경상남', region: 'Gyeongsangnam-do' },
    { keyword: '제주', region: 'Jeju' }
];

const ADDRESS_SUBREGION_RULES = [
    { keyword: '강남구', region: 'Seoul', subregion: 'Gangnam' },
    { keyword: '강동구', region: 'Seoul', subregion: 'Gandong' },
    { keyword: '강북구', region: 'Seoul', subregion: 'Gangbuk' },
    { keyword: '강서구', region: 'Seoul', subregion: 'Gangseo' },
    { keyword: '관악구', region: 'Seoul', subregion: 'Gwanak' },
    { keyword: '광진구', region: 'Seoul', subregion: 'Gwang-jin' },
    { keyword: '구로구', region: 'Seoul', subregion: 'Guro' },
    { keyword: '금천구', region: 'Seoul', subregion: 'Geum-cheon' },
    { keyword: '노원구', region: 'Seoul', subregion: 'Nowon' },
    { keyword: '도봉구', region: 'Seoul', subregion: 'Dobong' },
    { keyword: '동대문구', region: 'Seoul', subregion: 'Dong-daemun' },
    { keyword: '동작구', region: 'Seoul', subregion: 'Dongjak' },
    { keyword: '마포구', region: 'Seoul', subregion: 'Mapo' },
    { keyword: '서대문구', region: 'Seoul', subregion: 'Seodaemun' },
    { keyword: '서초구', region: 'Seoul', subregion: 'Seocho' },
    { keyword: '성동구', region: 'Seoul', subregion: 'Seongdong' },
    { keyword: '성북구', region: 'Seoul', subregion: 'Seongbuk' },
    { keyword: '송파구', region: 'Seoul', subregion: 'Songpa' },
    { keyword: '양천구', region: 'Seoul', subregion: 'Yangcheon' },
    { keyword: '영등포구', region: 'Seoul', subregion: 'Yeongdeungpo' },
    { keyword: '용산구', region: 'Seoul', subregion: 'Yongsan' },
    { keyword: '은평구', region: 'Seoul', subregion: 'Eun-pyeong' },
    { keyword: '종로구', region: 'Seoul', subregion: 'Jongro' },
    { keyword: '중구', region: 'Seoul', subregion: 'Jung' },
    { keyword: '중랑구', region: 'Seoul', subregion: 'Jungnang' }
];

const coordinateRegionRules = [
    { region: 'Seoul', test: ({ latitude, longitude }) => latitude >= 37.41 && latitude <= 37.7 && longitude >= 126.76 && longitude <= 127.2 },
    { region: 'Incheon', test: ({ latitude, longitude }) => latitude >= 37.25 && latitude <= 37.65 && longitude >= 126.35 && longitude <= 126.86 },
    { region: 'Gyeonggi-do', test: ({ latitude, longitude }) => latitude >= 36.85 && latitude <= 38.35 && longitude >= 126.55 && longitude <= 127.85 },
    { region: 'Jeju', test: ({ latitude, longitude }) => latitude >= 33.1 && latitude <= 33.65 && longitude >= 126.05 && longitude <= 126.95 },
    { region: 'Busan', test: ({ latitude, longitude }) => latitude >= 35.0 && latitude <= 35.4 && longitude >= 128.75 && longitude <= 129.35 },
    { region: 'Daegu', test: ({ latitude, longitude }) => latitude >= 35.75 && latitude <= 36.02 && longitude >= 128.45 && longitude <= 128.8 },
    { region: 'Daejeon', test: ({ latitude, longitude }) => latitude >= 36.2 && latitude <= 36.5 && longitude >= 127.25 && longitude <= 127.55 },
    { region: 'Gwangju', test: ({ latitude, longitude }) => latitude >= 35.05 && latitude <= 35.28 && longitude >= 126.75 && longitude <= 127.02 },
    { region: 'Ulsan', test: ({ latitude, longitude }) => latitude >= 35.35 && latitude <= 35.75 && longitude >= 129.0 && longitude <= 129.5 },
    { region: 'Gangwon-do', test: ({ latitude, longitude }) => latitude >= 37.0 && longitude >= 127.5 },
    { region: 'Chungcheongbuk-do', test: ({ latitude, longitude }) => latitude >= 36.45 && latitude < 37.25 && longitude >= 127.45 && longitude < 128.35 },
    { region: 'Chungcheongnam-do', test: ({ latitude, longitude }) => latitude >= 35.95 && latitude < 37.1 && longitude >= 126.45 && longitude < 127.45 },
    { region: 'Jeollabuk-do', test: ({ latitude, longitude }) => latitude >= 35.45 && latitude < 36.25 && longitude >= 126.45 && longitude < 127.85 },
    { region: 'Jeollanam-do', test: ({ latitude }) => latitude < 35.45 },
    { region: 'Gyeongsangbuk-do', test: ({ latitude, longitude }) => latitude >= 35.75 && latitude < 37.25 && longitude >= 128.0 },
    { region: 'Gyeongsangnam-do', test: ({ latitude, longitude }) => latitude < 35.75 && longitude >= 127.7 }
];

const projectCoordinate = ([longitude, latitude]) => {
    const rawX = ((longitude - KOREA_BOUNDS.minLng) / (KOREA_BOUNDS.maxLng - KOREA_BOUNDS.minLng)) * VIEW_BOX.width;
    const x = VIEW_BOX.width / 2 + (rawX - VIEW_BOX.width / 2) * KOREA_LONGITUDE_SCALE;
    const y = ((KOREA_BOUNDS.maxLat - latitude) / (KOREA_BOUNDS.maxLat - KOREA_BOUNDS.minLat)) * VIEW_BOX.height;
    return [x, y];
};

const resolveRegionName = (cafe) => {
    const address = cafe.address || '';
    const addressRule = ADDRESS_REGION_RULES.find((rule) => address.includes(rule.keyword));

    if (addressRule) {
        return addressRule.region;
    }

    const coordinateRule = coordinateRegionRules.find((rule) => rule.test(cafe));
    return coordinateRule ? coordinateRule.region : null;
};

const resolveSubregionName = (cafe, regionName) => {
    const address = cafe.address || '';
    const addressRule = ADDRESS_SUBREGION_RULES.find((rule) => {
        return rule.region === regionName && address.includes(rule.keyword);
    });

    return addressRule ? addressRule.subregion : null;
};

const cafesInRegion = (regionName) => {
    return currentCafes.filter((cafe) => resolveRegionName(cafe) === regionName);
};

const buildRegionCounts = (cafes) => {
    return cafes.reduce((counts, cafe) => {
        const regionName = resolveRegionName(cafe);

        if (!regionName) {
            return counts;
        }

        counts.set(regionName, (counts.get(regionName) || 0) + 1);
        return counts;
    }, new Map());
};

const interpolateColor = (from, to, progress) => {
    const value = (key) => Math.round(from[key] + (to[key] - from[key]) * progress);
    return `rgb(${value('r')}, ${value('g')}, ${value('b')})`;
};

const colorForCount = (count, maxCount) => {
    if (count === 0) {
        return EMPTY_REGION_COLOR;
    }

    const progress = maxCount <= 1 ? 0.58 : 0.22 + (count / maxCount) * 0.78;
    return interpolateColor(LIGHT_REGION_COLOR, DARK_REGION_COLOR, clamp(progress, 0, 1));
};

const decodeTopoArc = (topology, arcRef) => {
    const arcIndex = arcRef < 0 ? ~arcRef : arcRef;
    const arc = topology.arcs[arcIndex];
    const points = [];
    let x = 0;
    let y = 0;

    arc.forEach(([deltaX, deltaY]) => {
        x += deltaX;
        y += deltaY;
        points.push([
            x * topology.transform.scale[0] + topology.transform.translate[0],
            y * topology.transform.scale[1] + topology.transform.translate[1]
        ]);
    });

    return arcRef < 0 ? points.reverse() : points;
};

const geometryPolygons = (geometry) => {
    return geometry.type === 'Polygon' ? [geometry.arcs] : geometry.arcs;
};

const ringCoordinates = (topology, ring) => {
    return ring.flatMap((arcRef, arcIndex) => {
        const points = decodeTopoArc(topology, arcRef);
        return arcIndex === 0 ? points : points.slice(1);
    });
};

const collectProjectedCoordinates = (topology, geometry) => {
    return geometryPolygons(geometry).flatMap((polygon) => {
        return polygon.flatMap((ring) => ringCoordinates(topology, ring).map(projectCoordinate));
    });
};

const collectGeometryCoordinates = (topology, geometry) => {
    return geometryPolygons(geometry).map((polygon) => {
        return polygon.map((ring) => ringCoordinates(topology, ring));
    });
};

const boundsForGeometry = (topology, geometry) => {
    const points = collectProjectedCoordinates(topology, geometry);
    const xs = points.map(([x]) => x);
    const ys = points.map(([, y]) => y);

    return {
        minX: Math.min(...xs),
        maxX: Math.max(...xs),
        minY: Math.min(...ys),
        maxY: Math.max(...ys)
    };
};

const boundsForGeometries = (topology, geometries) => {
    const points = geometries.flatMap((geometry) => collectProjectedCoordinates(topology, geometry));
    const xs = points.map(([x]) => x);
    const ys = points.map(([, y]) => y);

    return {
        minX: Math.min(...xs),
        maxX: Math.max(...xs),
        minY: Math.min(...ys),
        maxY: Math.max(...ys)
    };
};

const centerForGeometry = (topology, geometry) => {
    const bounds = boundsForGeometry(topology, geometry);
    return {
        x: (bounds.minX + bounds.maxX) / 2,
        y: (bounds.minY + bounds.maxY) / 2
    };
};

const ringToPath = (topology, ring) => {
    const coordinates = ringCoordinates(topology, ring);

    if (coordinates.length === 0) {
        return '';
    }

    return coordinates.map((coordinate, index) => {
        const [x, y] = projectCoordinate(coordinate);
        return `${index === 0 ? 'M' : 'L'}${x.toFixed(2)},${y.toFixed(2)}`;
    }).join(' ') + ' Z';
};

const geometryToPath = (topology, geometry) => {
    return geometryPolygons(geometry).map((polygon) => {
        return polygon.map((ring) => ringToPath(topology, ring)).join(' ');
    }).join(' ');
};

const regionCollection = () => currentTopology.objects['skorea-provinces-geo'];
const municipalityCollection = () => municipalityTopology.objects['skorea-municipalities-geo'];

const findRegionGeometry = (regionName) => {
    return regionCollection().geometries.find((geometry) => geometry.properties.NAME_1 === regionName);
};

const findSubregionGeometries = (regionName) => {
    if (!municipalityTopology) {
        return [];
    }

    return municipalityCollection().geometries.filter((geometry) => geometry.properties.NAME_1 === regionName);
};

const pointInRing = ([longitude, latitude], ring) => {
    let isInside = false;

    for (let index = 0, previousIndex = ring.length - 1; index < ring.length; previousIndex = index++) {
        const [currentLongitude, currentLatitude] = ring[index];
        const [previousLongitude, previousLatitude] = ring[previousIndex];
        const intersects = (currentLatitude > latitude) !== (previousLatitude > latitude)
            && longitude < ((previousLongitude - currentLongitude) * (latitude - currentLatitude)) / (previousLatitude - currentLatitude) + currentLongitude;

        if (intersects) {
            isInside = !isInside;
        }
    }

    return isInside;
};

const pointInGeometry = (topology, geometry, coordinate) => {
    return collectGeometryCoordinates(topology, geometry).some((polygon) => {
        const [outerRing, ...innerRings] = polygon;
        const isInsideOuterRing = pointInRing(coordinate, outerRing);
        const isInsideInnerRing = innerRings.some((ring) => pointInRing(coordinate, ring));

        return isInsideOuterRing && !isInsideInnerRing;
    });
};

const findSubregionForCafe = (subregions, cafe) => {
    const regionName = resolveRegionName(cafe);
    const addressSubregionName = resolveSubregionName(cafe, regionName);
    const addressSubregion = subregions.find((geometry) => geometry.properties.NAME_2 === addressSubregionName);

    if (addressSubregion) {
        return addressSubregion;
    }

    return subregions.find((geometry) => {
        return pointInGeometry(municipalityTopology, geometry, [cafe.longitude, cafe.latitude]);
    });
};

const buildSubregionCounts = (subregions, cafes) => {
    return cafes.reduce((counts, cafe) => {
        const subregion = findSubregionForCafe(subregions, cafe);

        if (!subregion) {
            return counts;
        }

        const subregionName = subregion.properties.NAME_2;
        counts.set(subregionName, (counts.get(subregionName) || 0) + 1);
        return counts;
    }, new Map());
};

const createRegionPath = (geometry, count, maxCount, options = {}) => {
    const {
        topology = currentTopology,
        isDetail = false,
        isSubregion = false,
        onClick = null
    } = options;
    const regionName = geometry.properties.NAME_1;
    const subregionName = geometry.properties.NAME_2;
    const displayName = subregionName || regionName;
    const path = document.createElementNS(SVG_NS, 'path');

    path.setAttribute('class', `cafe-map__geo-path${isDetail ? ' cafe-map__geo-path--detail' : ''}${isSubregion ? ' cafe-map__geo-path--subregion' : ''}`);
    path.setAttribute('d', geometryToPath(topology, geometry));
    path.setAttribute('fill', colorForCount(count, maxCount));
    path.setAttribute('data-region', displayName);
    path.setAttribute('data-count', String(count));

    const title = document.createElementNS(SVG_NS, 'title');
    title.textContent = `${REGION_LABELS[displayName] || displayName}: 방문 카페 ${count}개`;
    path.appendChild(title);

    if (onClick) {
        path.addEventListener('click', onClick);
    }

    return path;
};

const renderSelectedRegion = (regionName, cafes) => {
    const label = REGION_LABELS[regionName] || regionName;

    selectedCafeElement.innerHTML = `
        <span>Selected Region</span>
        <strong>${label}</strong>
        <p>이 지역에 기록된 방문 카페 ${cafes.length}개</p>
        <small>지역 지도를 클릭해 카페 위치를 확인하세요.</small>
    `;
};

const renderSelectedCafe = (cafe) => {
    selectedCafeElement.innerHTML = `
        <span>Selected Cafe</span>
        <strong>${cafe.cafeName}</strong>
        <p>${cafe.address || '주소 미기록'}</p>
        <small>방문 ${cafe.visitCount}회</small>
    `;
};

const setActiveCafe = (cafe) => {
    renderSelectedCafe(cafe);

    document.querySelectorAll('[data-cafe-id]').forEach((element) => {
        element.classList.toggle('is-active', String(cafe.id) === element.dataset.cafeId);
    });
};

const markerPositionForCafe = (cafe, subregions) => {
    const coordinate = [cafe.longitude, cafe.latitude];
    const regionName = resolveRegionName(cafe);
    const addressSubregionName = resolveSubregionName(cafe, regionName);
    const addressSubregion = subregions.find((geometry) => geometry.properties.NAME_2 === addressSubregionName);

    if (addressSubregion && !pointInGeometry(municipalityTopology, addressSubregion, coordinate)) {
        return centerForGeometry(municipalityTopology, addressSubregion);
    }

    const [x, y] = projectCoordinate(coordinate);
    return { x, y };
};

const renderCafeMarker = (cafe, viewBoxSize, subregions = []) => {
    const { x, y } = markerPositionForCafe(cafe, subregions);
    const scale = clamp(viewBoxSize * 0.000034, 0.0022, 0.0052);
    const marker = document.createElementNS(SVG_NS, 'g');
    const pin = document.createElementNS(SVG_NS, 'path');
    const title = document.createElementNS(SVG_NS, 'title');

    marker.setAttribute('class', 'cafe-detail-marker');
    marker.setAttribute('data-cafe-id', String(cafe.id));
    marker.setAttribute('transform', `translate(${x.toFixed(2)} ${y.toFixed(2)}) scale(${scale.toFixed(4)})`);

    pin.setAttribute('d', 'M0,-240 C82,-240 148,-174 148,-92 C148,16 0,198 0,198 C0,198 -148,16 -148,-92 C-148,-174 -82,-240 0,-240 Z M0,-146 C-34,-146 -62,-118 -62,-84 C-62,-50 -34,-22 0,-22 C34,-22 62,-50 62,-84 C62,-118 34,-146 0,-146 Z');
    title.textContent = cafe.cafeName;

    marker.appendChild(pin);
    marker.appendChild(title);
    marker.addEventListener('click', () => setActiveCafe(cafe));

    return marker;
};

const renderOverviewMap = () => {
    const regionCounts = buildRegionCounts(currentCafes);
    const maxCount = Math.max(1, ...regionCounts.values());

    currentRegionName = null;
    pageElement.classList.remove('has-region-detail');
    koreaMapElement.setAttribute('viewBox', VIEW_BOX.value);
    koreaMapElement.innerHTML = '';
    markerLayerElement.innerHTML = '';
    mapBackButton.hidden = true;

    regionCollection().geometries.forEach((geometry) => {
        const regionName = geometry.properties.NAME_1;
        const count = regionCounts.get(regionName) || 0;
        koreaMapElement.appendChild(createRegionPath(geometry, count, maxCount, {
            onClick: () => selectRegion(regionName)
        }));
    });

    cafeCountElement.textContent = currentCafes.length;
};

const renderDetailMap = (regionName) => {
    const fallbackGeometry = findRegionGeometry(regionName);
    const subregions = findSubregionGeometries(regionName);
    const detailGeometries = subregions.length > 0 ? subregions : [fallbackGeometry];
    const detailTopology = subregions.length > 0 ? municipalityTopology : currentTopology;
    const regionCafes = cafesInRegion(regionName);
    const subregionCounts = buildSubregionCounts(subregions, regionCafes);
    const maxCount = Math.max(1, ...subregionCounts.values(), regionCafes.length);
    const bounds = boundsForGeometries(detailTopology, detailGeometries);
    const padding = Math.max(bounds.maxX - bounds.minX, bounds.maxY - bounds.minY) * 0.18;
    const minX = Math.max(0, bounds.minX - padding);
    const minY = Math.max(0, bounds.minY - padding);
    const width = Math.min(VIEW_BOX.width - minX, bounds.maxX - bounds.minX + padding * 2);
    const height = Math.min(VIEW_BOX.height - minY, bounds.maxY - bounds.minY + padding * 2);

    currentRegionName = regionName;
    pageElement.classList.add('has-region-detail');
    koreaMapElement.setAttribute('viewBox', `${minX} ${minY} ${width} ${height}`);
    koreaMapElement.innerHTML = '';
    markerLayerElement.innerHTML = '';
    mapBackButton.hidden = false;

    detailGeometries.forEach((geometry) => {
        const subregionName = geometry.properties.NAME_2;
        const count = subregionName ? (subregionCounts.get(subregionName) || 0) : regionCafes.length;

        koreaMapElement.appendChild(createRegionPath(geometry, count, maxCount, {
            topology: detailTopology,
            isDetail: true,
            isSubregion: Boolean(subregionName)
        }));
    });

    regionCafes.forEach((cafe) => koreaMapElement.appendChild(renderCafeMarker(cafe, Math.max(width, height), subregions)));

    renderSelectedRegion(regionName, regionCafes);
    renderCafeList(regionCafes);
};

const selectRegion = (regionName) => {
    renderDetailMap(regionName);
};

const renderCafeList = (cafes) => {
    cafeCountElement.textContent = currentRegionName ? cafes.length : currentCafes.length;

    if (cafes.length === 0) {
        cafeListElement.innerHTML = '<p>표시할 카페가 없습니다.</p>';
        return;
    }

    cafeListElement.innerHTML = cafes.map((cafe) => `
        <button class="cafe-list-card" type="button" data-cafe-id="${cafe.id}">
            <strong>${cafe.cafeName}</strong>
            <span>${cafe.address || '주소 미기록'}</span>
            <small>방문 ${cafe.visitCount}회</small>
        </button>
    `).join('');

    cafeListElement.querySelectorAll('[data-cafe-id]').forEach((card) => {
        card.addEventListener('click', () => {
            const cafe = currentCafes.find((item) => String(item.id) === card.dataset.cafeId);
            if (cafe) {
                setActiveCafe(cafe);
            }
        });
    });
};

mapBackButton.addEventListener('click', () => {
    renderOverviewMap();
    selectedCafeElement.innerHTML = '<p>지도나 목록에서 카페를 선택하세요.</p>';
});

Promise.all([
    fetch('/api/maps/cafes').then((response) => response.json()),
    fetch('/data/skorea-provinces-topo.json').then((response) => response.json()),
    fetch('/data/skorea-municipalities-topo.json').then((response) => response.json())
])
    .then(([cafes, topology, municipalityData]) => {
        currentCafes = cafes;
        currentTopology = topology;
        municipalityTopology = municipalityData;
        renderOverviewMap();
    })
    .catch(() => {
        cafeListElement.innerHTML = '<p>카페 데이터를 불러오지 못했습니다.</p>';
        koreaMapElement.innerHTML = '<text x="280" y="360" text-anchor="middle">지도를 불러오지 못했습니다.</text>';
    });
