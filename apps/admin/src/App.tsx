import type { AlertRule, FeatureFlag, ImportJob, WeeklyReport } from '@fundcat/contracts'
import {
  Activity,
  BellDot,
  DatabaseZap,
  ListTodo,
  LogOut,
  RefreshCcw,
  ShieldCheck,
  Sparkles,
} from 'lucide-react'
import type { ComponentType } from 'react'
import { startTransition, useDeferredValue, useEffect, useState } from 'react'
import { api } from './api/client'

type OpsSummary = Awaited<ReturnType<typeof api.summary>>
type AdminTab = 'overview' | 'flags' | 'providers' | 'queue' | 'reports'

type ConsoleData = {
  summary: OpsSummary
  featureFlags: FeatureFlag[]
  importJobs: ImportJob[]
  reports: WeeklyReport[]
  alerts: AlertRule[]
}

const tabs: Array<{
  id: AdminTab
  label: string
  icon: ComponentType<{ className?: string }>
  helper: string
}> = [
  { id: 'overview', label: '总览', icon: Activity, helper: '查看运行状态与关键指标' },
  { id: 'flags', label: '功能开关', icon: ShieldCheck, helper: '筛选、查看并切换能力开关' },
  { id: 'providers', label: '数据源', icon: DatabaseZap, helper: '查看 provider 健康状态和备注' },
  { id: 'queue', label: '任务队列', icon: ListTodo, helper: '查看 OCR 导入与异步任务样例' },
  { id: 'reports', label: '周报提醒', icon: BellDot, helper: '查看周报与提醒规则' },
]

