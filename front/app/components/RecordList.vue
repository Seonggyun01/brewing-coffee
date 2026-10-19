<script setup lang="ts">
const props = defineProps<{ kind: 'beans' | 'brews' | 'cafes' }>()
const route = useRoute(),
  query = ref(String(route.query.keyword || route.query.q || '')),
  cafeFilter = ref('')
const routes = { beans: '/coffee-beans', brews: '/brew-records', cafes: '/cafe-filter-coffees' }
const names = { beans: '원두 관리', brews: '브루잉 기록', cafes: '카페 필터커피' }
const { data, error, refresh } = await useAsyncData(
  `list-${props.kind}`,
  () =>
    useBackend().read<any[]>(`front/${props.kind}`, {
      countryCode: route.query.countryCode,
      cafeId: route.query.cafeId,
    }),
  { watch: [() => route.query.countryCode, () => route.query.cafeId] },
)
const filtered = computed(() =>
  (data.value || []).filter(
    (r: any) =>
      (!cafeFilter.value || r.purchasePlaceName === cafeFilter.value) &&
      [r.name, r.coffeeBeanName, r.roastery, r.purchasePlaceName, r.flavorNoteSummary, r.memo]
        .join(' ')
        .toLowerCase()
        .includes(query.value.toLowerCase()),
  ),
)
const groups = [
  { value: 'CURRENT', label: '보유중', hint: '현재 가지고 있는 원두' },
  { value: 'FINISHED', label: '모두 소진', hint: '다 마시고 기록으로 남긴 원두' },
  { value: 'CAFE', label: '카페 시식', hint: '카페에서 마셔본 원두' },
]
const cafeNames = computed(
  () =>
    [
      ...new Set((data.value || []).map((r: any) => r.purchasePlaceName).filter(Boolean)),
    ] as string[],
)
function scrollGroup(value: string) {
  document
    .getElementById(`bean-group-${value}`)
    ?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>
<template>
  <div :class="['page record-list', `record-list--${kind}`]">
    <div class="page-heading">
      <div>
        <span class="eyebrow">BLANK</span>
        <h1>{{ names[kind] }}</h1>
      </div>
      <div class="actions">
        <NuxtLink v-if="kind === 'beans'" to="/cafe-filter-coffees/new" class="button secondary"
          >카페 원두 기록</NuxtLink
        ><NuxtLink :to="`${routes[kind]}/new`" class="button">{{
          kind === 'beans' ? '새 원두 등록' : kind === 'brews' ? '새 기록' : '기록하기'
        }}</NuxtLink>
      </div>
    </div>
    <TasteSpace v-if="kind === 'beans'" />
    <div v-if="kind !== 'brews'" class="list-toolbar">
      <input
        v-model="query"
        class="list-search"
        type="search"
        aria-label="기록 검색"
        :placeholder="kind === 'beans' ? '원두 이름으로 검색' : '카페, 원두, 메모로 검색'"
      /><button class="button secondary" type="button" @click="refresh()">검색</button
      ><button v-if="kind === 'beans'" class="button secondary" @click="query = ''">초기화</button>
    </div>
    <div v-if="kind === 'beans'" class="tabs bean-group-tabs" role="group" aria-label="원두 상태">
      <button v-for="g in groups" :key="g.value" @click="scrollGroup(g.value)">
        {{ g.label }} {{ (data || []).filter((r: any) => r.status === g.value).length }}
      </button>
    </div>
    <div v-if="kind === 'cafes'" class="chips cafe-filters" role="group" aria-label="카페 필터">
      <button
        :class="{ selected: !cafeFilter }"
        :aria-pressed="!cafeFilter"
        @click="cafeFilter = ''"
      >
        전체</button
      ><button
        v-for="cafe in cafeNames"
        :key="cafe"
        :class="{ selected: cafeFilter === cafe }"
        :aria-pressed="cafeFilter === cafe"
        @click="cafeFilter = cafe"
      >
        {{ cafe }}
      </button>
    </div>
    <p v-if="route.query.countryCode" class="hint">
      {{ route.query.countryCode }} 국가의 원두 <NuxtLink to="/coffee-beans">필터 해제 ×</NuxtLink>
    </p>
    <ApiState :error="error" :empty="!error && !filtered.length" @retry="refresh()" />
    <template v-if="kind === 'beans' && !error && filtered.length"
      ><section v-for="g in groups" :id="`bean-group-${g.value}`" :key="g.value" class="bean-group">
        <p>{{ g.hint }}</p>
        <h2>{{ g.label }}</h2>
        <div :class="['record-grid', { 'cafe-bean-grid': g.value === 'CAFE' }]">
          <RecordCard
            v-for="r in filtered.filter((r: any) => r.status === g.value)"
            :key="r.id"
            :record="r"
            kind="beans"
          />
        </div>
        <p v-if="!filtered.some((r: any) => r.status === g.value)" class="hint">
          아직 이 상태의 원두가 없어요.
        </p>
      </section></template
    >
    <div v-else-if="kind !== 'beans'" class="record-grid">
      <RecordCard v-for="r in filtered" :key="r.id" :record="r" :kind="kind" />
    </div>
  </div>
</template>
