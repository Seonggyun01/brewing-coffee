<script setup lang="ts">
import options from '~/utils/options.json'
import { emptyForm, payload } from '~/utils/forms'
const props = defineProps<{ kind: 'beans' | 'brews' | 'cafes'; id?: string; demoStage?: number }>()
const api = useBackend(),
  step = ref(0),
  furthestStep = ref(0),
  saving = ref(false),
  error = ref(''),
  fieldErrors = ref<Record<string, string>>({}),
  ai = ref(false),
  extracting = ref(false)
const form = reactive<Record<string, any>>(emptyForm())
watch(
  () => props.demoStage,
  (value) => {
    if (value == null) return
    ai.value = value > 0
    extracting.value = value === 0
    step.value = 0
    Object.assign(form, {
      name: value > 0 ? '콜롬비아 사르사 파카마라 복합 발효 워시드' : '',
      roastery: value > 0 ? 'LOFE COFFEE' : '',
      originCountryCode: value > 0 ? 'CO' : '',
      farm: value > 0 ? 'ZARZA' : '',
      variety: value > 0 ? '파카마라' : '',
      processType: value > 0 ? 'WASHED' : '',
      customFlavorNotesText:
        value > 1
          ? '체리주스, 핑크구아바, 망고스틴, 밀키스, 화이트 그레이프 소다'
          : value > 0
            ? '체리주스, 핑크구아바, 망고스틴'
            : '',
    })
  },
  { immediate: true },
)
const routes = { beans: '/coffee-beans', brews: '/brew-records', cafes: '/cafe-filter-coffees' }
const labels = { beans: '원두', brews: '브루잉 기록', cafes: '카페 필터커피' }
const steps = computed(() =>
  props.kind === 'beans'
    ? [ai.value ? 'AI 결과 확인' : '원두 정보', '구매 정보 · 선택']
    : props.kind === 'brews'
      ? ['기본 정보', '레시피', '맛과 느낌']
      : ['마신 커피', '마신 카페', '맛 프로파일', '느낌'],
)
const {
  data: initial,
  error: loadError,
  refresh,
} = await useAsyncData(`form-${props.kind}-${props.id || 'new'}`, () =>
  props.id && props.demoStage == null
    ? api.read(`front/${props.kind}/${props.id}/form`)
    : Promise.resolve(null),
)
watch(
  initial,
  (value) => {
    if (value) Object.assign(form, value)
  },
  { immediate: true },
)
if (!props.id && props.kind === 'brews' && useRoute().query.beanId)
  form.coffeeBeanId = String(useRoute().query.beanId)
