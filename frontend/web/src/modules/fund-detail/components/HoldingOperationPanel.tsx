/** 基金详情页买入卖出补记面板，按 /holdings/operations 的最小语义单元采集表单输入。 */
function buildTradeDateLimit(daysAgo: number) {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  date.setDate(date.getDate() - daysAgo)
  const timezoneOffset = date.getTimezoneOffset() * 60 * 1000
  return new Date(date.getTime() - timezoneOffset).toISOString().slice(0, 10)
}

function isBeforeThreePmToday(selectedDate: string) {
  const now = new Date()
  const today = buildTradeDateLimit(0)
  return selectedDate === today && now.getHours() < 15
}

export function HoldingOperationPanel({
  fundName,
  operation,
  tradeDate,
  amount,
  feeRate,
  shares,
  timing,
  onTradeDateChange,
  onAmountChange,
  onFeeRateChange,
  onSharesChange,
  onTimingChange,
  onCancel,
  onConfirm,
}: {
  fundName: string
  operation: 'BUY' | 'SELL'
  tradeDate: string
  amount: string
  feeRate: string
  shares: string
  timing: 'BEFORE_3PM' | 'AFTER_3PM'
  onTradeDateChange: (value: string) => void
  onAmountChange: (value: string) => void
  onFeeRateChange: (value: string) => void
  onSharesChange: (value: string) => void
  onTimingChange: (value: 'BEFORE_3PM' | 'AFTER_3PM') => void
  onCancel: () => void
  onConfirm: () => void
}) {
  const today = buildTradeDateLimit(0)
  const earliestDate = buildTradeDateLimit(30)
  const isBuy = operation === 'BUY'
  const disableAfterThreePm = isBeforeThreePmToday(tradeDate)

  return (
    <div className="absolute left-0 top-[calc(100%+0.6rem)] z-30 w-80 rounded-[1.25rem] border border-sky-400/20 bg-slate-950/98 p-3 shadow-[0_18px_40px_rgba(15,23,42,0.45)] backdrop-blur-xl">
      <p className="text-xs uppercase tracking-[0.2em] text-sky-300">{isBuy ? '补记买入' : '补记卖出'}</p>
      <p className="mt-1 text-sm text-white">{fundName}</p>
      <p className="mt-1 text-xs text-slate-400">按后台接口约定，买入提交金额，卖出提交份额；补记日期仅支持近 30 天。</p>
      <div className="mt-3 space-y-3">
        <label className="block">
          <span className="text-xs text-slate-400">确认时间</span>
          <div className="mt-1 grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => onTimingChange('BEFORE_3PM')}
              className={`rounded-xl border px-3 py-2 text-sm transition ${
                timing === 'BEFORE_3PM'
                  ? 'border-sky-300/45 bg-sky-400/14 text-white'
                  : 'border-white/10 bg-white/5 text-slate-300 hover:border-white/20 hover:text-white'
              }`}
            >
              15:00 前
            </button>
            <button
              type="button"
              disabled={disableAfterThreePm}
              onClick={() => onTimingChange('AFTER_3PM')}
              className={`rounded-xl border px-3 py-2 text-sm transition ${
                disableAfterThreePm
                  ? 'cursor-not-allowed border-white/8 bg-white/5 text-slate-500'
                  : timing === 'AFTER_3PM'
                  ? 'border-sky-300/45 bg-sky-400/14 text-white'
                  : 'border-white/10 bg-white/5 text-slate-300 hover:border-white/20 hover:text-white'
              }`}
            >
              15:00 后
            </button>
          </div>
        </label>
        <label className="block">
          <span className="text-xs text-slate-400">补记日期</span>
          <input
            type="date"
            min={earliestDate}
            max={today}
            value={tradeDate}
            onChange={(event) => onTradeDateChange(event.target.value)}
            className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none focus:border-sky-300/40"
          />
        </label>
        {isBuy ? (
          <label className="block">
            <span className="text-xs text-slate-400">买入金额</span>
            <input
              type="number"
              min="0"
              step="0.01"
              value={amount}
              onChange={(event) => onAmountChange(event.target.value)}
              placeholder="例如 2000"
              className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none placeholder:text-slate-500 focus:border-sky-300/40"
            />
          </label>
        ) : (
          <label className="block">
            <span className="text-xs text-slate-400">卖出份额</span>
            <input
              type="number"
              min="0"
              step="0.0001"
              value={shares}
              onChange={(event) => onSharesChange(event.target.value)}
              placeholder="例如 300.5"
              className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none placeholder:text-slate-500 focus:border-sky-300/40"
            />
          </label>
        )}
        <label className="block">
          <span className="text-xs text-slate-400">手续费率（%）</span>
          <input
            type="text"
            inputMode="decimal"
            value={feeRate}
            onChange={(event) => onFeeRateChange(event.target.value)}
            placeholder="默认 0"
            className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none placeholder:text-slate-500 focus:border-sky-300/40"
          />
        </label>
        <div className="rounded-xl border border-white/8 bg-white/5 px-3 py-2 text-xs text-slate-400">
          选择 15:00 前时按所选当天提交，选择 15:00 后时前端会把接口日期改成后一天；手续费率会自动按百分比除以 100 后再传给后端。
        </div>
        {disableAfterThreePm ? <p className="text-xs text-amber-300">当前时间还未到 15:00，今天的补记日期暂时不能选择 15:00 后。</p> : null}
      </div>
      <div className="mt-3 flex justify-end gap-2">
        <button type="button" onClick={onCancel} className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-slate-300 transition hover:border-white/20 hover:text-white">
          取消
        </button>
        <button type="button" onClick={onConfirm} className="rounded-full border border-sky-300/40 bg-sky-400/16 px-3 py-1.5 text-xs text-sky-100 transition hover:border-sky-200/60 hover:bg-sky-400/22">
          {isBuy ? '确认买入' : '确认卖出'}
        </button>
      </div>
    </div>
  )
}
