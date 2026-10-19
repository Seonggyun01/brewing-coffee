<script setup lang="ts">
import options from '~/utils/options.json'
defineProps<{ record: any; kind: 'beans' | 'brews' | 'cafes' }>()
const routes = { beans: '/coffee-beans', brews: '/brew-records', cafes: '/cafe-filter-coffees' }
const method = (v: string) => options.methods.find((m) => m.value === v)?.label || v
</script>
<template>
  <NuxtLink :to="`${routes[kind]}/${record.id}`" :class="['record-card', `record-card--${kind}`]">
    <div
      class="flavor-bar"
      :style="{
        background: (record.flavorGradientStyle || '#DED4C8')
          .replace(/^background:\s*/, '')
          .replace(/;$/, ''),
      }"
    />
    <template v-if="kind === 'beans'">
      <h2>{{ record.name }}</h2>
      <div class="bean-card-meta">
        <strong>{{
          record.status === 'CAFE' ? '무게 미적용' : `${record.weight ?? '—'}g 남음`
        }}</strong
        ><span>{{ record.roastery || '로스터리 미입력' }}</span>
      </div>
    </template>
    <template v-else-if="kind === 'brews'">
      <span class="card-date">{{ record.brewedDate || '날짜 미입력' }}</span>
      <h2>{{ record.coffeeBeanName }}</h2>
      <div class="brew-card-profile">
        <TasteProfile :model="record" />
        <div class="card-rating">
          <span
            v-if="record.temperatureType"
            :class="['temperature-badge', record.temperatureType.toLowerCase()]"
            >{{ record.temperatureType }}</span
          ><small>{{ method(record.brewMethod) }}</small
          ><span class="rating-stars" aria-hidden="true"
            >{{ '★'.repeat(Math.round(record.rating || 0))
            }}{{ '☆'.repeat(5 - Math.round(record.rating || 0)) }}</span
          ><strong>{{ record.rating ? Number(record.rating).toFixed(1) : '—' }}</strong>
        </div>
      </div>
    </template>
    <template v-else>
      <span class="card-date">{{ record.purchasePlaceName || '카페 미입력' }}</span>
      <h2>{{ record.coffeeBeanName }}</h2>
      <div class="cafe-card-meta">
        <span>{{ record.brewedDate || '날짜 미입력' }}</span
        ><span>{{ record.temperatureType }}</span
        ><span>{{ record.rating ? Number(record.rating).toFixed(1) : '—' }}점</span>
      </div>
      <p>{{ record.memo || record.flavorNoteSummary || '아직 메모가 없어요.' }}</p>
    </template>
  </NuxtLink>
</template>
