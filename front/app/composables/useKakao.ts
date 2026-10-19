let sdk: Promise<any> | undefined
export async function useKakao() {
  if (!import.meta.client) return null
  const key: string = (await $fetch<any>('/api/backend/front/session')).kakaoJavascriptKey
  if (!key) throw new Error('카카오맵 JavaScript 키가 서버에 설정되지 않았습니다.')
  if (!sdk)
    sdk = new Promise((resolve, reject) => {
      const ready = () => (window as any).kakao.maps.load(() => resolve((window as any).kakao.maps))
      if ((window as any).kakao?.maps) return ready()
      const script = document.createElement('script')
      script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(key)}&libraries=services&autoload=false`
      script.onload = ready
      script.onerror = () => {
        sdk = undefined
        reject(new Error('지도를 불러오지 못했습니다.'))
      }
      document.head.appendChild(script)
    })
  return sdk
}
