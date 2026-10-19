import type { H3Event } from 'h3'
export function proxyAuth(event: H3Event, path: string) {
  const url = getRequestURL(event)
  return proxyRequest(event, `${useRuntimeConfig(event).backendUrl}/${path}${url.search}`, {
    headers: { 'x-forwarded-host': url.host, 'x-forwarded-proto': url.protocol.slice(0, -1) },
    fetchOptions: { redirect: 'manual' },
  })
}
