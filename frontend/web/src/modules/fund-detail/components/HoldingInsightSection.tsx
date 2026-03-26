/** 基金详情页持仓洞察区块，展示当前持仓指标和盘中走势参考。 */
import type { FundCard, FundDetail, PortfolioSummary } from '@fundcat/contracts'
import { ChevronDown, ChevronUp } from 'lucide-react'
import { useMemo } from 'react'
import { Area, AreaChart, CartesianGrid, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { buildHoldingInsight, formatAmount, formatCompactPercent, formatCurrency } from '../../../common/utils/fundInsights'
import { buildIntradayTrend, valueSizeClass } from '../model/helpers'

export function HoldingInsightSection({
  portfolios,
  funds,
  selectedFund,
  expanded,
  onToggle,
}: {
  portfolios: PortfolioSummary[]
  funds: FundCard[]
  selectedFund: FundDetail
  expanded: boolean
  onToggle: () => void
}) {
  const holdingInsight = useMemo(
    () => buildHoldingInsight(portfolios, funds, selectedFund),
    [funds, portfolios, selectedFund],
  )
  const intradayTrend = useMemo(
    () => buildIntradayTrend(selectedFund, holdingInsight?.averageCost),
    [selectedFund, holdingInsight?.averageCost],
  )

  if (!holdingInsight) return null

  return (
    <div className="rounded-[1.45rem] border border-sky-400/15 bg-sky-400/5 px-4 py-3.5">
      <button onClick={onToggle} className="flex w-full items-center justify-between gap-4 text-left">
        <div>
          <p className="text-sm uppercase tracking-[0.22em] text-sky-300">Holding insight</p>
          <h4 className="mt-1.5 font-[var(--fc-font-display)] text-[1.75rem] leading-none text-white">当前持仓命中</h4>
          <p className="mt-1.5 text-sm text-slate-300">已持有该基金，展开后查看收益、成本和仓位拆分。</p>
        </div>
        <span className="rounded-full border border-white/10 bg-white/5 p-2 text-slate-300">
          {expanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
        </span>
      </button>

      {expanded ? (
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
                <p className="mt-1 text-sm text-slate-300">{selectedFund.referenceOnly ? '参考估值口径' : '净值口径'}</p>
              </div>
              <div className="text-right">
                <p className={`font-[var(--fc-font-display)] text-2xl ${selectedFund.estimatedGrowth >= 0 ? 'text-emerald-300' : 'text-orange-300'}`}>
                  {selectedFund.estimatedGrowth >= 0 ? '+' : ''}{selectedFund.estimatedGrowth.toFixed(2)}%
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
          <ReferenceLine y={baseGrowth} stroke="rgba(96,165,250,0.85)" strokeDasharray="5 5" ifOverflow="extendDomain" />
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
