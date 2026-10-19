<script setup lang="ts">
useHead({ title: '로그인 · BLANK' })
const route = useRoute()
</script>
<template>
  <div class="login-page">
    <section class="login-card">
      <header class="login-brand">
        <img src="/img/blank-logo.svg" alt="" />
        <div><strong>BLANK</strong><small>COFFEE JOURNAL</small></div>
      </header>
      <div class="login-content">
        <span class="eyebrow">WELCOME BACK</span>
        <h1>커피 기록을<br />이어가세요.</h1>
        <p>오늘의 원두와 브루잉 경험을<br />한곳에서 차분히 쌓아보세요.</p>
        <a class="kakao-login" href="/auth/oauth2/authorization/kakao">● &nbsp; 카카오 로그인</a>
        <p class="login-hint">카카오 계정으로 간편하게 로그인합니다.</p>
        <p v-if="route.query.error" class="error-text" role="alert">
          로그인에 실패했습니다. 다시 시도해주세요.
        </p>
        <NuxtLink to="/" class="login-home"><span>나만의 커피 기록</span></NuxtLink>
      </div>
    </section>
  </div>
</template>
