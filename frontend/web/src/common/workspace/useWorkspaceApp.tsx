/** 工作台总编排 hook，串联路由、鉴权、页面数据加载以及跨模块状态协调。 */
import type { FundCard, FundDetail } from '@fundcat/contracts'
import type { FormEvent } from 'react'
import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate, useNavigationType } from 'react-router-dom'
import { authStorage } from '../data/authStorage'
import { workspaceApi } from '../data/workspaceApi'
import { pageMeta, navItems } from './config'
import type {
  AppDataState,
  LocalHoldingDraft,
  LocalSipPlanDraft,
  PageId,
  PendingHoldingInput,
  PendingSipInput,
  PendingWatchlistSelection,
  ScreenState,
  SipCadenceInput,
  SipWeekdayInput,
  WatchlistGroup,
} from '../appTypes'
import {
  currentFundCode,
  currentPageId,
  currentSipPlanId,
  defaultFundCodeFromWatchlist,
  detailBackTarget,
} from '../utils/dashboard'
import { LOCAL_SIP_PLAN_DRAFTS_KEY, loadLocalSipPlanDrafts } from '../utils/sipPlans'
import { buildWorkspaceDerivedState } from './derived'
import {
  createDefaultWatchlistGroups,
  defaultPassword,
  defaultSipCadence,
  defaultSipMonthDay,
  defaultSipWeekday,
  defaultUsername,
  watchlistGroupOrder,
} from './constants'
import { createWorkspaceEnsureLoaders } from './loaders'
import { createDefaultWorkspaceInputs, createEmptyAppData } from './state'
import { createSipPlanActions } from '../../modules/automation/model/sipPlanActions'
import { createHoldingActions } from '../../modules/holdings/model/holdingActions'
import { createWatchlistActions } from '../../modules/portfolio/model/watchlistActions'

