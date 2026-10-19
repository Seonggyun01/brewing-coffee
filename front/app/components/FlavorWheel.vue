<script setup lang="ts">
import options from '~/utils/options.json'
const model = defineModel<string[]>({ default: () => [] })
const family = ref(''),
  category = ref('')
const grainValues = ['TOAST', 'BAKED_BREAD']
const families = [
  {
    value: 'FRUIT',
    label: '과일',
    english: 'FRUIT',
    color: '#df0d79',
    categories: [
      'CITRUS',
      'APPLE_PEAR',
      'TROPICAL_FRUIT',
      'STONE_FRUIT',
      'BERRY',
      'WINEY_FERMENTED',
    ],
  },
  { value: 'FLORAL', label: '꽃', english: 'FLORAL', color: '#ada0cd', categories: ['FLORAL'] },
  {
    value: 'HERBAL',
    label: '허브·식물',
    english: 'HERBAL',
    color: '#439a4b',
    categories: ['EARTHY_HERBAL', 'TEA_LIKE'],
  },
  { value: 'SPICE', label: '향신료', english: 'SPICE', color: '#ec6c73', categories: ['SPICE'] },
  {
    value: 'ROAST',
    label: '구운 향',
    english: 'ROAST',
    color: '#81827b',
    categories: ['ROASTED_SMOKY'],
  },
  { value: 'GRAIN', label: '곡물', english: 'GRAIN', color: '#d8bd75', categories: ['GRAIN'] },
  { value: 'NUT', label: '견과류', english: 'NUT', color: '#b99563', categories: ['NUTTY'] },
  {
    value: 'CHOCOLATE',
    label: '초콜릿',
    english: 'CHOCOLATE',
    color: '#604737',
    categories: ['CHOCOLATE'],
  },
  { value: 'SWEET', label: '달콤함', english: 'SWEET', color: '#cea14f', categories: ['SWEET'] },
]
const activeFamily = computed(() => families.find((f) => f.value === family.value))
const categories = computed(() =>
  (activeFamily.value?.categories || []).map((value) =>
    value === 'GRAIN'
      ? { value, label: '곡물', color: '#d8bd75' }
      : options.categories.find((c) => c.value === value)!,
  ),
)
const notesFor = (value: string) =>
  options.flavors.filter((n) =>
    value === 'GRAIN'
      ? grainValues.includes(n.value)
      : n.category === value && !(value === 'ROASTED_SMOKY' && grainValues.includes(n.value)),
  )
