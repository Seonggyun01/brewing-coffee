<script setup lang="ts">
interface Usage {
  site: string
  model: string
  current: number
  max: number
  unit: string
  period: string
  color: string
}
interface Snapshot {
  items: Usage[]
  scope: string
  delayed: boolean
}
const expanded = ref(false)
const data = ref<Snapshot | null>(null)
const failed = ref(false)
const panel = ref<HTMLElement>()
const position = ref<{ x: number; y: number } | null>(null)
let timer: ReturnType<typeof setInterval> | undefined
let drag: { id: number; x: number; y: number; left: number; top: number } | null = null
let loading = false
const key = 'blank-api-usage-position'
const style = computed(() =>
  position.value
    ? { left: `${position.value.x}px`, top: `${position.value.y}px`, bottom: 'auto' }
    : {},
)
const fmt = (n: number) => new Intl.NumberFormat('ko-KR').format(n)
const modelName = (id: string) =>
  id
    .split('/')
    .pop()
    ?.replace(/:free$/, '') || id
function clamp() {
  if (!position.value || !panel.value) return
  position.value = {
    x: Math.max(16, Math.min(position.value.x, window.innerWidth - panel.value.offsetWidth - 16)),
    y: Math.max(16, Math.min(position.value.y, window.innerHeight - panel.value.offsetHeight - 16)),
  }
}
function save() {
  try {
    localStorage.setItem(key, JSON.stringify(position.value))
  } catch {}
}
function start(e: PointerEvent) {
  if (e.button !== 0 || (e.target as HTMLElement).closest('button')) return
  const b = panel.value!.getBoundingClientRect()
  position.value = { x: b.left, y: b.top }
  drag = { id: e.pointerId, x: e.clientX, y: e.clientY, left: b.left, top: b.top }
  ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
}
function move(e: PointerEvent) {
  if (!drag || drag.id !== e.pointerId) return
  position.value = { x: drag.left + e.clientX - drag.x, y: drag.top + e.clientY - drag.y }
  clamp()
}
function end() {
  if (drag) {
    drag = null
    save()
  }
}
function keyboardMove(e: KeyboardEvent) {
  if (
    e.target !== e.currentTarget ||
    !['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(e.key)
  )
    return
  e.preventDefault()
  const b = panel.value!.getBoundingClientRect()
  position.value = {
    x: b.left + (e.key === 'ArrowRight' ? 16 : e.key === 'ArrowLeft' ? -16 : 0),
    y: b.top + (e.key === 'ArrowDown' ? 16 : e.key === 'ArrowUp' ? -16 : 0),
  }
  clamp()
  save()
}
async function toggle() {
  expanded.value = !expanded.value
  await nextTick()
  clamp()
}
async function refresh() {
  if (document.hidden || loading) return
  loading = true
  try {
    data.value = await $fetch<Snapshot>('/api/backend/front/api-usage')
    failed.value = false
  } catch {
    failed.value = true
  } finally {
    loading = false
    await nextTick()
    clamp()
  }
}
onMounted(() => {
  try {
    const s = JSON.parse(localStorage.getItem(key) || 'null')
    if (s && Number.isFinite(s.x) && Number.isFinite(s.y)) position.value = s
  } catch {}
  clamp()
  refresh()
  timer = setInterval(refresh, 30_000)
  window.addEventListener('resize', clamp)
  document.addEventListener('visibilitychange', refresh)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  window.removeEventListener('resize', clamp)
  document.removeEventListener('visibilitychange', refresh)
})
</script>
<template>
  <aside
    ref="panel"
    class="api-usage"
    :class="{ 'is-expanded': expanded }"
    :style="style"
    aria-label="서비스 API 사용량"
  >
    <div
      class="api-usage__header"
      tabindex="0"
      aria-label="API 사용량 창 이동, 방향키로 이동 가능"
      @pointerdown="start"
      @pointermove="move"
      @pointerup="end"
      @pointercancel="end"
      @keydown="keyboardMove"
    >
      <span class="api-usage__grip" aria-hidden="true">⠿</span>
      <span v-if="expanded" class="api-usage__title">API 사용량</span>
      <button
        class="api-usage__toggle"
        :class="{ 'icon-only': expanded }"
        :aria-label="expanded ? '사용량 접기' : 'API 사용량 펼치기'"
        :aria-expanded="expanded"
        aria-controls="api-usage-content"
        @click="toggle"
      >
        <span v-if="!expanded">API 사용량</span
        ><span aria-hidden="true">{{ expanded ? '−' : '⌃' }}</span>
      </button>
    </div>
    <div
      v-if="expanded"
      id="api-usage-content"
      :title="`${data?.scope || '서비스 전체 · 앱 내 집계'} · 적용 이후 성공 응답 기준. 외부 사용량 제외. 최대치는 설정된 무료 기준입니다.`"
    >
      <p v-if="failed || data?.delayed" class="api-usage__status" role="status">
        갱신 지연{{ data ? ' · 마지막 집계 표시' : '' }}
      </p>
      <p v-else-if="!data" class="api-usage__status" role="status">사용량 확인 중…</p>
      <div v-for="item in data?.items" :key="item.site" class="api-usage__row">
        <div class="api-usage__numbers">
          <strong>{{ item.site }}</strong
          ><span>{{ fmt(item.current) }} / {{ fmt(item.max) }} {{ item.unit }}</span>
        </div>
        <div class="api-usage__model">{{ modelName(item.model) }} · {{ item.period }}</div>
        <div
          class="api-usage__track"
          role="meter"
          :aria-label="`${item.site} ${item.period} ${item.unit}`"
          :aria-valuemin="0"
          :aria-valuemax="item.max"
          :aria-valuenow="Math.min(item.current, item.max)"
          :aria-valuetext="`${fmt(item.current)} / ${fmt(item.max)} ${item.unit}`"
        >
          <span
            :style="{
              width: `${item.max > 0 ? Math.min(100, (item.current / item.max) * 100) : 0}%`,
              background: item.color,
            }"
          />
        </div>
      </div>
    </div>
  </aside>
