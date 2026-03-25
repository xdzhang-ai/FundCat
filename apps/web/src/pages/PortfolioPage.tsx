import type { DashboardResponse } from '@fundcat/contracts'
import { SectionCard } from '../components/SectionCard'

export function PortfolioPage({ dashboard }: { dashboard: DashboardResponse }) {
  return (
    <>
      <section className="grid gap-6 xl:grid-cols-[0.54fr_0.46fr]">
        <SectionCard title="自选观察" eyebrow="Watchlist">
          <div className="space-y-3">
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
        </SectionCard>

        <SectionCard title="模拟流水" eyebrow="Paper trade">
          <div className="space-y-3">
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
        </SectionCard>
      </section>

      <SectionCard title="组合持仓" eyebrow="Portfolio book">
        <div className="grid gap-4 xl:grid-cols-2">
          {dashboard.portfolios.map((portfolio) => (
            <div key={portfolio.id} className="rounded-[1.75rem] border border-white/8 bg-white/5 p-4">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="font-[var(--fc-font-display)] text-xl text-white">{portfolio.name}</p>
                  <p className="mt-1 text-sm text-slate-400">{portfolio.broker}</p>
                </div>
                <div className="text-right">
                  <p className="font-[var(--fc-font-display)] text-2xl text-white">¥{portfolio.marketValue}</p>
                  <p className={portfolio.totalPnl >= 0 ? 'text-emerald-300' : 'text-orange-300'}>
                    {portfolio.totalPnl >= 0 ? '+' : ''}
                    {portfolio.totalPnl}
                  </p>
                </div>
              </div>
              <div className="mt-4 space-y-2">
                {portfolio.holdings.map((holding) => (
                  <div
                    key={holding.id}
                    className="grid grid-cols-[1fr_auto_auto] items-center gap-3 rounded-2xl bg-slate-950/45 px-4 py-3"
                  >
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
    </>
  )
}
