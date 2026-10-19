<script setup lang="ts">
import options from '~/utils/options.json'
import { safeLink } from '~/utils/forms'
const props = defineProps<{
  kind: 'beans' | 'brews' | 'cafes'
  id: string
  demoRecord?: Record<string, any>
}>()
if (!props.demoRecord) useHead({ bodyAttrs: { style: 'overflow: hidden' } })
const modal = ref<HTMLElement>()
function close() {
  if (!props.demoRecord) navigateTo(routes[props.kind])
}
function modalKey(event: KeyboardEvent) {
  if (event.key === 'Escape' && !askDelete.value) close()
  if (event.key !== 'Tab' || !modal.value) return
  const nodes = Array.from(
    modal.value.querySelectorAll<HTMLElement>(
      'a[href], button:not(:disabled), input, select, textarea, [tabindex="0"]',
    ),
  ).filter((el) => el.offsetParent !== null)
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
onMounted(() => {
  if (!props.demoRecord) modal.value?.focus()
})
const api = useBackend(),
  removing = ref(false),
  askDelete = ref(false),
  deleteError = ref('')
const routes = { beans: '/coffee-beans', brews: '/brew-records', cafes: '/cafe-filter-coffees' }
const {
  data: record,
  error,
  refresh,
} = await useAsyncData(`detail-${props.kind}-${props.id}`, () =>
  props.demoRecord
    ? Promise.resolve(props.demoRecord)
    : api.read(`front/${props.kind}/${props.id}`),
)
const { data: bean } = await useAsyncData(`detail-cafe-bean-${props.id}`, () =>
  props.kind === 'cafes' && record.value?.coffeeBeanId
    ? api.read(`front/beans/${record.value.coffeeBeanId}`)
    : Promise.resolve(null),
)
const info = computed(() => (props.kind === 'beans' ? record.value : bean.value))
const beanFields = [
  { key: 'roastery', label: '로스터리' },
  { key: 'originCountryDisplayName', label: '산지 국가' },
  { key: 'region', label: '지역' },
  { key: 'farm', label: '농장 · 워싱스테이션' },
  { key: 'variety', label: '품종' },
  { key: 'altitude', label: '고도' },
  { key: 'processType', label: '가공 방식' },
  { key: 'purchasedDate', label: '구매 날짜' },
  { key: 'weight', label: '남은 중량 (g)' },
  { key: 'price', label: '구매 가격 (원)' },
]
const brewFields = [
  { key: 'brewMethod', label: '추출 도구' },
  { key: 'beanAmount', label: '원두량 (g)' },
  { key: 'waterAmount', label: '물량 (ml)' },
  { key: 'waterTemperature', label: '물 온도 (°C)' },
  { key: 'grinderModel', label: '그라인더' },
  { key: 'grinderSetting', label: '조절값' },
  { key: 'grindSizeMicron', label: '분쇄도 (μm)' },
  { key: 'brewTimeSec', label: '추출 시간 (초)' },
]
function label(key: string, value: any) {
  if (key === 'brewMethod') return options.methods.find((m) => m.value === value)?.label || value
  return value ?? '—'
}
async function remove() {
  if (props.demoRecord) return
  removing.value = true
  try {
    await api.write(`front/${props.kind}/${props.id}`, 'DELETE')
    await navigateTo(routes[props.kind])
  } catch (e) {
    deleteError.value = errorMessage(e)
  } finally {
    removing.value = false
  }
}
const pours = computed(() =>
  (record.value?.pourSteps || []).reduce(
    (acc: any[], p: any) => [
      ...acc,
      { time: p.timeSec, total: (acc.at(-1)?.total || 0) + Number(p.amountMl) },
    ],
    [],
  ),
)
const pourPoints = computed(() =>
  pours.value
    .map(
      (p: any) =>
        `${30 + (p.time / Math.max(record.value?.brewTimeSec || 180, ...pours.value.map((x: any) => x.time), 1)) * 480},${170 - (p.total / Math.max(...pours.value.map((x: any) => x.total), 1)) * 140}`,
    )
    .join(' '),
)
</script>
<template>
  <div class="detail-screen">
    <div v-if="!demoRecord" class="detail-list-background" inert aria-hidden="true">
      <RecordList :kind="kind" />
    </div>
    <div class="detail-overlay" @click.self="close">
      <section
        ref="modal"
        class="detail-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="detail-dialog-title"
        tabindex="-1"
        @keydown="modalKey"
      >
        <header class="detail-dialog-header">
          <div>
            <span>{{ kind === 'beans' ? '원두' : kind === 'brews' ? '브루잉' : '카페' }}</span>
            <h2 id="detail-dialog-title">
              {{
                kind === 'beans' ? '원두 상세' : kind === 'brews' ? '브루잉 상세' : '카페 경험 상세'
              }}
            </h2>
          </div>
          <div class="actions">
            <NuxtLink
              :to="`${routes[kind]}/${id}/edit`"
              class="button secondary"
              aria-label="수정하기"
              >수정</NuxtLink
            ><button class="button delete-button" @click="askDelete = true">삭제</button
            ><button class="modal-close" aria-label="상세 닫기" @click="close">×</button>
          </div>
        </header>
        <div class="detail-scroll">
          <div class="page detail-page">
            <ApiState :error="error" @retry="refresh" /><template v-if="record"
              ><section class="detail-hero">
                <div
                  class="flavor-bar"
                  :style="{
                    background: (
                      record.flavorGradientStyle || 'linear-gradient(90deg,#b6cc94,#e7b881,#be7c83)'
                    )
                      .replace(/^background:\s*/, '')
                      .replace(/;$/, ''),
                  }"
                />
                <span class="eyebrow">{{ record.roastery || 'COFFEE JOURNAL' }}</span>
                <h1>{{ record.name || record.coffeeBeanName }}</h1>

                <div class="detail-meta">
                  <template v-if="kind === 'beans'"
                    ><span>{{ record.weight ?? '—' }}g 남음</span
                    ><span>{{
                      record.status === 'CURRENT'
                        ? '보유중'
                        : record.status === 'FINISHED'
                          ? '모두 소진'
                          : '카페 시식'
                    }}</span
                    ><span>{{ record.processType || '' }}</span></template
                  >
                  <span
                    v-if="record.temperatureType"
                    :class="['temperature-badge', record.temperatureType.toLowerCase()]"
                    >{{ record.temperatureType }}</span
                  ><span>{{ record.brewedDate || record.originCountryDisplayName }}</span
                  ><span v-if="record.rating">★ {{ record.rating }} / 5</span>
                </div>
                <p>{{ record.flavorNoteSummary || '향미 미입력' }}</p>
                <NuxtLink
                  v-if="kind === 'beans'"
                  :to="`/brew-records/new?beanId=${id}`"
                  class="button bean-brew-action"
                  >브루잉 기록</NuxtLink
                >
              </section>
              <section v-if="info" class="panel">
                <h2>원두 세부 정보</h2>
                <dl class="info-grid">
                  <div
                    v-for="f in beanFields.filter(
                      (f) =>
                        kind === 'beans' || !['weight', 'price', 'purchasedDate'].includes(f.key),
                    )"
                    :key="f.key"
                  >
                    <dt>{{ f.label }}</dt>
                    <dd>{{ info[f.key] ?? '—' }}</dd>
                  </div>
                </dl>
              </section>
              <section v-if="kind === 'brews'" class="panel">
                <h2>레시피</h2>
                <dl class="info-grid">
                  <div v-for="f in brewFields" :key="f.key">
                    <dt>{{ f.label }}</dt>
                    <dd>{{ label(f.key, record[f.key]) }}</dd>
                  </div>
                </dl>
                <template v-if="pours.length"
                  ><h3>푸어링 타임라인</h3>
                  <svg
                    class="pour-chart"
                    viewBox="0 0 540 210"
                    role="img"
                    aria-label="시간에 따른 누적 물량"
                  >
                    <line x1="30" x2="510" y1="170" y2="170" stroke="#c8b9ac" />
                    <polyline
                      :points="`30,170 ${pourPoints}`"
                      fill="none"
                      stroke="#98613e"
                      stroke-width="3"
                    />
                    <text x="30" y="198" font-size="12">0초</text>
                    <text x="450" y="198" font-size="12">
                      {{ record.brewTimeSec || pours.at(-1)?.time }}초
                    </text>
                  </svg>
                  <ol>
                    <li v-for="(p, i) in record.pourSteps" :key="i">
                      {{ p.timeSec }}초 · {{ p.amountMl }}ml
                    </li>
                  </ol></template
                >
              </section>
              <section v-if="kind !== 'beans'" class="panel">
                <h2>맛과 느낌</h2>
                <TasteProfile :model="record" />
                <div class="chips">
                  <span v-for="f in record.feelingTags" :key="f">{{
                    options.feelings.find((o) => o.value === f)?.label || f
                  }}</span
                  ><span v-for="f in record.customFeelingTags" :key="f">{{ f }}</span>
                </div>
              </section>
              <section v-if="(info || record).purchasePlaceName" class="panel purchase-place-panel">
                <h2>{{ kind === 'cafes' ? '카페' : '구매처' }}</h2>
                <h3>{{ (info || record).purchasePlaceName }}</h3>
                <p>{{ info?.purchasePlaceAddress }}</p>
                <a
                  v-if="safeLink(info?.purchasePlaceUrl)"
                  :href="safeLink(info?.purchasePlaceUrl)"
                  target="_blank"
                  rel="noopener noreferrer"
                  >지도에서 확인 ↗</a
                ><PlaceDisplay
                  v-if="info?.purchasePlaceLatitude"
                  :latitude="info.purchasePlaceLatitude"
                  :longitude="info.purchasePlaceLongitude"
                />
              </section>
              <section class="panel">
                <h2>메모</h2>
                <p class="memo">{{ record.memo || '아직 메모가 없어요.' }}</p>
              </section></template
            >
          </div>
        </div>
      </section>
    </div>
    <div v-if="askDelete" class="dialog-backdrop">
      <section class="dialog" role="alertdialog" aria-modal="true" aria-labelledby="delete-title">
        <h2 id="delete-title">이 기록을 삭제할까요?</h2>
        <p>삭제한 기록은 복구할 수 없어요.</p>
        <p v-if="deleteError" class="error-text" role="alert">{{ deleteError }}</p>
        <div class="actions">
          <button class="button secondary" :disabled="removing" @click="askDelete = false">
            취소</button
          ><button class="button" :disabled="removing" @click="remove">
            {{ removing ? '삭제 중…' : '삭제하기' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>