const notes = computed(() => notesFor(category.value))
const spans = computed(() => {
  let start = -90
  return families.map((f) => {
    const degrees = !family.value ? 40 : f.value === family.value ? 144 : 27
    const span = { start, end: start + degrees }
    start += degrees
    return span
  })
})
const point = (r: number, a: number) => [
  210 + Math.cos((a * Math.PI) / 180) * r,
  210 + Math.sin((a * Math.PI) / 180) * r,
]
function wedge(inner: number, outer: number, start: number, end: number) {
  const a = point(outer, start + 0.3),
    b = point(outer, end - 0.3),
    c = point(inner, end - 0.3),
    d = point(inner, start + 0.3)
  return `M${a} A${outer} ${outer} 0 ${end - start > 180 ? 1 : 0} 1 ${b} L${c} A${inner} ${inner} 0 ${end - start > 180 ? 1 : 0} 0 ${d} Z`
}
function familyClick(value: string) {
  if (family.value === value) {
    family.value = ''
    category.value = ''
    return
  }
  family.value = value
  category.value = families.find((f) => f.value === value)?.categories[0] || ''
}
function clickFamily(value: string, event: MouseEvent) {
  familyClick(value)
  ;(event.currentTarget as SVGElement).blur()
}
function clickCategory(value: string, event: MouseEvent) {
  category.value = value
  ;(event.currentTarget as SVGElement).blur()
}
function resetExplorer() {
  family.value = ''
  category.value = ''
}
function chooseNote(group: string, value: string) {
  category.value = group
  toggle(value)
}
function toggle(value: string) {
  model.value = model.value.includes(value)
    ? model.value.filter((v) => v !== value)
    : model.value.length < 8
      ? [...model.value, value]
      : model.value
}
const categorySpans = computed(() => {
  const index = families.findIndex((f) => f.value === family.value),
    span = spans.value[index]
  if (!span) return []
  return categories.value.map((c, i) => ({
    ...c,
    start: span.start + ((span.end - span.start) * i) / categories.value.length,
    end: span.start + ((span.end - span.start) * (i + 1)) / categories.value.length,
  }))
})
</script>
<template>
  <div class="flavor-explorer">
    <p class="flavor-breadcrumb">
      <button type="button" @click="resetExplorer">전체 향미</button
      ><template v-if="activeFamily">
        › {{ activeFamily.label
        }}<template v-if="category">
          › {{ categories.find((c) => c.value === category)?.label }}</template
        ></template
      >
    </p>
    <div class="flavor-layout">
      <svg viewBox="0 0 420 420" class="flavor-wheel" aria-label="플레이버 휠">
        <circle cx="210" cy="210" r="195" fill="#fffcf7" stroke="#ede2d3" />
        <circle cx="210" cy="210" r="189" fill="none" stroke="#f3eade" />
        <g
          v-for="(f, i) in families"
          :key="f.value"
          role="button"
          tabindex="0"
          :aria-label="f.label + ' 하위 향미 펼치기'"
          :aria-pressed="family === f.value"
          @click="clickFamily(f.value, $event)"
          @keydown.enter="familyClick(f.value)"
          @keydown.space.prevent="familyClick(f.value)"
        >
          <path
            :d="
              wedge(
                56,
                family === f.value ? 109 : family ? 145 : 145,
                spans[i]!.start,
                spans[i]!.end,
              )
            "
            :fill="f.color"
            :opacity="!family || family === f.value ? 1 : 0.55"
            stroke="white"
            stroke-width="3"
          />
          <path
            v-if="family !== f.value"
            :d="wedge(147, 182, spans[i]!.start, spans[i]!.end)"
            :fill="f.color"
            :opacity="family ? 0.35 : 0.75"
            stroke="white"
            stroke-width="3"
          />
          <text
            :x="point(family === f.value ? 82 : 103, (spans[i]!.start + spans[i]!.end) / 2)[0]"
            :y="point(family === f.value ? 82 : 103, (spans[i]!.start + spans[i]!.end) / 2)[1]"
            :transform="`rotate(${(spans[i]!.start + spans[i]!.end) / 2 + 90},${point(family === f.value ? 82 : 103, (spans[i]!.start + spans[i]!.end) / 2)})`"
            text-anchor="middle"
            dominant-baseline="middle"
            font-size="13"
            :fill="['SWEET', 'GRAIN'].includes(f.value) ? '#403327' : 'white'"
          >
            {{ f.label }}
          </text>
          <text
            v-if="!family"
            :x="point(164, (spans[i]!.start + spans[i]!.end) / 2)[0]"
            :y="point(164, (spans[i]!.start + spans[i]!.end) / 2)[1]"
            :transform="`rotate(${(spans[i]!.start + spans[i]!.end) / 2 + 90},${point(164, (spans[i]!.start + spans[i]!.end) / 2)})`"
            text-anchor="middle"
            dominant-baseline="middle"
            font-size="8"
            fill="#403327"
          >
            {{ f.english }}
          </text>
        </g>
        <g
          v-for="c in categorySpans"
          :key="c.value"
          role="button"
          tabindex="0"
          :aria-label="c.label + ' 하위 향미 펼치기'"
          :aria-pressed="category === c.value"
          @click="clickCategory(c.value, $event)"
          @keydown.enter="category = c.value"
          @keydown.space.prevent="category = c.value"
        >
          <path
            :d="wedge(111, 145, c.start, c.end)"
            :fill="c.color"
            :opacity="category === c.value ? 1 : 0.6"
            stroke="white"
            stroke-width="2"
          />
          <text
            :x="point(128, (c.start + c.end) / 2)[0]"
            :y="point(128, (c.start + c.end) / 2)[1]"
            :transform="`rotate(${(c.start + c.end) / 2 + 90},${point(128, (c.start + c.end) / 2)})`"
            text-anchor="middle"
            dominant-baseline="middle"
            font-size="8"
            fill="#403327"
          >
            {{ c.label }}
          </text>
          <g
            v-for="(n, j) in notesFor(c.value)"
            :key="n.value"
            role="button"
            tabindex="0"
            :aria-label="n.label + ' 향미 선택'"
            :aria-pressed="model.includes(n.value)"
            @click.stop="chooseNote(c.value, n.value)"
            @keydown.enter.stop="chooseNote(c.value, n.value)"
            @keydown.space.stop.prevent="chooseNote(c.value, n.value)"
          >
            <path
              :d="
                wedge(
                  147,
                  182,
                  c.start + ((c.end - c.start) * j) / notesFor(c.value).length,
                  c.start + ((c.end - c.start) * (j + 1)) / notesFor(c.value).length,
                )
              "
              :fill="n.color"
              :opacity="category === c.value || model.includes(n.value) ? 1 : 0.35"
              stroke="white"
              stroke-width="1"
            />
            <path
              v-if="model.includes(n.value)"
              :d="
                wedge(
                  182,
                  187,
                  c.start + ((c.end - c.start) * j) / notesFor(c.value).length,
                  c.start + ((c.end - c.start) * (j + 1)) / notesFor(c.value).length,
                )
              "
              fill="#6f3f28"
            />
            <text
              v-if="category === c.value && notesFor(c.value).length < 7"
              :x="
                point(165, c.start + ((c.end - c.start) * (j + 0.5)) / notesFor(c.value).length)[0]
              "
              :y="
                point(165, c.start + ((c.end - c.start) * (j + 0.5)) / notesFor(c.value).length)[1]
              "
              :transform="`rotate(${c.start + ((c.end - c.start) * (j + 0.5)) / notesFor(c.value).length},${point(165, c.start + ((c.end - c.start) * (j + 0.5)) / notesFor(c.value).length)})`"
              text-anchor="middle"
              dominant-baseline="middle"
              font-size="7"
              fill="#312a22"
              pointer-events="none"
            >
              {{ n.label }}
            </text>
          </g>
        </g>
        <circle cx="210" cy="210" r="52" fill="#fffdf9" stroke="#e4d7c7" />
        <circle cx="210" cy="210" r="48" fill="none" stroke="#e4d7c7" />
        <text x="210" y="194" text-anchor="middle" font-size="8" letter-spacing="3" fill="#716255">
          BLANK
        </text>
        <text
          x="210"
          y="215"
          text-anchor="middle"
          font-size="14"
          font-weight="700"
          :fill="activeFamily?.color || '#302a25'"
        >
          {{ activeFamily?.label || 'FLAVOR' }}
        </text>
        <text x="210" y="234" text-anchor="middle" font-size="8" fill="#716255">
          {{ family ? '선택 향미 ' + model.length : 'WHEEL' }}
        </text>
      </svg>
      <div class="flavor-notes">
        <h3>
          {{
            activeFamily
              ? activeFamily.label + ' / ' + categories.find((c) => c.value === category)?.label
              : '향미를 찾아보세요'
          }}
        </h3>
        <p>
          {{ activeFamily ? '세부 향미를 선택하세요.' : '익숙한 큰 향미부터 가볍게 시작하세요.' }}
        </p>
        <div v-if="activeFamily" class="chips">
          <button
            v-for="f in notes"
            :key="f.value"
            type="button"
            :aria-pressed="model.includes(f.value)"
            :class="{ selected: model.includes(f.value) }"
            :style="{ borderColor: f.color }"
            @click="toggle(f.value)"
          >
            {{ f.label }}
          </button>
        </div>
        <p v-else class="hint">상위 → 향미 계열 → 세부 향미</p>
        <small>휠 또는 버튼으로 선택하세요. 다시 누르면 해제됩니다.</small>
      </div>
    </div>
    <slot name="custom-input" />
    <div class="selected-flavors">
      <div>
        <strong>선택한 향미</strong><small>{{ model.length }} / 8</small>
      </div>
      <div v-if="model.length" class="chips chosen">
        <button v-for="v in model" :key="v" type="button" @click="toggle(v)">
          {{ options.flavors.find((f) => f.value === v)?.label }} ×
        </button>
      </div>
    </div>
  </div>
</template>
