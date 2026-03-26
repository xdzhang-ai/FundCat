/** 后台基础 HTTP 请求层，统一 API 前缀、鉴权头和 JSON 响应处理。 */
import { getAccessToken } from './authStorage'

export const API_BASE = '/api/v1'

export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getAccessToken()
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init?.headers,
    },
  })

  if (!response.ok) {
    const problem = await response.json().catch(() => ({ detail: 'Request failed' }))
    throw new Error(problem.detail ?? 'Request failed')
  }

  return response.json() as Promise<T>
}

