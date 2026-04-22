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
  LocalSipPlanDraft,
  PageId,
  PendingHoldingAmountBasis,
  PendingHoldingInput,
  PendingHoldingOperationInput,
  PendingHoldingOperationTiming,
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
  detailBackTarget,
} from '../utils/dashboard'
import { buildWorkspaceDerivedState } from './derived'
import { createDefaultWatchlistGroup, defaultPassword, defaultSipCadence, defaultSipMonthDay, defaultSipWeekday, defaultUsername, defaultWatchlistGroupName } from './constants'
import { createWorkspaceEnsureLoaders } from './loaders'
import { createDefaultWorkspaceInputs, createEmptyAppData } from './state'
import { createSipPlanActions } from '../../modules/automation/model/sipPlanActions'
import { createHoldingActions } from '../../modules/holdings/model/holdingActions'
import { createHoldingOperationActions } from '../../modules/holdings/model/holdingOperationActions'
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
  const [watchlistGroups, setWatchlistGroups] = useState<Record<string, WatchlistGroup>>({})
  const [pendingWatchlistSelection, setPendingWatchlistSelection] = useState<PendingWatchlistSelection | null>(null)
  const [pendingWatchlistGroup, setPendingWatchlistGroup] = useState<WatchlistGroup>(() => createDefaultWatchlistGroup())
  const [pendingHoldingInput, setPendingHoldingInput] = useState<PendingHoldingInput | null>(null)
  const [pendingHoldingAmount, setPendingHoldingAmount] = useState('')
  const [pendingHoldingPnl, setPendingHoldingPnl] = useState('')
  const [pendingHoldingAmountBasis, setPendingHoldingAmountBasis] = useState<PendingHoldingAmountBasis>(
    createDefaultWorkspaceInputs().holdingAmountBasis,
  )
  const [pendingHoldingOperationInput, setPendingHoldingOperationInput] = useState<PendingHoldingOperationInput | null>(null)
  const [pendingHoldingOperationAmount, setPendingHoldingOperationAmount] = useState('')
  const [pendingHoldingOperationShares, setPendingHoldingOperationShares] = useState('')
  const [pendingHoldingOperationTradeDate, setPendingHoldingOperationTradeDate] = useState(createDefaultWorkspaceInputs().holdingOperationTradeDate)
  const [pendingHoldingOperationTiming, setPendingHoldingOperationTiming] = useState<PendingHoldingOperationTiming>(
    createDefaultWorkspaceInputs().holdingOperationTiming,
  )
  const [pendingHoldingOperationFeeRate, setPendingHoldingOperationFeeRate] = useState(createDefaultWorkspaceInputs().holdingOperationFeeRate)
  const [pendingSipInput, setPendingSipInput] = useState<PendingSipInput | null>(null)
  const [pendingSipCadence, setPendingSipCadence] = useState<SipCadenceInput>(defaultSipCadence)
  const [pendingSipWeekday, setPendingSipWeekday] = useState<SipWeekdayInput>(defaultSipWeekday)
  const [pendingSipMonthDay, setPendingSipMonthDay] = useState(defaultSipMonthDay)
  const [pendingSipAmount, setPendingSipAmount] = useState('')
  const [localSipPlanDrafts, setLocalSipPlanDrafts] = useState<LocalSipPlanDraft[]>([])
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
    localSipPlanDrafts,
  })
  const isFlagEnabled = (code: string) => featureFlags.find((flag) => flag.code === code)?.enabled ?? false

  const {
    ensureFeatureFlags,
    ensureFundDetail,
    ensureFundHoldingInsight,
    clearFundHoldingInsight,
    ensureFunds,
    ensureHoldingsOverview,
    ensureOrders,
    ensureOverviewDashboard,
    ensureSipRecords,
    ensureSipPlans,
    ensureWatchlist,
    ensureWatchlistGroups,
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
    const nextGroups: Record<string, WatchlistGroup> = Object.fromEntries(
      (data.watchlist ?? []).map((item) => [item.code, item.group ?? '全部']),
    )
    setWatchlistGroups(nextGroups)
  }, [data.watchlist])

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
    setPendingWatchlistGroup(defaults.watchlistGroup)
    setPendingHoldingInput(null)
    setPendingHoldingAmount(defaults.holdingAmount)
    setPendingHoldingPnl(defaults.holdingPnl)
    setPendingHoldingAmountBasis(defaults.holdingAmountBasis)
    setPendingHoldingOperationInput(null)
    setPendingHoldingOperationAmount(defaults.holdingOperationAmount)
    setPendingHoldingOperationShares(defaults.holdingOperationShares)
    setPendingHoldingOperationTradeDate(defaults.holdingOperationTradeDate)
    setPendingHoldingOperationTiming(defaults.holdingOperationTiming)
    setPendingHoldingOperationFeeRate(defaults.holdingOperationFeeRate)
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
      }

      if (page === 'funds') {
        if (preferredFundCode) {
          const [, , , , selectedFund] = await Promise.all([
            ensureFeatureFlags(force),
            ensureOrders(force),
            ensureSipPlans(force),
            ensureHoldingsOverview(force),
            ensureFundDetail(preferredFundCode, force),
          ])

          await ensureWatchlistGroups(force)

          if (selectedFund?.held) {
            await ensureFundHoldingInsight(preferredFundCode, force)
          } else {
            clearFundHoldingInsight()
          }
        } else {
          await Promise.all([ensureFunds(force), ensureWatchlist(force), ensureWatchlistGroups(force)])
          clearFundHoldingInsight()
        }
      }

      if (page === 'holdings') {
        await ensureHoldingsOverview(force)
      }

      if (page === 'watchlist') {
        await Promise.all([ensureWatchlist(force), ensureWatchlistGroups(force)])
      }

      if (page === 'sip') {
        await ensureSipPlans(force)
        if (routeSipPlanId) {
          await ensureSipRecords(routeSipPlanId, force)
        }
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
    getPendingWatchlistGroup: () => pendingWatchlistGroup,
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
    setPendingWatchlistGroup,
    setPendingWatchlistSelection,
    setWatchlistGroups,
  })

  const holdingActions = createHoldingActions({
    getData: () => dataRef.current,
    getPendingHoldingAmount: () => pendingHoldingAmount,
    getPendingHoldingAmountBasis: () => pendingHoldingAmountBasis,
    getPendingHoldingInput: () => pendingHoldingInput,
    getPendingHoldingPnl: () => pendingHoldingPnl,
    getSelectedFundHoldingInsight: () => dataRef.current.selectedFundHoldingInsight,
    refreshCurrentPage,
    setActionMessage,
    setData,
    setPendingHoldingAmount,
    setPendingHoldingAmountBasis,
    setPendingHoldingInput,
    setPendingHoldingPnl,
    setPendingSipAmount,
    setPendingSipCadence,
    setPendingSipInput,
    setPendingSipMonthDay,
    setPendingSipWeekday,
    setPendingWatchlistGroup,
    setPendingWatchlistSelection,
  })

  const holdingOperationActions = createHoldingOperationActions({
    getData: () => dataRef.current,
    getPendingHoldingOperationAmount: () => pendingHoldingOperationAmount,
    getPendingHoldingOperationFeeRate: () => pendingHoldingOperationFeeRate,
    getPendingHoldingOperationInput: () => pendingHoldingOperationInput,
    getPendingHoldingOperationShares: () => pendingHoldingOperationShares,
    getPendingHoldingOperationTradeDate: () => pendingHoldingOperationTradeDate,
    getPendingHoldingOperationTiming: () => pendingHoldingOperationTiming,
    refreshCurrentPage,
    setActionMessage,
    setPendingHoldingAmount,
    setPendingHoldingInput,
    setPendingHoldingOperationAmount,
    setPendingHoldingOperationFeeRate,
    setPendingHoldingOperationInput,
    setPendingHoldingOperationShares,
    setPendingHoldingOperationTradeDate,
    setPendingHoldingOperationTiming,
    setPendingHoldingPnl,
    setPendingSipAmount,
    setPendingSipCadence,
    setPendingSipInput,
    setPendingSipMonthDay,
    setPendingSipWeekday,
    setPendingWatchlistGroup,
    setPendingWatchlistSelection,
  })

  const sipPlanActions = createSipPlanActions({
    getData: () => dataRef.current,
    getPendingSipAmount: () => pendingSipAmount,
    getPendingSipCadence: () => pendingSipCadence,
    getPendingSipInput: () => pendingSipInput,
    getPendingSipMonthDay: () => pendingSipMonthDay,
    getPendingSipWeekday: () => pendingSipWeekday,
    navigateToPath: (path) => navigate(path),
    setActionMessage,
    setData,
    setPendingHoldingAmount,
    setPendingHoldingInput,
    setPendingHoldingPnl,
    setPendingSipAmount,
    setPendingSipCadence,
    setPendingSipInput,
    setPendingSipMonthDay,
    setPendingSipWeekday,
    setPendingWatchlistGroup,
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
    const defaults = createDefaultWorkspaceInputs()
    setPendingHoldingAmountBasis(defaults.holdingAmountBasis)
    setPendingHoldingOperationInput(null)
    setPendingHoldingOperationAmount(defaults.holdingOperationAmount)
    setPendingHoldingOperationShares(defaults.holdingOperationShares)
    setPendingHoldingOperationTradeDate(defaults.holdingOperationTradeDate)
    setPendingHoldingOperationTiming(defaults.holdingOperationTiming)
    setPendingHoldingOperationFeeRate(defaults.holdingOperationFeeRate)
    setIsRouteLoading(false)
    setShowRouteLoading(false)
    setScreen({ status: 'auth' })
  }

  async function runQuickAction(kind: 'watchlist' | 'holding' | 'editHolding' | 'sip' | 'buy' | 'sell') {
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
      if (kind === 'buy') {
        holdingOperationActions.openHoldingOperationInput(selectedFund, 'BUY')
        return
      }
      if (kind === 'sell') {
        holdingOperationActions.openHoldingOperationInput(selectedFund, 'SELL')
        return
      }
      await refreshCurrentPage(selectedFund.code)
    } catch (error) {
      setActionMessage(error instanceof Error ? error.message : 'Action failed')
    }
  }

  const currentPageMeta = pageMeta[activePage]
  const watchlistGroupOrder = [
    defaultWatchlistGroupName,
    ...((data.watchlistGroupOptions ?? []).map((group) => group.name).filter((name) => name !== defaultWatchlistGroupName)),
  ]
  return {
    activePage,
    actionMessage,
    actionMessageToneClass,
    automationDashboard,
    confirmAddHolding: holdingActions.confirmAddHolding,
    confirmHoldingOperation: holdingOperationActions.confirmHoldingOperation,
    confirmAddSip: sipPlanActions.confirmAddSip,
    confirmAddWatchlist: watchlistActions.confirmAddWatchlist,
    currentPageMeta,
    data,
    detailDashboard,
    effectiveFunds,
    effectiveFundSuggestions,
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
    pendingHoldingAmountBasis,
    pendingHoldingInput,
    pendingHoldingOperationAmount,
    pendingHoldingOperationFeeRate,
    pendingHoldingOperationInput,
    pendingHoldingOperationShares,
    pendingHoldingOperationTradeDate,
    pendingHoldingOperationTiming,
    pendingHoldingPnl,
    pendingSipAmount,
    pendingSipCadence,
    pendingSipInput,
    pendingSipMonthDay,
    pendingSipWeekday,
    pendingWatchlistGroup,
    pendingWatchlistSelection,
    runQuickAction,
    screen,
    search,
    setPendingHoldingAmount,
    setPendingHoldingAmountBasis,
    setPendingHoldingInput,
    setPendingHoldingOperationAmount,
    setPendingHoldingOperationFeeRate: holdingOperationActions.handleHoldingOperationFeeRateChange,
    setPendingHoldingOperationInput,
    setPendingHoldingOperationShares,
    setPendingHoldingOperationTradeDate,
    setPendingHoldingOperationTiming,
    setPendingHoldingPnl,
    setPendingSipAmount,
    setPendingSipCadence,
    setPendingSipInput,
    setPendingSipMonthDay,
    setPendingSipWeekday,
    setPendingWatchlistGroup,
    setPendingWatchlistSelection,
    setPassword,
    setShowRouteLoading,
    setUsername,
    showRouteLoading,
    sipPlanCodeSet,
    selectPendingWatchlistGroup: watchlistActions.selectPendingWatchlistGroup,
    username,
    password,
    routeSipPlanId,
    localHoldingHistory: data.localHoldingHistory,
    selectedFundHoldingInsight: data.selectedFundHoldingInsight,
    sipPlanRecords: routeSipPlanId ? data.sipRecordsByPlanId[routeSipPlanId] ?? [] : [],
    handleEditSipPlan: sipPlanActions.handleEditSipPlan,
    handleOpenSipPlan: sipPlanActions.handleOpenSipPlan,
    handleStopSipPlan: sipPlanActions.handleStopSipPlan,
    handleToggleSipPlan: sipPlanActions.handleToggleSipPlan,
    holdingsOverview: data.holdingsOverview,
    watchlistDashboard,
    watchlistGroupOptions: data.watchlistGroupOptions ?? [],
    watchlistGroupOrder,
    watchlistGroups,
    assignWatchlistGroup: watchlistActions.assignWatchlistGroup,
    createWatchlistGroup: watchlistActions.createWatchlistGroup,
  }
}
