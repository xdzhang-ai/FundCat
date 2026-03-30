/** 基础 HTTP 请求层，统一 API 前缀、鉴权头和 JSON 响应处理。 */
import type { ApiResponse } from '@fundcat/contracts'
import { getAccessToken } from './authStorage'

export const API_BASE = '/api/v1'

export function withAuthHeaders(headers: HeadersInit = {}) {
  const token = getAccessToken()
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...headers,
  }
}

export async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: withAuthHeaders(init?.headers),
  })

  const payload = await response.json().catch(() => null)

  if (!response.ok) {
    const errorMessage =
      (payload && typeof payload === 'object' && 'message' in payload && typeof payload.message === 'string' && payload.message) ||
      (payload && typeof payload === 'object' && 'detail' in payload && typeof payload.detail === 'string' && payload.detail) ||
      'Request failed'
    throw new Error(errorMessage)
  }

  if (response.status === 204) {
    return undefined as T
  }

  if (payload && typeof payload === 'object' && 'code' in payload && 'data' in payload) {
    return (payload as ApiResponse<T>).data
  }

  return payload as T
}
