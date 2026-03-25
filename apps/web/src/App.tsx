import type { DashboardResponse, FundCard, FundDetail } from '@fundcat/contracts'
import { BellRing, ChartCandlestick, LayoutDashboard, LogOut, Sparkles, WalletCards } from 'lucide-react'
import type { FormEvent } from 'react'
import { useEffect, useState } from 'react'
import { matchPath, Navigate, NavLink, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { api, authStorage } from './api/client'
import { LoadingScreen } from './components/workspace/WebUi'
import { AuthPage } from './pages/AuthPage'
import { AutomationPage } from './pages/AutomationPage'
import { FundDetailPage } from './pages/FundDetailPage'
import { FundsPage } from './pages/FundsPage'
import { HoldingsPage } from './pages/HoldingsPage'
import { OverviewPage } from './pages/OverviewPage'
import { PortfolioPage } from './pages/PortfolioPage'

type ScreenState =
  | { status: 'loading' }
  | { status: 'auth' }
  | { status: 'ready'; dashboard: DashboardResponse; funds: FundCard[]; selectedFund: FundDetail }
  | { status: 'error'; message: string }

type PageId = 'overview' | 'funds' | 'holdings' | 'portfolio' | 'automation'

const navItems: Array<{
  id: PageId
  label: string
  icon: typeof LayoutDashboard
  path: string
}> = [
  { id: 'overview', label: '仪表盘', icon: LayoutDashboard, path: '/' },
  { id: 'funds', label: '基金中心', icon: ChartCandlestick, path: '/funds' },
  { id: 'holdings', label: '持仓页', icon: WalletCards, path: '/holdings' },
  { id: 'portfolio', label: '自选组合', icon: WalletCards, path: '/portfolio' },
  { id: 'automation', label: '自动化', icon: BellRing, path: '/automation' },
]

const pageMeta: Record<PageId, { eyebrow: string; title: string; description: string }> = {
  overview: {
    eyebrow: 'Research Workspace',
    title: '仪表盘',
    description: '查看研究版的核心指标、关注基金和最近动作，快速进入今天的工作上下文。',
  },
  funds: {
    eyebrow: 'Fund Center',
    title: '基金中心',
    description: '先看基金列表与搜索结果，再进入独立基金详情页做更深的走势和持仓分析。',
  },
  holdings: {
    eyebrow: 'Holding Book',
    title: '持仓页',
    description: '聚合当前持有的基金，快速查看仓位、收益和成本，并直接进入对应基金详情。',
  },
  portfolio: {
    eyebrow: 'Portfolio Intelligence',
    title: '自选组合',
    description: '将自选观察、模拟流水和组合持仓拆到独立页面，浏览时更聚焦。',
  },
  automation: {
    eyebrow: 'Automation Surfaces',
    title: '自动化',
    description: '统一查看定投、OCR 导入、周报和提醒规则，方便后续继续扩展后台联动。',
  },
}

function currentPageId(pathname: string): PageId {
  if (pathname.startsWith('/funds')) return 'funds'
  if (pathname.startsWith('/holdings')) return 'holdings'
  if (pathname.startsWith('/portfolio')) return 'portfolio'
  if (pathname.startsWith('/automation')) return 'automation'
  return 'overview'
}

function currentFundCode(pathname: string) {
  return matchPath('/funds/:fundCode', pathname)?.params.fundCode ?? null
}

function App() {
  const location = useLocation()
  const navigate = useNavigate()
  const [screen, setScreen] = useState<ScreenState>({ status: 'loading' })
  const [search, setSearch] = useState('')
  const [username, setUsername] = useState('demo_analyst')
  const [password, setPassword] = useState('ChangeMe123!')
  const [actionMessage, setActionMessage] = useState<string | null>(null)

  const activePage = currentPageId(location.pathname)
  const routeFundCode = currentFundCode(location.pathname)
  const selectedCode = screen.status === 'ready' ? screen.selectedFund.code : ''
  const featureFlags = screen.status === 'ready' ? screen.dashboard.featureFlags : []
  const isFlagEnabled = (code: string) => featureFlags.find((flag) => flag.code === code)?.enabled ?? false

  useEffect(() => {
    if (!authStorage.hasToken()) {
      setScreen({ status: 'auth' })
      return
    }
    void loadApp(routeFundCode ?? undefined)
  }, [])

  useEffect(() => {
    if (screen.status !== 'ready' || activePage !== 'funds' || !routeFundCode || routeFundCode === screen.selectedFund.code) {
      return
    }
    void selectFund(routeFundCode, { navigateToRoute: false })
  }, [activePage, routeFundCode, screen.status, selectedCode])

  async function loadApp(preferredFundCode?: string) {
    try {
      setScreen({ status: 'loading' })
      const [dashboard, funds] = await Promise.all([api.dashboard(), api.funds()])
      const focusCode = preferredFundCode ?? dashboard.watchlist[0]?.code ?? funds[0]?.code ?? '000001'
      let selectedFund: FundDetail
      try {
        selectedFund = await api.fundDetail(focusCode)
      } catch (error) {
        const fallbackCode = dashboard.watchlist[0]?.code ?? funds[0]?.code
        if (!fallbackCode || fallbackCode === focusCode) {
          throw error
        }
        selectedFund = await api.fundDetail(fallbackCode)
      }
      setScreen({ status: 'ready', dashboard, funds, selectedFund })
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unexpected error'
      if (message.includes('401')) {
        authStorage.clear()
        setScreen({ status: 'auth' })
        return
      }
      setScreen({ status: 'error', message })
    }
  }

  function navigateToPage(page: PageId) {
    const target = navItems.find((item) => item.id === page)?.path ?? '/'
    navigate(target)
  }

  async function handleLogin(event: FormEvent) {
    event.preventDefault()
    try {
      await api.login({ username, password })
      await loadApp(routeFundCode ?? undefined)
    } catch (error) {
      setScreen({ status: 'error', message: error instanceof Error ? error.message : 'Login failed' })
    }
  }

  async function handleLogout() {
    await api.logout()
    authStorage.clear()
    setScreen({ status: 'auth' })
  }

  async function handleSearch(value: string) {
    setSearch(value)
    if (screen.status !== 'ready') return
    const funds = await api.funds(value)
    setScreen({ ...screen, funds })
  }

  async function selectFund(
    code: string,
    options: { navigateToRoute?: boolean } = {},
  ) {
    if (screen.status !== 'ready') return
    const { navigateToRoute = true } = options
    if (navigateToRoute) {
      navigate(`/funds/${code}`)
    }
    const selectedFund = await api.fundDetail(code)
    setScreen({ ...screen, selectedFund })
  }

  async function runQuickAction(kind: 'watchlist' | 'order' | 'sip' | 'ocr') {
    if (screen.status !== 'ready') return
    try {
      if (kind === 'watchlist') {
        await api.addWatchlist({
          fundCode: screen.selectedFund.code,
          note: `新增于 ${new Date().toLocaleDateString('zh-CN')}`,
        })
        setActionMessage(`已将 ${screen.selectedFund.name} 加入自选`)
      }
      if (kind === 'order') {
        const matchingHolding = screen.dashboard.portfolios
          .flatMap((portfolio) => portfolio.holdings.map((holding) => ({ portfolioId: portfolio.id, holding })))
          .find((item) => item.holding.fundCode === screen.selectedFund.code)
        const orderType = matchingHolding ? 'SELL' : 'BUY'
        const shares = matchingHolding
          ? Math.min(matchingHolding.holding.shares, Math.max(120, Math.min(360, Math.round(matchingHolding.holding.shares * 0.12))))
          : 780
        const amount = Number((screen.selectedFund.estimatedNav * shares).toFixed(2))
        await api.createOrder({
          portfolioId: matchingHolding?.portfolioId ?? screen.dashboard.portfolios[0].id,
          fundCode: screen.selectedFund.code,
          orderType,
          amount,
          shares,
          fee: 1.2,
          note: orderType === 'BUY' ? '本地演示快速模拟买入' : '本地演示快速模拟卖出',
        })
        setActionMessage(`已为 ${screen.selectedFund.code} 创建一笔模拟${orderType === 'BUY' ? '买入' : '卖出'}`)
      }
      if (kind === 'sip') {
        await api.createSip({
          portfolioId: screen.dashboard.portfolios[0].id,
          fundCode: screen.selectedFund.code,
          amount: 500,
          cadence: 'WEEKLY',
          nextRunAt: new Date(Date.now() + 3 * 24 * 3600 * 1000).toISOString().slice(0, 19),
        })
        setActionMessage(`已为 ${screen.selectedFund.code} 创建周定投计划`)
      }
      if (kind === 'ocr') {
        await api.createImportJob({
          sourcePlatform: '示例导入',
          fileName: `${screen.selectedFund.code}-mock-upload.png`,
        })
        setActionMessage('已创建 OCR 导入任务')
      }
      await loadApp(selectedCode)
    } catch (error) {
      setActionMessage(error instanceof Error ? error.message : 'Action failed')
    }
  }

  if (screen.status === 'loading') {
    return <LoadingScreen />
  }

  if (screen.status === 'auth' || screen.status === 'error') {
    return (
      <AuthPage
        username={username}
        password={password}
        errorMessage={screen.status === 'error' ? screen.message : undefined}
        onUsernameChange={setUsername}
        onPasswordChange={setPassword}
        onSubmit={handleLogin}
      />
    )
  }

  const { dashboard, funds, selectedFund } = screen
  const currentPageMeta = pageMeta[activePage]

  return (
    <div className="mx-auto min-h-screen max-w-[1600px] px-4 py-4 lg:px-6">
      <header className="mb-6 border-b border-white/8 px-1 pb-4">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-[color:var(--fc-color-accent)] text-slate-950">
              <Sparkles className="h-7 w-7" />
            </div>
            <div>
              <p className="text-sm uppercase tracking-[0.28em] text-[color:var(--fc-color-accent)]">FundCat</p>
              <p className="mt-1 text-sm text-slate-400">Research workspace for simulated fund decisions</p>
            </div>
          </div>
        </div>
      </header>

      <div className="grid gap-5 lg:grid-cols-[236px_minmax(0,1fr)]">
        <aside className="rounded-[1.75rem] border border-white/8 bg-slate-950/70 p-4 shadow-[var(--fc-shadow-card)] backdrop-blur-xl lg:sticky lg:top-4 lg:h-[calc(100vh-7.5rem)]">
          <div className="flex h-full flex-col">
            <nav className="grid gap-2">
              {navItems.map(({ id, label, icon: Icon, path }) => (
                <NavLink
                  key={id}
                  to={path}
                  onClick={(event) => {
                    if (activePage === id) {
                      event.preventDefault()
                      navigateToPage(id)
                    }
                  }}
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

        <main className="min-w-0 space-y-6">
          <header className="rounded-[2rem] border border-white/8 bg-[color:var(--fc-color-surface-glass)] px-5 py-6 shadow-[var(--fc-shadow-card)] backdrop-blur-xl">
            <p className="text-sm uppercase tracking-[0.24em] text-[color:var(--fc-color-accent)]">{currentPageMeta.eyebrow}</p>
            <div className="mt-4 flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
              <div>
                <h2 className="font-[var(--fc-font-display)] text-4xl font-semibold text-white">{currentPageMeta.title}</h2>
                <p className="mt-3 max-w-3xl text-slate-300">{currentPageMeta.description}</p>
              </div>
              <div className="flex flex-wrap gap-2">
                <span className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-300">
                  焦点基金 {selectedFund.code}
                </span>
                <span className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-300">
                  {selectedFund.referenceOnly ? '实时涨跌参考' : '前一交易日涨跌'}
                </span>
              </div>
            </div>
          </header>

          {actionMessage ? (
            <div className="rounded-3xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-200">
              {actionMessage}
            </div>
          ) : null}

          <Routes>
            <Route path="/" element={<OverviewPage dashboard={dashboard} selectedFund={selectedFund} isFlagEnabled={isFlagEnabled} />} />
            <Route
              path="/funds"
              element={
                <FundsPage
                  search={search}
                  funds={funds}
                  selectedCode={selectedCode}
                  onSearchChange={(value) => void handleSearch(value)}
                  onSelectFund={(code) => navigate(`/funds/${code}`)}
                />
              }
            />
            <Route
              path="/funds/:fundCode"
              element={
                <FundDetailPage
                  dashboard={dashboard}
                  funds={funds}
                  selectedFund={selectedFund}
                  isFlagEnabled={isFlagEnabled}
                  onBack={() => navigate(-1)}
                  onQuickAction={(kind) => void runQuickAction(kind)}
                />
              }
            />
            <Route
              path="/holdings"
              element={<HoldingsPage dashboard={dashboard} funds={funds} onOpenFund={(code) => navigate(`/funds/${code}`)} />}
            />
            <Route path="/portfolio" element={<PortfolioPage dashboard={dashboard} />} />
            <Route path="/automation" element={<AutomationPage dashboard={dashboard} isFlagEnabled={isFlagEnabled} />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  )
}

export default App
