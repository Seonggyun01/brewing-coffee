<script setup lang="ts">
const section = ref<HTMLElement>()
const screen = ref<HTMLElement>()
const elapsed = ref(0)
const visible = ref(false)
const reduced = ref(false)
const stage = computed(() => (reduced.value ? 5 : Math.min(5, Math.floor(elapsed.value / 3000))))
const cursorPoints = [
  [34, 45],
  [52, 34],
  [66, 48],
  [38, 26],
  [30, 15],
  [82, 68],
  [34, 45],
] as const
const cursorStyle = computed(() => {
  if (stage.value === 0) {
    const t = Math.min(1, Math.max(0, (elapsed.value - 350) / 1900))
    const progress = t * t * (3 - 2 * t)
    return { left: `${83 - 30 * progress}%`, top: `${82 - 30 * progress}%` }
  }
  const from = cursorPoints[stage.value]!
  const to = cursorPoints[stage.value + 1]!
  const t = (elapsed.value - stage.value * 3000) / (stage.value === 5 ? 6000 : 3000)
  const progress = t * t * (3 - 2 * t)
  return {
    left: `${from[0] + (to[0] - from[0]) * progress}%`,
    top: `${from[1] + (to[1] - from[1]) * progress}%`,
  }
})
const dragPhotoStyle = computed(() => {
  const t = Math.min(1, Math.max(0, (elapsed.value - 350) / 1900))
  const progress = t * t * (3 - 2 * t)
  return {
    left: `${78 - 30 * progress}%`,
    top: `${75 - 30 * progress}%`,
    transform: `translate(-50%, -50%) rotate(${-8 + 8 * progress}deg)`,
    opacity: elapsed.value > 2600 ? 0 : 1,
  }
})
const scenes = [
  { title: '사진 한 장으로', description: '원두 카드 사진을 올려주세요.', phase: 'upload' },
  { title: '글자를 읽고', description: '카드에 적힌 원두 정보를 찾습니다.', phase: 'read' },
  { title: '정보를 정리해요', description: '읽은 내용을 원두 정보로 정리해요.', phase: 'parse' },
  {
    title: '직접 확인하고',
    description: '원본 사진과 비교해 잘못 읽은 값을 고쳐주세요.',
    phase: 'review',
  },
  {
    title: '바로 수정하세요',
    description: 'AI가 읽은 향미를 확인하고 내 표현으로 수정해요.',
    phase: 'edit',
  },
  { title: '내 원두로 저장', description: '사진 한 장이 나의 커피 기록이 됩니다.', phase: 'saved' },
] as const
const scale = ref(0.6)
let resize: ResizeObserver | undefined
let observer: IntersectionObserver | undefined
let frame = 0
let previous = 0
let media: MediaQueryList | undefined
function tick(time: number) {
  if (previous) elapsed.value = (elapsed.value + Math.min(time - previous, 100)) % 21000
  previous = time
  frame = requestAnimationFrame(tick)
}
function sync() {
  cancelAnimationFrame(frame)
  previous = 0
  if (visible.value && !document.hidden && !reduced.value) frame = requestAnimationFrame(tick)
}
function motionChanged() {
  reduced.value = media?.matches ?? false
  sync()
}
onMounted(() => {
  media = window.matchMedia('(prefers-reduced-motion: reduce)')
  reduced.value = media.matches
  media.addEventListener('change', motionChanged)
  document.addEventListener('visibilitychange', sync)
  observer = new IntersectionObserver(
    (entries) => {
      visible.value = entries.some((entry) => entry.isIntersecting)
      sync()
    },
    { threshold: 0.3 },
  )
  if (section.value) observer.observe(section.value)
  resize = new ResizeObserver((entries) => {
    const box = entries[0]?.contentRect
    if (box) scale.value = Math.min((box.width * 0.8) / 860, (box.height - 32) / 850)
  })
  if (screen.value) resize.observe(screen.value)
})
onBeforeUnmount(() => {
  observer?.disconnect()
  resize?.disconnect()
  cancelAnimationFrame(frame)
  media?.removeEventListener('change', motionChanged)
  document.removeEventListener('visibilitychange', sync)
})
</script>
<template>
  <section ref="section" id="home-demo" class="photo-demo">
    <div
      class="native-demo"
      role="img"
      aria-label="원두 사진 업로드, 글자 읽기, 정보 정리, 결과 확인, 직접 수정, 저장까지의 과정"
    >
      <div class="demo-copy" aria-hidden="true">
        <span class="demo-brand">B L A N K</span>
        <span class="demo-eyebrow">{{
          [
            '01 사진 업로드',
            '02 사진 분석',
            '03 AI 정리',
            '04 결과 확인',
            '04 결과 수정',
            '05 저장 완료',
          ][stage]
        }}</span>
        <h2>{{ scenes[stage]!.title }}</h2>
        <p>{{ scenes[stage]!.description }}</p>
        <span class="demo-flow-caption">사진 → AI 입력 → 확인 → 저장</span>
        <div class="demo-progress">
          <span :style="{ width: `${reduced ? 100 : elapsed / 210}%` }" />
        </div>
      </div>
      <div
        ref="screen"
        class="demo-screen"
        :class="`scene-${scenes[stage]!.phase}`"
        inert
        aria-hidden="true"
      >
        <div class="demo-plane" :style="{ transform: `translateX(-50%) scale(${scale})` }">
          <PhotoExtraction
            v-if="stage < 3"
            :demo-stage="0"
            :demo-phase="scenes[stage]!.phase as 'upload' | 'read' | 'parse'"
          />
          <RecordForm v-else-if="stage < 5" kind="beans" :demo-stage="stage === 4 ? 2 : 1" />
          <RecordDetail
            v-else
            kind="beans"
            id="demo"
            :demo-record="{
              name: '콜롬비아 사르사 파카마라 복합 발효 워시드',
              roastery: 'LOFE COFFEE',
              status: 'CURRENT',
              originCountryDisplayName: '콜롬비아',
              farm: 'ZARZA',
              variety: '파카마라',
              processType: '복합 발효 워시드',
              flavorNoteSummary: '체리주스 · 핑크구아바 · 망고스틴 · 밀키스 · 화이트 그레이프 소다',
              flavorGradientStyle: 'linear-gradient(90deg, #cf7274, #ebbc6e, #b8c790)',
            }"
          />
        </div>
        <img
          v-if="stage === 0"
          class="demo-drag-photo"
          src="/img/bean-card.png"
          alt=""
          draggable="false"
          :style="dragPhotoStyle"
        />
        <img
          v-if="!reduced"
          class="demo-cursor"
          src="/img/demo-cursor.svg"
          alt=""
          draggable="false"
          :style="cursorStyle"
        />
      </div>
    </div>
    <a class="section-next" href="#home-final" aria-label="시작하기 화면으로 이동">↓</a>
  </section>