const { data: beans } = await useAsyncData(`form-beans-${props.kind}`, () =>
  props.kind === 'brews' ? api.read<any[]>('front/beans') : Promise.resolve([]),
)
const beanOptions = computed(() =>
  (beans.value || [])
    .filter((b: any) => b.status === 'CURRENT')
    .map((b: any) => ({ value: String(b.id), label: b.name })),
)
const processes = [
  { value: 'WASHED', label: '워시드' },
  { value: 'NATURAL', label: '내추럴' },
  { value: 'HONEY', label: '허니' },
  { value: 'ANAEROBIC', label: '무산소 발효' },
  { value: 'CARBONIC_MACERATION', label: '카보닉 매서레이션' },
  { value: 'DECAF', label: '디카페인' },
  { value: 'OTHER', label: '기타' },
]
const statusOptions = [
  { value: 'CURRENT', label: '보유 중' },
  { value: 'FINISHED', label: '소진 기록' },
  { value: 'CAFE', label: '카페 음용' },
]
function parsed(result: any) {
  const f = result.form
  const allowed = [
    'name',
    'roastery',
    'originCountryCode',
    'country',
    'region',
    'farm',
    'variety',
    'altitude',
    'processType',
    'price',
    'weight',
  ]
  for (const key of allowed) if (f[key] != null) form[key] = f[key]
  form.customFlavorNotesText = [
    ...(f.flavorNotes || []).map(
      (v: string) => options.flavors.find((o) => o.value === v)?.label || v,
    ),
    f.customFlavorNotesText,
  ]
    .filter(Boolean)
    .join(', ')
  form.flavorNotes = []
  ai.value = true
  step.value = 0
}
function next() {
  fieldErrors.value = {}
  error.value = ''
  if (step.value === 0 && props.kind !== 'brews' && !form.name?.trim()) {
    fieldErrors.value.name = '원두 이름을 입력해주세요.'
    return
  }
  if (step.value === 0 && props.kind === 'brews' && !form.coffeeBeanId) {
    fieldErrors.value.coffeeBeanId = '원두를 선택해주세요.'
    return
  }
  step.value++
  furthestStep.value = Math.max(furthestStep.value, step.value)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
const sharedBeanKeys = [
  'name',
  'roastery',
  'originCountryCode',
  'region',
  'variety',
  'processType',
  'flavorNotes',
  'customFlavorNotesText',
  'memo',
]
const beanKeys = [
  ...sharedBeanKeys,
  'country',
  'farm',
  'altitude',
  'status',
  'purchasedDate',
  'price',
  'weight',
  'purchasePlaceId',
  'purchasePlaceName',
  'purchasePlaceType',
  'purchasePlaceAddress',
  'purchasePlaceUrl',
  'purchasePlaceLatitude',
  'purchasePlaceLongitude',
  'purchasePlaceMemo',
]
const tasteKeys = [
  'rating',
  'acidity',
  'sweetness',
  'bitterness',
  'body',
  'aroma',
  'balance',
  'feelingTags',
  'customFeelingTagsText',
  'memo',
]
const brewKeys = [
  'coffeeBeanId',
  'brewedDate',
  'brewMethod',
  'temperatureType',
  'beanAmount',
  'waterAmount',
  'waterTemperature',
  'grinderModel',
  'grinderSetting',
  'grindSizeMicron',
  'brewTimeSec',
  'pourSteps',
  ...tasteKeys,
]
const cafeKeys = [
  ...sharedBeanKeys,
  'cafeName',
  'cafeAddress',
  'cafeUrl',
  'cafeLatitude',
  'cafeLongitude',
  'visitedDate',
  'temperatureType',
  'recordRecipe',
  ...tasteKeys,
]
async function save() {
  if (props.demoStage != null) return
  saving.value = true
  error.value = ''
  fieldErrors.value = {}
  try {
    const keys = props.kind === 'beans' ? beanKeys : props.kind === 'brews' ? brewKeys : cafeKeys
    const data = payload(Object.fromEntries(keys.map((key) => [key, form[key]])))
    if (props.kind === 'beans' && ai.value && step.value === 0 && furthestStep.value === 0) {
      data.price = null
      data.weight = null
    }
    data.recordRecipe = props.kind === 'cafes' ? false : undefined
    const result: any = await api.write(
      `front/${props.kind}${props.id ? '/' + props.id : ''}`,
      props.id ? 'PUT' : 'POST',
      data,
    )
    await navigateTo(`${routes[props.kind]}/${props.id || result.id}`)
  } catch (e: any) {
    error.value = errorMessage(e)
    fieldErrors.value = e.data?.fields || e.data?.data?.fields || {}
    if (fieldErrors.value.name || fieldErrors.value.coffeeBeanId) step.value = 0
  } finally {
    saving.value = false
  }
}
function toggleFeeling(v: string) {
  form.feelingTags = form.feelingTags.includes(v)
    ? form.feelingTags.filter((x: string) => x !== v)
    : [...form.feelingTags, v]
}
function addPour() {
  form.pourSteps.push({
    timeSec: form.pourSteps.length ? Number(form.pourSteps.at(-1).timeSec) + 30 : 0,
    amountMl: 50,
  })
}
</script>
<template>
  <div :class="['page', 'form-page', `form-page--${kind}`, { 'form-ai-review': ai }]">
    <div class="page-heading">
      <div>
        <span class="eyebrow">{{
          kind === 'beans' ? '원두 등록' : kind === 'brews' ? '브루잉 기록' : '카페 필터커피'
        }}</span>
        <h1>
          {{
            ai && step === 0
              ? 'AI가 읽은 정보를 확인해주세요'
              : step === 1 && kind === 'beans'
                ? '필요한 정보만 더 남겨보세요'
                : step === 2 && kind === 'brews'
                  ? '맛과 느낌을 남겨주세요'
                  : labels[kind] + (id ? ' 수정' : ' 등록')
          }}
        </h1>
        <p>{{ step + 1 }} / {{ steps.length }} 단계 · {{ steps.length - step - 1 }}단계 남았어요</p>
      </div>
      <NuxtLink :to="routes[kind]" class="link-button">닫기 ×</NuxtLink>
    </div>
    <ApiState v-if="loadError" :error="loadError" @retry="refresh" /><template v-else>
      <PhotoExtraction
        v-if="!id && kind !== 'brews'"
        :demo-stage="demoStage"
        v-show="step === 0 || kind === 'cafes'"
        @parsed="parsed"
        @busy="extracting = $event"
      />
      <nav :inert="extracting" class="stepper" aria-label="입력 단계">
        <button
          v-for="(s, i) in steps"
          :key="s"
          type="button"
          :class="{ current: step === i, complete: step > i }"
          :aria-current="step === i ? 'step' : undefined"
          :disabled="i > furthestStep"
          @click="step = i"
        >
          <span>{{ step > i ? '✓' : i + 1 }}</span
          >{{ s }}
        </button>
      </nav>

      <form
        :inert="extracting"
        novalidate
        @submit.prevent="step === steps.length - 1 ? save() : next()"
      >
        <section v-show="step === 0" class="panel initial-form-panel">
          <div class="section-heading">
            <span class="eyebrow">01</span>
            <h2>
              {{
                kind === 'brews'
                  ? '기본 정보'
                  : ai
                    ? '사진과 비교하며 확인해주세요'
                    : '어떤 원두인가요?'
              }}
            </h2>
            <p v-if="ai">잘못 읽은 부분은 바로 수정하세요. 중량과 가격은 다음 단계에서 확인해요.</p>
            <p v-else>* 표시된 항목만 필수예요. 나머지는 천천히 채워도 좋아요.</p>
          </div>
          <div v-if="kind === 'brews'" class="form-grid">
            <FormField
              v-model="form.coffeeBeanId"
              name="coffeeBeanId"
              label="원두"
              required
              :options="beanOptions"
              :error="fieldErrors.coffeeBeanId"
            /><FormField
              v-model="form.brewedDate"
              name="brewedDate"
              label="기록 날짜"
              type="date"
            />
            <div class="field">
              <label>추출 타입</label><TemperatureToggle v-model="form.temperatureType" />
            </div>
            <FormField
              v-model="form.brewMethod"
              name="brewMethod"
              label="추출 도구"
              :options="options.methods"
            />
          </div>
          <template v-else
            ><div class="form-grid">
              <FormField
                v-model="form.name"
                name="name"
                label="원두 이름"
                required
                :max="100"
                :error="fieldErrors.name"
              /><FormField
                v-model="form.roastery"
                name="roastery"
                label="로스터리"
                :max="100"
              /><FormField
                v-model="form.originCountryCode"
                name="originCountryCode"
                label="산지 국가"
                :options="options.countries"
              /><FormField v-model="form.region" name="region" label="지역" :max="100" /><FormField
                v-if="kind === 'beans'"
                v-model="form.farm"
                name="farm"
                label="농장 · 워싱스테이션"
                :max="100"
              /><FormField
                v-model="form.variety"
                name="variety"
                label="품종"
                :max="100"
              /><FormField
                v-if="kind === 'beans'"
                v-model="form.altitude"
                name="altitude"
                label="고도"
                :max="50"
              /><FormField
                v-model="form.processType"
                name="processType"
                label="가공 방식"
                :options="processes"
              />
            </div>
            <div class="flavor-section">
              <h3>향미 노트 <span class="optional">선택</span></h3>
              <FlavorWheel v-if="!ai" v-model="form.flavorNotes">
                <template #custom-input>
                  <div class="field flavor-custom-input">
                    <label for="customFlavorNotesText">향미 직접 추가</label>
                    <p id="custom-flavor-hint" class="hint">
                      휠에 없는 향미를 쉼표로 구분해 적어주세요.
                    </p>
                    <input
                      id="customFlavorNotesText"
                      v-model="form.customFlavorNotesText"
                      type="text"
                      placeholder="체리 주스, 핑크 구아바, 밀키스"
                      maxlength="500"
                      aria-describedby="custom-flavor-hint"
                    />
                  </div>
                </template>
              </FlavorWheel>
              <FormField
                v-if="ai"
                v-model="form.customFlavorNotesText"
                name="customFlavorNotesText"
                :label="ai ? '사진에서 읽은 향미' : '직접 입력한 향미'"
                placeholder="체리 주스, 핑크 구아바, 밀키스"
                :max="500"
              />
              <p v-if="ai" class="hint">향미는 쉼표로 구분해주세요.</p>
            </div></template
          >
        </section>
        <section v-if="kind === 'beans'" v-show="step === 1" class="panel">
          <div class="section-heading">
            <span class="eyebrow">02 · OPTIONAL</span>
            <h2>가격 / 보유 정보</h2>
            <p>선택 정보예요. 비워두고 저장할 수 있어요.</p>
          </div>
          <div class="form-grid">
            <FormField
              v-model="form.price"
              name="price"
              label="구매 가격 (원)"
              type="number"
              :min="0"
            /><FormField
              v-model="form.weight"
              name="weight"
              label="남은 중량 (g)"
              type="number"
              :min="0"
            /><FormField
              v-model="form.purchasedDate"
              name="purchasedDate"
              label="구매 날짜"
              type="date"
            /><FormField
              v-model="form.status"
              name="status"
              label="원두 상태"
              :options="statusOptions"
            />
          </div>
          <PlaceSearch :model="form" /><FormField
            v-model="form.memo"
            name="memo"
            label="메모"
            type="textarea"
            :max="1000"
          />
        </section>
        <section v-if="kind === 'brews'" v-show="step === 1" class="panel recipe-panel">
          <div class="section-heading">
            <span class="eyebrow">02 · OPTIONAL</span>
            <h2>레시피</h2>
            <p>다시 내리고 싶은 한 잔을 위해 남겨두세요.</p>
          </div>
          <div class="form-grid">
            <FormField
              v-model="form.beanAmount"
              name="beanAmount"
              label="원두량 (g)"
              type="number"
              :min="0.1"
              step="0.1"
            /><FormField
              v-model="form.waterAmount"
              name="waterAmount"
              label="물량 (ml)"
              type="number"
              :min="0.1"
              step="0.1"
            /><FormField
              v-model="form.waterTemperature"
              name="waterTemperature"
              label="물 온도 (°C)"
              type="number"
              :min="0.1"
              step="0.1"
            /><FormField
              v-model="form.grinderModel"
              name="grinderModel"
              label="그라인더 모델"
              :max="100"
            /><FormField
              v-model="form.grinderSetting"
              name="grinderSetting"
              label="그라인더 조절값"
              :max="50"
            /><FormField
              v-model="form.grindSizeMicron"
              name="grindSizeMicron"
              label="분쇄도 (μm)"
              type="number"
              :min="1"
              :max="5000"
            />
            <FormField
              v-model="form.brewTimeSec"
              name="brewTimeSec"
              label="총 추출 시간 (초)"
              type="number"
              :min="1"
            />
          </div>
          <div class="section-heading">
            <h3>푸어링 타임라인</h3>
            <p>붓기 시작한 시간과 물량을 기록하세요.</p>
          </div>
          <div v-for="(pour, i) in form.pourSteps" :key="i" class="pour-row">
            <FormField
              v-model="pour.timeSec"
              :name="`time-${i}`"
              :label="`${Number(i) + 1}차 시작 (초)`"
              type="number"
              :min="0"
              :max="3600"
            /><FormField
              v-model="pour.amountMl"
              :name="`amount-${i}`"
              label="한 번에 부은 물량 (ml)"
              type="number"
              :min="1"
              :max="3000"
            /><button
              type="button"
              class="link-button"
              :aria-label="`${Number(i) + 1}차 푸어링 삭제`"
              @click="form.pourSteps.splice(Number(i), 1)"
            >
              ×
            </button>
          </div>
          <button type="button" class="button secondary" @click="addPour">+ 푸어링 추가</button>
        </section>
        <section v-if="kind === 'cafes'" v-show="step === 1" class="panel">
          <div class="section-heading">
            <span class="eyebrow">02</span>
            <h2>마신 카페</h2>
          </div>
          <PlaceSearch :model="form" cafe />
          <div class="form-grid">
            <FormField
              v-model="form.visitedDate"
              name="visitedDate"
              label="방문 날짜"
              type="date"
            />
            <div class="field">
              <label>추출 타입</label><TemperatureToggle v-model="form.temperatureType" />
            </div>
          </div>
        </section>
        <section v-if="kind !== 'beans'" v-show="step === 2" class="panel taste-step">
          <div class="section-heading">
            <span class="eyebrow">03 · OPTIONAL</span>
            <h2>{{ kind === 'brews' ? '맛과 느낌을 남겨주세요' : '어떤 맛이었나요?' }}</h2>
          </div>
          <div v-if="kind === 'brews'" class="satisfaction-panel">
            <h3>만족도</h3>
            <div class="satisfaction-stars" role="group" aria-label="별점 선택">
              <button
                v-for="n in 5"
                :key="n"
                type="button"
                :aria-label="`만족도 ${n}점`"
                :aria-pressed="Number(form.rating) === n"
                @click="form.rating = n"
              >
                {{ Number(form.rating) >= n ? '★' : '☆' }}
              </button>
            </div>
            <FormField
              v-model="form.rating"
              name="rating"
              label="만족도 (1–5)"
              type="number"
              :min="1"
              :max="5"
              step="0.1"
            />
          </div>
          <div class="taste-profile-panel">
            <h3>맛 프로파일</h3>
            <TasteProfile :model="form" editable />
          </div>
          <template v-if="kind === 'brews'"
            ><h3>느낌 · 여러 개 선택 가능</h3>
            <div class="chips">
              <button
                v-for="f in options.feelings"
                :key="f.value"
                type="button"
                :class="{ selected: form.feelingTags.includes(f.value) }"
                :aria-pressed="form.feelingTags.includes(f.value)"
                @click="toggleFeeling(f.value)"
              >
                {{ f.label }}
              </button>
            </div>
            <FormField
              v-model="form.customFeelingTagsText"
              name="customFeelingTagsText"
              label="직접 입력한 느낌"
              :max="500" /><FormField
              v-model="form.memo"
              name="memo"
              label="메모"
              type="textarea"
              :max="1000"
          /></template>
        </section>
        <section v-if="kind === 'cafes'" v-show="step === 3" class="panel cafe-feeling-panel">
          <div class="section-heading">
            <span class="eyebrow">04 · OPTIONAL</span>
            <h2>느낌</h2>
          </div>
          <div class="cafe-rating">
            <FormField
              v-model="form.rating"
              name="rating"
              label="평점 / 5"
              type="number"
              :min="1"
              :max="5"
              step="0.1"
            />
          </div>
          <div class="chips">
            <button
              v-for="f in options.feelings"
              :key="f.value"
              type="button"
              :class="{ selected: form.feelingTags.includes(f.value) }"
              :aria-pressed="form.feelingTags.includes(f.value)"
              @click="toggleFeeling(f.value)"
            >
              {{ f.label }}
            </button>
          </div>
          <FormField
            v-model="form.customFeelingTagsText"
            name="customFeelingTagsText"
            label="직접 입력한 느낌"
            :max="500"
          /><FormField v-model="form.memo" name="memo" label="메모" type="textarea" :max="1000" />
        </section>
        <div v-if="error" class="error-text error-summary" role="alert">
          {{ error }}
          <ul>
            <li v-for="(msg, key) in fieldErrors" :key="key">{{ msg }}</li>
          </ul>
        </div>
        <div class="form-actions">
          <button v-if="step" type="button" class="button secondary" @click="step--">
            ← 이전 단계</button
          ><NuxtLink v-else :to="routes[kind]" class="button secondary">취소</NuxtLink
          ><button
            v-if="ai && kind === 'beans' && step === 0"
            type="button"
            class="button secondary ai-save"
            :disabled="saving"
            @click="save"
          >
            확인한 정보 저장</button
          ><button type="submit" class="button" :disabled="saving">
            {{
              saving ? '저장 중…' : step === steps.length - 1 ? '확인한 정보 저장' : '다음 단계 →'
            }}
          </button>
        </div>
      </form></template
    >
  </div>
</template>
