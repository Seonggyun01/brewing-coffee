<script setup lang="ts">
import { tasteAxes } from '~/utils/forms'
const props = defineProps<{ model: Record<string, any>; editable?: boolean }>()
const points = (scale: number) =>
  tasteAxes
    .map((axis, i) => {
      const angle = ((i * 60 - 90) * Math.PI) / 180
      const r = scale * 92
      return `${140 + Math.cos(angle) * r},${140 + Math.sin(angle) * r}`
    })
    .join(' ')
const scores = computed(() =>
  tasteAxes
    .map((axis, i) => {
      const angle = ((i * 60 - 90) * Math.PI) / 180
      const r = (Number(props.model[axis.key] || 0) / 5) * 92
      return `${140 + Math.cos(angle) * r},${140 + Math.sin(angle) * r}`
    })
    .join(' '),
)
</script>
<template>
  <div class="taste-profile">
    <svg
      viewBox="0 0 280 280"
      role="img"
      :aria-label="tasteAxes.map((a) => `${a.label} ${model[a.key] || '미입력'}`).join(', ')"
    >
      <polygon v-for="n in 5" :key="n" :points="points(n / 5)" fill="none" stroke="#dfd8d0" />
      <line
        v-for="(a, i) in tasteAxes"
        :key="a.key"
        x1="140"
        y1="140"
        :x2="140 + Math.cos(((i * 60 - 90) * Math.PI) / 180) * 92"
        :y2="140 + Math.sin(((i * 60 - 90) * Math.PI) / 180) * 92"
        stroke="#dfd8d0"
      />
      <polygon :points="scores" fill="#a66a4440" stroke="#98613e" stroke-width="2" />
      <text
        v-for="(a, i) in tasteAxes"
        :key="a.key"
        :x="140 + Math.cos(((i * 60 - 90) * Math.PI) / 180) * 119"
        :y="145 + Math.sin(((i * 60 - 90) * Math.PI) / 180) * 119"
        text-anchor="middle"
        font-size="12"
        fill="#61574e"
      >
        {{ a.label }}
      </text>
    </svg>
    <div v-if="editable" class="taste-controls">
      <div v-for="axis in tasteAxes" :key="axis.key" class="score-row">
        <span>{{ axis.label }}</span>
        <div role="group" :aria-label="axis.label">
          <button
            v-for="n in 5"
            :key="n"
            type="button"
            :class="{ selected: Number(model[axis.key]) === n }"
            :aria-pressed="Number(model[axis.key]) === n"
            @click="model[axis.key] = Number(model[axis.key]) === n ? '' : n"
          >
            {{ n }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
