import type { AlertRule, FeatureFlag, ImportJob, WeeklyReport } from '@fundcat/contracts'
import { Activity, BellDot, DatabaseZap, ListTodo, LogOut, RefreshCcw, ShieldCheck, Sparkles } from 'lucide-react'
import type { FormEvent } from 'react'
import { useEffect, useState } from 'react'
import { Navigate, NavLink, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { api } from './api/client'
import { LoginPage } from './pages/LoginPage'
import { FlagsPage } from './pages/FlagsPage'
import { OverviewPage } from './pages/OverviewPage'
import { ProvidersPage } from './pages/ProvidersPage'
import { QueuePage } from './pages/QueuePage'
import { ReportsPage } from './pages/ReportsPage'

type OpsSummary = Awaited<ReturnType<typeof api.summary>>

type ConsoleData = {
  summary: OpsSummary
  featureFlags: FeatureFlag[]
  importJobs: ImportJob[]
  reports: WeeklyReport[]
  alerts: AlertRule[]
}

type AdminTab = 'overview' | 'flags' | 'providers' | 'queue' | 'reports'

const tabs: Array<{
  id: AdminTab
  label: string
  icon: typeof Activity
  helper: string
  path: string
}> = [
  { id: 'overview', label: '总览', icon: Activity, helper: '查看运行状态与关键指标', path: '/' },
  { id: 'flags', label: '功能开关', icon: ShieldCheck, helper: '筛选、查看并切换能力开关', path: '/flags' },
  { id: 'providers', label: '数据源', icon: DatabaseZap, helper: '查看 provider 健康状态和备注', path: '/providers' },
  { id: 'queue', label: '任务队列', icon: ListTodo, helper: '查看 OCR 导入与异步任务样例', path: '/queue' },
  { id: 'reports', label: '周报提醒', icon: BellDot, helper: '查看周报与提醒规则', path: '/reports' },
]

function currentTab(pathname: string): AdminTab {
  if (pathname.startsWith('/flags')) return 'flags'
  if (pathname.startsWith('/providers')) return 'providers'
  if (pathname.startsWith('/queue')) return 'queue'
  if (pathname.startsWith('/reports')) return 'reports'
  return 'overview'
}

function App() {
  const location = useLocation()
  const navigate = useNavigate()
  const [consoleData, setConsoleData] = useState<ConsoleData | null>(null)
  const [message, setMessage] = useState<string>('正在读取后台数据...')
  const [username, setUsername] = useState('demo_analyst')
  const [password, setPassword] = useState('ChangeMe123!')
  const [needsLogin, setNeedsLogin] = useState(!api.hasToken())
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
        api.summary(),
        api.featureFlags(),
        api.importJobs(),
        api.reports(),
        api.alerts(),
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
      api.clearToken()
      setNeedsLogin(true)
      setMessage(error instanceof Error ? error.message : '无法读取后台数据，请重新登录。')
    } finally {
      setIsRefreshing(false)
    }
  }

  async function handleLogin(event: FormEvent) {
    event.preventDefault()
    try {
      await api.login({ username, password })
      setNeedsLogin(false)
      setMessage('')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '登录失败')
    }
  }

  async function handleToggleFlag(flag: FeatureFlag) {
    try {
      setIsSavingFlagCode(flag.code)
      const updated = await api.toggleFeatureFlag(flag.code, !flag.enabled)
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
    await api.logout()
    api.clearToken()
    setNeedsLogin(true)
  }

  if (needsLogin) {
    return (
      <LoginPage
        username={username}
        password={password}
        message={message}
        onUsernameChange={setUsername}
        onPasswordChange={setPassword}
        onSubmit={handleLogin}
      />
    )
  }

  const summary = consoleData?.summary ?? null

  return (
    <div className="mx-auto min-h-screen max-w-[1600px] px-4 py-4 lg:px-6">
      <header className="mb-6 border-b border-white/8 px-1 pb-4">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-[color:var(--fc-color-accent)] text-slate-950">
              <Sparkles className="h-7 w-7" />
            </div>
            <div>
              <p className="text-sm uppercase tracking-[0.28em] text-[color:var(--fc-color-accent)]">FundCat Ops</p>
              <p className="mt-1 text-sm text-slate-400">Operations workspace for fund controls</p>
            </div>
          </div>
        </div>
      </header>

      <div className="grid gap-5 lg:grid-cols-[236px_minmax(0,1fr)]">
        <aside className="rounded-[1.75rem] border border-white/8 bg-slate-950/70 p-4 shadow-[var(--fc-shadow-card)] backdrop-blur-xl lg:sticky lg:top-4 lg:h-[calc(100vh-7.5rem)]">
          <div className="flex h-full flex-col">
            <nav className="grid gap-2">
              {tabs.map(({ id, label, icon: Icon, path }) => (
                <NavLink
                  key={id}
                  to={path}
                  className={({ isActive }) =>
                    `inline-flex items-center gap-3 rounded-[1rem] border px-3 py-2.5 text-left text-sm transition ${
                      isActive
                        ? 'border-[color:var(--fc-color-accent)]/50 bg-[color:var(--fc-color-accent)]/10 text-white'
                        : 'border-white/8 bg-white/5 text-slate-300 hover:border-white/20 hover:text-white'
                    }`
                  }
                >
                  <Icon className="h-4 w-4" />
                  <span>{label}</span>
                </NavLink>
              ))}
            </nav>

            <button
              onClick={() => void handleLogout()}
              className="mt-auto inline-flex items-center justify-center gap-2 rounded-full border border-white/10 px-4 py-2.5 text-sm text-slate-300 transition hover:border-white/20 hover:text-white"
            >
              <LogOut className="h-4 w-4" />
              退出
            </button>
          </div>
        </aside>

        <main className="min-w-0">
          <header className="rounded-[2rem] border border-white/8 bg-[color:var(--fc-color-surface-glass)] px-5 py-6 shadow-[var(--fc-shadow-card)] backdrop-blur-xl">
            <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
              <div>
                <h1 className="font-[var(--fc-font-display)] text-4xl font-semibold text-[color:var(--fc-color-accent)]">{activeTabConfig.label}</h1>
                <p className="mt-3 max-w-3xl text-slate-300">{activeTabConfig.helper}</p>
              </div>
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={() => void loadConsole()}
                  className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-300 transition hover:border-white/20 hover:text-white"
                >
                  <RefreshCcw className={`h-4 w-4 ${isRefreshing ? 'animate-spin' : ''}`} />
                  刷新
                </button>
              </div>
            </div>
          </header>

          {message ? (
            <div className="mt-4 rounded-3xl border border-white/10 bg-slate-950/40 px-4 py-3 text-sm text-slate-300">
              {message}
            </div>
          ) : null}

          {!summary ? (
            <div className="mt-6 rounded-[2rem] border border-white/8 bg-white/5 px-6 py-10 text-center text-slate-300 shadow-[var(--fc-shadow-card)]">
              {message || '正在载入后台数据...'}
            </div>
          ) : (
            <Routes>
              <Route
                path="/"
                element={
                  <OverviewPage
                    featureFlags={summary.featureFlags}
                    providers={summary.providers}
                    importJobCount={consoleData?.importJobs.length ?? 0}
                    onNavigate={navigate}
                  />
                }
              />
              <Route
                path="/flags"
                element={
                  <FlagsPage
                    featureFlags={consoleData?.featureFlags ?? []}
                    isSavingFlagCode={isSavingFlagCode}
                    onToggleFlag={(flag) => void handleToggleFlag(flag)}
                  />
                }
              />
              <Route path="/providers" element={<ProvidersPage providers={summary.providers} />} />
              <Route path="/queue" element={<QueuePage importJobs={consoleData?.importJobs ?? []} />} />
              <Route
                path="/reports"
                element={<ReportsPage reports={consoleData?.reports ?? []} alerts={consoleData?.alerts ?? []} />}
              />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          )}
        </main>
      </div>
    </div>
  )
}

export default App
