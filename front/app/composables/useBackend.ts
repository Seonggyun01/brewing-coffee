export function useBackend() {
  const request = useRequestFetch()
  const read = <T = any>(path: string, query?: Record<string, any>) =>
    request<T>(`/api/backend/${path}`, { query })
  async function write<T = any>(path: string, method: 'POST' | 'PUT' | 'DELETE', body?: any) {
    const session: any = await $fetch('/api/backend/front/session')
    const headers: Record<string, string> = {}
    if (session.csrf) headers[session.csrf.headerName] = session.csrf.token
    return $fetch<T>(`/api/backend/${path}`, { method, body, headers })
  }
  return { read, write }
}
export function errorMessage(error: any) {
  const data = error?.data?.data || error?.data
  if (error?.statusCode === 413 || error?.status === 413)
    return '사진 용량이 너무 큽니다. 20MB 이하 사진을 선택해주세요.'
  if (error?.statusCode === 401 || error?.status === 401) return '로그인 후 이용해주세요.'
  if (error?.statusCode === 400 || error?.status === 400)
    return data?.message || '입력한 값과 숫자 형식을 확인해주세요.'
  if (error?.statusCode === 403 || error?.status === 403)
    return '로그인 상태가 변경됐어요. 새로고침 후 다시 시도해주세요.'
  if (error?.statusCode === 422 || error?.status === 422)
    return data?.message || '사진에서 원두 정보를 읽지 못했어요.'
  return data?.message || data?.detail || '서버에 연결하지 못했습니다. 잠시 후 다시 시도해주세요.'
}
