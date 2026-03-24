import type { AlertRule, FeatureFlag, ImportJob, WeeklyReport } from '@fundcat/contracts'

export type ProviderStatus = {
  providerKey: string
  status: string
  notes: string
}

type OpsSummary = {
  featureFlags: FeatureFlag[]
  providers: ProviderStatus[]
}

const ACCESS_TOKEN_KEY = 'fundcat.access-token'

export type LoginPayload = {
  phone: string
  password: string
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY)
  const response = await fetch(`/api/v1${path}`, {
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

export const api = {
  login: async (payload: LoginPayload) => {
    const response = await request<{ accessToken: string }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken)
    return response
  },
  summary: () => request<OpsSummary>('/ops/summary'),
  featureFlags: () => request<FeatureFlag[]>('/ops/feature-flags'),
  toggleFeatureFlag: (code: string, enabled: boolean) =>
    request<FeatureFlag>(`/ops/feature-flags/${code}`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    }),
  importJobs: () => request<ImportJob[]>('/import-jobs'),
  reports: () => request<WeeklyReport[]>('/reports/weekly'),
  alerts: () => request<AlertRule[]>('/alerts'),
  clearToken: () => localStorage.removeItem(ACCESS_TOKEN_KEY),
  hasToken: () => Boolean(localStorage.getItem(ACCESS_TOKEN_KEY)),
}
