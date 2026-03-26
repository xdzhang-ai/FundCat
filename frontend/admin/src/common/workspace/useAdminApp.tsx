/** 后台控制台总编排 hook，串联登录、数据加载、开关切换与页面导航状态。 */
import type { AlertRule, FeatureFlag, ImportJob, WeeklyReport } from '@fundcat/contracts'
import type { FormEvent } from 'react'
import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import type { ConsoleData } from '../appTypes'
import { authStorage } from '../data/authStorage'
import { opsApi } from '../data/opsApi'
import { currentTab, tabs } from './config'

export function useAdminApp() {
  const location = useLocation()
  const [consoleData, setConsoleData] = useState<ConsoleData | null>(null)
  const [message, setMessage] = useState<string>('正在读取后台数据...')
  const [username, setUsername] = useState('demo_analyst')
  const [password, setPassword] = useState('ChangeMe123!')
  const [needsLogin, setNeedsLogin] = useState(!authStorage.hasToken())
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [isSavingFlagCode, setIsSavingFlagCode] = useState<string | null>(null)

  const activeTab = currentTab(location.pathname)
  const activeTabConfig = tabs.find((tab) => tab.id === activeTab) ?? tabs[0]

  useEffect(() => {
    if (needsLogin) return
    void loadConsole()
  }, [needsLogin])

  async function loadConsole() {
    try {
      setIsRefreshing(true)
      setMessage('正在读取后台数据...')
      const [summaryResponse, featureFlagsResponse, importJobsResponse, reportsResponse, alertsResponse] = await Promise.all([
        opsApi.summary(),
        opsApi.featureFlags(),
        opsApi.importJobs(),
        opsApi.reports(),
        opsApi.alerts(),
      ])
      setConsoleData({
        summary: summaryResponse,
        featureFlags: featureFlagsResponse,
        importJobs: importJobsResponse,
        reports: reportsResponse,
        alerts: alertsResponse,
      })
      setMessage('')
    } catch (error) {
      authStorage.clear()
      setNeedsLogin(true)
      setMessage(error instanceof Error ? error.message : '无法读取后台数据，请重新登录。')
    } finally {
      setIsRefreshing(false)
    }
  }

  async function handleLogin(event: FormEvent) {
    event.preventDefault()
    try {
      await opsApi.login({ username, password })
      setNeedsLogin(false)
      setMessage('')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '登录失败')
    }
  }

  async function handleToggleFlag(flag: FeatureFlag) {
    try {
      setIsSavingFlagCode(flag.code)
      const updated = await opsApi.toggleFeatureFlag(flag.code, !flag.enabled)
      setConsoleData((current) => {
        if (!current) return current
        const nextFeatureFlags = current.featureFlags.map((item) => (item.code === updated.code ? updated : item))
        return {
          ...current,
          featureFlags: nextFeatureFlags,
          summary: {
            ...current.summary,
            featureFlags: current.summary.featureFlags.map((item) => (item.code === updated.code ? updated : item)),
          },
        }
      })
      setMessage(`${updated.name} 已${updated.enabled ? '启用' : '关闭'}`)
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '更新开关失败')
    } finally {
      setIsSavingFlagCode(null)
    }
  }

  async function handleLogout() {
    await opsApi.logout()
    authStorage.clear()
    setNeedsLogin(true)
  }

  return {
    activeTab,
    activeTabConfig,
    alerts: (consoleData?.alerts ?? []) as AlertRule[],
    consoleData,
    featureFlags: (consoleData?.featureFlags ?? []) as FeatureFlag[],
    handleLogin,
    handleLogout,
    handleToggleFlag,
    importJobs: (consoleData?.importJobs ?? []) as ImportJob[],
    isRefreshing,
    isSavingFlagCode,
    loadConsole,
    message,
    needsLogin,
    password,
    providers: consoleData?.summary.providers ?? [],
    reports: (consoleData?.reports ?? []) as WeeklyReport[],
    setPassword,
    setUsername,
    summary: consoleData?.summary ?? null,
    tabs,
    username,
  }
}

