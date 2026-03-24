import { Activity, DatabaseZap, ShieldCheck, SlidersHorizontal } from 'lucide-react'
import type { ComponentType } from 'react'
import { useEffect, useState } from 'react'
import { api } from './api/client'

type OpsSummary = Awaited<ReturnType<typeof api.summary>>

function App() {
  const [summary, setSummary] = useState<OpsSummary | null>(null)
  const [message, setMessage] = useState<string>('正在读取研究后台摘要...')
  const [phone, setPhone] = useState('10000000000')
  const [password, setPassword] = useState('ChangeMe123!')
  const [needsLogin, setNeedsLogin] = useState(!api.hasToken())

  useEffect(() => {
    if (needsLogin) return
    void (async () => {
      try {
        const response = await api.summary()
        setSummary(response)
        setMessage('')
      } catch (error) {
        api.clearToken()
        setNeedsLogin(true)
        setMessage(error instanceof Error ? error.message : '无法读取后台数据，请登录后台。')
      }
    })()
  }, [needsLogin])

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

  if (needsLogin) {
    return (
      <div className="mx-auto flex min-h-screen max-w-6xl items-center px-6 py-8">
        <div className="grid w-full gap-8 lg:grid-cols-[1.1fr_0.9fr]">
          <div className="rounded-[2rem] border border-white/8 bg-white/5 p-8 shadow-[var(--fc-shadow-card)] backdrop-blur-xl">
            <p className="text-sm uppercase tracking-[0.28em] text-[color:var(--fc-color-accent)]">FundCat Ops</p>
            <h1 className="mt-5 font-[var(--fc-font-display)] text-4xl font-semibold text-white">后台联调入口</h1>
            <p className="mt-4 max-w-xl text-slate-300">
              由于开发环境中 Web 和 Admin 运行在不同端口，浏览器不会共享 `localStorage`。
              后台现在提供独立登录页，避免本地联调时拿不到 token。
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
                className="w-full rounded-2xl border border-white/10 bg-slate-950/60 px-4 py-3 text-white outline-none transition focus:border-[color:var(--fc-color-accent)]"
              />
            </label>
            <label className="mt-5 block">
              <span className="mb-2 block text-sm text-slate-400">密码</span>
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
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
    <div className="mx-auto min-h-screen max-w-7xl px-6 py-8">
      <header className="rounded-[2rem] border border-white/8 bg-white/5 px-6 py-6 shadow-[var(--fc-shadow-card)] backdrop-blur-xl">
        <p className="text-sm uppercase tracking-[0.28em] text-[color:var(--fc-color-accent)]">FundCat Ops</p>
        <div className="mt-4 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h1 className="font-[var(--fc-font-display)] text-4xl font-semibold text-white">研究版后台总览</h1>
            <p className="mt-3 max-w-3xl text-slate-300">
              当前后台聚焦高风险能力开关、数据提供方状态和研究功能灰度。后续可在这里继续挂接运营位、任务重试和用户反馈。
            </p>
          </div>
          <div className="rounded-full border border-white/10 bg-slate-950/40 px-4 py-2 text-sm text-slate-300">
            Admin 端独立登录，避免不同端口下 token 不共享
          </div>
        </div>
      </header>

      {!summary ? (
        <div className="mt-6 rounded-[2rem] border border-white/8 bg-white/5 px-6 py-10 text-center text-slate-300 shadow-[var(--fc-shadow-card)]">
          {message}
        </div>
      ) : (
        <>
          <section className="mt-6 grid gap-4 md:grid-cols-4">
            <MetricCard icon={ShieldCheck} label="启用开关" value={String(summary.featureFlags.filter((flag) => flag.enabled).length)} />
            <MetricCard icon={SlidersHorizontal} label="总开关数" value={String(summary.featureFlags.length)} />
            <MetricCard icon={DatabaseZap} label="数据源" value={String(summary.providers.length)} />
            <MetricCard icon={Activity} label="健康状态" value={summary.providers.every((provider) => provider.status === 'healthy') ? 'Healthy' : 'Check'} />
          </section>

          <section className="mt-6 grid gap-6 xl:grid-cols-[0.55fr_0.45fr]">
            <div className="rounded-[2rem] border border-white/8 bg-white/5 p-6 shadow-[var(--fc-shadow-card)]">
              <h2 className="font-[var(--fc-font-display)] text-2xl text-white">Feature Flags</h2>
              <div className="mt-5 space-y-3">
                {summary.featureFlags.map((flag) => (
                  <div key={flag.code} className="rounded-[1.6rem] border border-white/8 bg-slate-950/40 p-4">
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <p className="font-medium text-white">{flag.name}</p>
                        <p className="mt-1 text-sm text-slate-400">{flag.description}</p>
                      </div>
                      <span
                        className={`rounded-full px-3 py-1 text-xs uppercase tracking-[0.18em] ${
                          flag.enabled
                            ? 'bg-emerald-400/10 text-emerald-300'
                            : 'bg-slate-500/20 text-slate-300'
                        }`}
                      >
                        {flag.environment}
                      </span>
                    </div>
                    <div className="mt-4 flex items-center justify-between text-sm text-slate-500">
                      <span>{flag.code}</span>
                      <span className="rounded-full border border-white/10 px-3 py-1">{flag.riskLevel}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="rounded-[2rem] border border-white/8 bg-white/5 p-6 shadow-[var(--fc-shadow-card)]">
              <h2 className="font-[var(--fc-font-display)] text-2xl text-white">Provider Status</h2>
              <div className="mt-5 space-y-3">
                {summary.providers.map((provider) => (
                  <div key={provider.providerKey} className="rounded-[1.6rem] border border-white/8 bg-slate-950/40 p-4">
                    <div className="flex items-center justify-between gap-4">
                      <p className="font-medium text-white">{provider.providerKey}</p>
                      <span className="rounded-full bg-emerald-400/10 px-3 py-1 text-xs uppercase tracking-[0.18em] text-emerald-300">
                        {provider.status}
                      </span>
                    </div>
                    <p className="mt-3 text-sm leading-6 text-slate-400">{provider.notes}</p>
                  </div>
                ))}
              </div>
            </div>
          </section>
        </>
      )}
    </div>
  )
}

function MetricCard({
  icon: Icon,
  label,
  value,
}: {
  icon: ComponentType<{ className?: string }>
  label: string
  value: string
}) {
  return (
    <div className="rounded-[1.8rem] border border-white/8 bg-white/5 p-5 shadow-[var(--fc-shadow-card)] backdrop-blur-xl">
      <Icon className="h-5 w-5 text-[color:var(--fc-color-accent)]" />
      <p className="mt-4 text-sm text-slate-400">{label}</p>
      <p className="mt-2 font-[var(--fc-font-display)] text-3xl text-white">{value}</p>
    </div>
  )
}

export default App