</template>
<style scoped>
.api-usage {
  position: fixed;
  left: 16px;
  bottom: 24px;
  z-index: 60;
  width: 146px;
  max-width: calc(100vw - 32px);
  border: 1px solid #e7dcd4;
  border-radius: 12px;
  background: #fff;
  color: #2a211d;
  box-shadow: 0 12px 36px #41271720;
  overflow: hidden;
  font-size: 13px;
}
.api-usage.is-expanded {
  width: 320px;
  max-height: calc(100dvh - 32px);
  overflow-y: auto;
}
.api-usage__header {
  height: 44px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  background: #faf8f6;
  cursor: grab;
  touch-action: none;
  user-select: none;
}
.api-usage__header:active {
  cursor: grabbing;
}
.api-usage__grip {
  color: #9a8a7d;
  font-size: 20px;
}
.api-usage__toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex: 1;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  color: inherit;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}
.api-usage__title {
  flex: 1;
  font-weight: 700;
}
.api-usage__toggle.icon-only {
  flex: 0;
}
.api-usage__toggle > span:last-child {
  color: #7a6b63;
  font-size: 20px;
  font-weight: 400;
}
.api-usage__row {
  padding: 14px 16px;
  border-top: 1px solid #eee6e1;
}
.api-usage__numbers {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.api-usage__numbers strong {
  font-size: 13px;
}
.api-usage__numbers > span {
  font-size: 11px;
  color: #6f3f28;
  white-space: nowrap;
}
.api-usage__model {
  margin-top: 7px;
  font-size: 11px;
  color: #8b7b71;
  overflow-wrap: anywhere;
}
.api-usage__track {
  height: 4px;
  margin-top: 7px;
  border-radius: 4px;
  background: #eee7e1;
  overflow: hidden;
}
.api-usage__track > span {
  display: block;
  height: 100%;
  border-radius: inherit;
  transition: width 0.3s ease;
}
.api-usage__status {
  margin: 0;
  padding: 14px 16px;
  color: #8b7b71;
  font-size: 12px;
}
@media (max-width: 640px) {
  .api-usage {
    bottom: calc(80px + env(safe-area-inset-bottom));
  }
}
@media (prefers-reduced-motion: reduce) {
  .api-usage__track > span {
    transition: none;
  }
}
</style>
