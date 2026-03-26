/** 定投计划编辑面板，负责周期、日期和金额的表单输入展示。 */
import type { SipCadenceInput, SipWeekdayInput } from '../model/sipPlanDetail'

export function SipPlanEditPanel({
  amount,
  cadence,
  weekday,
  monthDay,
  onAmountChange,
  onCadenceChange,
  onWeekdayChange,
  onMonthDayChange,
  onCancel,
  onSave,
}: {
  amount: string
  cadence: SipCadenceInput
  weekday: SipWeekdayInput
  monthDay: string
  onAmountChange: (value: string) => void
  onCadenceChange: (value: SipCadenceInput) => void
  onWeekdayChange: (value: SipWeekdayInput) => void
  onMonthDayChange: (value: string) => void
  onCancel: () => void
  onSave: () => void
}) {
  return (
    <div className="rounded-[1.6rem] border border-[color:var(--fc-color-accent)]/20 bg-white/5 px-5 py-5">
      <p className="text-xs uppercase tracking-[0.18em] text-[color:var(--fc-color-accent)]/80">编辑计划</p>
      <div className="mt-4 grid gap-3 lg:grid-cols-3">
        <label className="block">
          <span className="text-xs text-slate-400">定投周期</span>
          <select value={cadence} onChange={(event) => onCadenceChange(event.target.value as SipCadenceInput)} className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none focus:border-[color:var(--fc-color-accent)]/40">
            <option value="DAILY">每日</option>
            <option value="WEEKLY">每周</option>
            <option value="MONTHLY">每月</option>
          </select>
        </label>
        {cadence === 'WEEKLY' ? (
          <label className="block">
            <span className="text-xs text-slate-400">定投时间</span>
            <select value={weekday} onChange={(event) => onWeekdayChange(event.target.value as SipWeekdayInput)} className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none focus:border-[color:var(--fc-color-accent)]/40">
              <option value="1">周一</option>
              <option value="2">周二</option>
              <option value="3">周三</option>
              <option value="4">周四</option>
              <option value="5">周五</option>
              <option value="6">周六</option>
              <option value="0">周日</option>
            </select>
          </label>
        ) : null}
        {cadence === 'MONTHLY' ? (
          <label className="block">
            <span className="text-xs text-slate-400">定投日期</span>
            <select value={monthDay} onChange={(event) => onMonthDayChange(event.target.value)} className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none focus:border-[color:var(--fc-color-accent)]/40">
              {Array.from({ length: 28 }, (_, index) => {
                const day = String(index + 1)
                return (
                  <option key={day} value={day}>
                    每月 {day} 号
                  </option>
                )
              })}
            </select>
          </label>
        ) : null}
        <label className="block">
          <span className="text-xs text-slate-400">定投金额</span>
          <input type="number" min="0" step="0.01" value={amount} onChange={(event) => onAmountChange(event.target.value)} className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none focus:border-[color:var(--fc-color-accent)]/40" />
        </label>
      </div>
      <div className="mt-4 flex justify-end gap-2">
        <button type="button" onClick={onCancel} className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-300 transition hover:border-white/20 hover:text-white">
          取消
        </button>
        <button type="button" onClick={onSave} className="rounded-full border border-[color:var(--fc-color-accent)]/35 bg-[color:var(--fc-color-accent)]/14 px-4 py-2 text-sm text-[color:var(--fc-color-accent)] transition hover:border-[color:var(--fc-color-accent)]/55 hover:bg-[color:var(--fc-color-accent)]/20">
          保存修改
        </button>
      </div>
    </div>
  )
}
