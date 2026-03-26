import type {
  AlertRule,
  DashboardResponse,
  FeatureFlag,
  FundCard,
  FundDetail,
  HoldingLot,
  ImportJob,
  PaperOrder,
  PortfolioSummary,
  SipPlan,
  WatchlistItem,
  WeeklyReport,
} from '@fundcat/contracts'
import { BellRing, ChartCandlestick, LayoutDashboard, LogOut, Sparkles, WalletCards } from 'lucide-react'
import type { FormEvent, ReactNode } from 'react'
import { useEffect, useRef, useState } from 'react'
import { matchPath, Navigate, NavLink, Route, Routes, useLocation, useNavigate, useNavigationType } from 'react-router-dom'
import { api, authStorage } from './api/client'
import { LoadingScreen } from './components/workspace/WebUi'
import { formatCurrency } from './lib/fundInsights'
import { AuthPage } from './pages/AuthPage'
import { AutomationPage } from './pages/AutomationPage'
import { FundDetailPage } from './pages/FundDetailPage'
import { FundsPage } from './pages/FundsPage'
import { HoldingsPage } from './pages/HoldingsPage'
import { OverviewPage } from './pages/OverviewPage'
import { PortfolioPage } from './pages/PortfolioPage'

type PageId = 'overview' | 'funds' | 'holdings' | 'portfolio' | 'automation'
type WatchlistGroup = '全部' | '成长进攻' | '稳健配置' | '行业主题'

type ScreenState =
  | { status: 'loading' }
  | { status: 'auth' }
  | { status: 'ready' }
  | { status: 'error'; message: string }

type AppDataState = {
  overviewDashboard: DashboardResponse | null
  featureFlags: FeatureFlag[] | null
  funds: FundCard[] | null
  selectedFund: FundDetail | null
  watchlist: WatchlistItem[] | null
  portfolios: PortfolioSummary[] | null
  orders: PaperOrder[] | null
  sipPlans: SipPlan[] | null
  reports: WeeklyReport[] | null
  alerts: AlertRule[] | null
  importJobs: ImportJob[] | null
}

type PendingWatchlistSelection = {
  code: string
  name: string
}

type PendingHoldingInput = {
  code: string
  name: string
  mode: 'add' | 'edit'
}

type LocalHoldingDraft = HoldingLot & {
  portfolioId: string
}

const navItems: Array<{
  id: PageId
  label: string
  icon: typeof LayoutDashboard
  path: string
}> = [
  { id: 'overview', label: '仪表盘', icon: LayoutDashboard, path: '/' },
  { id: 'funds', label: '基金中心', icon: ChartCandlestick, path: '/funds' },
  { id: 'holdings', label: '持仓页', icon: WalletCards, path: '/holdings' },
  { id: 'portfolio', label: '自选基金', icon: WalletCards, path: '/portfolio' },
  { id: 'automation', label: '基金定投', icon: BellRing, path: '/automation' },
]

