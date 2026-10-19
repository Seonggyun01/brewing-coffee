export default defineNuxtConfig({
  compatibilityDate: '2026-10-14',
  ssr: true,
  devtools: { enabled: false },
  css: ['~/assets/css/main.css', '~/assets/css/canvas.css'],
  runtimeConfig: { backendUrl: 'http://127.0.0.1:8080' },
  app: {
    head: {
      htmlAttrs: { lang: 'ko' },
      title: 'BLANK · 나의 커피 기록',
      meta: [
        { name: 'description', content: '사진 한 장으로 시작하는 원두와 브루잉, 카페의 기록.' },
      ],
    },
  },
  routeRules: { '/api/**': { headers: { 'cache-control': 'no-store' } } },
})
