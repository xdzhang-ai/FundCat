import type {
  DashboardResponse,
  FundCard,
  FundDetail,
} from '@fundcat/contracts'
import {
  BellRing,
  ChartCandlestick,
  DatabaseZap,
  LayoutDashboard,
  LogOut,
  Radar,
  ScanSearch,
  Search,
  ShieldAlert,
  Sparkles,
  WalletCards,
} from 'lucide-react'
import type { ComponentType, FormEvent, PropsWithChildren } from 'react'
import { useEffect, useState } from 'react'
import { api, authStorage } from './api/client'
import { SectionCard } from './components/SectionCard'
import { StatCard } from './components/StatCard'
import { FundTrendChart } from './components/charts/FundTrendChart'

type ScreenState =
  | { status: 'loading' }
  | { status: 'auth' }
  | { status: 'ready'; dashboard: DashboardResponse; funds: FundCard[]; selectedFund: FundDetail }
  | { status: 'error'; message: string }

const navItems = [
  { id: 'overview', label: '仪表盘', icon: LayoutDashboard },
  { id: 'funds', label: '基金中心', icon: ChartCandlestick },
  { id: 'portfolio', label: '组合仓位', icon: WalletCards },
  { id: 'ops', label: '研究开关', icon: ShieldAlert },
] as const

const quickSignalItems = [
  {
    label: '数据源',
    value: 'demo-aggregator',
    helper: '可替换 provider 适配层',
    icon: DatabaseZap,
  },
  {
    label: 'OCR 状态',
    value: '排队导入',
    helper: '截图上传后由后台异步识别',
    icon: ScanSearch,
  },
  {
    label: '提醒中心',
    value: 'Inbox',
    helper: '默认走站内消息',
    icon: BellRing,
  },
]

