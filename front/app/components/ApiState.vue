<script setup lang="ts">
defineProps<{ error?: any; empty?: boolean }>()
defineEmits(['retry'])
</script>
<template>
  <div v-if="error" class="state-box" role="alert">
    <h2>잠시 연결이 어려워요</h2>
    <p>{{ errorMessage(error) }}</p>
    <div class="actions">
      <button class="button" @click="$emit('retry')">다시 시도</button
      ><NuxtLink class="button secondary" to="/login">로그인</NuxtLink>
    </div>
  </div>
  <div v-else-if="empty" class="state-box">
    <span class="eyebrow">YOUR FIRST CUP</span>
    <h2>첫 번째 기록을 기다리고 있어요</h2>
    <p>오늘 마신 커피부터 천천히 남겨보세요.</p>
  </div>
</template>
