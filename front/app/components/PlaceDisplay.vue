<script setup lang="ts">
const props = defineProps<{ latitude: number; longitude: number }>(),
  el = ref<HTMLElement>(),
  error = ref('')
onMounted(async () => {
  try {
    const maps = await useKakao(),
      center = new maps.LatLng(props.latitude, props.longitude),
      map = new maps.Map(el.value, { center, level: 3 })
    new maps.Marker({ position: center, map })
  } catch (e: any) {
    error.value = e.message
  }
})
</script>
<template>
  <div ref="el" class="place-map" aria-label="장소 지도" />
  <p v-if="error" class="hint">{{ error }}</p>
</template>