function App() {
  const [screen, setScreen] = useState<ScreenState>({ status: 'loading' })
  const [search, setSearch] = useState('')
  const [phone, setPhone] = useState('10000000000')
  const [password, setPassword] = useState('ChangeMe123!')
  const [actionMessage, setActionMessage] = useState<string | null>(null)

  const selectedCode =
    screen.status === 'ready' ? screen.selectedFund.code : ''

  useEffect(() => {
    if (!authStorage.hasToken()) {
      setScreen({ status: 'auth' })
      return
    }
    void loadApp()
  }, [])

  async function loadApp(preferredFundCode?: string) {
    try {
      setScreen({ status: 'loading' })
      const [dashboard, funds] = await Promise.all([api.dashboard(), api.funds()])
      const focusCode = preferredFundCode ?? dashboard.watchlist[0]?.code ?? funds[0]?.code ?? '000001'
      const selectedFund = await api.fundDetail(focusCode)
      setScreen({ status: 'ready', dashboard, funds, selectedFund })
    } catch (error) {
      authStorage.clear()
      const message = error instanceof Error ? error.message : 'Unexpected error'
      setScreen(message.includes('401') ? { status: 'auth' } : { status: 'error', message })
    }
  }

  async function handleLogin(event: FormEvent) {
    event.preventDefault()
    try {
      await api.login({ phone, password })
      await loadApp()
    } catch (error) {
      setScreen({ status: 'error', message: error instanceof Error ? error.message : 'Login failed' })
    }
  }

  async function handleSearch(value: string) {
    setSearch(value)
    if (screen.status !== 'ready') return
    const funds = await api.funds(value)
    setScreen({ ...screen, funds })
  }

  async function selectFund(code: string) {
    if (screen.status !== 'ready') return
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
        await api.createOrder({
          portfolioId: screen.dashboard.portfolios[0].id,
          fundCode: screen.selectedFund.code,
          orderType: 'BUY',
          amount: 1200,
          shares: 780,
          fee: 1.2,
          note: '本地演示快速模拟买入',
        })
        setActionMessage(`已为 ${screen.selectedFund.code} 创建一笔模拟买入`)
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
      <div className="relative mx-auto flex min-h-screen max-w-6xl items-center px-6 py-12">
        <div className="grid w-full gap-8 lg:grid-cols-[1.1fr_0.9fr]">
          <div className="rounded-[2rem] border border-white/8 bg-white/5 p-8 shadow-[var(--fc-shadow-card)] backdrop-blur-xl">
            <div className="inline-flex items-center gap-2 rounded-full border border-[color:var(--fc-color-accent)]/30 bg-[color:var(--fc-color-accent)]/10 px-4 py-2 text-xs uppercase tracking-[0.24em] text-[color:var(--fc-color-accent)]">
              FundCat Demo Workspace
            </div>
            <h1 className="mt-6 max-w-xl font-[var(--fc-font-display)] text-5xl font-semibold text-white">
              面向基金观察、模拟决策与运营控制的演示工作台
            </h1>
            <p className="mt-6 max-w-2xl text-lg text-slate-300">
              这个首版实现了登录、基金搜索与详情、持仓看板、模拟买卖、模拟定投、OCR 导入占位、周报与高风险功能开关。
            </p>
            <div className="mt-10 grid gap-4 sm:grid-cols-3">
              {quickSignalItems.map((item) => (
                <div key={item.label} className="rounded-3xl border border-white/8 bg-slate-950/40 p-4">
                  <item.icon className="h-5 w-5 text-[color:var(--fc-color-accent)]" />
                  <p className="mt-4 text-sm text-slate-400">{item.label}</p>
                  <p className="mt-1 font-[var(--fc-font-display)] text-lg text-white">{item.value}</p>
                  <p className="mt-1 text-sm text-slate-500">{item.helper}</p>
                </div>
              ))}
            </div>
          </div>

          <form
            onSubmit={handleLogin}
            className="rounded-[2rem] border border-white/8 bg-[color:var(--fc-color-surface-glass)] p-8 shadow-[var(--fc-shadow-card)] backdrop-blur-xl"
          >
            <p className="text-sm uppercase tracking-[0.28em] text-[color:var(--fc-color-accent)]">Demo Access</p>
            <h2 className="mt-4 font-[var(--fc-font-display)] text-3xl font-semibold text-white">登录研究版</h2>
            <p className="mt-3 text-slate-400">默认演示账号已预置，你也可以直接拿这些凭证启动前后端联调。</p>
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
            {screen.status === 'error' ? (
              <p className="mt-4 text-sm text-orange-300">{screen.message}</p>
            ) : null}
            <button className="mt-8 inline-flex w-full items-center justify-center rounded-2xl bg-[color:var(--fc-color-accent)] px-4 py-3 font-semibold text-slate-950 transition hover:brightness-105">
              进入研究工作台
            </button>
            <p className="mt-4 text-xs text-slate-500">Demo credentials: 10000000000 / ChangeMe123!</p>
          </form>
        </div>
      </div>
    )
  }

  const { dashboard, funds, selectedFund } = screen
  const visibleFunds = funds.slice(0, 8)

  return (
    <div className="mx-auto flex min-h-screen max-w-[1600px] flex-col gap-6 px-4 py-4 lg:px-6">
      <header className="sticky top-4 z-20 rounded-[2rem] border border-white/8 bg-slate-950/65 px-5 py-4 shadow-[var(--fc-shadow-card)] backdrop-blur-xl">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex items-center gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[color:var(--fc-color-accent)] text-slate-950">
              <Sparkles className="h-6 w-6" />
            </div>
            <div>
              <p className="text-sm uppercase tracking-[0.24em] text-[color:var(--fc-color-accent)]">FundCat</p>
              <h1 className="font-[var(--fc-font-display)] text-2xl font-semibold text-white">
                {dashboard.profile.displayName}
              </h1>
            </div>
          </div>
          <nav className="grid grid-cols-2 gap-2 md:flex">
            {navItems.map(({ id, label, icon: Icon }) => (
              <a
                key={id}
                href={`#${id}`}
                className="inline-flex items-center justify-center gap-2 rounded-full border border-white/8 bg-white/5 px-4 py-2 text-sm text-slate-300 transition hover:border-[color:var(--fc-color-accent)]/50 hover:text-white"
              >
                <Icon className="h-4 w-4" />
                {label}
              </a>
            ))}
          </nav>
          <button
            onClick={() => {
              authStorage.clear()
              setScreen({ status: 'auth' })
            }}
            className="inline-flex items-center gap-2 self-start rounded-full border border-white/10 px-4 py-2 text-sm text-slate-300 transition hover:border-white/20 hover:text-white"
          >
            <LogOut className="h-4 w-4" />
            退出
          </button>
        </div>
      </header>

      <section
        id="overview"
        className="grid gap-6 rounded-[2rem] border border-white/8 bg-[linear-gradient(135deg,rgba(243,186,47,0.12),rgba(8,12,20,0.2)_45%,rgba(56,189,248,0.08))] px-5 py-8 shadow-[var(--fc-shadow-card)] lg:grid-cols-[1.3fr_0.7fr]"
      >
        <div>
          <p className="text-sm uppercase tracking-[0.28em] text-[color:var(--fc-color-accent)]">Research Mode</p>
          <h2 className="mt-4 max-w-3xl font-[var(--fc-font-display)] text-4xl font-semibold text-white md:text-5xl">
            盘中参考估值、模拟交易与 OCR 导入在同一张研究画布上协同工作。
          </h2>
          <p className="mt-5 max-w-2xl text-lg text-slate-300">
            研究版默认将高风险能力收敛在 Feature Flag 与数据适配层之后，面向公开上线时可以按环境一键关闭。
          </p>
          <div className="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {dashboard.heroMetrics.map((metric) => (
              <StatCard key={metric.label} {...metric} />
            ))}
          </div>
        </div>

        <SectionCard title="研究快照" eyebrow="Signal board">
          <div className="space-y-4">
            {quickSignalItems.map((item) => (
              <div
                key={item.label}
                className="flex items-start gap-4 rounded-3xl border border-white/8 bg-slate-950/40 p-4"
              >
                <item.icon className="mt-0.5 h-5 w-5 text-[color:var(--fc-color-accent)]" />
                <div>
                  <p className="text-sm text-slate-400">{item.label}</p>
                  <p className="font-medium text-white">{item.value}</p>
                  <p className="mt-1 text-sm text-slate-500">{item.helper}</p>
                </div>
              </div>
            ))}
            {actionMessage ? (
              <div className="rounded-3xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-200">
                {actionMessage}
              </div>
            ) : null}
          </div>
        </SectionCard>
      </section>

      <section id="funds" className="grid gap-6 xl:grid-cols-[0.42fr_0.58fr]">
        <SectionCard
          title="基金雷达"
          eyebrow="Fund center"
          action={
            <label className="flex items-center gap-2 rounded-full border border-white/10 bg-slate-950/50 px-4 py-2 text-sm text-slate-300">
              <Search className="h-4 w-4" />
              <input
                value={search}
                onChange={(event) => void handleSearch(event.target.value)}
                placeholder="基金代码 / 名称"
                className="w-32 bg-transparent outline-none placeholder:text-slate-500 md:w-48"
              />
            </label>
          }
        >
          <div className="space-y-3">
            {visibleFunds.map((fund) => (
              <button
                key={fund.code}
                onClick={() => void selectFund(fund.code)}
                className={`w-full rounded-[1.6rem] border px-4 py-4 text-left transition ${
                  fund.code === selectedCode
                    ? 'border-[color:var(--fc-color-accent)]/60 bg-[color:var(--fc-color-accent)]/10'
                    : 'border-white/8 bg-white/5 hover:border-white/20'
                }`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-[var(--fc-font-display)] text-lg text-white">{fund.name}</p>
                    <p className="mt-1 text-sm text-slate-400">
                      {fund.code} · {fund.category} · {fund.riskLevel}
                    </p>
                  </div>
                  <span
                    className={`rounded-full px-3 py-1 text-xs ${
                      fund.estimatedGrowth >= 0
                        ? 'bg-emerald-400/10 text-emerald-300'
                        : 'bg-orange-400/10 text-orange-300'
                    }`}
                  >
                    {fund.estimatedGrowth >= 0 ? '+' : ''}
                    {fund.estimatedGrowth}%
                  </span>
                </div>
                <div className="mt-4 flex flex-wrap gap-2">
                  {fund.tags.map((tag) => (
                    <span key={tag} className="rounded-full border border-white/10 px-3 py-1 text-xs text-slate-300">
                      {tag}
                    </span>
                  ))}
                </div>
              </button>
            ))}
          </div>
        </SectionCard>

        <SectionCard
          title={selectedFund.name}
          eyebrow="Selected fund"
          action={
            <div className="flex flex-wrap gap-2">
              <QuickActionButton icon={Radar} label="加自选" onClick={() => void runQuickAction('watchlist')} />
              <QuickActionButton icon={WalletCards} label="模拟买入" onClick={() => void runQuickAction('order')} />
              <QuickActionButton icon={BellRing} label="设定投" onClick={() => void runQuickAction('sip')} />
              <QuickActionButton icon={ScanSearch} label="建 OCR 任务" onClick={() => void runQuickAction('ocr')} />
            </div>
          }
        >
          <div className="grid gap-5 xl:grid-cols-[0.66fr_0.34fr]">
            <div className="rounded-[1.75rem] border border-white/8 bg-slate-950/40 p-4">
              <div className="mb-4 flex flex-wrap items-center gap-3">
                <p className="font-[var(--fc-font-display)] text-3xl text-white">{selectedFund.estimatedNav}</p>
                <span
                  className={`rounded-full px-3 py-1 text-sm ${
                    selectedFund.estimatedGrowth >= 0
                      ? 'bg-emerald-400/10 text-emerald-300'
                      : 'bg-orange-400/10 text-orange-300'
                  }`}
                >
                  {selectedFund.estimatedGrowth >= 0 ? '+' : ''}
                  {selectedFund.estimatedGrowth}%
                </span>
                <span className="rounded-full border border-[color:var(--fc-color-accent)]/25 bg-[color:var(--fc-color-accent)]/10 px-3 py-1 text-xs uppercase tracking-[0.24em] text-[color:var(--fc-color-accent)]">
                  reference only
                </span>
              </div>
              <FundTrendChart data={selectedFund.navHistory} />
            </div>

            <div className="space-y-4">
              <div className="rounded-[1.75rem] border border-white/8 bg-white/5 p-4">
                <p className="text-sm text-slate-400">基金画像</p>
                <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                  <Metric label="管理费" value={`${selectedFund.managementFee}%`} />
                  <Metric label="托管费" value={`${selectedFund.custodyFee}%`} />
                  <Metric label="申购费" value={`${selectedFund.purchaseFee}%`} />
                  <Metric label="规模" value={`${selectedFund.assetSize} 亿`} />
                  <Metric label="股票仓位" value={`${Math.round(selectedFund.stockRatio * 100)}%`} />
                  <Metric label="债券仓位" value={`${Math.round(selectedFund.bondRatio * 100)}%`} />
                </div>
              </div>

              <div className="rounded-[1.75rem] border border-white/8 bg-white/5 p-4">
                <p className="text-sm text-slate-400">前排持仓</p>
                <div className="mt-4 flex flex-wrap gap-2">
                  {selectedFund.topHoldings.map((holding) => (
                    <span key={holding} className="rounded-full bg-slate-950/60 px-3 py-2 text-sm text-slate-200">
                      {holding}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </SectionCard>
      </section>

      <section id="portfolio" className="grid gap-6 xl:grid-cols-[0.54fr_0.46fr]">
        <SectionCard title="自选与组合" eyebrow="Portfolio intelligence">
          <div className="grid gap-4 lg:grid-cols-2">
            <div className="rounded-[1.75rem] border border-white/8 bg-slate-950/35 p-4">
              <p className="text-sm text-slate-400">自选观察</p>
              <div className="mt-4 space-y-3">
                {dashboard.watchlist.map((item) => (
                  <div key={item.code} className="flex items-center justify-between rounded-2xl bg-white/5 px-4 py-3">
                    <div>
                      <p className="font-medium text-white">{item.name}</p>
                      <p className="text-xs text-slate-500">
                        {item.code} · {item.note}
                      </p>
                    </div>
                    <p className={item.estimatedGrowth >= 0 ? 'text-emerald-300' : 'text-orange-300'}>
                      {item.estimatedGrowth >= 0 ? '+' : ''}
                      {item.estimatedGrowth}%
                    </p>
                  </div>
                ))}
              </div>
            </div>

            <div className="rounded-[1.75rem] border border-white/8 bg-slate-950/35 p-4">
              <p className="text-sm text-slate-400">模拟流水</p>
              <div className="mt-4 space-y-3">
                {dashboard.orders.map((order) => (
                  <div key={order.id} className="rounded-2xl bg-white/5 px-4 py-3">
                    <div className="flex items-center justify-between">
                      <p className="font-medium text-white">{order.fundName}</p>
                      <span
                        className={`rounded-full px-3 py-1 text-xs ${
                          order.orderType === 'BUY'
                            ? 'bg-emerald-400/10 text-emerald-300'
                            : 'bg-orange-400/10 text-orange-300'
                        }`}
                      >
                        {order.orderType}
                      </span>
                    </div>
                    <p className="mt-1 text-sm text-slate-400">
                      ¥{order.amount} · {order.executedAt.replace('T', ' ')}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="mt-4 grid gap-4 lg:grid-cols-2">
            {dashboard.portfolios.map((portfolio) => (
              <div key={portfolio.id} className="rounded-[1.75rem] border border-white/8 bg-white/5 p-4">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="font-[var(--fc-font-display)] text-xl text-white">{portfolio.name}</p>
                    <p className="mt-1 text-sm text-slate-400">{portfolio.broker}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm text-slate-400">组合市值</p>
                    <p className="font-[var(--fc-font-display)] text-2xl text-white">¥{portfolio.marketValue}</p>
                    <p className={portfolio.totalPnl >= 0 ? 'text-emerald-300' : 'text-orange-300'}>
                      {portfolio.totalPnl >= 0 ? '+' : ''}
                      {portfolio.totalPnl}
                    </p>
                  </div>
                </div>
                <div className="mt-4 space-y-2">
                  {portfolio.holdings.map((holding) => (
                    <div key={holding.id} className="grid grid-cols-[1fr_auto_auto] items-center gap-3 rounded-2xl bg-slate-950/45 px-4 py-3">
                      <div>
                        <p className="text-sm font-medium text-white">{holding.fundName}</p>
                        <p className="text-xs text-slate-500">
                          {holding.fundCode} · {holding.source}
                        </p>
                      </div>
                      <p className="text-sm text-slate-300">¥{holding.currentValue}</p>
                      <p className={holding.pnl >= 0 ? 'text-emerald-300' : 'text-orange-300'}>
                        {holding.pnl >= 0 ? '+' : ''}
                        {holding.pnl}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard title="周报 / 定投 / OCR" eyebrow="Automation surfaces">
          <div className="space-y-4">
            <Panel title="模拟定投">
              {dashboard.sipPlans.map((plan) => (
                <Row
                  key={plan.id}
                  title={plan.fundName}
                  meta={`${plan.cadence} · 下次 ${plan.nextRunAt.replace('T', ' ')}`}
                  value={`¥${plan.amount}`}
                />
              ))}
            </Panel>
            <Panel title="OCR 导入">
              {dashboard.importJobs.map((job) => (
                <Row
                  key={job.id}
                  title={job.fileName}
                  meta={`${job.sourcePlatform} · ${job.createdAt.replace('T', ' ')}`}
                  value={job.status}
                />
              ))}
            </Panel>
            <Panel title="研究周报">
              {dashboard.reports.map((report) => (
                <div key={report.id} className="rounded-2xl bg-white/5 px-4 py-3">
                  <div className="flex items-center justify-between gap-4">
                    <p className="font-medium text-white">{report.weekLabel}</p>
                    <p className={report.returnRate >= 0 ? 'text-emerald-300' : 'text-orange-300'}>
                      {report.returnRate >= 0 ? '+' : ''}
                      {report.returnRate}%
                    </p>
                  </div>
                  <p className="mt-2 text-sm text-slate-300">{report.summary}</p>
                  <p className="mt-1 text-xs text-slate-500">{report.riskNote}</p>
                </div>
              ))}
            </Panel>
          </div>
        </SectionCard>
      </section>

      <section id="ops" className="grid gap-6 xl:grid-cols-[0.45fr_0.55fr]">
        <SectionCard title="研究能力开关" eyebrow="Feature flags">
          <div className="space-y-3">
            {dashboard.featureFlags.map((flag) => (
              <div key={flag.code} className="rounded-[1.6rem] border border-white/8 bg-white/5 p-4">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="font-medium text-white">{flag.name}</p>
                    <p className="mt-1 text-sm text-slate-400">{flag.description}</p>
                  </div>
                  <span
                    className={`rounded-full px-3 py-1 text-xs uppercase tracking-[0.18em] ${
                      flag.enabled
                        ? 'bg-emerald-400/10 text-emerald-300'
                        : 'bg-slate-500/15 text-slate-300'
                    }`}
                  >
                    {flag.environment}
                  </span>
                </div>
                <div className="mt-4 flex items-center justify-between text-sm text-slate-400">
                  <span>{flag.code}</span>
                  <span className="rounded-full border border-white/10 px-3 py-1">{flag.riskLevel}</span>
                </div>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard title="移动端预埋关注点" eyebrow="RN ready">
          <div className="grid gap-4 md:grid-cols-2">
            {[
              ['统一鉴权', 'Bearer token + refresh 协议已统一，Web 与未来 RN 不再拆两套会话逻辑。'],
              ['共享 Token', '颜色、圆角、阴影、字体全部沉淀为 design tokens，可映射到 RN。'],
              ['标准图表数据', '后端输出趋势序列，Web 只负责渲染，后续切换 RN 图表库无需改 API。'],
              ['上传/OCR 协议', '截图上传与任务轮询按跨端协议设计，不绑定浏览器专属流程。'],
            ].map(([title, description]) => (
              <div key={title} className="rounded-[1.75rem] border border-white/8 bg-slate-950/35 p-5">
                <p className="font-[var(--fc-font-display)] text-lg text-white">{title}</p>
                <p className="mt-3 text-sm leading-6 text-slate-400">{description}</p>
              </div>
            ))}
          </div>
        </SectionCard>
      </section>
    </div>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl bg-slate-950/50 px-3 py-3">
      <p className="text-xs uppercase tracking-[0.16em] text-slate-500">{label}</p>
      <p className="mt-2 font-medium text-white">{value}</p>
    </div>
  )
}

function Panel({ title, children }: PropsWithChildren<{ title: string }>) {
  return (
    <div className="rounded-[1.75rem] border border-white/8 bg-slate-950/35 p-4">
      <p className="mb-4 text-sm text-slate-400">{title}</p>
      <div className="space-y-3">{children}</div>
    </div>
  )
}

function Row({ title, meta, value }: { title: string; meta: string; value: string }) {
  return (
    <div className="flex items-start justify-between gap-4 rounded-2xl bg-white/5 px-4 py-3">
      <div>
        <p className="font-medium text-white">{title}</p>
        <p className="mt-1 text-sm text-slate-500">{meta}</p>
      </div>
      <p className="text-sm text-slate-200">{value}</p>
    </div>
  )
}

function QuickActionButton({
  icon: Icon,
  label,
  onClick,
}: {
  icon: ComponentType<{ className?: string }>
  label: string
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-200 transition hover:border-[color:var(--fc-color-accent)]/50 hover:text-white"
    >
      <Icon className="h-4 w-4" />
      {label}
    </button>
  )
}

function LoadingScreen() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <div className="rounded-[2rem] border border-white/8 bg-white/5 px-8 py-6 text-center text-slate-300 shadow-[var(--fc-shadow-card)] backdrop-blur-xl">
        <Sparkles className="mx-auto h-8 w-8 text-[color:var(--fc-color-accent)]" />
        <p className="mt-4 font-[var(--fc-font-display)] text-xl text-white">正在载入 FundCat 工作台…</p>
      </div>
    </div>
  )
}

export default App
