<script setup lang="ts">
const model = defineModel<string>()
</script>
<template>
  <div class="temperature" role="group" aria-label="추출 타입">
    <button
      v-for="t in ['HOT', 'ICE']"
      :key="t"
      type="button"
      :class="[t.toLowerCase(), { selected: model === t }]"
      :aria-pressed="model === t"
      @click="model = t"
    >
      {{ model === t ? '✓ ' : '' }}{{ t }}
    </button>
  </div>
</template>
