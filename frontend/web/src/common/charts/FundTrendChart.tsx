/** 基金走势通用图表，封装详情页和其他视图共用的趋势曲线渲染。 */
import type { TrendPoint } from '@fundcat/contracts'
import {
  Area,
  AreaChart,
  CartesianGrid,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { formatAmount } from '../utils/fundInsights'

export type TrendMarker = {
  type: 'BUY' | 'SELL' | 'SIP'
  title: string
  detail: string
}

type TrendDatum = TrendPoint & {
  markers?: TrendMarker[]
  cumulativeReturn?: number
}

type FundTrendChartProps = {
  data: TrendDatum[]
  accent?: string
  costLine?: number
}

function formatAxisLabel(label: string) {
  const date = new Date(label)
  if (Number.isNaN(date.getTime())) {
    return label
  }
  if (label.includes('T')) {
    return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }
  return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

export function FundTrendChart({ data, accent = '#f3ba2f', costLine }: FundTrendChartProps) {
  return (
    <div className="h-72 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 12, right: 8, left: -18, bottom: 0 }}>
          <defs>
            <linearGradient id="fund-trend" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={accent} stopOpacity={0.45} />
              <stop offset="100%" stopColor={accent} stopOpacity={0.02} />
            </linearGradient>
          </defs>
          <CartesianGrid stroke="rgba(255,255,255,0.06)" vertical={false} />
          {costLine ? (
            <ReferenceLine
              y={costLine}
              stroke="rgba(96,165,250,0.9)"
              strokeDasharray="6 6"
              ifOverflow="extendDomain"
              label={{ value: '成本线', fill: '#93c5fd', position: 'insideTopRight', fontSize: 12 }}
            />
          ) : null}
          <XAxis
            dataKey="date"
            axisLine={false}
            tickLine={false}
            tickFormatter={formatAxisLabel}
            tick={{ fill: '#94a3b8', fontSize: 12 }}
            minTickGap={24}
          />
          <YAxis
            axisLine={false}
            tickLine={false}
            tick={{ fill: '#94a3b8', fontSize: 12 }}
            domain={['auto', 'auto']}
            tickFormatter={(value: number) => formatAmount(value)}
          />
          <Tooltip
            content={({ active, payload, label }) => {
              if (!active || !payload?.length) return null
              const point = payload[0]?.payload as TrendDatum
              return (
                <div className="min-w-[220px] rounded-[1.1rem] border border-white/10 bg-[rgba(18,21,28,0.96)] px-4 py-3 shadow-2xl">
                  <p className="text-xs uppercase tracking-[0.22em] text-slate-500">{formatAxisLabel(String(label ?? ''))}</p>
                  <p className="mt-2 font-[var(--fc-font-display)] text-xl text-white">{formatAmount(Number(point.value))}</p>
                  {typeof point.cumulativeReturn === 'number' ? (
                    <p className={`mt-1 text-sm ${point.cumulativeReturn >= 0 ? 'text-emerald-300' : 'text-orange-300'}`}>
                      区间累计 {point.cumulativeReturn >= 0 ? '+' : ''}
                      {point.cumulativeReturn.toFixed(2)}%
                    </p>
                  ) : null}
                  {point.markers?.length ? (
                    <div className="mt-3 space-y-2 border-t border-white/8 pt-3">
                      {point.markers.map((marker) => (
                        <div key={`${marker.type}-${marker.title}-${marker.detail}`}>
                          <p
                            className={`text-sm font-medium ${
                              marker.type === 'BUY'
                                ? 'text-emerald-300'
                                : marker.type === 'SELL'
                                  ? 'text-orange-300'
                                  : 'text-sky-300'
                            }`}
                          >
                            {marker.title}
                          </p>
                          <p className="text-xs text-slate-400">{marker.detail}</p>
                        </div>
                      ))}
                    </div>
                  ) : null}
                </div>
              )
            }}
          />
          <Area
            type="monotone"
            dataKey="value"
            stroke={accent}
            strokeWidth={3}
            fill="url(#fund-trend)"
            dot={(props) => {
              const { cx, cy, payload } = props
              if (!payload?.markers?.length || cx == null || cy == null) {
                return <></>
              }
              const marker = payload.markers[0] as TrendMarker
              const fill =
                marker.type === 'BUY' ? '#34d399' : marker.type === 'SELL' ? '#fdba74' : '#7dd3fc'
              return (
                <g>
                  <circle cx={cx} cy={cy} r={6} fill="rgba(15,23,42,0.96)" stroke={fill} strokeWidth={2} />
                  <circle cx={cx} cy={cy} r={2.2} fill={fill} />
                </g>
              )
            }}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
