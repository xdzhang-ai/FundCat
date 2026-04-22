/** 后台共享类型定义，承接控制台数据、登录载荷和路由标签结构。 */
import type { FeatureFlag } from '@fundcat/contracts'

export type ProviderStatus = {
  providerKey: string
  status: string
  notes: string
}

export type OpsSummary = {
  featureFlags: FeatureFlag[]
  providers: ProviderStatus[]
}

export type ConsoleData = {
  summary: OpsSummary
  featureFlags: FeatureFlag[]
}

export type AdminTab = 'overview' | 'flags' | 'providers' | 'queue' | 'reports'

export type LoginPayload = {
  username: string
  password: string
}

