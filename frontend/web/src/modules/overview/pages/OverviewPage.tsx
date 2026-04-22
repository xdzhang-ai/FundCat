/** 仪表盘首页，展示最近关注、用户动作与定投计划概览。 */
import type { PaperOrder, SipPlanDigest, WatchlistItem } from '@fundcat/contracts'
import { SectionCard } from '../../../common/components/SectionCard'
import { Panel, Row } from '../../../common/components/WebUi'
import { formatCurrency } from '../../../common/utils/fundInsights'

export function OverviewPage({
  watchlistPulse,
  recentActions,
  sipPlanDigests,
}: {
  watchlistPulse: WatchlistItem[]
  recentActions: PaperOrder[]
  sipPlanDigests: SipPlanDigest[]
}) {
  return (
    <>
      <section className="grid gap-6 xl:grid-cols-[0.5fr_0.5fr]">
        <SectionCard title="今日关注" eyebrow="Watchlist pulse">
          <div className="space-y-3">
            {watchlistPulse.slice(0, 4).map((item) => (
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
            {recentActions.slice(0, 4).map((order) => (
              <div data-testid={`overview-recent-action-${order.id}`} key={order.id} className="rounded-2xl bg-white/5 px-4 py-3">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <p className="font-medium text-white">{order.fundName}</p>
                    <p className="mt-1 text-sm text-slate-500">{formatOrderTime(order)}</p>
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
          {sipPlanDigests.slice(0, 6).map((plan) => (
            <Row
              key={plan.id}
              title={plan.fundName}
              meta={plan.nextRunOn ? `${plan.cadenceLabel} · 下次 ${plan.nextRunOn}` : `${plan.cadenceLabel} · ${plan.status}`}
              value={formatCurrency(plan.amount)}
            />
          ))}
        </Panel>
      </SectionCard>
    </>
  )
}

function formatOrderTime(order: PaperOrder) {
  return typeof order.executedAt === 'string' && order.executedAt.length > 0
    ? order.executedAt.replace('T', ' ')
    : '时间待补充'
}