export function useWorkspaceApp() {
  const location = useLocation()
  const navigate = useNavigate()
  const navigationType = useNavigationType()
  const [screen, setScreen] = useState<ScreenState>({ status: 'loading' })
  const [data, setData] = useState<AppDataState>(() => createEmptyAppData())
  const dataRef = useRef(data)
  const [fundDetailCache, setFundDetailCache] = useState<Record<string, FundDetail>>({})
  const fundDetailCacheRef = useRef(fundDetailCache)
  const [search, setSearch] = useState('')
  const [fundSuggestions, setFundSuggestions] = useState<FundCard[]>([])
  const [username, setUsername] = useState(defaultUsername)
  const [password, setPassword] = useState(defaultPassword)
  const [actionMessage, setActionMessage] = useState<string | null>(null)
  const [watchlistGroups, setWatchlistGroups] = useState<Record<string, WatchlistGroup[]>>({})
  const [pendingWatchlistSelection, setPendingWatchlistSelection] = useState<PendingWatchlistSelection | null>(null)
  const [pendingWatchlistGroups, setPendingWatchlistGroups] = useState<WatchlistGroup[]>(() => createDefaultWatchlistGroups())
  const [pendingHoldingInput, setPendingHoldingInput] = useState<PendingHoldingInput | null>(null)
  const [pendingHoldingAmount, setPendingHoldingAmount] = useState('')
  const [pendingHoldingPnl, setPendingHoldingPnl] = useState('')
  const [pendingSipInput, setPendingSipInput] = useState<PendingSipInput | null>(null)
  const [pendingSipCadence, setPendingSipCadence] = useState<SipCadenceInput>(defaultSipCadence)
  const [pendingSipWeekday, setPendingSipWeekday] = useState<SipWeekdayInput>(defaultSipWeekday)
  const [pendingSipMonthDay, setPendingSipMonthDay] = useState(defaultSipMonthDay)
  const [pendingSipAmount, setPendingSipAmount] = useState('')
  const [localHoldingDrafts, setLocalHoldingDrafts] = useState<LocalHoldingDraft[]>([])
  const [localSipPlanDrafts, setLocalSipPlanDrafts] = useState<LocalSipPlanDraft[]>(() => loadLocalSipPlanDrafts())
  const [isRouteLoading, setIsRouteLoading] = useState(false)
  const [showRouteLoading, setShowRouteLoading] = useState(false)
  const routeLoadingTimerRef = useRef<number | null>(null)

  const activePage = currentPageId(location.pathname)
  const routeFundCode = currentFundCode(location.pathname)
  const routeSipPlanId = currentSipPlanId(location.pathname)
  const selectedCode = data.selectedFund?.code ?? ''
  const fundDetailBackPath = detailBackTarget(location.state)
  const {
    actionMessageToneClass,
    automationDashboard,
    detailDashboard,
    effectiveFunds,
    effectiveFundSuggestions,
    effectivePortfolios,
    effectiveSelectedFund,
    effectiveSipPlans,
    featureFlags,
    holdingsDashboard,
    holdingsMarketValue,
    sipPlanCodeSet,
    watchlistDashboard,
  } = buildWorkspaceDerivedState({
    actionMessage,
    data,
    fundSuggestions,
    localHoldingDrafts,
    localSipPlanDrafts,
  })
  const isFlagEnabled = (code: string) => featureFlags.find((flag) => flag.code === code)?.enabled ?? false

  const {
    ensureAlerts,
    ensureFeatureFlags,
    ensureFundDetail,
    ensureFunds,
    ensureImportJobs,
    ensureOrders,
    ensureOverviewDashboard,
    ensurePortfolios,
    ensureReports,
    ensureSipPlans,
    ensureWatchlist,
  } = createWorkspaceEnsureLoaders({
    getData: () => dataRef.current,
    getFundDetailCache: () => fundDetailCacheRef.current,
    setData,
    setFundDetailCache,
  })

  useEffect(() => {
    dataRef.current = data
  }, [data])

  useEffect(() => {
    fundDetailCacheRef.current = fundDetailCache
  }, [fundDetailCache])

  useEffect(() => {
    try {
      if (localSipPlanDrafts.length === 0) {
        window.localStorage.removeItem(LOCAL_SIP_PLAN_DRAFTS_KEY)
        return
      }
      window.localStorage.setItem(LOCAL_SIP_PLAN_DRAFTS_KEY, JSON.stringify(localSipPlanDrafts))
    } catch {
      // Ignore local persistence failures and keep the in-memory draft behavior.
    }
  }, [localSipPlanDrafts])

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
    const defaults = createDefaultWorkspaceInputs()
    setPendingWatchlistGroups(defaults.watchlistGroups)
    setPendingHoldingInput(null)
    setPendingHoldingAmount(defaults.holdingAmount)
    setPendingHoldingPnl(defaults.holdingPnl)
    setPendingSipInput(null)
    setPendingSipCadence(defaults.sipCadence)
    setPendingSipWeekday(defaults.sipWeekday)
    setPendingSipMonthDay(defaults.sipMonthDay)
    setPendingSipAmount(defaults.sipAmount)
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
      void workspaceApi
        .funds(keyword)
        .then((result) => setFundSuggestions(result))
        .catch(() => setFundSuggestions([]))
    }, 500)

    return () => window.clearTimeout(timeoutId)
  }, [activePage, search])

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
        await ensureOverviewDashboard(force)
        const focusCode = preferredFundCode ?? defaultFundCodeFromWatchlist(dataRef.current.overviewWatchlistPulse?.items ?? [])
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

  function handleSearch(value: string) {
    setSearch(value)
  }

  async function refreshCurrentPage(preferredFundCode?: string) {
    await loadPageData(activePage, preferredFundCode ?? routeFundCode ?? undefined, { force: true })
  }

  const watchlistActions = createWatchlistActions({
    getData: () => dataRef.current,
    getPendingWatchlistGroups: () => pendingWatchlistGroups,
    getPendingWatchlistSelection: () => pendingWatchlistSelection,
    getSelectedCode: () => selectedCode,
    refreshCurrentPage,
    setActionMessage,
    setData,
    setFundDetailCache,
    setFundSuggestions,
    setPendingHoldingAmount,
    setPendingHoldingInput,
    setPendingHoldingPnl,
    setPendingSipAmount,
    setPendingSipCadence,
    setPendingSipInput,
    setPendingSipMonthDay,
    setPendingSipWeekday,
    setPendingWatchlistGroups,
    setPendingWatchlistSelection,
    setWatchlistGroups,
  })

  const holdingActions = createHoldingActions({
    getData: () => dataRef.current,
    getLocalHoldingDrafts: () => localHoldingDrafts,
    getPendingHoldingAmount: () => pendingHoldingAmount,
    getPendingHoldingInput: () => pendingHoldingInput,
    getPendingHoldingPnl: () => pendingHoldingPnl,
    setActionMessage,
    setData,
    setFundDetailCache,
    setFundSuggestions,
    setLocalHoldingDrafts,
    setPendingHoldingAmount,
    setPendingHoldingInput,
    setPendingHoldingPnl,
    setPendingSipAmount,
    setPendingSipCadence,
    setPendingSipInput,
    setPendingSipMonthDay,
    setPendingSipWeekday,
    setPendingWatchlistGroups,
    setPendingWatchlistSelection,
  })

  const sipPlanActions = createSipPlanActions({
    getData: () => dataRef.current,
    getLocalSipPlanDrafts: () => localSipPlanDrafts,
    getPendingSipAmount: () => pendingSipAmount,
    getPendingSipCadence: () => pendingSipCadence,
    getPendingSipInput: () => pendingSipInput,
    getPendingSipMonthDay: () => pendingSipMonthDay,
    getPendingSipWeekday: () => pendingSipWeekday,
    navigateToPath: (path) => navigate(path),
    setActionMessage,
    setData,
    setLocalSipPlanDrafts,
    setPendingHoldingAmount,
    setPendingHoldingInput,
    setPendingHoldingPnl,
    setPendingSipAmount,
    setPendingSipCadence,
    setPendingSipInput,
    setPendingSipMonthDay,
    setPendingSipWeekday,
    setPendingWatchlistGroups,
    setPendingWatchlistSelection,
  })

  async function handleLogin(event: FormEvent) {
    event.preventDefault()
    try {
      await workspaceApi.login({ username, password })
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
    await workspaceApi.logout()
    authStorage.clear()
    setFundSuggestions([])
    setFundDetailCache({})
    watchlistActions.resetWorkspaceStateAfterLogout()
    setLocalSipPlanDrafts([])
    setLocalHoldingDrafts([])
    setIsRouteLoading(false)
    setShowRouteLoading(false)
    setScreen({ status: 'auth' })
  }

  async function runQuickAction(kind: 'watchlist' | 'holding' | 'editHolding' | 'sip' | 'ocr') {
    const selectedFund = dataRef.current.selectedFund
    if (!selectedFund) return

    try {
      if (kind === 'watchlist') {
        watchlistActions.openWatchlistPicker(selectedFund)
        return
      }
      if (kind === 'holding') {
        holdingActions.openHoldingInput(selectedFund)
        return
      }
      if (kind === 'editHolding') {
        holdingActions.openHoldingInput(selectedFund, 'edit')
        return
      }
      if (kind === 'sip') {
        sipPlanActions.openSipInput(selectedFund)
        return
      }
      if (kind === 'ocr') {
        await workspaceApi.createImportJob({
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

  const currentPageMeta = pageMeta[activePage]
  return {
    activePage,
    actionMessage,
    actionMessageToneClass,
    automationDashboard,
    confirmAddHolding: holdingActions.confirmAddHolding,
    confirmAddSip: sipPlanActions.confirmAddSip,
    confirmAddWatchlist: watchlistActions.confirmAddWatchlist,
    currentPageMeta,
    data,
    detailDashboard,
    effectiveFunds,
    effectiveFundSuggestions,
    effectivePortfolios,
    effectiveSelectedFund,
    effectiveSipPlans,
    fundDetailBackPath,
    handleAddWatchlistFromFunds: watchlistActions.handleAddWatchlistFromFunds,
    handleLogin,
    handleLogout,
    handleRemoveWatchlist: watchlistActions.handleRemoveWatchlist,
    handleSearch,
    holdingsDashboard,
    holdingsMarketValue,
    isFlagEnabled,
    isRouteLoading,
    navigate,
    navigateToPage,
    openFundDetail,
    pendingHoldingAmount,
    pendingHoldingInput,
    pendingHoldingPnl,
    pendingSipAmount,
    pendingSipCadence,
    pendingSipInput,
    pendingSipMonthDay,
    pendingSipWeekday,
    pendingWatchlistGroups,
    pendingWatchlistSelection,
    runQuickAction,
    screen,
    search,
    setPendingHoldingAmount,
    setPendingHoldingInput,
    setPendingHoldingPnl,
    setPendingSipAmount,
    setPendingSipCadence,
    setPendingSipInput,
    setPendingSipMonthDay,
    setPendingSipWeekday,
    setPendingWatchlistGroups,
    setPendingWatchlistSelection,
    setPassword,
    setShowRouteLoading,
    setUsername,
    showRouteLoading,
    sipPlanCodeSet,
    togglePendingWatchlistGroup: watchlistActions.togglePendingWatchlistGroup,
    username,
    password,
    routeSipPlanId,
    handleEditSipPlan: sipPlanActions.handleEditSipPlan,
    handleOpenSipPlan: sipPlanActions.handleOpenSipPlan,
    handleStopSipPlan: sipPlanActions.handleStopSipPlan,
    handleToggleSipPlan: sipPlanActions.handleToggleSipPlan,
    watchlistDashboard,
    watchlistGroupOrder,
    watchlistGroups,
    assignWatchlistGroup: watchlistActions.assignWatchlistGroup,
  }
}
