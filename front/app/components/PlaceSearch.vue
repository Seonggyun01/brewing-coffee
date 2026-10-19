<script setup lang="ts">
import { safeLink } from '~/utils/forms'
const props = defineProps<{ model: Record<string, any>; cafe?: boolean }>()
const query = ref(''),
  results = ref<any[]>([]),
  message = ref(''),
  busy = ref(false),
  open = ref(false),
  draft = ref<any>(null)
const mapEl = ref<HTMLElement>(),
  dialogMapEl = ref<HTMLElement>(),
  searchInput = ref<HTMLInputElement>(),
  dialog = ref<HTMLElement>(),
  section = ref<HTMLElement>()
let maps: any,
  mainMap: any,
  mainMarker: any,
  draftMap: any,
  draftMarker: any,
  resize: ResizeObserver | undefined
let opener: HTMLElement | null = null
useHead({ bodyAttrs: { style: computed(() => (open.value ? 'overflow: hidden' : undefined)) } })
const prefix = computed(() => (props.cafe ? 'cafe' : 'purchasePlace'))
const selected = computed(() => props.model[`${prefix.value}Name`])
async function search() {
  if (!query.value.trim()) return
  busy.value = true
  message.value = ''
  try {
    maps = await useKakao()
    new maps.services.Places().keywordSearch(
      query.value,
      (data: any, status: any) => {
        busy.value = false
        results.value = status === maps.services.Status.OK ? data : []
        if (!results.value.length)
          message.value = '검색 결과가 없습니다. 다른 이름으로 검색해보세요.'
      },
      { category_group_code: 'CE7' },
    )
  } catch (e: any) {
    message.value = e.message
    busy.value = false
  }
}
function zoomMap(delta: number, isDraft = false) {
  const target = isDraft ? draftMap : mainMap
  if (target?.getLevel) target.setLevel(Math.max(1, Math.min(12, target.getLevel() + delta)))
}
async function renderMap(isDraft = false) {
  const host = isDraft ? dialogMapEl.value : mapEl.value
  const lat = isDraft ? draft.value?.y : props.model[`${prefix.value}Latitude`],
    lng = isDraft ? draft.value?.x : props.model[`${prefix.value}Longitude`]
  if (!host || lat == null || lng == null || host.clientWidth === 0) return
  try {
    maps = await useKakao()
    const center = new maps.LatLng(Number(lat), Number(lng))
    if (isDraft) {
      if (!draftMap) draftMap = new maps.Map(host, { center, level: 3 })
      draftMarker?.setMap(null)
      draftMarker = new maps.Marker({ position: center, map: draftMap })
      draftMap.relayout()
      draftMap.setCenter(center)
    } else {
      if (!mainMap) mainMap = new maps.Map(host, { center, level: 3 })
      mainMarker?.setMap(null)
      mainMarker = new maps.Marker({ position: center, map: mainMap })
      mainMap.relayout()
      mainMap.setCenter(center)
    }
  } catch (e: any) {
    message.value = e.message
  }
}
async function showSearch(event?: MouseEvent) {
  opener = event?.currentTarget as HTMLElement | null
  query.value = selected.value || ''
  results.value = []
  message.value = ''
  draft.value = selected.value
    ? {
        place_name: selected.value,
        road_address_name: props.model.purchasePlaceAddress,
        place_url: props.model.purchasePlaceUrl,
        y: props.model.purchasePlaceLatitude,
        x: props.model.purchasePlaceLongitude,
      }
    : null
  open.value = true
  await nextTick()
  searchInput.value?.focus()
  await renderMap(true)
}
function closeSearch() {
  open.value = false
  draftMarker?.setMap(null)
  draftMarker = null
  draftMap = null
  nextTick(() => {
    if (opener?.isConnected) opener.focus()
    else
      section.value
        ?.querySelector<HTMLButtonElement>('.place-selection-actions button, .place-open-search')
        ?.focus()
  })
}
async function select(place: any) {
  if (!props.cafe) {
    draft.value = place
    await nextTick()
    await renderMap(true)
    return
  }
  applyPlace(place)
  results.value = []
  await nextTick()
  await observeMap()
}
function applyPlace(place: any) {
  const p = prefix.value
  props.model[`${p}Name`] = place.place_name
  props.model[`${p}Address`] = place.road_address_name || place.address_name
  props.model[`${p}Url`] = place.place_url
  props.model[`${p}Latitude`] = Number(place.y)
  props.model[`${p}Longitude`] = Number(place.x)
  if (!props.cafe) {
    props.model.purchasePlaceId = null
    props.model.purchasePlaceType = 'CAFE'
  }
}
async function apply() {
  if (!draft.value) return
  applyPlace(draft.value)
  closeSearch()
  await nextTick()
  await observeMap()
}
function clear() {
  for (const k of ['Name', 'Address', 'Url', 'Latitude', 'Longitude'])
    props.model[`${prefix.value}${k}`] = ['Latitude', 'Longitude'].includes(k) ? null : ''
  if (!props.cafe) props.model.purchasePlaceId = null
  mainMarker?.setMap(null)
  mainMap = null
  mainMarker = null
  resize?.disconnect()
}
async function observeMap() {
  await nextTick()
  resize?.disconnect()
  if (mapEl.value) {
    resize = new ResizeObserver(() => {
      if (mapEl.value?.clientWidth) renderMap()
    })
    resize.observe(mapEl.value)
  }
  await renderMap()
}
function modalKey(event: KeyboardEvent) {
  if (event.key === 'Escape') closeSearch()
  if (event.key !== 'Tab' || !dialog.value) return
  const nodes = Array.from(
    dialog.value.querySelectorAll<HTMLElement>('input,button:not(:disabled),a[href]'),
  )
  const first = nodes[0],
    last = nodes.at(-1)
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last?.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first?.focus()
  }
}
onMounted(observeMap)
watch(
  () => [props.model[`${prefix.value}Latitude`], props.model[`${prefix.value}Longitude`]],
  observeMap,
)
onBeforeUnmount(() => {
  resize?.disconnect()
  mainMarker?.setMap(null)
  draftMarker?.setMap(null)
})
</script>
<template>
  <section ref="section" class="place-search">
    <h3 v-if="!cafe">구매처 · 선택</h3>
    <p class="hint">
      {{
        cafe
          ? '카페 이름으로 검색하고 지도에서 위치를 확인해주세요.'
          : '카카오맵에서 검색한 장소의 이름과 주소, 좌표를 자동으로 저장합니다.'
      }}
    </p>
    <template v-if="cafe"
      ><label for="cafe-search">카페 검색 <span class="optional">선택</span></label>
      <div class="search-row">
        <input
          id="cafe-search"
          v-model="query"
          placeholder="카페 이름으로 검색"
          @keydown.enter.prevent="search"
        /><button type="button" class="button secondary" :disabled="busy" @click="search">
          {{ busy ? '검색 중' : '검색' }}
        </button>
      </div>
      <ul v-if="results.length" class="place-results">
        <li v-for="place in results" :key="place.id">
          <button type="button" @click="select(place)">
            <strong>{{ place.place_name }}</strong
            ><span>{{ place.road_address_name || place.address_name }}</span
            ><small>이 장소 선택 →</small>
          </button>
        </li>
      </ul></template
    >
    <button
      v-else-if="!selected"
      type="button"
      class="button secondary place-open-search"
      @click="showSearch"
    >
      구매처 검색
    </button>
    <p v-if="message && !open" role="status" class="hint">{{ message }}</p>
    <div v-if="selected" class="selected-place">
      <div>
        <small class="selected-place-label">✓ 선택한 {{ cafe ? '카페' : '구매처' }}</small
        ><strong>{{ selected }}</strong>
        <p>{{ model[`${prefix}Address`] }}</p>
        <a
          v-if="safeLink(model[`${prefix}Url`])"
          :href="safeLink(model[`${prefix}Url`])"
          target="_blank"
          rel="noopener noreferrer"
          >카카오맵에서 보기 ↗</a
        >
      </div>
      <div class="place-selection-actions">
        <button v-if="!cafe" type="button" class="button secondary" @click="showSearch">
          다른 구매처 검색</button
        ><button type="button" class="link-button" @click="clear">선택 해제</button>
      </div>
    </div>
    <div v-if="selected" class="place-map-wrapper">
      <div ref="mapEl" class="place-map selected-place-map" aria-label="선택한 장소 지도" />
      <div class="place-zoom-controls">
        <button type="button" aria-label="장소 지도 확대" @click="zoomMap(-1)">+</button
        ><button type="button" aria-label="장소 지도 축소" @click="zoomMap(1)">−</button>
      </div>
    </div>
    <p v-else class="place-empty hint">
      {{
        cafe
          ? '검색한 장소를 선택하면 지도에서 확인할 수 있어요.'
          : '구매처 없이도 원두를 저장할 수 있어요.'
      }}
    </p>
    <div v-if="open" class="place-dialog-overlay" @click.self="closeSearch">
      <section
        ref="dialog"
        class="place-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="place-dialog-title"
        @keydown="modalKey"
      >
        <h2 id="place-dialog-title">구매처 검색</h2>
        <p>원두를 구매한 장소를 검색하고 지도에서 확인해주세요.</p>
        <label class="visually-hidden" for="purchase-search">구매처 검색</label>
        <div class="search-row">
          <input
            id="purchase-search"
            ref="searchInput"
            v-model="query"
            placeholder="장소 이름으로 검색"
            @keydown.enter.prevent="search"
          /><button type="button" class="button" :disabled="busy" @click="search">
            {{ busy ? '검색 중' : '검색' }}
          </button>
        </div>
        <p v-if="message" class="hint" role="status">{{ message }}</p>
        <h3>검색 결과</h3>
        <ul class="place-results">
          <li v-for="place in results" :key="place.id" :class="{ chosen: draft?.id === place.id }">
            <div>
              <strong>{{ place.place_name }}</strong
              ><span>{{ place.road_address_name || place.address_name }}</span
              ><a :href="safeLink(place.place_url)" target="_blank" rel="noopener noreferrer"
                >카카오맵에서 보기 ↗</a
              >
            </div>
            <button
              type="button"
              :class="['button', draft?.id === place.id ? '' : 'secondary']"
              :aria-label="`${place.place_name} 선택`"
              :aria-pressed="draft?.id === place.id"
              @click="select(place)"
            >
              {{ draft?.id === place.id ? '✓ 선택됨' : '선택' }}
            </button>
          </li>
        </ul>
        <p v-if="!results.length" class="hint">장소 이름을 입력하고 검색해주세요.</p>
        <div v-if="draft" class="place-draft">
          <strong>{{ draft.place_name }}</strong>
          <div class="place-map-wrapper">
            <div ref="dialogMapEl" class="place-map" aria-label="선택 후보 장소 지도" />
            <div class="place-zoom-controls">
              <button type="button" aria-label="선택 후보 지도 확대" @click="zoomMap(-1, true)">
                +</button
              ><button type="button" aria-label="선택 후보 지도 축소" @click="zoomMap(1, true)">
                −
              </button>
            </div>
          </div>
        </div>
        <p class="hint">지도에서 위치를 확인한 뒤 장소를 적용해주세요.</p>
        <div class="place-dialog-actions">
          <button type="button" class="button secondary" @click="closeSearch">닫기</button
          ><button type="button" class="button" :disabled="!draft" @click="apply">
            이 장소 적용
          </button>
        </div>
      </section>
    </div>
  </section>
</template>
