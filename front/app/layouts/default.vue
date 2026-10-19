<script setup lang="ts">
const route = useRoute()
useHead({ htmlAttrs: { class: computed(() => (route.path === '/' ? 'home-snap' : '')) } })
const nav = [
  { to: '/', label: '홈' },
  { to: '/coffee-beans', label: '원두' },
  { to: '/brew-records', label: '브루잉' },
  { to: '/cafe-filter-coffees', label: '카페' },
  { to: '/cafes/map', label: '지도' },
]
const { data: session } = await useAsyncData('session', () =>
  useBackend()
    .read('front/session')
    .catch(() => null),
)
async function logout() {
  const s: any = await $fetch('/api/backend/front/session')
  await $fetch('/auth/logout', {
    method: 'POST',
    headers: s.csrf ? { [s.csrf.headerName]: s.csrf.token } : {},
  })
  window.location.assign('/login')
}
</script>
<template>
  <a href="#main" class="skip-link">본문으로 이동</a>
  <header v-if="route.path !== '/login'" class="header">
    <div class="header-inner">
      <NuxtLink to="/" class="brand"
        ><img src="/img/blank-logo.svg" alt="" /><span>BLANK</span></NuxtLink
      >
      <nav aria-label="주요 메뉴" class="desktop-nav">
        <NuxtLink
          v-for="n in nav"
          :key="n.to"
          :to="n.to"
          :class="{ active: n.to === '/' ? route.path === '/' : route.path.startsWith(n.to) }"
          >{{ n.label }}</NuxtLink
        >
      </nav>
      <div class="account">
        <template v-if="session?.user"
          ><span>{{ session.user.nickname }}</span
          ><button class="link-button" @click="logout">로그아웃</button></template
        ><NuxtLink v-else to="/login">로그인 ↗</NuxtLink>
      </div>
    </div>
  </header>
  <main id="main"><slot /></main>
  <AppFooter v-if="route.path !== '/' && route.path !== '/login'" />
  <nav v-if="route.path !== '/login'" aria-label="하단 주요 메뉴" class="mobile-nav">
    <NuxtLink v-for="n in nav" :key="n.to" :to="n.to">{{ n.label }}</NuxtLink>
  </nav>
</template>