const watchlistGroupOrder: WatchlistGroup[] = ['全部', '成长进攻', '稳健配置', '行业主题']

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
    eyebrow: 'Watchlist Book',
    title: '自选基金',
    description: '使用与持仓页一致的列表结构聚合当前自选基金，快速查看涨跌并进入基金详情。',
  },
  automation: {
    eyebrow: 'Fund SIP',
    title: '基金定投',
    description: '查看当前基金定投计划与下次执行时间，集中管理持续买入节奏。',
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

function detailBackTarget(state: unknown) {
  if (!state || typeof state !== 'object' || !('from' in state)) {
    return '/funds'
  }
  return typeof state.from === 'string' ? state.from : '/funds'
}

function defaultFundCodeFromDashboard(dashboard: DashboardResponse) {
  return dashboard.watchlist[0]?.code ?? '000001'
}

function buildPartialDashboard({
  profile,
  featureFlags = [],
  watchlist = [],
  portfolios = [],
  orders = [],
  sipPlans = [],
  reports = [],
  alerts = [],
  importJobs = [],
}: {
  profile?: DashboardResponse['profile']
  featureFlags?: FeatureFlag[]
  watchlist?: WatchlistItem[]
  portfolios?: PortfolioSummary[]
  orders?: PaperOrder[]
  sipPlans?: SipPlan[]
  reports?: WeeklyReport[]
  alerts?: AlertRule[]
  importJobs?: ImportJob[]
}): DashboardResponse {
  return {
    profile: profile ?? {
      id: '',
      displayName: '',
      username: '',
      riskMode: 'research',
    },
    heroMetrics: [],
    featureFlags,
    watchlist,
    portfolios,
    orders,
    sipPlans,
    reports,
    alerts,
    importJobs,
  }
}

function holdingCodesFromPortfolios(portfolios: PortfolioSummary[] | null) {
  return new Set(portfolios?.flatMap((portfolio) => portfolio.holdings.map((holding) => holding.fundCode)) ?? [])
}

function dedupeHoldingDrafts(drafts: LocalHoldingDraft[]) {
  const seen = new Set<string>()
  return drafts.filter((draft) => {
    const key = `${draft.portfolioId}:${draft.fundCode}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

function findHoldingTarget(code: string, portfolios: PortfolioSummary[] | null) {
  if (!portfolios) return null
  for (const portfolio of portfolios) {
    const holding = portfolio.holdings.find((item) => item.fundCode === code)
    if (holding) {
      return { portfolioId: portfolio.id, holding }
    }
  }
  return null
}

function App() {
  const location = useLocation()
  const navigate = useNavigate()
  const navigationType = useNavigationType()
  const [screen, setScreen] = useState<ScreenState>({ status: 'loading' })
  const [data, setData] = useState<AppDataState>({
    overviewDashboard: null,
    featureFlags: null,
    funds: null,
    selectedFund: null,
    watchlist: null,
    portfolios: null,
    orders: null,
    sipPlans: null,
    reports: null,
    alerts: null,
    importJobs: null,
  })
  const dataRef = useRef(data)
  const [fundDetailCache, setFundDetailCache] = useState<Record<string, FundDetail>>({})
  const fundDetailCacheRef = useRef(fundDetailCache)
  const [search, setSearch] = useState('')
  const [fundSuggestions, setFundSuggestions] = useState<FundCard[]>([])
  const [username, setUsername] = useState('demo_analyst')
  const [password, setPassword] = useState('ChangeMe123!')
  const [actionMessage, setActionMessage] = useState<string | null>(null)
  const [watchlistGroups, setWatchlistGroups] = useState<Record<string, WatchlistGroup[]>>({})
  const [pendingWatchlistSelection, setPendingWatchlistSelection] = useState<PendingWatchlistSelection | null>(null)
  const [pendingWatchlistGroups, setPendingWatchlistGroups] = useState<WatchlistGroup[]>(['全部'])
  const [pendingHoldingInput, setPendingHoldingInput] = useState<PendingHoldingInput | null>(null)
  const [pendingHoldingAmount, setPendingHoldingAmount] = useState('')
  const [pendingHoldingPnl, setPendingHoldingPnl] = useState('')
  const [localHoldingDrafts, setLocalHoldingDrafts] = useState<LocalHoldingDraft[]>([])
  const [isRouteLoading, setIsRouteLoading] = useState(false)
  const [showRouteLoading, setShowRouteLoading] = useState(false)
  const routeLoadingTimerRef = useRef<number | null>(null)

  const activePage = currentPageId(location.pathname)
  const routeFundCode = currentFundCode(location.pathname)
  const selectedCode = data.selectedFund?.code ?? ''
  const fundDetailBackPath = detailBackTarget(location.state)
  const featureFlags = data.featureFlags ?? data.overviewDashboard?.featureFlags ?? []
  const isFlagEnabled = (code: string) => featureFlags.find((flag) => flag.code === code)?.enabled ?? false

  useEffect(() => {
    dataRef.current = data
  }, [data])

  useEffect(() => {
    fundDetailCacheRef.current = fundDetailCache
  }, [fundDetailCache])

  useEffect(() => {
    return () => {
      if (routeLoadingTimerRef.current) {
        window.clearTimeout(routeLoadingTimerRef.current)
      }
    }
  }, [])

  useEffect(() => {
    const performanceEntry = window.performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming | undefined
    const isReload = navigationType === 'POP' && performanceEntry?.type === 'reload'

    if (isReload && location.pathname !== '/') {
      navigate('/', { replace: true })
    }
  }, [location.pathname, navigate, navigationType])

  useEffect(() => {
    if (!authStorage.hasToken()) {
      setScreen({ status: 'auth' })
      return
    }
    void loadPageData(activePage, routeFundCode ?? undefined, { force: true })
  }, [activePage, routeFundCode])

  useEffect(() => {
    setActionMessage(null)
    setPendingWatchlistSelection(null)
    setPendingWatchlistGroups(['全部'])
    setPendingHoldingInput(null)
    setPendingHoldingAmount('')
    setPendingHoldingPnl('')
  }, [location.pathname])

  useEffect(() => {
    if (activePage !== 'funds') {
      setFundSuggestions([])
      return
    }

    if (!authStorage.hasToken()) {
      setFundSuggestions([])
      return
    }

    const keyword = search.trim()
    if (!keyword) {
      setFundSuggestions([])
      return
    }

    const timeoutId = window.setTimeout(() => {
      void api
        .funds(keyword)
        .then((result) => setFundSuggestions(result))
        .catch(() => setFundSuggestions([]))
    }, 500)

    return () => window.clearTimeout(timeoutId)
  }, [activePage, search])

  async function ensureOverviewDashboard(force = false) {
    const cached = dataRef.current.overviewDashboard
    if (!force && cached) return cached
    const overviewDashboard = await api.dashboard()
    setData((current) => ({ ...current, overviewDashboard }))
    return overviewDashboard
  }

  async function ensureFeatureFlags(force = false) {
    const cached = dataRef.current.featureFlags
    if (!force && cached) return cached
    const featureFlags = await api.featureFlags()
    setData((current) => ({ ...current, featureFlags }))
    return featureFlags
  }

  async function ensureFunds(force = false) {
    const cached = dataRef.current.funds
    if (!force && cached) return cached
    const funds = await api.funds()
    setData((current) => ({ ...current, funds }))
    return funds
  }

  async function ensureWatchlist(force = false) {
    const cached = dataRef.current.watchlist
    if (!force && cached) return cached
    const watchlist = await api.watchlist()
    setData((current) => ({ ...current, watchlist }))
    return watchlist
  }

  async function ensurePortfolios(force = false) {
    const cached = dataRef.current.portfolios
    if (!force && cached) return cached
    const portfolios = await api.portfolios()
    setData((current) => ({ ...current, portfolios }))
    return portfolios
  }

  async function ensureOrders(force = false) {
    const cached = dataRef.current.orders
    if (!force && cached) return cached
    const orders = await api.orders()
    setData((current) => ({ ...current, orders }))
    return orders
  }

  async function ensureSipPlans(force = false) {
    const cached = dataRef.current.sipPlans
    if (!force && cached) return cached
    const sipPlans = await api.sipPlans()
    setData((current) => ({ ...current, sipPlans }))
    return sipPlans
  }

  async function ensureReports(force = false) {
    const cached = dataRef.current.reports
    if (!force && cached) return cached
    const reports = await api.weeklyReports()
    setData((current) => ({ ...current, reports }))
    return reports
  }

  async function ensureAlerts(force = false) {
    const cached = dataRef.current.alerts
    if (!force && cached) return cached
    const alerts = await api.alerts()
    setData((current) => ({ ...current, alerts }))
    return alerts
  }

  async function ensureImportJobs(force = false) {
    const cached = dataRef.current.importJobs
    if (!force && cached) return cached
    const importJobs = await api.importJobs()
    setData((current) => ({ ...current, importJobs }))
    return importJobs
  }

  async function ensureFundDetail(code: string, force = false) {
    const cached = fundDetailCacheRef.current[code]
    if (!force && cached) {
      setData((current) => ({ ...current, selectedFund: cached }))
      return cached
    }
    const detail = await api.fundDetail(code)
    setFundDetailCache((current) => ({ ...current, [code]: detail }))
    setData((current) => ({ ...current, selectedFund: detail }))
    return detail
  }

  async function loadPageData(page: PageId, preferredFundCode?: string, options?: { force?: boolean }) {
    const force = options?.force ?? false
    const isInitialLoad = screen.status !== 'ready'

    try {
      if (routeLoadingTimerRef.current) {
        window.clearTimeout(routeLoadingTimerRef.current)
        routeLoadingTimerRef.current = null
      }

      if (isInitialLoad) {
        setScreen({ status: 'loading' })
      } else {
        setIsRouteLoading(true)
        setShowRouteLoading(false)
        routeLoadingTimerRef.current = window.setTimeout(() => {
          setShowRouteLoading(true)
        }, 500)
      }

      if (page === 'overview') {
        const overviewDashboard = await ensureOverviewDashboard(force)
        const focusCode = preferredFundCode ?? defaultFundCodeFromDashboard(overviewDashboard)
        await ensureFundDetail(focusCode, force)
      }

      if (page === 'funds') {
        if (preferredFundCode) {
          await Promise.all([
            ensureFunds(force),
            ensureFeatureFlags(force),
            ensureWatchlist(force),
            ensurePortfolios(force),
            ensureOrders(force),
            ensureSipPlans(force),
            ensureFundDetail(preferredFundCode, force),
          ])
        } else {
          await Promise.all([ensureFunds(force), ensureWatchlist(force)])
        }
      }

      if (page === 'holdings') {
        await Promise.all([ensurePortfolios(force), ensureFunds(force)])
      }

      if (page === 'portfolio') {
        await Promise.all([ensureWatchlist(force), ensureFunds(force)])
      }

      if (page === 'automation') {
        await Promise.all([
          ensureFeatureFlags(force),
          ensureSipPlans(force),
          ensureImportJobs(force),
          ensureReports(force),
          ensureAlerts(force),
        ])
      }

      if (!isInitialLoad) {
        if (routeLoadingTimerRef.current) {
          window.clearTimeout(routeLoadingTimerRef.current)
          routeLoadingTimerRef.current = null
        }
        setIsRouteLoading(false)
        setShowRouteLoading(false)
      }
      setScreen({ status: 'ready' })
    } catch (error) {
      if (routeLoadingTimerRef.current) {
        window.clearTimeout(routeLoadingTimerRef.current)
        routeLoadingTimerRef.current = null
      }
      setIsRouteLoading(false)
      setShowRouteLoading(false)
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

  function openFundDetail(code: string) {
    navigate(`/funds/${code}`, {
      state: { from: location.pathname },
    })
  }

  function openWatchlistPicker(fund: Pick<FundCard, 'code' | 'name' | 'watchlisted'>) {
    if (fund.watchlisted) {
      setActionMessage(`${fund.name} 已在自选基金中`)
      return
    }
    setPendingHoldingInput(null)
    setPendingHoldingAmount('')
    setPendingHoldingPnl('')
    setPendingWatchlistSelection({ code: fund.code, name: fund.name })
    setPendingWatchlistGroups(['全部'])
  }

  function togglePendingWatchlistGroup(group: WatchlistGroup) {
    if (group === '全部') {
      return
    }
    setPendingWatchlistGroups((current) => {
      if (current.includes(group)) {
        return current.filter((item) => item !== group)
      }
      if (current.length >= 2) {
        return current
      }
      return [...current, group]
    })
  }

  function assignWatchlistGroup(codes: string[], group: Exclude<WatchlistGroup, '全部'>) {
    setWatchlistGroups((current) => {
      const next = { ...current }
      codes.forEach((code) => {
        next[code] = ['全部', group]
      })
      return next
    })
  }

  function formatHoldingFieldValue(value: number) {
    return Number.isFinite(value) ? String(Number(value.toFixed(2))) : ''
  }

  function openHoldingInput(fund: Pick<FundCard, 'code' | 'name'>, mode: 'add' | 'edit' = 'add') {
    setPendingWatchlistSelection(null)
    setPendingWatchlistGroups(['全部'])
    const existingHolding = findHoldingTarget(fund.code, mergeLocalHoldingDrafts(dataRef.current.portfolios))
    if (mode === 'edit' && !existingHolding) {
      setActionMessage(`${fund.name} 当前还没有持仓可修改`)
      return
    }
    setPendingHoldingInput({ code: fund.code, name: fund.name, mode })
    setPendingHoldingAmount(mode === 'edit' && existingHolding ? formatHoldingFieldValue(existingHolding.holding.currentValue) : '')
    setPendingHoldingPnl(mode === 'edit' && existingHolding ? formatHoldingFieldValue(existingHolding.holding.pnl) : '')
  }

  function recalculatePortfolio(portfolio: PortfolioSummary, nextHoldings: HoldingLot[]): PortfolioSummary {
    const marketValue = nextHoldings.reduce((sum, holding) => sum + holding.currentValue, 0)
    const totalPnl = nextHoldings.reduce((sum, holding) => sum + holding.pnl, 0)
    return {
      ...portfolio,
      marketValue: Number(marketValue.toFixed(4)),
      totalPnl: Number(totalPnl.toFixed(4)),
      holdings: nextHoldings.map((holding) => ({
        ...holding,
        allocation: marketValue === 0 ? 0 : Number((holding.currentValue / marketValue).toFixed(4)),
      })),
    }
  }

  function mergeLocalHoldingDrafts(portfolios: PortfolioSummary[] | null) {
    if (!portfolios) return portfolios
    if (localHoldingDrafts.length === 0) return portfolios
    return portfolios.map((portfolio) => {
      const drafts = dedupeHoldingDrafts(localHoldingDrafts.filter((draft) => draft.portfolioId === portfolio.id))
      if (drafts.length === 0) return portfolio
      const overriddenCodes = new Set(drafts.map((draft) => draft.fundCode))
      return recalculatePortfolio(portfolio, [...drafts, ...portfolio.holdings.filter((holding) => !overriddenCodes.has(holding.fundCode))])
    })
  }

  function confirmAddHolding() {
    const pending = pendingHoldingInput
    const selectedFund = dataRef.current.selectedFund
    const portfolios = dataRef.current.portfolios
    const amount = Number(pendingHoldingAmount)
    const pnl = Number(pendingHoldingPnl)

    if (!pending || !selectedFund) return
    if (!portfolios?.length) {
      setActionMessage(`当前没有可用组合，暂时无法${pending.mode === 'edit' ? '修改' : '加入'}持仓`)
      return
    }
    if (!Number.isFinite(amount) || amount <= 0) {
      setActionMessage('持有金额必须大于 0')
      return
    }
    if (!Number.isFinite(pnl)) {
      setActionMessage('持有收益必须是有效数字')
      return
    }

    const currentPrice = selectedFund.referenceOnly ? selectedFund.estimatedNav : selectedFund.unitNav
    if (!Number.isFinite(currentPrice) || currentPrice <= 0) {
      setActionMessage(`当前基金净值异常，暂时无法${pending.mode === 'edit' ? '修改' : '添加'}持仓`)
      return
    }

    const costBasis = amount - pnl
    if (costBasis <= 0) {
      setActionMessage('持有收益不能大于等于持有金额')
      return
    }

    const shares = Number((amount / currentPrice).toFixed(4))
    const averageCost = Number((costBasis / shares).toFixed(4))
    const updatedAt = new Date().toISOString()
    const effectivePortfolios = mergeLocalHoldingDrafts(portfolios)
    const existingHolding = findHoldingTarget(pending.code, effectivePortfolios)
    const targetPortfolio = portfolios.find((portfolio) => portfolio.id === existingHolding?.portfolioId) ?? portfolios[0]
    const newHolding: HoldingLot = {
      id: existingHolding?.holding.id ?? `manual-${pending.code}-${Date.now()}`,
      fundCode: pending.code,
      fundName: pending.name,
      shares,
      averageCost,
      currentValue: Number(amount.toFixed(4)),
      pnl: Number(pnl.toFixed(4)),
      allocation: 0,
      source: existingHolding?.holding.source ?? 'manual',
      updatedAt,
    }

    setLocalHoldingDrafts((current) => [
      { ...newHolding, portfolioId: targetPortfolio.id },
      ...current.filter((draft) => !(draft.portfolioId === targetPortfolio.id && draft.fundCode === pending.code)),
    ])
    setData((current) => ({
      ...current,
      funds: current.funds?.map((item) => (item.code === pending.code ? { ...item, held: true } : item)) ?? null,
      selectedFund: current.selectedFund?.code === pending.code ? { ...current.selectedFund, held: true } : current.selectedFund,
    }))
    setFundSuggestions((current) => current.map((item) => (item.code === pending.code ? { ...item, held: true } : item)))
    setFundDetailCache((current) => {
      const detail = current[pending.code]
      if (!detail) return current
      return {
        ...current,
        [pending.code]: {
          ...detail,
          held: true,
        },
      }
    })
    setPendingHoldingInput(null)
    setPendingHoldingAmount('')
    setPendingHoldingPnl('')
    setActionMessage(pending.mode === 'edit' ? `已更新 ${pending.name} 的持仓` : `已将 ${pending.name} 加入持仓`)
  }

  async function confirmAddWatchlist() {
    const pending = pendingWatchlistSelection
    if (!pending) return

    try {
      const note = `新增于 ${new Date().toLocaleDateString('zh-CN')}`
      await api.addWatchlist({
        fundCode: pending.code,
        note,
      })

      setActionMessage(`已将 ${pending.name} 加入自选`)
      setWatchlistGroups((current) => ({
        ...current,
        [pending.code]: pendingWatchlistGroups.length > 0 ? pendingWatchlistGroups : ['全部'],
      }))
      setFundSuggestions((current) => current.map((item) => (item.code === pending.code ? { ...item, watchlisted: true } : item)))
      setFundDetailCache((current) => {
        const detail = current[pending.code]
        if (!detail) return current
        return {
          ...current,
          [pending.code]: {
            ...detail,
            watchlisted: true,
          },
        }
      })
      setData((current) => ({
        ...current,
        funds: current.funds?.map((item) => (item.code === pending.code ? { ...item, watchlisted: true } : item)) ?? null,
        selectedFund: current.selectedFund?.code === pending.code ? { ...current.selectedFund, watchlisted: true } : current.selectedFund,
      }))

      if (dataRef.current.watchlist) {
        const watchlist = await api.watchlist()
        setData((current) => ({ ...current, watchlist }))
      }
      setPendingWatchlistSelection(null)
      setPendingWatchlistGroups(['全部'])
    } catch (error) {
      setActionMessage(error instanceof Error ? error.message : '加入自选失败')
    }
  }

  async function handleLogin(event: FormEvent) {
    event.preventDefault()
    try {
      await api.login({ username, password })
      await loadPageData(activePage, routeFundCode ?? undefined, { force: true })
    } catch (error) {
      setScreen({ status: 'error', message: error instanceof Error ? error.message : 'Login failed' })
    }
  }

  async function handleLogout() {
    if (routeLoadingTimerRef.current) {
      window.clearTimeout(routeLoadingTimerRef.current)
      routeLoadingTimerRef.current = null
    }
    await api.logout()
    authStorage.clear()
    setFundSuggestions([])
    setData({
      overviewDashboard: null,
      featureFlags: null,
      funds: null,
      selectedFund: null,
      watchlist: null,
      portfolios: null,
      orders: null,
      sipPlans: null,
      reports: null,
      alerts: null,
      importJobs: null,
    })
    setFundDetailCache({})
    setWatchlistGroups({})
    setPendingWatchlistSelection(null)
    setPendingWatchlistGroups(['全部'])
    setPendingHoldingInput(null)
    setPendingHoldingAmount('')
    setPendingHoldingPnl('')
    setLocalHoldingDrafts([])
    setIsRouteLoading(false)
    setShowRouteLoading(false)
    setScreen({ status: 'auth' })
  }

  function handleSearch(value: string) {
    setSearch(value)
  }

  async function refreshCurrentPage(preferredFundCode?: string) {
    await loadPageData(activePage, preferredFundCode ?? routeFundCode ?? undefined, { force: true })
  }

  async function runQuickAction(kind: 'watchlist' | 'holding' | 'editHolding' | 'sip' | 'ocr') {
    const selectedFund = dataRef.current.selectedFund
    if (!selectedFund) return

    try {
      if (kind === 'watchlist') {
        openWatchlistPicker(selectedFund)
        return
      }
      if (kind === 'holding') {
        openHoldingInput(selectedFund)
        return
      }
      if (kind === 'editHolding') {
        openHoldingInput(selectedFund, 'edit')
        return
      }
      if (kind === 'sip') {
        const portfolios = await ensurePortfolios()
        await api.createSip({
          portfolioId: portfolios[0].id,
          fundCode: selectedFund.code,
          amount: 500,
          cadence: 'WEEKLY',
          nextRunAt: new Date(Date.now() + 3 * 24 * 3600 * 1000).toISOString().slice(0, 19),
        })
        setActionMessage(`已为 ${selectedFund.code} 创建周定投计划`)
      }
      if (kind === 'ocr') {
        await api.createImportJob({
          sourcePlatform: '示例导入',
          fileName: `${selectedFund.code}-mock-upload.png`,
        })
        setActionMessage('已创建 OCR 导入任务')
      }
      await refreshCurrentPage(selectedFund.code)
    } catch (error) {
      setActionMessage(error instanceof Error ? error.message : 'Action failed')
    }
  }

  async function handleAddWatchlistFromFunds(fund: FundCard) {
    openWatchlistPicker(fund)
  }

  async function handleRemoveWatchlist(code: string) {
    try {
      await api.removeWatchlist(code)
      setActionMessage(`已将 ${code} 移出自选`)
      setWatchlistGroups((current) => {
        if (!(code in current)) return current
        const next = { ...current }
        delete next[code]
        return next
      })
      await refreshCurrentPage(selectedCode || code)
    } catch (error) {
      setActionMessage(error instanceof Error ? error.message : '移出自选失败')
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

  const currentPageMeta = pageMeta[activePage]
  const effectivePortfolios = mergeLocalHoldingDrafts(data.portfolios)
  const holdingsMarketValue = effectivePortfolios?.reduce((sum, portfolio) => sum + portfolio.marketValue, 0) ?? 0
  const watchlistCodeSet = new Set((data.watchlist ?? data.overviewDashboard?.watchlist ?? []).map((item) => item.code))
  const heldCodeSet = holdingCodesFromPortfolios(effectivePortfolios)
  const enrichFundState = <TFund extends FundCard>(fund: TFund): TFund => ({
    ...fund,
    watchlisted: fund.watchlisted || watchlistCodeSet.has(fund.code),
    held: fund.held || heldCodeSet.has(fund.code),
  })
  const effectiveFunds = data.funds?.map((fund) => enrichFundState(fund)) ?? null
  const effectiveFundSuggestions = fundSuggestions.map((fund) => enrichFundState(fund))
  const effectiveSelectedFund = data.selectedFund ? enrichFundState(data.selectedFund) : null
  const detailDashboard = buildPartialDashboard({
    profile: data.overviewDashboard?.profile,
    featureFlags,
    portfolios: effectivePortfolios ?? [],
    orders: data.orders ?? [],
    sipPlans: data.sipPlans ?? [],
  })
  const holdingsDashboard = buildPartialDashboard({
    profile: data.overviewDashboard?.profile,
    portfolios: effectivePortfolios ?? [],
  })
  const watchlistDashboard = buildPartialDashboard({
    profile: data.overviewDashboard?.profile,
    watchlist: data.watchlist ?? [],
  })
  const automationDashboard = buildPartialDashboard({
    profile: data.overviewDashboard?.profile,
    featureFlags,
    sipPlans: data.sipPlans ?? [],
    reports: data.reports ?? [],
    alerts: data.alerts ?? [],
    importJobs: data.importJobs ?? [],
  })

  function renderRouteContent(isReady: boolean, element: ReactNode) {
    if (isReady) return element
    if (isRouteLoading && showRouteLoading) return <LoadingScreen />
    return null
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
                {activePage === 'holdings' ? (
                  <div className="mt-5 max-w-[340px] rounded-[1.45rem] border border-white/8 bg-[linear-gradient(135deg,rgba(243,186,47,0.12),rgba(255,255,255,0.04),rgba(8,12,20,0.2))] px-5 py-4">
                    <p className="text-xs uppercase tracking-[0.18em] text-[color:var(--fc-color-accent)]/80">组合市值</p>
                    <p className="mt-3 font-[var(--fc-font-display)] text-3xl text-white">{formatCurrency(holdingsMarketValue)}</p>
                    <p className="mt-2 text-xs text-slate-500">汇总当前组合下所有基金持仓市值</p>
                  </div>
                ) : null}
              </div>
              {data.selectedFund ? (
                <div className="flex flex-wrap gap-2">
                  <span className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-300">
                    焦点基金 {effectiveSelectedFund?.code ?? data.selectedFund.code}
                  </span>
                  <span className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-300">
                    {(effectiveSelectedFund ?? data.selectedFund).referenceOnly ? '实时涨跌参考' : '前一交易日涨跌'}
                  </span>
                </div>
              ) : null}
            </div>
          </header>

          {actionMessage ? (
            <div className="rounded-3xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-200">
              {actionMessage}
            </div>
          ) : null}

          <Routes>
            <Route
              path="/"
              element={
                renderRouteContent(
                  Boolean(data.overviewDashboard && data.selectedFund),
                  <OverviewPage dashboard={data.overviewDashboard!} />
                )
              }
            />
            <Route
              path="/funds"
              element={
                renderRouteContent(
                  Boolean(effectiveFunds),
                  <FundsPage
                    search={search}
                    funds={effectiveFunds!}
                    suggestions={effectiveFundSuggestions}
                    onSearchChange={handleSearch}
                    onAddToWatchlist={(fund) => void handleAddWatchlistFromFunds(fund)}
                    watchlistPickerFundCode={pendingWatchlistSelection?.code ?? null}
                    watchlistPickerGroups={pendingWatchlistGroups}
                    watchlistGroupOptions={watchlistGroupOrder}
                    onToggleWatchlistGroup={togglePendingWatchlistGroup}
                    onCancelWatchlistPicker={() => {
                      setPendingWatchlistSelection(null)
                      setPendingWatchlistGroups(['全部'])
                    }}
                    onConfirmWatchlistPicker={() => void confirmAddWatchlist()}
                    onSelectFund={openFundDetail}
                  />
                )
              }
            />
            <Route
              path="/funds/:fundCode"
              element={
                renderRouteContent(
                  Boolean(effectiveFunds && effectiveSelectedFund && data.portfolios && data.orders && data.sipPlans),
                  <FundDetailPage
                    dashboard={detailDashboard}
                    funds={effectiveFunds!}
                    selectedFund={effectiveSelectedFund!}
                    isFlagEnabled={isFlagEnabled}
                    onBack={() => navigate(fundDetailBackPath)}
                    onQuickAction={(kind) => void runQuickAction(kind)}
                    watchlistPickerOpen={pendingWatchlistSelection?.code === effectiveSelectedFund!.code}
                    watchlistPickerGroups={pendingWatchlistGroups}
                    watchlistGroupOptions={watchlistGroupOrder}
                    onToggleWatchlistGroup={togglePendingWatchlistGroup}
                    onCancelWatchlistPicker={() => {
                      setPendingWatchlistSelection(null)
                      setPendingWatchlistGroups(['全部'])
                    }}
                    onConfirmWatchlistPicker={() => void confirmAddWatchlist()}
                    holdingInputOpen={pendingHoldingInput?.code === effectiveSelectedFund!.code}
                    holdingFormMode={pendingHoldingInput?.mode ?? 'add'}
                    holdingAmount={pendingHoldingAmount}
                    holdingPnl={pendingHoldingPnl}
                    onHoldingAmountChange={setPendingHoldingAmount}
                    onHoldingPnlChange={setPendingHoldingPnl}
                    onCancelHoldingInput={() => {
                      setPendingHoldingInput(null)
                      setPendingHoldingAmount('')
                      setPendingHoldingPnl('')
                    }}
                    onConfirmHoldingInput={confirmAddHolding}
                    onJumpToHoldings={() => navigate('/holdings')}
                  />
                )
              }
            />
            <Route
              path="/holdings"
              element={
                renderRouteContent(
                  Boolean(data.portfolios && effectiveFunds),
                  <HoldingsPage dashboard={holdingsDashboard} funds={effectiveFunds!} onOpenFund={openFundDetail} />
                )
              }
            />
            <Route
              path="/portfolio"
              element={
                renderRouteContent(
                  Boolean(data.watchlist && effectiveFunds),
                  <PortfolioPage
                    dashboard={watchlistDashboard}
                    funds={effectiveFunds!}
                    groupSelections={watchlistGroups}
                    onAssignGroup={assignWatchlistGroup}
                    onOpenFund={openFundDetail}
                    onRemoveFund={(code) => handleRemoveWatchlist(code)}
                  />
                )
              }
            />
            <Route
              path="/automation"
              element={
                renderRouteContent(
                  Boolean(data.sipPlans && data.importJobs && data.reports && data.alerts),
                  <AutomationPage dashboard={automationDashboard} />
                )
              }
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  )
}

export default App
