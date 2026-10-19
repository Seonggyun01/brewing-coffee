<script setup lang="ts">
const {
  data: cafes,
  error,
  refresh,
} = await useAsyncData('cafe-map', () => useBackend().read<any[]>('maps/cafes'))
const mapEl = ref<HTMLElement>(),
  selected = ref<any>(),
  mapError = ref(''),
  nearby = ref<any[]>([]),
  nearbyMode = ref(false),
  searchError = ref(''),
  busy = ref(false),
  mapReady = ref(false)
let searchId = 0
let maps: any,
  map: any,
  markers: any[] = []
async function initialize() {
  try {
    maps = await useKakao()
    map = new maps.Map(mapEl.value, { center: new maps.LatLng(36.2, 127.8), level: 13 })
    const bounds = new maps.LatLngBounds()
    for (const c of cafes.value || []) {
      if (c.latitude == null || c.longitude == null) continue
      const point = new maps.LatLng(c.latitude, c.longitude),
        marker = new maps.Marker({ position: point, map })
      maps.event.addListener(marker, 'click', () => choose(c))
      bounds.extend(point)
      markers.push(marker)
    }
    if (markers.length) map.setBounds(bounds)
    mapReady.value = true
  } catch (e: any) {
    mapError.value = e.message
  }
}
function choose(c: any) {
  selected.value = c
  if (map && c.latitude != null) {
    map.setCenter(new maps.LatLng(c.latitude, c.longitude))
    map.setLevel(4)
  }
}
function all() {
  searchId++
  nearbyMode.value = false
  busy.value = false
  searchError.value = ''
  selected.value = null
  map?.setCenter(new maps.LatLng(36.2, 127.8))
  map?.setLevel(13)
}
function searchNearby() {
  if (!maps || !map) return
  const requestId = ++searchId
  nearbyMode.value = true
  selected.value = null
  nearby.value = []
  searchError.value = ''
  busy.value = true
  new maps.services.Places(map).categorySearch(
    'CE7',
    (data: any, status: any) => {
      if (requestId !== searchId) return
      if (status !== maps.services.Status.OK && status !== maps.services.Status.ZERO_RESULT)
        searchError.value = '주변 카페를 검색하지 못했어요. 다시 시도해주세요.'
      nearby.value = status === maps.services.Status.OK ? data : []
      busy.value = false
    },
    { useMapBounds: true },
  )
}
onMounted(initialize)
</script>
<template>
  <div class="page cafe-map-page">
    <div class="page-heading">
      <div>
        <span class="eyebrow">Cafe Map</span>
        <h1>카페 지도</h1>
        <p>방문했거나 기록한 카페를 지도에서 확인해보세요.</p>
      </div>
      <NuxtLink to="/cafe-filter-coffees/new" class="button">카페 필터 기록</NuxtLink>
    </div>
    <ApiState :error="error" @retry="refresh" />
    <div class="cafe-map-layout">
      <div class="cafe-map-wrapper">
        <div ref="mapEl" class="cafe-map" aria-label="카페 지도" />
        <p v-if="mapError" class="map-error" role="status">{{ mapError }}</p>
        <div class="map-controls">
          <button @click="all">전체 지도</button
          ><button :disabled="!mapReady || busy" @click="searchNearby">
            {{ busy ? '검색 중' : '주변 카페 찾기' }}
          </button>
        </div>
      </div>
      <aside class="cafe-sidebar">
        <h2>
          {{ nearbyMode ? '주변 카페' : selected?.cafeName || '방문 카페' }}
          <span class="cafe-count">{{ nearbyMode ? nearby.length : cafes?.length || 0 }}</span>
        </h2>
        <p v-if="selected && !nearbyMode">
          {{ selected.address }} · {{ selected.visitCount }}번의 방문
        </p>
        <NuxtLink
          v-if="selected && !nearbyMode"
          :to="`/cafe-filter-coffees?cafeId=${selected.id}`"
          class="button"
          >이 카페의 기록 →</NuxtLink
        ><template v-if="!nearbyMode"
          ><button v-for="c in cafes || []" :key="c.id" class="cafe-list-item" @click="choose(c)">
            <strong>{{ c.cafeName }}</strong
            ><span>{{ c.address }}</span
            ><small>{{ c.visitCount }}번의 방문</small>
          </button>
          <p v-if="!cafes?.length" class="hint">카페 경험을 기록하면 지도에 표시돼요.</p>
        </template>
        <template v-else>
          <p v-if="busy" class="hint" role="status">주변 카페를 찾고 있어요.</p>
          <p v-else-if="searchError" class="error-text" role="status">{{ searchError }}</p>
          <p v-else-if="!nearby.length" class="hint" role="status">
            현재 지도 영역에 검색된 카페가 없어요.
          </p>
          <button
            v-for="c in nearby"
            :key="c.id"
            class="cafe-list-item"
            @click="
              choose({
                cafeName: c.place_name,
                address: c.road_address_name || c.address_name,
                latitude: Number(c.y),
                longitude: Number(c.x),
                visitCount: 0,
              })
            "
          >
            <strong>{{ c.place_name }}</strong
            ><span>{{ c.road_address_name || c.address_name }}</span>
          </button>
        </template>
      </aside>
    </div>
  </div>
</template>
