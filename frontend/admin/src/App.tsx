/** 后台应用壳层，负责承接控制台布局、全局提示和运维页面路由装配。 */
import { LogOut, RefreshCcw, Sparkles } from 'lucide-react'
import { Navigate, NavLink, Route, Routes, useNavigate } from 'react-router-dom'
import { useAdminApp } from './common/workspace/useAdminApp'
import { LoginPage } from './modules/auth/pages/LoginPage'
import { FlagsPage } from './modules/flags/pages/FlagsPage'
import { OverviewPage } from './modules/overview/pages/OverviewPage'
import { ProvidersPage } from './modules/providers/pages/ProvidersPage'
import { QueuePage } from './modules/queue/pages/QueuePage'
import { ReportsPage } from './modules/reports/pages/ReportsPage'

function App() {
  const navigate = useNavigate()
  const admin = useAdminApp()

  if (admin.needsLogin) {
    return (
      <LoginPage
        username={admin.username}
        password={admin.password}
        message={admin.message}
        onUsernameChange={admin.setUsername}
        onPasswordChange={admin.setPassword}
        onSubmit={admin.handleLogin}
      />
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
              {admin.tabs.map(({ id, label, icon: Icon, path }) => (
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
              onClick={() => void admin.handleLogout()}
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
                <h1 className="font-[var(--fc-font-display)] text-4xl font-semibold text-[color:var(--fc-color-accent)]">{admin.activeTabConfig.label}</h1>
                <p className="mt-3 max-w-3xl text-slate-300">{admin.activeTabConfig.helper}</p>
              </div>
              <div className="flex flex-wrap gap-2">
                <button
                  onClick={() => void admin.loadConsole()}
                  className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-300 transition hover:border-white/20 hover:text-white"
                >
                  <RefreshCcw className={`h-4 w-4 ${admin.isRefreshing ? 'animate-spin' : ''}`} />
                  刷新
                </button>
              </div>
            </div>
          </header>

          {admin.message ? (
            <div className="mt-4 rounded-3xl border border-white/10 bg-slate-950/40 px-4 py-3 text-sm text-slate-300">{admin.message}</div>
          ) : null}

          {!admin.summary ? (
            <div className="mt-6 rounded-[2rem] border border-white/8 bg-white/5 px-6 py-10 text-center text-slate-300 shadow-[var(--fc-shadow-card)]">
              {admin.message || '正在载入后台数据...'}
            </div>
          ) : (
            <Routes>
              <Route
                path="/"
                element={
                  <OverviewPage
                    featureFlags={admin.summary.featureFlags}
                    providers={admin.summary.providers}
                    importJobCount={admin.importJobs.length}
                    onNavigate={navigate}
                  />
                }
              />
              <Route
                path="/flags"
                element={
                  <FlagsPage
                    featureFlags={admin.featureFlags}
                    isSavingFlagCode={admin.isSavingFlagCode}
                    onToggleFlag={(flag) => void admin.handleToggleFlag(flag)}
                  />
                }
              />
              <Route path="/providers" element={<ProvidersPage providers={admin.providers} />} />
              <Route path="/queue" element={<QueuePage importJobs={admin.importJobs} />} />
              <Route path="/reports" element={<ReportsPage reports={admin.reports} alerts={admin.alerts} />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          )}
        </main>
      </div>
    </div>
  )
}

export default App

