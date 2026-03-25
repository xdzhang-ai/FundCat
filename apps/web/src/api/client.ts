import type {
  AuthResponse,
  CreateImportJobPayload,
  CreatePaperOrderPayload,
  CreateSipPlanPayload,
  CreateWatchlistPayload,
  DashboardResponse,
  FundCard,
  FundDetail,
  LoginPayload,
} from '@fundcat/contracts'

const API_BASE = '/api/v1'
const ACCESS_TOKEN_KEY = 'fundcat.access-token'
const REFRESH_TOKEN_KEY = 'fundcat.refresh-token'

function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

function withHeaders(headers: HeadersInit = {}) {
  const token = getAccessToken()
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...headers,
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: withHeaders(init?.headers),
  })

  if (!response.ok) {
    const problem = await response.json().catch(() => ({ detail: 'Request failed' }))
    throw new Error(problem.detail ?? 'Request failed')
  }

  return response.json() as Promise<T>
}

export const api = {
  login: async (payload: LoginPayload) => {
    const response = await request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken)
    return response
  },
  logout: async () => {
    const token = getAccessToken()
    if (!token) return
    await fetch(`${API_BASE}/auth/logout`, {
      method: 'POST',
      headers: withHeaders(),
    }).catch(() => undefined)
  },
  dashboard: () => request<DashboardResponse>('/dashboard'),
  funds: (query = '') =>
    request<FundCard[]>(`/funds${query ? `?query=${encodeURIComponent(query)}` : ''}`),
  fundDetail: (code: string) => request<FundDetail>(`/funds/${code}`),
  addWatchlist: (payload: CreateWatchlistPayload) =>
    request('/watchlist', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  createOrder: (payload: CreatePaperOrderPayload) =>
    request('/orders', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  createSip: (payload: CreateSipPlanPayload) =>
    request('/sips', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  createImportJob: (payload: CreateImportJobPayload) =>
    request('/import-jobs', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
}

export const authStorage = {
  clear() {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  },
  hasToken() {
    return Boolean(getAccessToken())
  },
}
