import type { DashboardResponse, FundCard, FundDetail } from '@fundcat/contracts'
import { ArrowLeft, BellRing, BriefcaseBusiness, ChevronDown, ChevronUp, Radar, ScanSearch, WalletCards } from 'lucide-react'
import type { ReactNode } from 'react'
import { useEffect, useMemo, useState } from 'react'
import { Area, AreaChart, CartesianGrid, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { SectionCard } from '../components/SectionCard'
import { FundTrendChart } from '../components/charts/FundTrendChart'
import { QuickActionButton, formatSignedPercent } from '../components/workspace/WebUi'
import {
  buildHoldingInsight,
  buildTrendMarkers,
  calculateRangeReturn,
  filterRange,
  formatAmount,
  formatCompactPercent,
  formatCurrency,
  rangeOptions,
  type FundRangeKey,
} from '../lib/fundInsights'

type WatchlistGroup = '全部' | '成长进攻' | '稳健配置' | '行业主题'

export function FundDetailPage({
  dashboard,
  funds,
  selectedFund,
  isFlagEnabled,
  onBack,
  onQuickAction,
  watchlistPickerOpen,
  watchlistPickerGroups,
  watchlistGroupOptions,
  onToggleWatchlistGroup,
  onCancelWatchlistPicker,
  onConfirmWatchlistPicker,
  holdingInputOpen,
  holdingFormMode,
  holdingAmount,
  holdingPnl,
  onHoldingAmountChange,
  onHoldingPnlChange,
  onCancelHoldingInput,
  onConfirmHoldingInput,
  onJumpToHoldings,
}: {
  dashboard: DashboardResponse
  funds: FundCard[]
  selectedFund: FundDetail
  isFlagEnabled: (code: string) => boolean
  onBack: () => void
  onQuickAction: (kind: 'watchlist' | 'holding' | 'editHolding' | 'sip' | 'ocr') => void
  watchlistPickerOpen: boolean
  watchlistPickerGroups: WatchlistGroup[]
  watchlistGroupOptions: WatchlistGroup[]
  onToggleWatchlistGroup: (group: WatchlistGroup) => void
  onCancelWatchlistPicker: () => void
  onConfirmWatchlistPicker: () => void
  holdingInputOpen: boolean
  holdingFormMode: 'add' | 'edit'
  holdingAmount: string
  holdingPnl: string
  onHoldingAmountChange: (value: string) => void
  onHoldingPnlChange: (value: string) => void
  onCancelHoldingInput: () => void
  onConfirmHoldingInput: () => void
  onJumpToHoldings: () => void
}) {
  const [range, setRange] = useState<FundRangeKey>('3m')
  const [holdingExpanded, setHoldingExpanded] = useState(true)
  const [trendExpanded, setTrendExpanded] = useState(true)
  const [quarterlyExpanded, setQuarterlyExpanded] = useState(true)
  const [industryExpanded, setIndustryExpanded] = useState(true)

  useEffect(() => {
    setRange('3m')
    setHoldingExpanded(true)
    setTrendExpanded(true)
    setQuarterlyExpanded(true)
    setIndustryExpanded(true)
  }, [selectedFund.code])

  const holdingInsight = useMemo(
    () => buildHoldingInsight(dashboard, funds, selectedFund),
    [dashboard, funds, selectedFund],
  )
  const activeSeries = selectedFund.referenceOnly ? selectedFund.estimateHistory : selectedFund.navHistory
  const markerMap = useMemo(() => buildTrendMarkers(dashboard, selectedFund), [dashboard, selectedFund])
  const rangeSeries = useMemo(() => filterRange(activeSeries, range), [activeSeries, range])
  const rangeReturn = calculateRangeReturn(rangeSeries)
  const rangeData = rangeSeries.map((point) => ({
    ...point,
    markers: markerMap.get(point.date.slice(0, 10)) ?? markerMap.get(point.date) ?? [],
  }))
  const intradayTrend = useMemo(
    () => buildIntradayTrend(selectedFund, holdingInsight?.averageCost),
    [selectedFund, holdingInsight?.averageCost],
  )

  return (
    <section className="space-y-6">
      <SectionCard
        title="基金详情"
        eyebrow="Fund detail page"
        action={
          <div className="flex flex-wrap gap-2">
            <button
              onClick={onBack}
              className="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-[color:var(--fc-color-accent)]/55 bg-[color:var(--fc-color-accent)] text-slate-950 shadow-[0_12px_30px_rgba(243,186,47,0.24)] transition hover:-translate-y-0.5 hover:shadow-[0_16px_36px_rgba(243,186,47,0.32)]"
              aria-label="返回上一页"
            >
              <ArrowLeft className="h-5 w-5" />
            </button>
            <div className="relative">
              <button
                type="button"
                disabled={selectedFund.watchlisted}
                onClick={() => onQuickAction('watchlist')}
                className={`inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm transition ${
                  selectedFund.watchlisted
                    ? 'cursor-not-allowed border-[color:var(--fc-color-accent)]/20 bg-[color:var(--fc-color-accent)]/10 text-[color:var(--fc-color-accent)]/65'
                    : 'border-white/10 bg-white/5 text-slate-200 hover:border-[color:var(--fc-color-accent)]/50 hover:text-white'
                }`}
              >
                <Radar className="h-4 w-4" />
                {selectedFund.watchlisted ? '已自选' : '加自选'}
              </button>
              {watchlistPickerOpen ? (
                <div className="absolute left-0 top-[calc(100%+0.6rem)] z-30 w-72 rounded-[1.25rem] border border-sky-400/20 bg-slate-950/98 p-3 shadow-[0_18px_40px_rgba(15,23,42,0.45)] backdrop-blur-xl">
                  <p className="text-xs uppercase tracking-[0.2em] text-sky-300">加入自选分组</p>
                  <p className="mt-1 text-sm text-white">{selectedFund.name}</p>
                  <p className="mt-1 text-xs text-slate-400">默认勾选“全部”，最多只能勾选 2 个。</p>
                  <div className="mt-3 space-y-2">
                    {watchlistGroupOptions.map((group) => {
                      const checked = watchlistPickerGroups.includes(group)
                      const disabled = group !== '全部' && !checked && watchlistPickerGroups.length >= 2
                      return (
                        <button
                          key={group}
                          type="button"
                          disabled={disabled}
                          onClick={() => onToggleWatchlistGroup(group)}
                          className={`flex w-full items-center justify-between rounded-xl border px-3 py-2 text-sm transition ${
                            checked
                              ? 'border-sky-300/45 bg-sky-400/14 text-white'
                              : disabled
                                ? 'cursor-not-allowed border-white/8 bg-white/5 text-slate-500'
                                : 'border-white/10 bg-white/5 text-slate-200 hover:border-white/20 hover:text-white'
                          }`}
                        >
                          <span>{group}</span>
                          <span
                            className={`inline-flex h-4 w-4 items-center justify-center rounded border text-[0.72rem] ${
                              checked ? 'border-sky-200/70 bg-sky-300/20 text-white' : 'border-white/20 text-transparent'
                            }`}
                          >
                            ✓
                          </span>
                        </button>
                      )
                    })}
                  </div>
                  <div className="mt-3 flex justify-end gap-2">
                    <button
                      type="button"
                      onClick={onCancelWatchlistPicker}
                      className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-slate-300 transition hover:border-white/20 hover:text-white"
                    >
                      取消
                    </button>
                    <button
                      type="button"
                      onClick={onConfirmWatchlistPicker}
                      className="rounded-full border border-sky-300/40 bg-sky-400/16 px-3 py-1.5 text-xs text-sky-100 transition hover:border-sky-200/60 hover:bg-sky-400/22"
                    >
                      确认
                    </button>
                  </div>
                </div>
              ) : null}
            </div>
            <div className="relative">
              {selectedFund.held ? (
                <button
                  type="button"
                  onClick={onJumpToHoldings}
                  className="inline-flex items-center gap-2 rounded-full border border-emerald-300/30 bg-emerald-400/12 px-4 py-2 text-sm text-emerald-100 transition hover:border-emerald-200/55 hover:bg-emerald-400/18"
                >
                  <BriefcaseBusiness className="h-4 w-4" />
                  已持仓
                </button>
              ) : (
                <QuickActionButton icon={BriefcaseBusiness} label="加持仓" onClick={() => onQuickAction('holding')} />
              )}
              {holdingInputOpen ? (
                <div className="absolute left-0 top-[calc(100%+0.6rem)] z-30 w-80 rounded-[1.25rem] border border-emerald-400/20 bg-slate-950/98 p-3 shadow-[0_18px_40px_rgba(15,23,42,0.45)] backdrop-blur-xl">
                  <p className="text-xs uppercase tracking-[0.2em] text-emerald-300">{holdingFormMode === 'edit' ? '修改持仓' : '加入持仓'}</p>
                  <p className="mt-1 text-sm text-white">{selectedFund.name}</p>
                  <div className="mt-3 space-y-3">
                    <label className="block">
                      <span className="text-xs text-slate-400">持有金额</span>
                      <input
                        type="number"
                        min="0"
                        step="0.01"
                        value={holdingAmount}
                        onChange={(event) => onHoldingAmountChange(event.target.value)}
                        placeholder="例如 5000"
                        className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none placeholder:text-slate-500 focus:border-emerald-300/40"
                      />
                    </label>
                    <label className="block">
                      <span className="text-xs text-slate-400">持有收益</span>
                      <input
                        type="number"
                        step="0.01"
                        value={holdingPnl}
                        onChange={(event) => onHoldingPnlChange(event.target.value)}
                        placeholder="例如 320 或 -120"
                        className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none placeholder:text-slate-500 focus:border-emerald-300/40"
                      />
                    </label>
                  </div>
                  <div className="mt-3 flex justify-end gap-2">
                    <button
                      type="button"
                      onClick={onCancelHoldingInput}
                      className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-slate-300 transition hover:border-white/20 hover:text-white"
                    >
                      取消
                    </button>
                    <button
                      type="button"
                      onClick={onConfirmHoldingInput}
                      className="rounded-full border border-emerald-300/40 bg-emerald-400/16 px-3 py-1.5 text-xs text-emerald-100 transition hover:border-emerald-200/60 hover:bg-emerald-400/22"
                    >
                      {holdingFormMode === 'edit' ? '确认修改' : '加入持仓'}
                    </button>
                  </div>
                </div>
              ) : null}
            </div>
            <button
              type="button"
              disabled={!selectedFund.held}
              onClick={() => onQuickAction('editHolding')}
              className={`inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm transition ${
                selectedFund.held
                  ? 'border-white/10 bg-white/5 text-slate-200 hover:border-[color:var(--fc-color-accent)]/50 hover:text-white'
                  : 'cursor-not-allowed border-white/8 bg-white/5 text-slate-500'
              }`}
            >
              <WalletCards className="h-4 w-4" />
              修改持仓
            </button>
            <QuickActionButton icon={BellRing} label="设定投" onClick={() => onQuickAction('sip')} />
            {isFlagEnabled('ocr_import') ? (
              <QuickActionButton icon={ScanSearch} label="建 OCR 任务" onClick={() => onQuickAction('ocr')} />
            ) : null}
          </div>
        }
      >
        <div className="space-y-5">
          <div className="rounded-[1.8rem] border border-white/8 bg-[linear-gradient(135deg,rgba(255,255,255,0.05),rgba(8,12,20,0.16)_55%,rgba(243,186,47,0.1))] p-5">
            <div className="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
              <div className="min-w-0">
                <p className="text-xs uppercase tracking-[0.28em] text-[color:var(--fc-color-accent)]">{selectedFund.code}</p>
                <h3 className="mt-3 font-[var(--fc-font-display)] text-3xl font-semibold text-white xl:text-4xl">
                  {selectedFund.name}
                </h3>
                <p className="mt-3 text-sm text-slate-400">
                  {selectedFund.category} · {selectedFund.riskLevel} · 跟踪基准 {selectedFund.benchmark}
                </p>
                <div className="mt-4 flex flex-wrap gap-2">
                  {selectedFund.tags.map((tag) => (
                    <span key={tag} className="rounded-full border border-white/10 bg-slate-950/45 px-3 py-1.5 text-xs text-slate-200">
                      {tag}
                    </span>
                  ))}
                </div>
              </div>

              <div className="grid gap-3 xl:min-w-[240px] xl:justify-items-end">
                <p className="font-[var(--fc-font-display)] text-4xl text-white">{formatAmount(selectedFund.estimatedNav)}</p>
                <div className="flex flex-wrap gap-2 xl:justify-end">
                  <span
                    className={`rounded-full px-3 py-1 text-sm ${
                      selectedFund.estimatedGrowth >= 0 ? 'bg-emerald-400/10 text-emerald-300' : 'bg-orange-400/10 text-orange-300'
                    }`}
                  >
                    {formatSignedPercent(selectedFund.estimatedGrowth)}
                  </span>
                  {selectedFund.referenceOnly ? (
                    <span className="rounded-full border border-[color:var(--fc-color-accent)]/25 bg-[color:var(--fc-color-accent)]/10 px-3 py-1 text-xs uppercase tracking-[0.24em] text-[color:var(--fc-color-accent)]">
                      reference only
                    </span>
                  ) : null}
                </div>
                <p className="text-sm text-slate-400">单位净值 {formatAmount(selectedFund.unitNav)}</p>
              </div>
            </div>
          </div>

          {holdingInsight ? (
            <div className="rounded-[1.45rem] border border-sky-400/15 bg-sky-400/5 px-4 py-3.5">
              <button
                onClick={() => setHoldingExpanded((current) => !current)}
                className="flex w-full items-center justify-between gap-4 text-left"
              >
                <div>
                  <p className="text-sm uppercase tracking-[0.22em] text-sky-300">Holding insight</p>
                  <h4 className="mt-1.5 font-[var(--fc-font-display)] text-[1.75rem] leading-none text-white">当前持仓命中</h4>
                  <p className="mt-1.5 text-sm text-slate-300">已持有该基金，展开后查看收益、成本和仓位拆分。</p>
                </div>
                <span className="rounded-full border border-white/10 bg-white/5 p-2 text-slate-300">
                  {holdingExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                </span>
              </button>

              {holdingExpanded ? (
                <div className="mt-4 grid gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(420px,1.1fr)]">
                  <div className="flex flex-wrap items-start gap-x-3 gap-y-2">
                    <HoldingMetric label="持有金额" value={formatCurrency(holdingInsight.amountHeld)} />
                    <HoldingMetric label="持有收益" value={formatCurrency(holdingInsight.holdingPnl)} tone={holdingInsight.holdingPnl >= 0 ? 'positive' : 'negative'} />
                    <HoldingMetric label="持有收益率" value={formatCompactPercent(holdingInsight.holdingReturnRate)} tone={holdingInsight.holdingReturnRate >= 0 ? 'positive' : 'negative'} />
                    <HoldingMetric label="持有份额" value={holdingInsight.shares.toLocaleString('zh-CN', { maximumFractionDigits: 2 })} />
                    <HoldingMetric label="持仓成本" value={formatAmount(holdingInsight.averageCost)} />
                    <HoldingMetric label="持仓占比" value={formatCompactPercent(holdingInsight.allocation)} />
                    <HoldingMetric label="当日涨幅" value={formatCompactPercent(holdingInsight.dayChange)} tone={holdingInsight.dayChange != null && holdingInsight.dayChange < 0 ? 'negative' : 'positive'} />
                    <HoldingMetric label="当日收益" value={holdingInsight.todayPnl == null ? '--' : formatCurrency(holdingInsight.todayPnl)} tone={holdingInsight.todayPnl != null && holdingInsight.todayPnl < 0 ? 'negative' : 'positive'} />
                    <HoldingMetric label="昨日收益" value={formatCurrency(holdingInsight.yesterdayPnl)} tone={holdingInsight.yesterdayPnl >= 0 ? 'positive' : 'negative'} />
                    <HoldingMetric label="近1年" value={formatCompactPercent(holdingInsight.oneYearReturn)} tone={holdingInsight.oneYearReturn >= 0 ? 'positive' : 'negative'} />
                    <HoldingMetric label="持有天数" value={`${holdingInsight.holdingDays}`} />
                  </div>

                  <div className="rounded-[1.1rem] border border-white/8 bg-slate-950/45 px-4 py-3">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-xs uppercase tracking-[0.16em] text-slate-500">当日实时走势</p>
                        <p className="mt-1 text-sm text-slate-300">
                          {selectedFund.referenceOnly ? '参考估值口径' : '净值口径'}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className={`font-[var(--fc-font-display)] text-2xl ${selectedFund.estimatedGrowth >= 0 ? 'text-emerald-300' : 'text-orange-300'}`}>
                          {formatSignedPercent(selectedFund.estimatedGrowth)}
                        </p>
                        <p className="mt-1 text-xs text-slate-500">{formatAmount(selectedFund.estimatedNav)}</p>
                      </div>
                    </div>
                    <div className="mt-4">
                      <IntradayHoldingChart data={intradayTrend} costLine={holdingInsight.averageCost} />
                    </div>
                  </div>
                </div>
              ) : null}
            </div>
          ) : null}

          <CollapsibleBlock
            title="业绩走势"
            subtitle={`当前区间：${rangeOptions().find((item) => item.key === range)?.label} · ${selectedFund.referenceOnly ? '盘中参考估值口径' : '前一交易日净值口径'}`}
            expanded={trendExpanded}
            onToggle={() => setTrendExpanded((current) => !current)}
          >
            <div className="mb-4 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
              <div>
                <p className="font-[var(--fc-font-display)] text-4xl text-white">{formatSignedPercent(rangeReturn)}</p>
              </div>
              <div className="flex flex-wrap gap-2">
                {rangeOptions().map((option) => (
                  <button
                    key={option.key}
                    onClick={() => setRange(option.key)}
                    className={`rounded-full border px-3 py-1.5 text-sm transition ${
                      option.key === range
                        ? 'border-[color:var(--fc-color-accent)]/60 bg-[color:var(--fc-color-accent)]/10 text-white'
                        : 'border-white/10 bg-white/5 text-slate-300 hover:border-white/20'
                    }`}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>
            <FundTrendChart
              key={`${selectedFund.code}-${range}-${selectedFund.referenceOnly ? 'estimate' : 'nav'}`}
              data={rangeData}
              costLine={holdingInsight?.averageCost}
            />
            <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-400">
              <span className="rounded-full border border-emerald-400/20 bg-emerald-400/10 px-3 py-1">买入点位</span>
              <span className="rounded-full border border-orange-400/20 bg-orange-400/10 px-3 py-1">卖出点位</span>
              <span className="rounded-full border border-sky-400/20 bg-sky-400/10 px-3 py-1">定投点位预留</span>
              {holdingInsight ? <span className="rounded-full border border-sky-300/20 bg-sky-300/10 px-3 py-1">蓝色虚线为当前持仓成本线</span> : null}
            </div>
          </CollapsibleBlock>

          {isFlagEnabled('quarterly_holdings') ? (
            <CollapsibleBlock
              title="上季度持仓主要股票"
              subtitle="用于参考基金经理上一季披露的主仓股票结构。"
              expanded={quarterlyExpanded}
              onToggle={() => setQuarterlyExpanded((current) => !current)}
            >
              <div className="overflow-hidden rounded-[1.4rem] border border-white/8 bg-slate-950/45">
                <div className="grid grid-cols-[1.4fr_0.7fr_0.7fr_0.8fr] gap-3 border-b border-white/8 px-4 py-3 text-xs uppercase tracking-[0.16em] text-slate-500">
                  <span>股票名称</span>
                  <span>涨幅</span>
                  <span>持仓占比</span>
                  <span>较上期占比</span>
                </div>
                {selectedFund.quarterlyHoldings.map((holding) => (
                  <div key={holding.symbol} className="grid grid-cols-[1.4fr_0.7fr_0.7fr_0.8fr] gap-3 px-4 py-3.5 text-[1.08rem] text-slate-200">
                    <div>
                      <p className="font-medium text-white">{holding.name}</p>
                      <p className="mt-1 text-sm text-slate-500">{holding.symbol}</p>
                    </div>
                    <p className={holding.dailyChange >= 0 ? 'text-emerald-300' : 'text-orange-300'}>{formatSignedPercent(holding.dailyChange)}</p>
                    <p>{holding.positionRatio.toFixed(2)}%</p>
                    <p className={holding.previousChange != null && holding.previousChange < 0 ? 'text-emerald-300' : 'text-orange-300'}>
                      {holding.changeLabel ?? (holding.previousChange == null ? '--' : `${holding.previousChange >= 0 ? '+' : ''}${holding.previousChange.toFixed(2)}%`)}
                    </p>
                  </div>
                ))}
              </div>
            </CollapsibleBlock>
          ) : null}

          {isFlagEnabled('industry_distribution') ? (
            <CollapsibleBlock
              title="行业分布"
              subtitle="按披露股票仓位拆解行业权重，可与上季度持仓交叉查看。"
              expanded={industryExpanded}
              onToggle={() => setIndustryExpanded((current) => !current)}
            >
              <div className="space-y-4">
                {selectedFund.industryDistribution.map((industry) => (
                  <div key={industry.name} className="grid grid-cols-[120px_minmax(0,1fr)_90px] items-center gap-4">
                    <p className="text-2xl text-slate-200">{industry.name}</p>
                    <div className="h-12 rounded-2xl bg-slate-950/45 p-1">
                      <div
                        className="h-full rounded-[1rem] bg-[linear-gradient(90deg,#82aef7,#4567f2)]"
                        style={{ width: `${Math.max(industry.weight, 8)}%` }}
                      />
                    </div>
                    <p className="text-right font-[var(--fc-font-display)] text-2xl text-slate-200">{industry.weight.toFixed(2)}%</p>
                  </div>
                ))}
              </div>
            </CollapsibleBlock>
          ) : null}
        </div>
      </SectionCard>
    </section>
  )
}

function HoldingMetric({
  label,
  value,
  tone = 'neutral',
}: {
  label: string
  value: string
  tone?: 'positive' | 'negative' | 'neutral'
}) {
  return (
    <div className="flex h-[76px] w-[176px] flex-col items-center justify-center rounded-[0.85rem] bg-slate-950/50 px-2.5 py-1.5">
      <p className="text-[11px] uppercase tracking-[0.14em] text-slate-500">{label}</p>
      <p
        className={`mt-2 text-center font-[var(--fc-font-display)] leading-none ${valueSizeClass(value)} ${
          tone === 'positive' ? 'text-emerald-300' : tone === 'negative' ? 'text-orange-300' : 'text-white'
        }`}
      >
        {value}
      </p>
    </div>
  )
}

function valueSizeClass(value: string) {
  const length = value.length
  if (length >= 12) return 'text-[1.00rem]'
  if (length >= 10) return 'text-[1.12rem]'
  if (length >= 8) return 'text-[1.24rem]'
  return 'text-[1.58rem]'
}

function IntradayHoldingChart({
  data,
  costLine,
}: {
  data: Array<{ x: number; time: string; value: number | null; growth: number | null }>
  costLine: number
}) {
  const firstValue = data.find((point) => point.value != null)?.value
  const baseGrowth = firstValue ? ((costLine - firstValue) / firstValue) * 100 : 0
  const intradayTicks = [9.5, 10.5, 11.5, 12.5, 13.5]

  return (
    <div className="h-56 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 12, right: 16, left: 8, bottom: 8 }}>
          <defs>
            <linearGradient id="holding-intraday" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#7dd3fc" stopOpacity={0.35} />
              <stop offset="100%" stopColor="#7dd3fc" stopOpacity={0.02} />
            </linearGradient>
          </defs>
          <CartesianGrid stroke="rgba(255,255,255,0.05)" vertical={false} />
          <XAxis
            dataKey="x"
            type="number"
            axisLine={false}
            tickLine={false}
            ticks={intradayTicks}
            tick={{ fill: '#94a3b8', fontSize: 11 }}
            domain={[9.5, 13.5]}
            padding={{ left: 10, right: 18 }}
            tickFormatter={(value: number) => {
              if (value === 9.5) return '09:30'
              if (value === 10.5) return '10:30'
              if (value === 11.5) return '11:30/13:00'
              if (value === 12.5) return '14:00'
              if (value === 13.5) return '15:00'
              return ''
            }}
          />
          <YAxis
            axisLine={false}
            tickLine={false}
            tick={{ fill: '#94a3b8', fontSize: 11 }}
            domain={['auto', 'auto']}
            tickFormatter={(value: number) => `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`}
            width={52}
          />
          <ReferenceLine
            y={baseGrowth}
            stroke="rgba(96,165,250,0.85)"
            strokeDasharray="5 5"
            ifOverflow="extendDomain"
          />
          <Tooltip
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null
              const point = payload[0]?.payload as { time: string; value: number; growth: number }
              if (point.value == null || point.growth == null) return null
              return (
                <div className="rounded-[1rem] border border-white/10 bg-[rgba(18,21,28,0.96)] px-3 py-2.5 shadow-xl">
                  <p className="text-xs uppercase tracking-[0.16em] text-slate-500">{point.time}</p>
                  <p className="mt-1 font-[var(--fc-font-display)] text-lg text-white">
                    {point.growth >= 0 ? '+' : ''}{point.growth.toFixed(2)}%
                  </p>
                </div>
              )
            }}
          />
          <Area type="monotone" dataKey="growth" stroke="#7dd3fc" strokeWidth={2.5} fill="url(#holding-intraday)" connectNulls={false} />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}

function buildIntradayTrend(detail: FundDetail, costLine?: number) {
  const intradayMockOffsets = [
    { x: 9.5, time: '09:30', ratio: 0.18 },
    { x: 10, time: '10:00', ratio: 0.34 },
    { x: 10.5, time: '10:30', ratio: 0.56 },
    { x: 11, time: '11:00', ratio: 0.71 },
    { x: 11.5, time: '11:30/13:00', ratio: 0.63 },
    { x: 12, time: '13:30', ratio: 0.88 },
    { x: 12.5, time: '14:00', ratio: 1 },
  ]
  const start = detail.unitNav
  const end = detail.estimatedNav

  const trend = intradayMockOffsets.map((point, index) => {
    const baseline = start + (end - start) * point.ratio
    const wave = Math.sin((index + 1) * 0.9) * Math.max(start * 0.006, 0.0018)
    const pullToCost = costLine ? (costLine - baseline) * 0.018 : 0
    return {
      x: point.x,
      time: point.time,
      value: Number((baseline + wave + pullToCost).toFixed(4)),
      growth: Number((((baseline + wave + pullToCost - detail.unitNav) / detail.unitNav) * 100).toFixed(2)),
    }
  })

  return [
    ...trend,
    {
      x: 13.5,
      time: '15:00',
      value: null,
      growth: null,
    },
  ]
}

function CollapsibleBlock({
  title,
  subtitle,
  expanded,
  onToggle,
  children,
}: {
  title: string
  subtitle?: string
  expanded: boolean
  onToggle: () => void
  children: ReactNode
}) {
  return (
    <div className="rounded-[1.75rem] border border-white/8 bg-white/5 p-4">
      <button onClick={onToggle} className="flex w-full items-start justify-between gap-4 text-left">
        <div>
          <p className="text-sm text-slate-300">{title}</p>
          {subtitle ? <p className="mt-1 text-xs text-slate-500">{subtitle}</p> : null}
        </div>
        <span className="rounded-full border border-white/10 bg-slate-950/45 p-2 text-slate-300">
          {expanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
        </span>
      </button>
      {expanded ? <div className="mt-4">{children}</div> : null}
    </div>
  )
}
