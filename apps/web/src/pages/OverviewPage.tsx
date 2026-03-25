import type { DashboardResponse, FundDetail } from '@fundcat/contracts'
import { SectionCard } from '../components/SectionCard'
import { StatCard } from '../components/StatCard'
import { FundTrendChart } from '../components/charts/FundTrendChart'
import { Metric, Panel, Row } from '../components/workspace/WebUi'

export function OverviewPage({
  dashboard,
  selectedFund,
  isFlagEnabled,
}: {
  dashboard: DashboardResponse
  selectedFund: FundDetail
  isFlagEnabled: (code: string) => boolean
}) {
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
          <div className="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {dashboard.heroMetrics.map((metric) => (
              <StatCard key={metric.label} {...metric} />
            ))}
          </div>
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
                    <p className="mt-2 text-sm text-slate-300">¥{order.amount}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </SectionCard>
      </section>

      <section className="grid gap-6 xl:grid-cols-[0.58fr_0.42fr]">
        <SectionCard title="焦点基金" eyebrow="Selected fund">
          <div className="grid gap-5 xl:grid-cols-[0.62fr_0.38fr]">
            <div className="rounded-[1.75rem] border border-white/8 bg-slate-950/40 p-4">
              <div className="mb-4 flex flex-wrap items-center gap-3">
                <p className="font-[var(--fc-font-display)] text-3xl text-white">{selectedFund.estimatedNav}</p>
                <span
                  className={`rounded-full px-3 py-1 text-sm ${
                    selectedFund.estimatedGrowth >= 0
                      ? 'bg-emerald-400/10 text-emerald-300'
                      : 'bg-orange-400/10 text-orange-300'
                  }`}
                >
                  {selectedFund.estimatedGrowth >= 0 ? '+' : ''}
                  {selectedFund.estimatedGrowth}%
                </span>
                {selectedFund.referenceOnly ? (
                  <span className="rounded-full border border-[color:var(--fc-color-accent)]/25 bg-[color:var(--fc-color-accent)]/10 px-3 py-1 text-xs uppercase tracking-[0.24em] text-[color:var(--fc-color-accent)]">
                    reference only
                  </span>
                ) : null}
              </div>
              <FundTrendChart data={selectedFund.navHistory} />
            </div>

            <div className="space-y-4">
              <div className="rounded-[1.75rem] border border-white/8 bg-white/5 p-4">
                <p className="text-sm text-slate-400">基金画像</p>
                <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                  <Metric label="管理费" value={`${selectedFund.managementFee}%`} />
                  <Metric label="托管费" value={`${selectedFund.custodyFee}%`} />
                  <Metric label="申购费" value={`${selectedFund.purchaseFee}%`} />
                  <Metric label="规模" value={`${selectedFund.assetSize} 亿`} />
                </div>
              </div>
              <div className="rounded-[1.75rem] border border-white/8 bg-white/5 p-4">
                <p className="text-sm text-slate-400">比较标签</p>
                <div className="mt-4 flex flex-wrap gap-2">
                  {selectedFund.comparisonLabels.map((label) => (
                    <span key={label} className="rounded-full bg-slate-950/60 px-3 py-2 text-sm text-slate-200">
                      {label}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </SectionCard>

        <SectionCard title="自动化摘要" eyebrow="Daily cadence">
          <div className="space-y-4">
            <Panel title="模拟定投">
              {dashboard.sipPlans.slice(0, 3).map((plan) => (
                <Row
                  key={plan.id}
                  title={plan.fundName}
                  meta={`${plan.cadence} · 下次 ${plan.nextRunAt.replace('T', ' ')}`}
                  value={`¥${plan.amount}`}
                />
              ))}
            </Panel>
            {isFlagEnabled('weekly_digest') ? (
              <Panel title="最新周报">
                {dashboard.reports.slice(0, 2).map((report) => (
                  <Row
                    key={report.id}
                    title={report.weekLabel}
                    meta={report.summary}
                    value={`${report.returnRate >= 0 ? '+' : ''}${report.returnRate}%`}
                  />
                ))}
              </Panel>
            ) : null}
          </div>
        </SectionCard>
      </section>
    </>
  )
}