function App() {
  const [consoleData, setConsoleData] = useState<ConsoleData | null>(null)
  const [message, setMessage] = useState<string>('正在读取后台数据...')
  const [phone, setPhone] = useState('10000000000')
  const [password, setPassword] = useState('ChangeMe123!')
  const [needsLogin, setNeedsLogin] = useState(!api.hasToken())
  const [activeTab, setActiveTab] = useState<AdminTab>('overview')
  const [selectedFlagCode, setSelectedFlagCode] = useState<string | null>(null)
  const [selectedProviderKey, setSelectedProviderKey] = useState<string | null>(null)
  const [flagQuery, setFlagQuery] = useState('')
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [isSavingFlagCode, setIsSavingFlagCode] = useState<string | null>(null)

  const deferredFlagQuery = useDeferredValue(flagQuery.trim().toLowerCase())

  useEffect(() => {
    if (needsLogin) return
    void loadConsole()
  }, [needsLogin])

  const summary = consoleData?.summary ?? null
  const featureFlags = consoleData?.featureFlags ?? []
  const providers = summary?.providers ?? []
  const filteredFlags = featureFlags.filter((flag) => {
    if (!deferredFlagQuery) return true
    const haystack = `${flag.name} ${flag.code} ${flag.description} ${flag.environment} ${flag.riskLevel}`.toLowerCase()
    return haystack.includes(deferredFlagQuery)
  })

  const selectedFlag =
    filteredFlags.find((flag) => flag.code === selectedFlagCode) ??
    filteredFlags[0] ??
    null

  const selectedProvider =
    providers.find((provider) => provider.providerKey === selectedProviderKey) ??
    providers[0] ??
    null
  const activeTabConfig = tabs.find((tab) => tab.id === activeTab) ?? tabs[0]
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
      setSelectedFlagCode((current) => current ?? featureFlagsResponse[0]?.code ?? null)
      setSelectedProviderKey((current) => current ?? summaryResponse.providers[0]?.providerKey ?? null)
      setMessage('')
    } catch (error) {
      api.clearToken()
      setNeedsLogin(true)
      setMessage(error instanceof Error ? error.message : '无法读取后台数据，请重新登录。')
    } finally {
      setIsRefreshing(false)
    }
  }

  async function handleLogin(event: React.FormEvent) {
    event.preventDefault()
    try {
      await api.login({ phone, password })
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

  function switchTab(tab: AdminTab) {
    startTransition(() => {
      setActiveTab(tab)
    })
  }

  if (needsLogin) {
    return (
      <div className="mx-auto flex min-h-screen max-w-6xl items-center px-6 py-8">
        <div className="grid w-full gap-8 lg:grid-cols-[1.1fr_0.9fr]">
          <div className="rounded-[2rem] border border-white/8 bg-white/5 p-8 shadow-[var(--fc-shadow-card)] backdrop-blur-xl">
            <p className="text-sm uppercase tracking-[0.28em] text-[color:var(--fc-color-accent)]">FundCat Ops</p>
            <h1 className="mt-5 font-[var(--fc-font-display)] text-4xl font-semibold text-white">后台联调入口</h1>
            <p className="mt-4 max-w-xl text-slate-300">
              后台已支持独立登录，并提供总览、功能开关、数据源、任务队列和周报提醒等可切换工作区。
            </p>
          </div>

          <form
            onSubmit={handleLogin}
            className="rounded-[2rem] border border-white/8 bg-white/5 p-8 shadow-[var(--fc-shadow-card)] backdrop-blur-xl"
          >
            <h2 className="font-[var(--fc-font-display)] text-3xl font-semibold text-white">登录后台</h2>
            <p className="mt-3 text-slate-400">演示账号与主站一致。</p>
            <label className="mt-8 block">
              <span className="mb-2 block text-sm text-slate-400">手机号</span>
              <input
                value={phone}
                onChange={(event) => setPhone(event.target.value)}
                autoComplete="username"
                className="w-full rounded-2xl border border-white/10 bg-slate-950/60 px-4 py-3 text-white outline-none transition focus:border-[color:var(--fc-color-accent)]"
              />
            </label>
            <label className="mt-5 block">
              <span className="mb-2 block text-sm text-slate-400">密码</span>
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
                className="w-full rounded-2xl border border-white/10 bg-slate-950/60 px-4 py-3 text-white outline-none transition focus:border-[color:var(--fc-color-accent)]"
              />
            </label>
            {message ? <p className="mt-4 text-sm text-orange-300">{message}</p> : null}
            <button className="mt-8 inline-flex w-full items-center justify-center rounded-2xl bg-[color:var(--fc-color-accent)] px-4 py-3 font-semibold text-slate-950 transition hover:brightness-105">
              进入后台
            </button>
          </form>
        </div>
      </div>
    )
  }

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
              {tabs.map(({ id, label, icon: Icon }) => {
                const active = activeTab === id
                return (
                  <button
                    key={id}
                    onClick={() => switchTab(id)}
                    className={`inline-flex items-center gap-3 rounded-[1rem] border px-3 py-2.5 text-left text-sm transition ${
                      active
                        ? 'border-[color:var(--fc-color-accent)]/50 bg-[color:var(--fc-color-accent)]/10 text-white'
                        : 'border-white/8 bg-white/5 text-slate-300 hover:border-white/20 hover:text-white'
                    }`}
                  >
                    <Icon className="h-4 w-4" />
                    <span>{label}</span>
                  </button>
                )
              })}
            </nav>

            <button
              onClick={() => {
                api.clearToken()
                setNeedsLogin(true)
              }}
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
            <>
              {activeTab === 'overview' ? (
                <>
                  <section className="mt-6 grid gap-4 md:grid-cols-4">
                    <MetricCard
                      icon={ShieldCheck}
                      label="开关"
                      value={`${summary.featureFlags.filter((flag) => flag.enabled).length}/${summary.featureFlags.length}`}
                      onClick={() => switchTab('flags')}
                    />
                    <MetricCard
                      icon={DatabaseZap}
                      label="数据源"
                      value={String(summary.providers.length)}
                      onClick={() => switchTab('providers')}
                    />
                    <MetricCard
                      icon={Activity}
                      label="健康状态"
                      value={summary.providers.every((provider) => provider.status === 'healthy') ? 'Healthy' : 'Check'}
                      onClick={() => switchTab('providers')}
                    />
                    <MetricCard
                      icon={ListTodo}
                      label="任务队列"
                      value={String((consoleData?.importJobs ?? []).length)}
                      onClick={() => switchTab('queue')}
                    />
                  </section>
                </>
              ) : null}

              {activeTab === 'flags' ? (
                <section className="mt-6 grid gap-6 xl:grid-cols-[0.58fr_0.42fr]">
                  <div className="rounded-[2rem] border border-white/8 bg-white/5 p-6 shadow-[var(--fc-shadow-card)]">
                    <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                      <div>
                        <h2 className="font-[var(--fc-font-display)] text-2xl text-white">Feature Flags</h2>
                        <p className="mt-2 text-sm text-slate-400">支持搜索、切换与查看风险等级。</p>
                      </div>
                      <input
                        value={flagQuery}
                        onChange={(event) => setFlagQuery(event.target.value)}
                        placeholder="搜索开关名 / code / 描述"
                        className="rounded-full border border-white/10 bg-slate-950/50 px-4 py-2 text-sm text-white outline-none placeholder:text-slate-500"
                      />
                    </div>
                    <div className="mt-5 space-y-3">
                      {filteredFlags.map((flag) => (
                        <button
                          key={flag.code}
                          onClick={() => setSelectedFlagCode(flag.code)}
                          className={`w-full rounded-[1.6rem] border p-4 text-left transition ${
                            selectedFlag?.code === flag.code
                              ? 'border-[color:var(--fc-color-accent)]/45 bg-[color:var(--fc-color-accent)]/8'
                              : 'border-white/8 bg-slate-950/35 hover:border-white/20'
                          }`}
                        >
                          <div className="flex items-start justify-between gap-4">
                            <div>
                              <p className="font-medium text-white">{flag.name}</p>
                              <p className="mt-1 text-sm text-slate-400">{flag.description}</p>
                            </div>
                            <FlagBadge flag={flag} />
                          </div>
                          <div className="mt-4 flex items-center justify-between text-sm text-slate-500">
                            <span>{flag.code}</span>
                            <span className="rounded-full border border-white/10 px-3 py-1">{flag.riskLevel}</span>
                          </div>
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="rounded-[2rem] border border-white/8 bg-white/5 p-6 shadow-[var(--fc-shadow-card)]">
                    {selectedFlag ? (
                      <>
                        <p className="text-sm uppercase tracking-[0.22em] text-[color:var(--fc-color-accent)]">Flag detail</p>
                        <h2 className="mt-4 font-[var(--fc-font-display)] text-3xl text-white">{selectedFlag.name}</h2>
                        <p className="mt-4 text-sm leading-7 text-slate-300">{selectedFlag.description}</p>
                        <div className="mt-6 grid gap-3 sm:grid-cols-2">
                          <DetailPill label="Code" value={selectedFlag.code} />
                          <DetailPill label="Environment" value={selectedFlag.environment} />
                          <DetailPill label="Risk" value={selectedFlag.riskLevel} />
                          <DetailPill label="Status" value={selectedFlag.enabled ? 'enabled' : 'disabled'} />
                        </div>
                        <button
                          onClick={() => void handleToggleFlag(selectedFlag)}
                          disabled={isSavingFlagCode === selectedFlag.code}
                          className={`mt-8 inline-flex items-center justify-center rounded-2xl px-4 py-3 font-semibold transition ${
                            selectedFlag.enabled
                              ? 'bg-orange-400/90 text-slate-950 hover:brightness-105'
                              : 'bg-[color:var(--fc-color-accent)] text-slate-950 hover:brightness-105'
                          } disabled:cursor-not-allowed disabled:opacity-70`}
                        >
                          {isSavingFlagCode === selectedFlag.code
                            ? '提交中...'
                            : selectedFlag.enabled
                              ? '关闭该功能'
                              : '启用该功能'}
                        </button>
                      </>
                    ) : (
                      <EmptyPanel title="没有匹配的功能开关" body="调整搜索词后再试，或先点击刷新重新获取后台数据。" />
                    )}
                  </div>
                </section>
              ) : null}

              {activeTab === 'providers' ? (
                <section className="mt-6 grid gap-6 xl:grid-cols-[0.48fr_0.52fr]">
                  <div className="rounded-[2rem] border border-white/8 bg-white/5 p-6 shadow-[var(--fc-shadow-card)]">
                    <h2 className="font-[var(--fc-font-display)] text-2xl text-white">Provider Status</h2>
                    <div className="mt-5 space-y-3">
                      {providers.map((provider) => (
                        <button
                          key={provider.providerKey}
                          onClick={() => setSelectedProviderKey(provider.providerKey)}
                          className={`w-full rounded-[1.6rem] border p-4 text-left transition ${
                            selectedProvider?.providerKey === provider.providerKey
                              ? 'border-[color:var(--fc-color-accent)]/45 bg-[color:var(--fc-color-accent)]/8'
                              : 'border-white/8 bg-slate-950/35 hover:border-white/20'
                          }`}
                        >
                          <div className="flex items-center justify-between gap-4">
                            <p className="font-medium text-white">{provider.providerKey}</p>
                            <span className="rounded-full bg-emerald-400/10 px-3 py-1 text-xs uppercase tracking-[0.18em] text-emerald-300">
                              {provider.status}
                            </span>
                          </div>
                          <p className="mt-3 text-sm leading-6 text-slate-400">{provider.notes}</p>
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="rounded-[2rem] border border-white/8 bg-white/5 p-6 shadow-[var(--fc-shadow-card)]">
                    {selectedProvider ? (
                      <>
                        <p className="text-sm uppercase tracking-[0.22em] text-[color:var(--fc-color-accent)]">Provider detail</p>
                        <h2 className="mt-4 font-[var(--fc-font-display)] text-3xl text-white">{selectedProvider.providerKey}</h2>
                        <div className="mt-6 grid gap-3 sm:grid-cols-2">
                          <DetailPill label="Status" value={selectedProvider.status} />
                          <DetailPill label="Scope" value="research adapter" />
                        </div>
                        <div className="mt-6 rounded-[1.6rem] border border-white/8 bg-slate-950/35 p-4">
                          <p className="text-sm text-slate-400">备注</p>
                          <p className="mt-3 leading-7 text-slate-200">{selectedProvider.notes}</p>
                        </div>
                        <div className="mt-6 rounded-[1.6rem] border border-white/8 bg-slate-950/35 p-4">
                          <p className="text-sm text-slate-400">下一步建议</p>
                          <ul className="mt-3 space-y-2 text-sm text-slate-300">
                            <li>检查最近一次同步时间与失败重试记录</li>
                            <li>补充 provider 详情页与任务重试操作</li>
                            <li>将健康状态和开关策略联动展示</li>
                          </ul>
                        </div>
                      </>
                    ) : (
                      <EmptyPanel title="暂无数据源" body="当前环境没有可显示的数据源状态。" />
                    )}
                  </div>
                </section>
              ) : null}

              {activeTab === 'queue' ? (
                <section className="mt-6 rounded-[2rem] border border-white/8 bg-white/5 p-6 shadow-[var(--fc-shadow-card)]">
                  <div className="flex items-end justify-between gap-4">
                    <div>
                      <h2 className="font-[var(--fc-font-display)] text-2xl text-white">任务队列</h2>
                      <p className="mt-2 text-sm text-slate-400">当前以 OCR 导入任务为主，可继续扩展成统一异步任务中心。</p>
                    </div>
                    <p className="text-sm text-slate-500">共 {consoleData?.importJobs.length ?? 0} 条</p>
                  </div>
                  <div className="mt-6 grid gap-4 lg:grid-cols-2">
                    {(consoleData?.importJobs ?? []).map((job) => (
                      <div key={job.id} className="rounded-[1.6rem] border border-white/8 bg-slate-950/35 p-5">
                        <div className="flex items-center justify-between gap-4">
                          <p className="font-medium text-white">{job.fileName}</p>
                          <span className="rounded-full border border-white/10 px-3 py-1 text-xs uppercase tracking-[0.18em] text-slate-200">
                            {job.status}
                          </span>
                        </div>
                        <p className="mt-3 text-sm text-slate-400">{job.sourcePlatform}</p>
                        <div className="mt-5 grid gap-3 sm:grid-cols-2">
                          <DetailPill label="识别持仓数" value={String(job.recognizedHoldings)} />
                          <DetailPill label="创建时间" value={formatDate(job.createdAt)} />
                        </div>
                      </div>
                    ))}
                  </div>
                </section>
              ) : null}

              {activeTab === 'reports' ? (
                <section className="mt-6 grid gap-6 xl:grid-cols-[0.56fr_0.44fr]">
                  <div className="rounded-[2rem] border border-white/8 bg-white/5 p-6 shadow-[var(--fc-shadow-card)]">
                    <h2 className="font-[var(--fc-font-display)] text-2xl text-white">周报摘要</h2>
                    <div className="mt-5 space-y-4">
                      {(consoleData?.reports ?? []).map((report) => (
                        <div key={report.id} className="rounded-[1.6rem] border border-white/8 bg-slate-950/35 p-5">
                          <div className="flex items-center justify-between gap-4">
                            <p className="font-medium text-white">{report.weekLabel}</p>
                            <span className={`rounded-full px-3 py-1 text-xs ${report.returnRate >= 0 ? 'bg-emerald-400/10 text-emerald-300' : 'bg-orange-400/10 text-orange-300'}`}>
                              {report.returnRate >= 0 ? '+' : ''}
                              {report.returnRate}%
                            </span>
                          </div>
                          <p className="mt-3 text-sm leading-7 text-slate-300">{report.summary}</p>
                          <div className="mt-4 grid gap-3 sm:grid-cols-2">
                            <DetailPill label="最佳基金" value={report.bestFundCode} />
                            <DetailPill label="风险提示" value={report.riskNote} />
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  <div className="rounded-[2rem] border border-white/8 bg-white/5 p-6 shadow-[var(--fc-shadow-card)]">
                    <h2 className="font-[var(--fc-font-display)] text-2xl text-white">提醒规则</h2>
                    <div className="mt-5 space-y-3">
                      {(consoleData?.alerts ?? []).map((alert) => (
                        <div key={alert.id} className="rounded-[1.6rem] border border-white/8 bg-slate-950/35 p-4">
                          <div className="flex items-center justify-between gap-4">
                            <p className="font-medium text-white">{alert.fundCode}</p>
                            <span className={`rounded-full px-3 py-1 text-xs uppercase tracking-[0.18em] ${alert.enabled ? 'bg-emerald-400/10 text-emerald-300' : 'bg-slate-500/20 text-slate-300'}`}>
                              {alert.enabled ? 'enabled' : 'disabled'}
                            </span>
                          </div>
                          <p className="mt-3 text-sm text-slate-400">{alert.ruleType}</p>
                          <div className="mt-4 grid gap-3 sm:grid-cols-2">
                            <DetailPill label="阈值" value={String(alert.thresholdValue)} />
                            <DetailPill label="渠道" value={alert.channel} />
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </section>
              ) : null}
            </>
          )}
        </main>
      </div>
    </div>
  )
}

function FlagBadge({ flag }: { flag: FeatureFlag }) {
  return (
    <span
      className={`rounded-full px-3 py-1 text-xs uppercase tracking-[0.18em] ${
        flag.enabled
          ? 'bg-emerald-400/10 text-emerald-300'
          : 'bg-slate-500/20 text-slate-300'
      }`}
    >
      {flag.environment}
    </span>
  )
}

function DetailPill({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[1.4rem] border border-white/8 bg-slate-950/35 px-4 py-4">
      <p className="text-xs uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className="mt-3 text-sm leading-6 text-slate-200">{value}</p>
    </div>
  )
}

function EmptyPanel({ title, body }: { title: string; body: string }) {
  return (
    <div className="flex h-full min-h-72 items-center justify-center rounded-[1.6rem] border border-dashed border-white/10 bg-slate-950/30 p-6 text-center">
      <div>
        <p className="font-medium text-white">{title}</p>
        <p className="mt-3 text-sm leading-6 text-slate-400">{body}</p>
      </div>
    </div>
  )
}

function MetricCard({
  icon: Icon,
  label,
  value,
  onClick,
}: {
  icon: ComponentType<{ className?: string }>
  label: string
  value: string
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      className="rounded-[1.8rem] border border-white/8 bg-white/5 p-5 text-left shadow-[var(--fc-shadow-card)] backdrop-blur-xl transition hover:border-white/20"
    >
      <Icon className="h-5 w-5 text-[color:var(--fc-color-accent)]" />
      <p className="mt-4 text-sm text-slate-400">{label}</p>
      <p className="mt-2 font-[var(--fc-font-display)] text-3xl text-white">{value}</p>
    </button>
  )
}

function formatDate(value: string) {
  return value.replace('T', ' ').slice(0, 16)
}

export default App
