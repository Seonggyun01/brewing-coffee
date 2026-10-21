<script setup lang="ts">
const props = defineProps<{ demoStage?: number; demoPhase?: 'upload' | 'read' | 'parse' }>()
const emit = defineEmits<{ parsed: [result: any]; busy: [value: boolean] }>()
const parsed = ref(false)
const fileName = ref('')
const previewFailed = ref(false)
const showPhoto = ref(false)
const inputEl = ref<HTMLInputElement>()
const busy = ref(false),
  phase = ref('OCR'),
  error = ref(''),
  preview = ref(''),
  warning = ref<string[]>([])
watch(
  () => [props.demoStage, props.demoPhase] as const,
  ([value, demoPhase]) => {
    if (value == null) return
    preview.value = '/img/bean-card.png'
    fileName.value = '원두 카드.png'
    busy.value = value === 0 && demoPhase !== 'upload'
    parsed.value = value > 0
    phase.value = demoPhase === 'parse' ? 'PARSING' : 'OCR'
  },
  { immediate: true },
)
const waitingSeconds = ref(0)
let waitingTimer: ReturnType<typeof setInterval> | undefined
watch(
  busy,
  (value) => {
    clearInterval(waitingTimer)
    waitingSeconds.value = 0
    if (value) waitingTimer = setInterval(() => waitingSeconds.value++, 1000)
  },
  { immediate: true },
)
let events: EventSource | undefined, aborter: AbortController | undefined
let cancelled = false
function manualEntry() {
  cancelled = true
  events?.close()
  aborter?.abort()
  busy.value = false
  emit('busy', false)
}
const label = computed(() =>
  phase.value === 'CONVERTING'
    ? '사진을 표시할 수 있도록 변환하고 있어요'
    : phase.value === 'OCR'
      ? '사진에서 글자를 읽고 있어요'
      : phase.value === 'PARSING'
        ? '원두 정보를 폼에 정리하고 있어요'
        : 'AI가 원두 정보를 확인하고 있어요',
)
async function upload(event: Event) {
  if (props.demoStage != null) return
  const input = event.target as HTMLInputElement,
    file = input.files?.[0]
  if (!file) return
  if (file.size > 20 * 1024 * 1024) {
    error.value = '사진은 20MB 이하로 선택해주세요.'
    input.value = ''
    return
  }
  if (preview.value.startsWith('blob:')) URL.revokeObjectURL(preview.value)
  const needsConversion = /image\/(heic|heif)/i.test(file.type) || /\.hei[cf]$/i.test(file.name)
  preview.value = needsConversion ? '' : URL.createObjectURL(file)
  cancelled = false
  busy.value = true
  emit('busy', true)
  parsed.value = false
  fileName.value = file.name
  error.value = ''
  previewFailed.value = false
  phase.value = needsConversion ? 'CONVERTING' : 'OCR'
  const id = crypto.randomUUID(),
    body = new FormData()
  body.append('image', file)
  body.append('extractionProgressId', id)
  aborter = new AbortController()
  try {
    const session: any = await $fetch('/api/backend/front/session', {
      signal: aborter.signal,
      timeout: 15000,
    })
    if (needsConversion) {
      const previewBody = new FormData()
      previewBody.append('image', file)
      const blob = await $fetch<Blob>('/api/backend/front/image-preview', {
        method: 'POST',
        body: previewBody,
        responseType: 'blob',
        timeout: 60000,
        signal: aborter.signal,
        headers: session.csrf ? { [session.csrf.headerName]: session.csrf.token } : {},
      })
      if (cancelled) return
      preview.value = URL.createObjectURL(blob)
      previewFailed.value = false
      // Reuse the converted image; the OCR request no longer converts HEIC again.
      body.set(
        'image',
        new File([blob], file.name.replace(/\.hei[cf]$/i, '') + '.png', { type: 'image/png' }),
      )
      await nextTick()
    }
    if (cancelled) return
    phase.value = 'OCR'
    events = new EventSource(`/api/backend/coffee-bean-card-extractions/${id}/events`)
    events.addEventListener('progress', (e) => {
      const p = JSON.parse((e as MessageEvent).data).phase
      if (p === 'OCR' || p === 'LLM' || p === 'PARSING') phase.value = p
    })
    const result: any = await $fetch('/api/backend/front/extraction', {
      method: 'POST',
      body,
      signal: aborter.signal,
      timeout: 180000,
      headers: session.csrf ? { [session.csrf.headerName]: session.csrf.token } : {},
    })
    warning.value = result.warnings || []
    parsed.value = true
    emit('parsed', result)
  } catch (e: any) {
    if (!cancelled)
      error.value =
        e.name === 'TimeoutError' || e.cause?.name === 'TimeoutError'
          ? '분석 응답이 제한 시간 안에 도착하지 않았어요. 잠시 후 다시 시도하거나 직접 입력해주세요.'
          : errorMessage(e)
  } finally {
    busy.value = false
    emit('busy', false)
    events?.close()
    input.value = ''
  }
}
onBeforeUnmount(() => {
  clearInterval(waitingTimer)
  events?.close()
  aborter?.abort()
  if (preview.value.startsWith('blob:')) URL.revokeObjectURL(preview.value)
})
</script>
<template>
  <section :class="['photo-upload', { 'photo-upload--parsed': parsed }]">
    <template v-if="!parsed"
      ><div>
        <span class="eyebrow">PHOTO ASSIST</span>
        <h2>원두 카드 사진으로 자동 입력</h2>
        <p>사진에서 원두 정보를 읽어 입력해드려요. 저장 전 직접 확인할 수 있어요.</p>
        <div class="upload-row">
          <button type="button" class="button secondary" :disabled="busy" @click="inputEl?.click()">
            {{ busy ? '분석 중…' : '원두 사진 선택' }}</button
          ><span>{{ fileName || '선택한 사진 없음' }}</span>
        </div>
      </div></template
    >
    <template v-else
      ><img v-if="preview" :src="preview" alt="선택한 원두 카드" />
      <div>
        <button type="button" class="link-button" @click="showPhoto = true">
          원본 사진 보기 ↗
        </button>
        <p>사진을 확대해서 AI가 읽은 값과 비교할 수 있어요.</p>
      </div></template
    >
    <div v-if="demoPhase === 'upload'" class="demo-drop-zone">
      <span>사진을 여기에 놓아주세요</span>
    </div>
    <input
      ref="inputEl"
      class="visually-hidden"
      type="file"
      accept="image/jpeg,image/png,image/webp,image/heic,image/heif"
      :disabled="busy"
      @change="upload"
    />
    <div v-if="busy" class="analysis-wait-screen" role="status" aria-live="polite">
      <div class="analysis-wait-card">
        <div class="analysis-layout">
          <div class="analysis-photo">
            <img
              v-if="preview && !previewFailed"
              :src="preview"
              alt="분석 중인 원두 카드"
              @error="previewFailed = true"
            />
            <p v-else-if="phase === 'CONVERTING'" class="preview-unavailable">사진 변환 중…</p>
            <p v-else class="preview-unavailable">
              사진 미리보기를 준비하지 못했어요.<br />정보 분석은 계속 진행됩니다.
            </p>
            <span v-if="preview && !previewFailed" class="analysis-scan-line" aria-hidden="true" />
          </div>
          <div class="analysis-copy">
            <span class="eyebrow">PHOTO ASSIST</span>
            <h2>{{ label }}</h2>
            <p>완료되면 읽은 정보를 확인하고 수정할 수 있어요.</p>
            <ol class="analysis-phases">
              <li class="done"><span>✓</span><strong>사진 업로드</strong><small>완료</small></li>
              <li :class="{ active: phase === 'OCR', done: phase !== 'OCR' }">
                <span>{{ phase !== 'OCR' ? '✓' : '2' }}</span
                ><strong>글자 읽기</strong><small>{{ phase === 'OCR' ? '진행 중' : '완료' }}</small>
              </li>
              <li :class="{ active: phase !== 'OCR' }">
                <span>3</span><strong>AI 정보 정리</strong
                ><small>{{ phase === 'OCR' ? '대기' : '진행 중' }}</small>
              </li>
            </ol>
            <p v-if="demoStage == null" class="analysis-elapsed">
              {{ waitingSeconds }}초 경과<span v-if="waitingSeconds >= 30">
                ·
                {{
                  phase === 'LLM' || phase === 'PARSING'
                    ? 'AI 응답을 기다리고 있어요.'
                    : '사진 처리에 시간이 걸리고 있어요.'
                }}</span
              >
            </p>
            <button type="button" class="button secondary" @click="manualEntry">
              직접 입력으로 전환
            </button>
          </div>
        </div>
      </div>
    </div>
    <p v-if="error" class="error-text" role="alert">
      {{ error }} 직접 입력하거나 다시 사진을 선택해주세요.
    </p>
    <p v-for="w in warning" :key="w" class="hint">{{ w }}</p>
    <div
      v-if="showPhoto"
      class="photo-lightbox"
      role="dialog"
      aria-modal="true"
      aria-label="원본 원두 사진"
      @click.self="showPhoto = false"
      @keydown.esc="showPhoto = false"
    >
      <button
        type="button"
        class="modal-close"
        aria-label="원본 사진 닫기"
        autofocus
        @click="showPhoto = false"
      >
        ×</button
      ><img :src="preview" alt="확대한 원본 원두 카드" />
    </div>
  </section>
