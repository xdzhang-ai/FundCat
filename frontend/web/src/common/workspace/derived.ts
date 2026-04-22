/** 工作台派生状态层，负责把原始接口数据整理成页面可直接消费的视图数据。 */
import type { FundCard } from '@fundcat/contracts'
import type { AppDataState, LocalSipPlanDraft } from '../appTypes'
import { buildPartialDashboard } from '../utils/dashboard'
import { mergeLocalSipPlanDrafts, resolveSipStatus } from '../utils/sipPlans'

type BuildWorkspaceDerivedStateOptions = {
  actionMessage: string | null
  data: AppDataState
  fundSuggestions: FundCard[]
  localSipPlanDrafts: LocalSipPlanDraft[]
}

export function buildWorkspaceDerivedState({
  actionMessage,
  data,
  fundSuggestions,
  localSipPlanDrafts,
}: BuildWorkspaceDerivedStateOptions) {
  const featureFlags = data.featureFlags ?? data.overviewDashboard?.featureFlags ?? []
  const effectiveSipPlans = mergeLocalSipPlanDrafts(data.sipPlans, localSipPlanDrafts)
  const holdingsMarketValue = data.holdingsOverview?.totalMarketValue ?? 0
  const watchlistCodeSet = new Set((data.watchlist ?? data.overviewDashboard?.watchlist ?? []).map((item) => item.code))
  const heldCodeSet =
    data.holdingsOverview?.items?.length
      ? new Set(data.holdingsOverview.items.map((item) => item.fundCode))
      : new Set<string>()
  const sipPlanCodeSet = new Set(
    (effectiveSipPlans ?? []).filter((plan) => resolveSipStatus(plan) !== '停止').map((plan) => plan.fundCode),
  )
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
    orders: data.orders ?? [],
    sipPlans: effectiveSipPlans ?? [],
  })
  const holdingsDashboard = buildPartialDashboard({
    profile: data.overviewDashboard?.profile,
  })
  const watchlistDashboard = buildPartialDashboard({
    profile: data.overviewDashboard?.profile,
    watchlist: data.watchlist ?? [],
  })
  const automationDashboard = buildPartialDashboard({
    profile: data.overviewDashboard?.profile,
    featureFlags,
    sipPlans: effectiveSipPlans ?? []
  })
  const actionMessageToneClass = actionMessage?.startsWith('已停止')
    ? 'border-red-400/20 bg-red-500/10 text-red-200'
    : actionMessage?.startsWith('已暂停')
      ? 'border-amber-400/20 bg-amber-500/10 text-amber-100'
      : 'border-emerald-400/20 bg-emerald-400/10 text-emerald-200'

  return {
    actionMessageToneClass,
    automationDashboard,
    detailDashboard,
    effectiveFunds,
    effectiveFundSuggestions,
    effectiveSelectedFund,
    effectiveSipPlans,
    featureFlags,
    heldCodeSet,
    holdingsDashboard,
    holdingsMarketValue,
    sipPlanCodeSet,
    watchlistDashboard,
    watchlistCodeSet,
  }
}
