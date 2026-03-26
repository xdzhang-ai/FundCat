import type { DashboardResponse } from '@fundcat/contracts'
import { SectionCard } from '../components/SectionCard'
import { StatCard } from '../components/StatCard'
import { Panel, Row } from '../components/workspace/WebUi'
import { formatCurrency } from '../lib/fundInsights'

export function OverviewPage({
  dashboard,
}: {
  dashboard: DashboardResponse
}) {
  const visibleHeroMetrics = dashboard.heroMetrics.filter(
    (metric) => !['组合市值', 'OCR 队列', '高风险开关'].includes(metric.label),
  )

  return (
    <>
      <section className="rounded-[2rem] border border-white/8 bg-[linear-gradient(135deg,rgba(243,186,47,0.12),rgba(8,12,20,0.2)_45%,rgba(56,189,248,0.08))] px-5 py-8 shadow-[var(--fc-shadow-card)]">
        <div>
          <p className="text-sm uppercase tracking-[0.28em] text-[color:var(--fc-color-accent)]">Research Mode</p>
          <h2 className="mt-4 max-w-3xl font-[var(--fc-font-display)] text-4xl font-semibold text-white md:text-5xl">
            盘中参考估值、模拟交易与 OCR 导入在同一张研究画布上协同工作。
          </h2>
          <p className="mt-5 max-w-2xl text-lg text-slate-300">
            研究版默认将高风险能力收敛在 Feature Flag 与数据适配层之后，面向公开上线时可以按环境一键关闭。
          </p>
          {visibleHeroMetrics.length > 0 ? (
            <div className="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {visibleHeroMetrics.map((metric) => (
                <StatCard key={metric.label} {...metric} />
              ))}
            </div>
          ) : null}
        </div>
      </section>

      <section className="grid gap-6 xl:grid-cols-[0.5fr_0.5fr]">
        <SectionCard title="今日关注" eyebrow="Watchlist pulse">
          <div className="space-y-3">
            {dashboard.watchlist.slice(0, 4).map((item) => (
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

        <SectionCard title="最近动作" eyebrow="Action flow">
          <div className="space-y-3">
            {dashboard.orders.slice(0, 4).map((order) => (
              <div key={order.id} className="rounded-2xl bg-white/5 px-4 py-3">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <p className="font-medium text-white">{order.fundName}</p>
                    <p className="mt-1 text-sm text-slate-500">{order.executedAt.replace('T', ' ')}</p>
                  </div>
                  <div className="text-right">
                    <p
                      className={`rounded-full px-3 py-1 text-xs ${
                        order.orderType === 'BUY'
                          ? 'bg-emerald-400/10 text-emerald-300'
                          : 'bg-orange-400/10 text-orange-300'
                      }`}
                    >
                      {order.orderType}
                    </p>
                    <p className="mt-2 text-sm text-slate-300">{formatCurrency(order.amount)}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </SectionCard>
      </section>

      <SectionCard title="定投队列" eyebrow="SIP plans">
        <Panel title="">
          {dashboard.sipPlans.slice(0, 6).map((plan) => (
            <Row
              key={plan.id}
              title={plan.fundName}
              meta={`${plan.cadence} · 下次 ${plan.nextRunAt.replace('T', ' ')}`}
              value={formatCurrency(plan.amount)}
            />
          ))}
        </Panel>
      </SectionCard>
    </>
  )
}
