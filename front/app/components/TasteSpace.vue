<script setup lang="ts">
const { data: session } = useNuxtData<any>('session')
const api = useBackend(),
  selected = ref<any>(),
  hovered = ref<any>(),
  rotating = ref(true),
  hoverPosition = reactive({ x: 0, y: 0 }),
  busy = ref(false),
  message = ref(''),
  root = ref<HTMLElement>(),
  mode = ref('space')
const { data, error, refresh } = await useAsyncData('taste-space', () =>
  session.value?.user ? api.read('taste-space') : Promise.resolve(null),
)
let cleanup: (() => void) | undefined
let resetCamera: (() => void) | undefined
async function rebuild() {
  busy.value = true
  message.value = ''
  try {
    data.value = await api.write('taste-space/rebuild', 'POST')
    if (mode.value === 'space') await render()
  } catch (e) {
    message.value = errorMessage(e)
  } finally {
    busy.value = false
  }
}
async function render() {
  await nextTick()
  cleanup?.()
  if (!root.value || mode.value !== 'space') return
  const threePath = '/vendor/three/three.module.js',
    controlsPath = '/vendor/three/addons/controls/OrbitControls.js'
  try {
    const THREE = await import(/* @vite-ignore */ threePath),
      { OrbitControls } = await import(/* @vite-ignore */ controlsPath)
    const el = root.value,
      scene = new THREE.Scene()
    scene.background = new THREE.Color('#10182b')
    scene.fog = new THREE.FogExp2('#10182b', 0.016)
    const camera = new THREE.PerspectiveCamera(38, el.clientWidth / el.clientHeight, 0.1, 100),
      renderer = new THREE.WebGLRenderer({ antialias: true })
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    renderer.setSize(el.clientWidth, el.clientHeight)
    el.appendChild(renderer.domElement)
    camera.position.set(3.5, 2.5, 5)
    const controls = new OrbitControls(camera, renderer.domElement)
    controls.enableDamping = true
    controls.minDistance = 2.5
    controls.maxDistance = 12
    controls.enablePan = false
    controls.autoRotateSpeed = 0.3
    const motion = window.matchMedia('(prefers-reduced-motion: reduce)')
    let interacting = false,
      resumeAt = 0
    const interactionStart = () => {
      interacting = true
    }
    const interactionEnd = () => {
      interacting = false
      resumeAt = performance.now() + 1800
    }
    controls.addEventListener('start', interactionStart)
    controls.addEventListener('end', interactionEnd)
    resetCamera = () => {
      camera.position.set(3.5, 2.5, 5)
      controls.target.set(0, 0, 0)
      controls.update()
    }
    // Distant stars stay visually quieter than the user's bean planets.
    let seed = 37
    const random = () => {
      seed = (seed * 16807) % 2147483647
      return (seed - 1) / 2147483646
    }
    const positions: number[] = [],
      starColors: number[] = []
    for (let i = 0; i < 1600; i++) {
      const angle = random() * Math.PI * 2,
        distance = 9 + random() * 24
      const elevation = (random() - 0.5) * Math.PI
      positions.push(
        Math.cos(angle) * Math.cos(elevation) * distance,
        Math.sin(elevation) * distance,
        Math.sin(angle) * Math.cos(elevation) * distance,
      )
      const brightness = 0.3 + random() * 0.6
      starColors.push(brightness * 0.8, brightness * 0.86, brightness)
    }
    const starGeometry = new THREE.BufferGeometry()
    starGeometry.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3))
    starGeometry.setAttribute('color', new THREE.Float32BufferAttribute(starColors, 3))
    const stars = new THREE.Points(
      starGeometry,
      new THREE.PointsMaterial({
        size: 0.034,
        vertexColors: true,
        transparent: true,
        opacity: 0.8,
        depthWrite: false,
      }),
    )
    scene.add(stars)
    // A distant spiral dust field, separate from the actual bean coordinates.
    const dustPositions: number[] = [],
      dustColors: number[] = []
    for (let i = 0; i < 2400; i++) {
      const radius = 1 + Math.pow(random(), 0.65) * 11
      const angle = ((i % 3) * Math.PI * 2) / 3 + radius * 0.46 + (random() - 0.5) * 0.65
      dustPositions.push(Math.cos(angle) * radius, (random() - 0.5) * 0.7, Math.sin(angle) * radius)
      const tint = new THREE.Color(i % 3 === 0 ? '#8677a1' : i % 3 === 1 ? '#638b9a' : '#a18b79')
      dustColors.push(tint.r * 0.8, tint.g * 0.8, tint.b * 0.8)
    }
    const dustGeometry = new THREE.BufferGeometry()
    dustGeometry.setAttribute('position', new THREE.Float32BufferAttribute(dustPositions, 3))
    dustGeometry.setAttribute('color', new THREE.Float32BufferAttribute(dustColors, 3))
    const dust = new THREE.Points(
      dustGeometry,
      new THREE.PointsMaterial({
        size: 0.035,
        vertexColors: true,
        transparent: true,
        opacity: 0.42,
        depthWrite: false,
        blending: THREE.AdditiveBlending,
      }),
    )
    dust.position.set(0, 1, -9)
    dust.rotation.x = 0.4
    scene.add(dust)
    // Soft, sparse dust clouds add depth without competing with the records.
    for (const [x, y, z, color] of [
      [-4, 2, -7, '#66538c'],
      [3, -2, -8, '#386b82'],
      [0, 3, -9, '#926b56'],
    ] as const) {
      const canvas = document.createElement('canvas')
      canvas.width = canvas.height = 128
      const ctx = canvas.getContext('2d')!
      const gradient = ctx.createRadialGradient(64, 64, 0, 64, 64, 64)
      gradient.addColorStop(0, color)
      gradient.addColorStop(1, color + '00')
      ctx.fillStyle = gradient
      ctx.fillRect(0, 0, 128, 128)
      const cloud = new THREE.Sprite(
        new THREE.SpriteMaterial({
          map: new THREE.CanvasTexture(canvas),
          transparent: true,
          opacity: 0.48,
          depthWrite: false,
          blending: THREE.AdditiveBlending,
        }),
      )
      cloud.position.set(x, y, z)
      cloud.scale.set(13, 8, 1)
      scene.add(cloud)
    }
    const meshes: any[] = []
    for (const point of data.value?.points || []) {
      const colors = (point.colors?.length ? point.colors : [point.color || '#98613e']).filter(
        (color: string) => /^#[0-9a-f]{6}$/i.test(color),
      )
      if (!colors.length) colors.push('#98613e')
      const planetCanvas = document.createElement('canvas')
      planetCanvas.width = 256
      planetCanvas.height = 128
      const planetCtx = planetCanvas.getContext('2d')!
      const planetGradient = planetCtx.createLinearGradient(0, 0, 256, 100)
      colors.forEach((color: string, i: number) =>
        planetGradient.addColorStop(colors.length === 1 ? 0 : i / (colors.length - 1), color),
      )
      planetCtx.fillStyle = planetGradient
      planetCtx.fillRect(0, 0, 256, 128)
      const texture = new THREE.CanvasTexture(planetCanvas)
      texture.colorSpace = THREE.SRGBColorSpace
      const mesh = new THREE.Mesh(
        new THREE.SphereGeometry(0.115, 32, 24),
        new THREE.MeshStandardMaterial({
          map: texture,
          roughness: 0.55,
          metalness: 0.05,
          emissive: colors[0],
          emissiveIntensity: 0.16,
        }),
      )
      mesh.position.set(point.x, point.y, point.z)
      const glowCanvas = document.createElement('canvas')
      glowCanvas.width = glowCanvas.height = 128
      const ctx = glowCanvas.getContext('2d')!
      const gradient = ctx.createRadialGradient(64, 64, 0, 64, 64, 64)
      gradient.addColorStop(0, colors[0])
      gradient.addColorStop(0.35, colors[Math.floor(colors.length / 2)] + '66')
      gradient.addColorStop(1, colors[colors.length - 1] + '00')
      ctx.fillStyle = gradient
      ctx.fillRect(0, 0, 128, 128)
      const glow = new THREE.Sprite(
        new THREE.SpriteMaterial({
          map: new THREE.CanvasTexture(glowCanvas),
          transparent: true,
          depthWrite: false,
        }),
      )
      glow.position.copy(mesh.position)
      glow.scale.set(0.6, 0.6, 0.6)
      scene.add(glow)
      mesh.userData = point
      meshes.push(mesh)
      scene.add(mesh)
    }
    scene.add(new THREE.AmbientLight('#c3cce4', 1.6))
    const light = new THREE.DirectionalLight('#fff2dc', 3)
    light.position.set(3, 5, 4)
    scene.add(light)
    const linked = new Set<string>()
    for (const mesh of meshes) {
      const other = meshes.find((m) => m.userData.id === mesh.userData.closestBeanId)
      if (!other) continue
      const key = [mesh.userData.id, other.userData.id].sort().join('-')
      if (linked.has(key)) continue
      linked.add(key)
      const line = new THREE.Line(
        new THREE.BufferGeometry().setFromPoints([mesh.position, other.position]),
        new THREE.LineBasicMaterial({
          color: '#a2adc4',
          transparent: true,
          opacity: 0.12,
          depthWrite: false,
        }),
      )
      scene.add(line)
    }
    const raycaster = new THREE.Raycaster(),
      mouse = new THREE.Vector2()
    let pointerInside = false
    const hitPoint = () => {
      raycaster.setFromCamera(mouse, camera)
      return raycaster.intersectObjects(meshes)[0]?.object
    }
    const pointer = (e: PointerEvent) => {
      const rect = renderer.domElement.getBoundingClientRect()
      mouse.set(
        ((e.clientX - rect.left) / rect.width) * 2 - 1,
        (-(e.clientY - rect.top) / rect.height) * 2 + 1,
      )
      pointerInside = true
      hoverPosition.x = Math.max(8, Math.min(rect.width - 210, e.clientX - rect.left + 16))
      hoverPosition.y = Math.max(8, Math.min(rect.height - 75, e.clientY - rect.top - 46))
    }
    const leave = () => {
      pointerInside = false
      hovered.value = null
      renderer.domElement.style.cursor = 'grab'
    }
    let down = { x: 0, y: 0 }
    const pointerDown = (e: PointerEvent) => {
      down = { x: e.clientX, y: e.clientY }
      pointer(e)
    }
    const click = (e: PointerEvent) => {
      if (Math.hypot(e.clientX - down.x, e.clientY - down.y) > 5) return
      pointer(e)
      const hit = hitPoint()
      if (hit) selected.value = hit.userData
    }
    renderer.domElement.addEventListener('pointermove', pointer)
    renderer.domElement.addEventListener('pointerleave', leave)
    renderer.domElement.addEventListener('pointerdown', pointerDown)
    renderer.domElement.addEventListener('pointerup', click)
    const resize = new ResizeObserver(() => {
      camera.aspect = el.clientWidth / el.clientHeight
      camera.updateProjectionMatrix()
      renderer.setSize(el.clientWidth, el.clientHeight)
    })
    resize.observe(el)
    let lastFrame = 0
    renderer.setAnimationLoop((time: number) => {
      const delta = lastFrame ? Math.min((time - lastFrame) / 1000, 0.1) : 0
      lastFrame = time
      controls.autoRotate =
        rotating.value &&
        !motion.matches &&
        !document.hidden &&
        !interacting &&
        !hovered.value &&
        time > resumeAt
      controls.enableDamping = controls.autoRotate || interacting || time < resumeAt
      controls.update(delta)
      const hit = pointerInside ? hitPoint() : null
      hovered.value = hit?.userData || null
      renderer.domElement.style.cursor = hit ? 'pointer' : 'grab'
      for (const mesh of meshes) mesh.scale.setScalar(mesh === hit ? 1.25 : 1)
      renderer.render(scene, camera)
    })
    cleanup = () => {
      resize.disconnect()
      renderer.setAnimationLoop(null)
      hovered.value = null
      renderer.domElement.removeEventListener('pointermove', pointer)
      renderer.domElement.removeEventListener('pointerleave', leave)
      renderer.domElement.removeEventListener('pointerdown', pointerDown)
      renderer.domElement.removeEventListener('pointerup', click)
      controls.removeEventListener('start', interactionStart)
      controls.removeEventListener('end', interactionEnd)
      controls.dispose()
      scene.traverse((o: any) => {
        o.geometry?.dispose()
        if (o.material) {
          o.material.map?.dispose()
          o.material.dispose()
        }
      })
      renderer.dispose()
      renderer.domElement.remove()
    }
  } catch {
    message.value = '이 브라우저에서 3D 보기를 사용할 수 없어요. 원두 목록으로 확인해주세요.'
  }
}
watch(mode, render)
onMounted(render)
onBeforeUnmount(() => cleanup?.())
</script>
<template>
  <section id="taste-space" class="taste-space">
    <div class="taste-space-stage">
      <div class="taste-space-toolbar">
        <small>● 내가 기록한 원두 공간</small>
        <div>
          <button class="button secondary" :aria-pressed="rotating" @click="rotating = !rotating">
            {{ rotating ? '회전 멈춤' : '자동 회전' }}
          </button>
          <button class="button secondary" @click="resetCamera?.()">중심으로</button
          ><button class="button secondary" :disabled="busy || !session?.user" @click="rebuild">
            {{ busy ? '정리 중…' : '다시 계산' }}
          </button>
        </div>
      </div>
      <div
        ref="root"
        class="taste-space-canvas"
        aria-label="마우스로 회전하고 원두 점을 선택하는 3D 취향 공간"
      />
      <p v-if="!data?.points?.length" class="taste-empty">
        {{
          session?.user
            ? '원두와 맛을 기록하면 취향 공간에 나타나요.'
            : '로그인하면 내가 기록한 원두를 볼 수 있어요.'
        }}
      </p>
      <small class="taste-space-caption">드래그해 회전 · 점 위에 마우스를 올려 원두 확인</small>
      <div
        v-if="hovered"
        class="taste-hover-label"
        :style="{ left: `${hoverPosition.x}px`, top: `${hoverPosition.y}px` }"
      >
        <strong>{{ hovered.name }}</strong
        ><small>{{ hovered.flavorNotes?.join(' · ') }}</small>
      </div>
    </div>
    <aside class="taste-analysis">
      <span class="eyebrow">나의 원두 기록</span>
      <h2>나의 취향 분석</h2>
      <p>{{ data?.profile?.description || '원두 기록이 쌓일수록 취향의 연결도 선명해져요.' }}</p>
      <section class="taste-analysis-card">
        <small>취향 해석</small>
        <h3>{{ data?.profile?.title || '나만의 취향을 발견해보세요' }}</h3>
        <p>
          {{
            data?.profile?.dominantNotes?.join(' · ') ||
            '향미와 맛을 기록하며 좋아하는 커피를 찾아보세요.'
          }}
        </p>
      </section>
      <section class="taste-analysis-card">
        <dl>
          <div>
            <dt>선호 향미</dt>
            <dd>{{ data?.profile?.dominantNotes?.join(' · ') || '—' }}</dd>
          </div>
          <div>
            <dt>평균 만족도</dt>
            <dd>
              {{
                data?.profile?.averageRating
                  ? Number(data.profile.averageRating).toFixed(1) + ' / 5'
                  : '—'
              }}
            </dd>
          </div>
          <div>
            <dt>선호 산지</dt>
            <dd>{{ data?.profile?.preferredOrigin || '—' }}</dd>
          </div>
          <div>
            <dt>선호 가공</dt>
            <dd>{{ data?.profile?.preferredProcess || '—' }}</dd>
          </div>
          <div>
            <dt>쌓인 기록</dt>
            <dd>{{ data?.profile?.totalBrewCount ?? 0 }}회</dd>
          </div>
        </dl>
      </section>
      <div v-if="selected" class="taste-analysis-card">
        <strong>{{ selected.name }}</strong>
        <p>{{ selected.flavorNotes?.join(', ') }}</p>
        <NuxtLink :to="`/coffee-beans/${selected.id}`" class="button secondary"
          >원두 자세히 보기 ↗</NuxtLink
        >
      </div>
      <p v-if="message || error" class="hint" role="status">{{ message || errorMessage(error) }}</p>
      <a class="button secondary" href="#bean-group-CURRENT">▸ 원두 목록으로 보기</a>
    </aside>
  </section>
</template>

<style scoped>
.taste-hover-label {
  position: absolute;
  z-index: 3;
  width: 200px;
  padding: 12px 14px;
  background: #151b2ded;
  border: 1px solid #ffffff20;
  border-radius: 10px;
  color: #f2eee7;
  box-shadow: 0 8px 28px #0005;
  pointer-events: none;
  backdrop-filter: blur(12px);
}
.taste-hover-label strong {
  display: block;
  font-size: 12px;
  line-height: 1.5;
}
.taste-hover-label small {
  display: block;
  font-size: 10px;
  line-height: 1.6;
  color: #b4b8c6;
  margin-top: 5px;
}
.taste-space-caption {
  color: #9195a8;
}
.taste-space-stage {
  background: #10182b;
}
</style>
