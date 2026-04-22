/** 定投计划总览页，展示基金定投卡片、状态筛选与计划入口。 */
import type { SipPlan } from '@fundcat/contracts'
import { useMemo, useState } from 'react'
import { SectionCard } from '../../../common/components/SectionCard'
import { formatCurrency } from '../../../common/utils/fundInsights'

type SipStatus = '生效' | '暂停' | '停止'
type SipFilter = '全部' | SipStatus

const sipFilters: SipFilter[] = ['全部', '生效', '暂停', '停止']

export function AutomationPage({
  sipPlans,
  onOpenPlan,
}: {
  sipPlans: SipPlan[]
  onOpenPlan: (sipPlanId: string) => void
}) {
  const [activeFilter, setActiveFilter] = useState<SipFilter>('全部')

  const plans = useMemo(
    () =>
      uniqueSipPlans(sipPlans).map((plan) => ({
        ...plan,
        status: resolveSipStatus(plan),
      })),
    [sipPlans],
  )

  const visiblePlans = useMemo(
    () => (activeFilter === '全部' ? plans : plans.filter((plan) => plan.status === activeFilter)),
    [activeFilter, plans],
  )

  return (
    <SectionCard title="定投计划" eyebrow="SIP plans">
      <div className="flex flex-wrap gap-2">
        {sipFilters.map((filter) => (
          <button
            key={filter}
            type="button"
            onClick={() => setActiveFilter(filter)}
            className={`rounded-full border px-3 py-1.5 text-sm transition ${
              activeFilter === filter
                ? 'border-[color:var(--fc-color-accent)]/60 bg-[color:var(--fc-color-accent)]/10 text-white'
                : 'border-white/10 bg-white/5 text-slate-300 hover:border-white/20 hover:text-white'
            }`}
          >
            {filter}
          </button>
        ))}
      </div>

      <div className="mt-4 grid gap-3 xl:grid-cols-2">
        {visiblePlans.map((plan) => (
          <button
            key={plan.id}
            type="button"
            onClick={() => onOpenPlan(plan.id)}
            className="rounded-[1.5rem] border border-white/8 bg-white/5 px-4 py-4 text-left transition hover:border-white/15 hover:bg-white/[0.07]"
          >
            <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
              <div className="min-w-0">
                <p className="truncate font-medium text-white">{plan.fundName}</p>
                <p className="mt-1 text-sm text-slate-500">{plan.fundCode}</p>
                <p className="mt-1 text-xs text-slate-600">计划 ID {plan.id}</p>
              </div>

              <div className="flex flex-wrap items-center gap-3 lg:justify-end">
                <div className="rounded-full border border-white/10 bg-slate-950/45 px-3 py-1.5 text-sm text-slate-200">
                  定投金额 {formatCurrency(plan.amount)}
                </div>
                <span className={`rounded-full border px-3 py-1.5 text-sm ${statusToneClass(plan.status)}`}>
                  状态：{plan.status}
                </span>
              </div>
            </div>

            {plan.status === '生效' ? (
              <p className="mt-3 text-sm text-slate-400">下次定投时间：{plan.nextRunAt.slice(0, 10)}</p>
            ) : null}
          </button>
        ))}

        {visiblePlans.length === 0 ? (
          <div className="rounded-2xl border border-white/8 bg-white/5 px-4 py-5 text-sm text-slate-400 xl:col-span-2">
            当前筛选条件下没有定投计划。
          </div>
        ) : null}
      </div>
    </SectionCard>
  )
}

function resolveSipStatus(plan: SipPlan): SipStatus {
  if (plan.status === '生效' || plan.status === '暂停' || plan.status === '停止') {
    return plan.status
  }
  if (plan.active) {
    return '生效'
  }
  return new Date(plan.nextRunAt).getTime() < Date.now() ? '停止' : '暂停'
}

function uniqueSipPlans(plans: SipPlan[]) {
  const planMap = new Map<string, SipPlan>()
  plans.forEach((plan) => {
    const status = resolveSipStatus(plan)
    const key = status === '停止' ? `${plan.fundCode}:停止` : `${plan.fundCode}:当前`
    const current = planMap.get(key)
    if (!current) {
      planMap.set(key, plan)
      return
    }

    const currentStatus = resolveSipStatus(current)
    if (status !== '停止' && currentStatus === '停止') {
      planMap.set(key, plan)
      return
    }

    if (new Date(plan.nextRunAt).getTime() > new Date(current.nextRunAt).getTime()) {
      planMap.set(key, plan)
    }
  })
  return Array.from(planMap.values())
}

function statusToneClass(status: SipStatus) {
  switch (status) {
    case '生效':
      return 'border-emerald-400/25 bg-emerald-400/10 text-emerald-200'
    case '暂停':
      return 'border-white/10 bg-white/5 text-slate-300'
    case '停止':
      return 'border-red-400/25 bg-red-500/10 text-red-200'
  }
}
