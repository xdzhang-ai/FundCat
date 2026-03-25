import type { DashboardResponse, FundCard, FundDetail } from '@fundcat/contracts'
import { ArrowLeft, BellRing, ChevronDown, ChevronUp, Radar, ScanSearch, WalletCards } from 'lucide-react'
import type { ReactNode } from 'react'
import { useEffect, useMemo, useState } from 'react'
import { SectionCard } from '../components/SectionCard'
import { FundTrendChart } from '../components/charts/FundTrendChart'
import { QuickActionButton, formatSignedPercent } from '../components/workspace/WebUi'
import {
  buildHoldingInsight,
  buildTrendMarkers,
  calculateRangeReturn,
  filterRange,
  formatCompactPercent,
  formatCurrency,
  rangeOptions,
  type FundRangeKey,
} from '../lib/fundInsights'

export function FundDetailPage({
  dashboard,
  funds,
  selectedFund,
  isFlagEnabled,
  onBack,
  onQuickAction,
}: {
  dashboard: DashboardResponse
  funds: FundCard[]
  selectedFund: FundDetail
  isFlagEnabled: (code: string) => boolean
  onBack: () => void
  onQuickAction: (kind: 'watchlist' | 'order' | 'sip' | 'ocr') => void
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
            <QuickActionButton icon={Radar} label="加自选" onClick={() => onQuickAction('watchlist')} />
            <QuickActionButton icon={WalletCards} label="模拟下单" onClick={() => onQuickAction('order')} />
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
                <p className="font-[var(--fc-font-display)] text-4xl text-white">{selectedFund.estimatedNav.toFixed(4)}</p>
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
                <p className="text-sm text-slate-400">单位净值 {selectedFund.unitNav.toFixed(4)}</p>
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
                <div className="mt-4 grid gap-2 md:grid-cols-2 xl:grid-cols-5 2xl:grid-cols-6">
                  <HoldingMetric label="当日涨幅" value={formatCompactPercent(holdingInsight.dayChange)} />
                  <HoldingMetric label="近1年" value={formatCompactPercent(holdingInsight.oneYearReturn)} tone={holdingInsight.oneYearReturn >= 0 ? 'positive' : 'negative'} />
                  <HoldingMetric label="持有金额" value={formatCurrency(holdingInsight.amountHeld)} />
                  <HoldingMetric label="持有份额" value={holdingInsight.shares.toLocaleString('zh-CN', { maximumFractionDigits: 2 })} />
                  <HoldingMetric label="持仓占比" value={formatCompactPercent(holdingInsight.allocation)} />
                  <HoldingMetric label="持有收益" value={formatCurrency(holdingInsight.holdingPnl)} tone={holdingInsight.holdingPnl >= 0 ? 'positive' : 'negative'} />
                  <HoldingMetric label="持有收益率" value={formatCompactPercent(holdingInsight.holdingReturnRate)} tone={holdingInsight.holdingReturnRate >= 0 ? 'positive' : 'negative'} />
                  <HoldingMetric label="持仓成本" value={holdingInsight.averageCost.toFixed(4)} />
                  <HoldingMetric label="当日收益" value={holdingInsight.todayPnl == null ? '--' : formatCurrency(holdingInsight.todayPnl)} tone={holdingInsight.todayPnl != null && holdingInsight.todayPnl < 0 ? 'negative' : 'positive'} />
                  <HoldingMetric label="昨日收益" value={formatCurrency(holdingInsight.yesterdayPnl)} tone={holdingInsight.yesterdayPnl >= 0 ? 'positive' : 'negative'} />
                  <HoldingMetric label="持有天数" value={`${holdingInsight.holdingDays}`} />
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
    <div className="rounded-[1.05rem] bg-slate-950/50 px-3 py-2.5">
      <p className="text-xs uppercase tracking-[0.16em] text-slate-500">{label}</p>
      <p
        className={`mt-1.5 font-[var(--fc-font-display)] leading-none ${valueSizeClass(value)} ${
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
  if (length >= 12) return 'text-[1.55rem]'
  if (length >= 10) return 'text-[1.7rem]'
  if (length >= 8) return 'text-[1.82rem]'
  return 'text-[1.95rem]'
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
