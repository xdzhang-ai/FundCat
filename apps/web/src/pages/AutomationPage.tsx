import type { DashboardResponse } from '@fundcat/contracts'
import { SectionCard } from '../components/SectionCard'
import { Panel, Row } from '../components/workspace/WebUi'

export function AutomationPage({
  dashboard,
  isFlagEnabled,
}: {
  dashboard: DashboardResponse
  isFlagEnabled: (code: string) => boolean
}) {
  return (
    <SectionCard title="周报 / 定投 / OCR" eyebrow="Automation surfaces">
      <div className="grid gap-4 xl:grid-cols-2">
        <Panel title="">
          {dashboard.sipPlans.map((plan) => (
            <Row
              key={plan.id}
              title={plan.fundName}
              meta={`${plan.cadence} · 下次 ${plan.nextRunAt.replace('T', ' ')}`}
              value={`¥${plan.amount}`}
            />
          ))}
        </Panel>
        {isFlagEnabled('ocr_import') ? (
          <Panel title="OCR 导入">
            {dashboard.importJobs.map((job) => (
              <Row
                key={job.id}
                title={job.fileName}
                meta={`${job.sourcePlatform} · ${job.createdAt.replace('T', ' ')}`}
                value={job.status}
              />
            ))}
          </Panel>
        ) : null}
        {isFlagEnabled('weekly_digest') ? (
          <Panel title="研究周报">
            {dashboard.reports.map((report) => (
              <div key={report.id} className="rounded-2xl bg-white/5 px-4 py-3">
                <div className="flex items-center justify-between gap-4">
                  <p className="font-medium text-white">{report.weekLabel}</p>
                  <p className={report.returnRate >= 0 ? 'text-emerald-300' : 'text-orange-300'}>
                    {report.returnRate >= 0 ? '+' : ''}
                    {report.returnRate}%
                  </p>
                </div>
                <p className="mt-2 text-sm text-slate-300">{report.summary}</p>
                {isFlagEnabled('risk_signal_board') ? (
                  <p className="mt-1 text-xs text-slate-500">{report.riskNote}</p>
                ) : null}
              </div>
            ))}
          </Panel>
        ) : null}
        {isFlagEnabled('risk_signal_board') ? (
          <Panel title="提醒规则">
            {dashboard.alerts.map((alert) => (
              <Row
                key={alert.id}
                title={alert.fundCode}
                meta={`${alert.ruleType} · ${alert.channel}`}
                value={`${alert.thresholdValue}%`}
              />
            ))}
          </Panel>
        ) : null}
      </div>
    </SectionCard>
  )
}