</template>

<style scoped>
.demo-drop-zone {
  display: grid;
  place-items: center;
  width: 100%;
  height: 385px;
  border: 1px dashed #d6c5b7;
  background: #f7f3ee;
  color: #9c8472;
  margin: 32px auto;
  border-radius: 8px;
}
</style>

<style scoped>
.analysis-wait-screen .analysis-wait-card {
  width: 760px;
  padding: 24px;
  text-align: left;
}
.analysis-layout {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 28px;
  align-items: center;
}
.analysis-photo {
  height: 320px;
  position: relative;
  overflow: hidden;
  border-radius: 8px;
  background: #f4eee6;
  display: grid;
  place-items: center;
}
.analysis-photo img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.analysis-scan-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  top: 0;
  background: #ad8058;
  box-shadow: 0 0 15px 4px #c49a7055;
  animation: photo-scan 2.8s ease-in-out infinite alternate;
}
@keyframes photo-scan {
  from {
    top: 4%;
  }
  to {
    top: 96%;
  }
}
.form-page .analysis-copy h2,
.analysis-copy h2 {
  font-size: 20px;
  line-height: 1.5;
  margin: 10px 0;
}
.analysis-copy > p {
  font-size: 12px;
  line-height: 1.7;
  color: var(--muted);
  margin: 0 0 18px;
}
.analysis-copy .analysis-phases {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin: 0 0 18px;
  padding: 0;
  border-bottom: 0;
}
.analysis-copy .analysis-phases li {
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
  gap: 10px;
}
.analysis-phases strong {
  font-size: 12px;
  font-weight: 500;
}
.analysis-phases small {
  margin-left: auto;
  font-size: 11px;
}
.analysis-copy .button {
  font-size: 12px;
  min-height: 36px;
}
.preview-unavailable {
  font-size: 12px;
  line-height: 1.8;
  text-align: center;
  padding: 16px;
  color: var(--muted);
}
@media (max-width: 600px) {
  .analysis-wait-screen .analysis-wait-card {
    padding: 18px;
  }
  .analysis-layout {
    grid-template-columns: minmax(90px, 0.8fr) minmax(0, 1.2fr);
    gap: 16px;
  }
  .analysis-photo {
    height: 260px;
  }
  .analysis-copy h2,
  .form-page .analysis-copy h2 {
    font-size: 16px;
  }
  .analysis-phases small {
    font-size: 9px;
  }
  .analysis-copy .analysis-phases li {
    gap: 6px;
  }
  .analysis-copy .analysis-phases li > span {
    width: 20px;
    height: 20px;
    flex-shrink: 0;
  }
}
@media (prefers-reduced-motion: reduce) {
  .analysis-scan-line {
    animation: none;
    top: 50%;
  }
}
</style>

<style scoped>
.analysis-copy > .analysis-elapsed {
  font-size: 11px;
  margin: 0 0 12px;
  font-weight: 400;
}
</style>
