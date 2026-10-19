/** A wheel/vertical swipe advances one home scene; cleanup restores ordinary page scrolling. */
export function useHomePaging() {
  let lockUntil = 0,
    lastWheel = 0,
    wheelDistance = 0
  let touch: { x: number; y: number; stage: number } | null = null
  const stageIds = ['home-origin', 'home-demo', 'home-final']
  const boundaries = () =>
    stageIds.map((id, index) =>
      index === 0
        ? 0
        : (document.getElementById(id)?.getBoundingClientRect().top || 0) + window.scrollY,
    )
  const current = () =>
    boundaries().reduce(
      (best, top, index, all) =>
        Math.abs(top - window.scrollY) < Math.abs(all[best]! - window.scrollY) ? index : best,
      0,
    )
  const oversized = () => {
    const section = document.getElementById(stageIds[current()]!)
    return !!section && section.offsetHeight > window.innerHeight + 2
  }
  const interactive = (target: EventTarget | null) =>
    (target as HTMLElement)?.closest?.(
      'input, textarea, select, [contenteditable=true], .country-panel, .world-map[data-zoomed=true]',
    )
  function go(stage: number) {
    const top = boundaries()[Math.max(0, Math.min(stageIds.length - 1, stage))]!
    lockUntil = performance.now() + 900
    wheelDistance = 0
    window.scrollTo({
      top,
      behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches
        ? 'instant'
        : 'smooth',
    })
  }
  function wheel(event: WheelEvent) {
    if (
      event.ctrlKey ||
      Math.abs(event.deltaX) >= Math.abs(event.deltaY) ||
      interactive(event.target)
    )
      return
    event.preventDefault()
    const now = performance.now(),
      quiet = now - lastWheel
    lastWheel = now
    if (now < lockUntil) return
    if (quiet > 180) wheelDistance = 0
    wheelDistance +=
      event.deltaY * (event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? window.innerHeight : 1)
    if (Math.abs(wheelDistance) >= 32) go(current() + Math.sign(wheelDistance))
  }
  function key(event: KeyboardEvent) {
    if (
      (event.target as HTMLElement)?.closest?.(
        'a, button, input, select, textarea, [contenteditable=true]',
      )
    )
      return
    const delta = ['ArrowDown', 'PageDown', ' '].includes(event.key)
      ? event.shiftKey
        ? -1
        : 1
      : ['ArrowUp', 'PageUp'].includes(event.key)
        ? -1
        : 0
    if (!delta && event.key !== 'Home' && event.key !== 'End') return
    event.preventDefault()
    if (performance.now() < lockUntil) return
    go(event.key === 'Home' ? 0 : event.key === 'End' ? 2 : current() + delta)
  }
  function touchStart(event: TouchEvent) {
    if (oversized() || event.touches.length !== 1 || interactive(event.target)) {
      touch = null
      return
    }
    const point = event.touches[0]!
    touch = { x: point.clientX, y: point.clientY, stage: current() }
  }
  function touchMove(event: TouchEvent) {
    if (!touch || event.touches.length !== 1) return
    const point = event.touches[0]!,
      dy = point.clientY - touch.y,
      dx = point.clientX - touch.x
    if (Math.abs(dy) > 10 && Math.abs(dy) > Math.abs(dx) && event.cancelable) event.preventDefault()
  }
  function touchEnd(event: TouchEvent) {
    if (!touch || !event.changedTouches.length) return
    const point = event.changedTouches[0]!,
      dy = point.clientY - touch.y,
      dx = point.clientX - touch.x
    if (Math.abs(dy) > 50 && Math.abs(dy) > Math.abs(dx) && performance.now() >= lockUntil)
      go(touch.stage + (dy < 0 ? 1 : -1))
    touch = null
  }
  onMounted(() => {
    window.addEventListener('wheel', wheel, { passive: false })
    window.addEventListener('keydown', key)
    window.addEventListener('touchstart', touchStart, { passive: true })
    window.addEventListener('touchmove', touchMove, { passive: false })
    window.addEventListener('touchend', touchEnd, { passive: true })
  })
  onBeforeUnmount(() => {
    window.removeEventListener('wheel', wheel)
    window.removeEventListener('keydown', key)
    window.removeEventListener('touchstart', touchStart)
    window.removeEventListener('touchmove', touchMove)
    window.removeEventListener('touchend', touchEnd)
  })
}
