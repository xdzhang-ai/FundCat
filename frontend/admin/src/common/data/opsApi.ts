/** 后台业务 API 层，按控制台场景组织登录、总览与开关等接口调用。 */
import type { AlertRule, FeatureFlag, ImportJob, WeeklyReport } from '@fundcat/contracts'
import type { LoginPayload, OpsSummary } from '../appTypes'
import { authStorage, getAccessToken } from './authStorage'
import { API_BASE, requestJson } from './http'

export const opsApi = {
  login: async (payload: LoginPayload) => {
    const response = await requestJson<{ accessToken: string; refreshToken: string }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    authStorage.save(response.accessToken, response.refreshToken)
    return response
  },
  logout: async () => {
    const token = getAccessToken()
    if (!token) return
    await fetch(`${API_BASE}/auth/logout`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
    }).catch(() => undefined)
  },
  summary: () => requestJson<OpsSummary>('/ops/summary'),
  featureFlags: () => requestJson<FeatureFlag[]>('/ops/feature-flags'),
  toggleFeatureFlag: (code: string, enabled: boolean) =>
    requestJson<FeatureFlag>(`/ops/feature-flags/${code}`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    }),
  importJobs: () => requestJson<ImportJob[]>('/import-jobs'),
  reports: () => requestJson<WeeklyReport[]>('/reports/weekly'),
  alerts: () => requestJson<AlertRule[]>('/alerts'),
}

