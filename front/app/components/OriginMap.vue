<script setup lang="ts">
import world from '~/utils/world.json'
const {
  data: summary,
  error,
  refresh,
} = await useAsyncData('origin-summary', () => useBackend().read('origin-map/countries'))
const selected = ref<any>(null),
  zoom = ref(1),
  pan = reactive({ x: 0, y: 0 }),
  svg = ref<SVGSVGElement>()
let drag: { x: number; y: number; px: number; py: number } | null = null,
  moved = false
const countryData = computed(
  () => new Map((summary.value?.countries || []).map((c: any) => [c.countryCode, c])),
)
function choose(code: string) {
  if (!moved) selected.value = countryData.value.get(code) || null
}
function start(e: PointerEvent) {
  if (zoom.value <= 1) return
  moved = false
  drag = { x: e.clientX, y: e.clientY, px: pan.x, py: pan.y }
  svg.value?.setPointerCapture(e.pointerId)
}
function move(e: PointerEvent) {
  if (!drag || !svg.value) return
  const scale = 1200 / svg.value.getBoundingClientRect().width
  moved = Math.abs(e.clientX - drag.x) + Math.abs(e.clientY - drag.y) > 4
  pan.x = Math.max(-500, Math.min(500, drag.px + (e.clientX - drag.x) * scale))
  pan.y = Math.max(-250, Math.min(250, drag.py + (e.clientY - drag.y) * scale))
}
function nextSection() {
  document.getElementById('home-demo')?.scrollIntoView({
    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth',
  })
}
function reset() {
  zoom.value = 1
  pan.x = 0
  pan.y = 0
}
</script>
<template>
  <section class="origin-section">
    <div class="page map-heading">
      <span class="eyebrow">MY COFFEE JOURNEY</span>
      <h1>경험한 산지</h1>
      <p>한 잔씩 만난 커피의 세계.</p>
    </div>
    <div class="world-map-container">
      <svg
        ref="svg"
        viewBox="0 0 1200 620"
        class="world-map"
        :data-zoomed="zoom > 1"
        aria-label="원두 산지 세계지도"
        @pointerdown="start"
        @pointermove="move"
        @pointerup="drag = null"
        @pointercancel="drag = null"
      >
        <g
          :transform="`translate(${pan.x} ${pan.y}) translate(600 310) scale(${zoom}) translate(-600 -310)`"
        >
          <path
            v-for="country in world"
            :key="country.code"
            :d="country.d"
            :fill="(countryData.get(country.code) as any)?.fillColor || '#e7e2d6'"
            :class="{ experienced: countryData.has(country.code) }"
            stroke="#f7f3ee"
            stroke-width=".7"
            :tabindex="countryData.has(country.code) ? 0 : undefined"
            :role="countryData.has(country.code) ? 'button' : undefined"
            :aria-label="country.name"
            @click="choose(country.code)"
            @keydown.enter="choose(country.code)"
          >
            <title>{{ country.name }}</title>
          </path>
        </g>
      </svg>
      <div class="map-controls">
        <button
          aria-label="지도 축소"
          :disabled="zoom <= 1"
          @click="zoom = Math.max(1, zoom - 0.25)"
        >
          −
        </button>
        <span>{{ Math.round(zoom * 100) }}%</span>
        <button
          aria-label="지도 확대"
          :disabled="zoom >= 4"
          @click="zoom = Math.min(4, zoom + 0.25)"
        >
          +
        </button>
        <button @click="reset">초기화</button>
      </div>
      <div v-if="selected" class="country-panel">
        <button class="close-button" aria-label="산지 정보 닫기" @click="selected = null">×</button
        ><span class="eyebrow">ORIGIN</span>
        <h2>{{ selected.koreanCountryName }}</h2>
        <p>원두 {{ selected.beanCount }}개 · 브루잉 {{ selected.brewRecordCount }}회</p>
        <NuxtLink
          v-for="bean in selected.recentBeans"
          :key="bean.id"
          :to="`/coffee-beans/${bean.id}`"
          class="country-bean"
          >{{ bean.name }} ↗</NuxtLink
        ><NuxtLink :to="`/coffee-beans?countryCode=${selected.countryCode}`" class="button"
          >이 산지의 원두 보기 →</NuxtLink
        >
      </div>
    </div>
    <div class="map-stats">
      <div>
        <span>Countries</span><strong>{{ summary?.experiencedCountryCount ?? '—' }}</strong
        ><small>경험한 산지</small>
      </div>
      <div>
        <span>Beans</span><strong>{{ summary?.totalBeanCount ?? '—' }}</strong
        ><small>기록한 원두</small>
      </div>
      <div>
        <span>Brews</span><strong>{{ summary?.totalBrewRecordCount ?? '—' }}</strong
        ><small>브루잉 순간</small>
      </div>
    </div>
    <button class="section-next" aria-label="사진 등록 설명으로 이동" @click="nextSection">
      ↓
    </button>
    <button v-if="error" class="map-retry link-button" @click="refresh()">
      내 기록 다시 불러오기
    </button>
  </section>
</template>
