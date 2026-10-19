<script setup lang="ts">
useHomePaging()
const { data: session } = useNuxtData<any>('session')
const startLink = computed(() =>
  session.value?.securityEnabled && !session.value?.user ? '/login' : '/coffee-beans/new',
)
</script>
<template>
  <div class="home-story">
    <OriginMap id="home-origin" />
    <PhotoDemo />
    <section id="home-final" class="home-final">
      <section class="start-section">
        <h2>오늘의 한 잔부터, 나의 취향까지.</h2>
        <p>원두를 등록하고, 커피의 이야기를 차곡차곡 쌓아보세요.</p>
        <NuxtLink :to="startLink" class="button large">지금 바로 시작하기 →</NuxtLink>
        <span class="final-arrow" aria-hidden="true">↓</span>
      </section>
      <AppFooter />
    </section>
  </div>
</template>
