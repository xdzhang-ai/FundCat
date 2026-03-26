/** 基金详情页路由入口，负责组装走势、持仓洞察与功能开关区块。 */
import { useEffect, useMemo, useState } from 'react'
import { FundTrendChart } from '../../../common/charts/FundTrendChart'
import { SectionCard } from '../../../common/components/SectionCard'
import {
  buildHoldingInsight,
  buildTrendMarkers,
  calculateRangeReturn,
  filterRange,
  rangeOptions,
  type FundRangeKey,
} from '../../../common/utils/fundInsights'
import { CollapsibleBlock } from '../components/CollapsibleBlock'
import { FundDetailActionBar } from '../components/FundDetailActionBar'
import { FundDetailHero } from '../components/FundDetailHero'
import { HoldingInsightSection } from '../components/HoldingInsightSection'
import type { FundDetailPageProps } from '../model/types'

export function FundDetailPage(props: FundDetailPageProps) {
  const { portfolios, orders, sipPlans, funds, selectedFund, isFlagEnabled } = props
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
    () => buildHoldingInsight(portfolios, funds, selectedFund),
    [funds, portfolios, selectedFund],
  )
  const activeSeries = selectedFund.referenceOnly ? selectedFund.estimateHistory : selectedFund.navHistory
  const markerMap = useMemo(() => buildTrendMarkers(orders, sipPlans, selectedFund), [orders, selectedFund, sipPlans])
  const rangeSeries = useMemo(() => filterRange(activeSeries, range), [activeSeries, range])
  const rangeReturn = calculateRangeReturn(rangeSeries)
  const rangeData = rangeSeries.map((point) => ({
    ...point,
    markers: markerMap.get(point.date.slice(0, 10)) ?? markerMap.get(point.date) ?? [],
  }))

  return (
    <section className="space-y-6">
      <SectionCard title="基金详情" eyebrow="Fund detail page" action={<FundDetailActionBar {...props} />}>
        <div className="space-y-5">
          <FundDetailHero selectedFund={selectedFund} />

          <HoldingInsightSection
            portfolios={portfolios}
            funds={funds}
            selectedFund={selectedFund}
            expanded={holdingExpanded}
            onToggle={() => setHoldingExpanded((current) => !current)}
          />

          <CollapsibleBlock
            title="业绩走势"
            subtitle={`当前区间：${rangeOptions().find((item) => item.key === range)?.label} · ${
              selectedFund.referenceOnly ? '盘中参考估值口径' : '前一交易日净值口径'
            }`}
            expanded={trendExpanded}
            onToggle={() => setTrendExpanded((current) => !current)}
          >
            <div className="mb-4 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
              <div>
                <p className="font-[var(--fc-font-display)] text-4xl text-white">{`${rangeReturn >= 0 ? '+' : ''}${rangeReturn.toFixed(2)}%`}</p>
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
                    <p className={holding.dailyChange >= 0 ? 'text-emerald-300' : 'text-orange-300'}>{`${holding.dailyChange >= 0 ? '+' : ''}${holding.dailyChange.toFixed(2)}%`}</p>
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
                      <div className="h-full rounded-[1rem] bg-[linear-gradient(90deg,#82aef7,#4567f2)]" style={{ width: `${Math.max(industry.weight, 8)}%` }} />
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
