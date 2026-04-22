/** 定投详情页，组装计划概览、编辑面板和执行记录列表。 */
import type { SipExecutionRecord, SipPlan } from '@fundcat/contracts'
import { ArrowLeft } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { SectionCard } from '../../../common/components/SectionCard'
import { formatCurrency } from '../../../common/utils/fundInsights'
import { SipPlanEditPanel } from '../components/SipPlanEditPanel'
import {
  deriveMonthDay,
  deriveWeekday,
  describeCadence,
  normalizeCadence,
  recordToneClass,
  resolveSipStatus,
  statusToneClass,
  type SipCadenceInput,
  type SipWeekdayInput,
} from '../model/sipPlanDetail'

export function SipPlanDetailPage({
  plan,
  records,
  onBack,
  onEditPlan,
  onTogglePlan,
  onStopPlan,
}: {
  plan: SipPlan
  records: SipExecutionRecord[]
  onBack: () => void
  onEditPlan: (
    planId: string,
    payload: {
      amount: number
      cadence: SipCadenceInput
      weekday?: SipWeekdayInput
      monthDay?: string
    },
  ) => void
  onTogglePlan: (planId: string) => void
  onStopPlan: (planId: string) => void
}) {
  const status = resolveSipStatus(plan)
  const [editOpen, setEditOpen] = useState(false)
  const [amount, setAmount] = useState(String(Number(plan.amount.toFixed(2))))
  const [cadence, setCadence] = useState<SipCadenceInput>(normalizeCadence(plan.cadence))
  const [weekday, setWeekday] = useState<SipWeekdayInput>(deriveWeekday(plan.nextRunAt))
  const [monthDay, setMonthDay] = useState(String(deriveMonthDay(plan.nextRunAt)))

  useEffect(() => {
    setEditOpen(false)
    setAmount(String(Number(plan.amount.toFixed(2))))
    setCadence(normalizeCadence(plan.cadence))
    setWeekday(deriveWeekday(plan.nextRunAt))
    setMonthDay(String(deriveMonthDay(plan.nextRunAt)))
  }, [plan])

  const cadenceLabel = useMemo(() => describeCadence(plan), [plan])

  return (
    <SectionCard
      title="定投记录页"
      eyebrow="SIP record page"
      action={
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={onBack}
            className="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-[color:var(--fc-color-accent)]/55 bg-[color:var(--fc-color-accent)] text-slate-950 shadow-[0_12px_30px_rgba(243,186,47,0.24)] transition hover:-translate-y-0.5 hover:shadow-[0_16px_36px_rgba(243,186,47,0.32)]"
            aria-label="返回定投计划"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          {status !== '停止' ? (
            <button
              type="button"
              onClick={() => setEditOpen((current) => !current)}
              className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-200 transition hover:border-white/20 hover:text-white"
            >
              编辑计划
            </button>
          ) : null}
          {status !== '停止' ? (
            <button
              type="button"
              onClick={() => onTogglePlan(plan.id)}
              className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-200 transition hover:border-white/20 hover:text-white"
            >
              {status === '生效' ? '暂停计划' : '恢复计划'}
            </button>
          ) : null}
          {status !== '停止' ? (
            <button
              type="button"
              onClick={() => onStopPlan(plan.id)}
              className="rounded-full border border-red-400/25 bg-red-500/10 px-4 py-2 text-sm text-red-200 transition hover:border-red-300/40 hover:bg-red-500/16"
            >
              停止计划
            </button>
          ) : null}
        </div>
      }
    >
      <div data-testid={`sip-plan-detail-${plan.id}`} className="space-y-5">
        <div className="rounded-[1.6rem] border border-white/8 bg-white/5 px-5 py-5">
          <p className="text-xs uppercase tracking-[0.18em] text-[color:var(--fc-color-accent)]/80">计划概览</p>
          <div className="mt-4 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <h3 className="font-[var(--fc-font-display)] text-3xl text-white">{plan.fundName}</h3>
              <p className="mt-2 text-sm text-slate-400">{plan.fundCode}</p>
              <p className="mt-1 text-xs text-slate-600">计划 ID {plan.id}</p>
              <p className="mt-2 text-sm text-slate-500">{cadenceLabel}</p>
            </div>
            <div className="flex flex-wrap items-center gap-3">
              <span className="rounded-full border border-white/10 bg-slate-950/45 px-3 py-1.5 text-sm text-slate-200">
                定投金额 {formatCurrency(plan.amount)}
              </span>
              <span className={`rounded-full border px-3 py-1.5 text-sm ${statusToneClass(status)}`}>状态：{status}</span>
              {status === '生效' ? (
                <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-sm text-slate-300">
                  下次定投 {plan.nextRunAt.slice(0, 10)}
                </span>
              ) : null}
            </div>
          </div>
        </div>

        {editOpen && status !== '停止' ? (
          <SipPlanEditPanel
            amount={amount}
            cadence={cadence}
            weekday={weekday}
            monthDay={monthDay}
            onAmountChange={setAmount}
            onCadenceChange={setCadence}
            onWeekdayChange={setWeekday}
            onMonthDayChange={setMonthDay}
            onCancel={() => setEditOpen(false)}
            onSave={() => {
              const parsedAmount = Number(amount)
              if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
                return
              }
              onEditPlan(plan.id, {
                amount: parsedAmount,
                cadence,
                weekday: cadence === 'WEEKLY' ? weekday : undefined,
                monthDay: cadence === 'MONTHLY' ? monthDay : undefined,
              })
              setEditOpen(false)
            }}
          />
        ) : null}

        <div className="rounded-[1.6rem] border border-white/8 bg-white/5">
          <div className="grid grid-cols-[1.1fr_0.9fr_0.8fr] gap-3 border-b border-white/8 px-4 py-3 text-xs uppercase tracking-[0.16em] text-slate-500">
            <span>执行日期</span>
            <span className="text-center">定投金额</span>
            <span className="text-right">状态</span>
          </div>
          <div data-testid="sip-record-list" className="divide-y divide-white/8">
            {records.map((record) => (
              <div data-testid={`sip-record-row-${record.id}`} key={record.id} className="grid grid-cols-[1.1fr_0.9fr_0.8fr] items-center gap-3 px-4 py-4">
                <p className="text-sm text-slate-300">{record.executedOn}</p>
                <p className="text-center font-[var(--fc-font-display)] text-lg text-white">{formatCurrency(record.amount)}</p>
                <p className={`text-right text-sm ${recordToneClass(record.status)}`}>{record.status}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </SectionCard>
  )
}