</template>
<style scoped>
.demo-drag-photo {
  position: absolute;
  width: 18%;
  height: auto;
  z-index: 4;
  pointer-events: none;
  border-radius: 5px;
  box-shadow: 0 8px 24px #2a211d30;
  transition: opacity 0.3s;
}
.demo-plane :deep(.detail-screen) {
  min-height: 0;
}
.demo-plane :deep(.detail-overlay) {
  position: absolute;
  inset: 0;
  padding: 0;
  background: white;
  display: block;
}
.demo-plane :deep(.detail-dialog) {
  width: 100%;
  max-width: none;
  max-height: none;
  margin: 0;
  box-shadow: none;
  border: 0;
}
.demo-plane :deep(.detail-dialog-header) {
  display: none;
}
.demo-plane :deep(.detail-scroll) {
  overflow: visible;
  max-height: none;
}
.demo-plane :deep(.detail-page) {
  width: 100%;
  max-width: none;
  padding: 24px;
}
.demo-cursor {
  position: absolute;
  width: 26px;
  height: 33px;
  z-index: 5;
  pointer-events: none;
  filter: drop-shadow(0 2px 3px #2a211d24);
}
.native-demo {
  width: min(1120px, 100%);
  height: min(630px, calc(100svh - 180px));
  display: grid;
  grid-template-columns: 0.8fr 1.6fr;
  gap: 40px;
  align-items: stretch;
  flex: 0 0 auto;
  pointer-events: none;
  user-select: none;
}
.demo-copy {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  padding-top: 72px;
  position: relative;
  padding-bottom: 80px;
  min-width: 0;
}
.demo-eyebrow {
  font-size: 11px;
  letter-spacing: 0.16em;
  color: #9d7e68;
  margin-bottom: 24px;
}
.home-story .photo-demo .demo-copy h2 {
  font-size: 36px;
  line-height: 1.35;
  min-height: 98px;
  width: 100%;
  margin: 0 0 20px;
  text-align: left;
}
.home-story .photo-demo .demo-copy p {
  font-size: 15px;
  line-height: 1.8;
  min-height: 80px;
  width: 100%;
  text-align: left;
  margin: 0;
  color: #8b796c;
}
.demo-timeline {
  position: absolute;
  bottom: 32px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  color: #c5b9ac;
  font-size: 11px;
}
.demo-timeline .active,
.demo-timeline .done {
  color: #6f3f28;
}
.demo-timeline .active {
  font-weight: 700;
}
.demo-progress {
  position: absolute;
  bottom: 16px;
  left: 0;
  right: 0;
  height: 2px;
  background: #e6dbcf;
}
.demo-progress span {
  display: block;
  height: 100%;
  background: #6f3f28;
}
.demo-screen {
  height: 100%;
  position: relative;
  overflow: hidden;
  background: white;
  border: 1px solid #eee6e1;
  border-radius: 10px;
  contain: layout paint;
  overflow-anchor: none;
  box-shadow: 0 16px 50px #63482e0b;
}
.demo-plane {
  position: absolute;
  top: 24px;
  left: 50%;
  width: 860px;
  height: 950px;
  transform-origin: top center;
}
.demo-plane :deep(.form-page) {
  width: 100%;
  max-width: none;
  padding: 24px;
  margin: 0;
}
.demo-plane :deep(.form-page > .page-heading),
.demo-plane :deep(.form-page > .photo-upload--parsed),
.demo-plane :deep(.stepper),
.demo-plane :deep(.initial-form-panel > .section-heading) {
  display: none;
}
.demo-plane :deep(.initial-form-panel) {
  border: 0;
  margin: 0;
  padding: 0;
}
.demo-plane :deep(.form-grid) {
  grid-template-columns: 1fr 1fr;
}
.demo-plane :deep(.form-grid .field) {
  min-width: 0;
}
.demo-plane :deep(.form-actions) {
  margin-top: 24px;
}
.demo-plane :deep(.analysis-wait-screen) {
  position: absolute;
  inset: 0;
  background: white;
  padding: 24px;
}
.demo-plane :deep(.analysis-wait-card) {
  box-shadow: none;
  width: 100%;
  max-width: none;
}
.demo-plane :deep(.photo-upload) {
  margin: 24px;
  padding: 28px;
}
.demo-plane :deep(.photo-upload h2) {
  font-size: 24px;
}
.demo-plane :deep(.analysis-preview) {
  grid-template-columns: 260px 1fr;
}
.demo-plane :deep(.analysis-preview img) {
  max-height: 360px;
}
.demo-plane :deep(.photo-lightbox) {
  display: none;
}
.scene-upload .demo-plane :deep(.photo-upload) {
  display: flex;
  flex-direction: column;
  align-items: stretch;
}
.scene-upload .demo-plane :deep(.photo-upload > div:first-child),
.scene-upload .demo-plane :deep(.photo-upload h2),
.scene-upload .demo-plane :deep(.photo-upload p) {
  text-align: center !important;
}
.scene-upload .demo-plane :deep(.upload-row) {
  justify-content: center;
}
.scene-edit :deep(input#customFlavorNotesText) {
  border-color: #a47758;
  background: #fffaf4;
  box-shadow: 0 0 0 3px #a477581a;
  transition: background 0.5s;
}
.demo-saved-screen {
  padding: 40px 28px;
}
.home-story .photo-demo .saved-notice {
  font-size: 18px;
  color: #627653;
  margin-bottom: 30px;
}
.demo-saved-screen .record-card {
  max-width: none;
}
.native-demo {
  position: relative;
  width: min(1280px, 100%, calc((100svh - 120px) * 16 / 9));
  height: auto;
  aspect-ratio: 16 / 9;
  display: block;
  container-type: inline-size;
}
.demo-copy {
  display: contents;
}
.demo-brand {
  position: absolute;
  left: 5%;
  top: 7.04%;
  font-size: 1.25cqw;
  font-weight: 700;
  color: #6f3f28;
}
.demo-eyebrow {
  position: absolute;
  left: 5%;
  top: 23.33%;
  font-size: 0.938cqw;
  letter-spacing: 0;
  margin: 0;
}
.home-story .photo-demo .demo-copy h2 {
  position: absolute;
  left: 5%;
  top: 29.26%;
  width: 31.77%;
  font-size: 3.54cqw;
  min-height: 0;
  margin: 0;
  text-align: left !important;
  line-height: 1.18;
}
.home-story .photo-demo .demo-copy p {
  position: absolute;
  left: 5%;
  top: 38.7%;
  width: 29.43%;
  font-size: 1.406cqw;
  font-weight: 400;
  min-height: 0;
  margin: 0;
  text-align: left !important;
  line-height: 1.6;
}
.demo-flow-caption {
  position: absolute;
  left: 5%;
  top: 83.15%;
  font-size: 0.938cqw;
  color: #7a6b63;
}
.demo-progress {
  position: absolute;
  left: 5%;
  top: 88.52%;
  right: auto;
  bottom: auto;
  width: 29.69%;
  height: 2px;
}
.demo-screen {
  position: absolute;
  left: 39.375%;
  top: 15.37%;
  width: 56.25%;
  height: 71.3%;
  border-color: #e5dad0;
  border-radius: 12px;
}
.demo-plane :deep(label),
.demo-plane :deep(.flavor-section h3),
.demo-plane :deep(.hint) {
  text-align: left !important;
}
@media (max-width: 700px) {
  .native-demo {
    display: grid;
    aspect-ratio: auto;
    height: calc(100svh - 160px);
    grid-template-columns: 1fr;
    grid-template-rows: 130px minmax(0, 1fr);
    gap: 16px;
    width: min(540px, 100%);
  }
  .demo-copy {
    display: flex;
    position: relative;
    padding: 0 0 32px;
    justify-content: start;
  }
  .demo-brand {
    display: none;
  }
  .demo-eyebrow {
    position: static;
    margin-bottom: 8px;
    font-size: 9px;
  }
  .home-story .photo-demo .demo-copy h2 {
    position: static;
    width: 100%;
    font-size: 25px;
    min-height: 34px;
    margin-bottom: 6px;
  }
  .home-story .photo-demo .demo-copy p {
    position: static;
    width: 100%;
    font-size: 12px;
    min-height: 24px;
  }
  .demo-timeline {
    bottom: 13px;
  }
  .demo-progress {
    bottom: 4px;
  }
  .demo-plane {
    top: 12px;
  }
  .demo-flow-caption {
    display: none;
  }
  .demo-progress {
    top: auto;
    bottom: 4px;
    width: 100%;
    left: 0;
  }
  .demo-screen {
    position: relative;
    left: auto;
    top: auto;
    width: 100%;
  }
}
@media (prefers-reduced-motion: reduce) {
  * {
    transition: none !important;
  }
}
</style>
