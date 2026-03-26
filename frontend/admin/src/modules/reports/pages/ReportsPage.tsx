/** 周报提醒页，展示研究周报摘要和提醒规则列表。 */
import type { AlertRule, WeeklyReport } from '@fundcat/contracts'
import { DetailPill } from '../../../common/components/OpsUi'

export function ReportsPage({ reports, alerts }: { reports: WeeklyReport[]; alerts: AlertRule[] }) {
  return (
    <section className="mt-6 grid gap-6 xl:grid-cols-[0.56fr_0.44fr]">
      <div className="rounded-[2rem] border border-white/8 bg-white/5 p-6 shadow-[var(--fc-shadow-card)]">
        <h2 className="font-[var(--fc-font-display)] text-2xl text-white">周报摘要</h2>
        <div className="mt-5 space-y-4">
          {reports.map((report) => (
            <div key={report.id} className="rounded-[1.6rem] border border-white/8 bg-slate-950/35 p-5">
              <div className="flex items-center justify-between gap-4">
                <p className="font-medium text-white">{report.weekLabel}</p>
                <span className={`rounded-full px-3 py-1 text-xs ${report.returnRate >= 0 ? 'bg-emerald-400/10 text-emerald-300' : 'bg-orange-400/10 text-orange-300'}`}>
                  {report.returnRate >= 0 ? '+' : ''}
                  {report.returnRate}%
                </span>
              </div>
              <p className="mt-3 text-sm leading-7 text-slate-300">{report.summary}</p>
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <DetailPill label="最佳基金" value={report.bestFundCode} />
                <DetailPill label="风险提示" value={report.riskNote} />
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="rounded-[2rem] border border-white/8 bg-white/5 p-6 shadow-[var(--fc-shadow-card)]">
        <h2 className="font-[var(--fc-font-display)] text-2xl text-white">提醒规则</h2>
        <div className="mt-5 space-y-3">
          {alerts.map((alert) => (
            <div key={alert.id} className="rounded-[1.6rem] border border-white/8 bg-slate-950/35 p-4">
              <div className="flex items-center justify-between gap-4">
                <p className="font-medium text-white">{alert.fundCode}</p>
                <span className={`rounded-full px-3 py-1 text-xs uppercase tracking-[0.18em] ${alert.enabled ? 'bg-emerald-400/10 text-emerald-300' : 'bg-slate-500/20 text-slate-300'}`}>
                  {alert.enabled ? 'enabled' : 'disabled'}
                </span>
              </div>
              <p className="mt-3 text-sm text-slate-400">{alert.ruleType}</p>
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <DetailPill label="阈值" value={String(alert.thresholdValue)} />
                <DetailPill label="渠道" value={alert.channel} />
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

