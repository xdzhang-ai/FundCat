/** 基金详情页买入卖出补记面板，按 /holdings/operations 的最小语义单元采集表单输入。 */
import type { PendingHoldingOperationTiming } from '../../../common/appTypes'

function buildTradeDateLimit(daysAgo: number) {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  date.setDate(date.getDate() - daysAgo)
  const timezoneOffset = date.getTimezoneOffset() * 60 * 1000
  return new Date(date.getTime() - timezoneOffset).toISOString().slice(0, 10)
}

function nextBusinessDate(baseDate: string) {
  const date = new Date(`${baseDate}T00:00:00`)
  do {
    date.setDate(date.getDate() + 1)
  } while (date.getDay() === 0 || date.getDay() === 6)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function HoldingOperationPanel({
  fundName,
  operation,
  tradeDate,
  timing,
  amount,
  feeRate,
  shares,
  onTradeDateChange,
  onTimingChange,
  onAmountChange,
  onFeeRateChange,
  onSharesChange,
  onCancel,
  onConfirm,
}: {
  fundName: string
  operation: 'BUY' | 'SELL'
  tradeDate: string
  timing: PendingHoldingOperationTiming
  amount: string
  feeRate: string
  shares: string
  onTradeDateChange: (value: string) => void
  onTimingChange: (value: PendingHoldingOperationTiming) => void
  onAmountChange: (value: string) => void
  onFeeRateChange: (value: string) => void
  onSharesChange: (value: string) => void
  onCancel: () => void
  onConfirm: () => void
}) {
  const today = buildTradeDateLimit(0)
  const earliestDate = buildTradeDateLimit(30)
  const isBuy = operation === 'BUY'
  const effectiveTradeDate = timing === 'AFTER_3PM' ? nextBusinessDate(tradeDate) : tradeDate

  return (
    <div
      data-testid={`holding-operation-panel-${operation.toLowerCase()}`}
      className="absolute left-0 top-[calc(100%+0.6rem)] z-30 w-80 rounded-[1.25rem] border border-sky-400/20 bg-slate-950/98 p-3 shadow-[0_18px_40px_rgba(15,23,42,0.45)] backdrop-blur-xl"
    >
      <p className="text-xs uppercase tracking-[0.2em] text-sky-300">{isBuy ? '补记买入' : '补记卖出'}</p>
      <p className="mt-1 text-sm text-white">{fundName}</p>
      <p className="mt-1 text-xs text-slate-400">按后台接口约定，买入提交金额，卖出提交份额；补记日期仅支持近 30 天，最终成交日由当前日期和 15:00 前后共同决定。</p>
      <div className="mt-3 space-y-3">
        <label className="block">
          <span className="text-xs text-slate-400">补记日期</span>
          <input
            data-testid="holding-operation-trade-date"
            type="date"
            min={earliestDate}
            max={today}
            value={tradeDate}
            onChange={(event) => onTradeDateChange(event.target.value)}
            className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none focus:border-sky-300/40"
          />
        </label>
        <div data-testid="holding-operation-timing" className="space-y-2">
          <span className="text-xs text-slate-400">买卖时间点</span>
          <div className="grid grid-cols-2 gap-2">
            <button
              data-testid="holding-operation-timing-before-3pm"
              type="button"
              onClick={() => onTimingChange('BEFORE_3PM')}
              className={`rounded-xl border px-3 py-2 text-xs transition ${
                timing === 'BEFORE_3PM'
                  ? 'border-sky-300/50 bg-sky-400/16 text-sky-100'
                  : 'border-white/10 bg-white/5 text-slate-300 hover:border-white/20 hover:text-white'
              }`}
            >
              15:00 前
            </button>
            <button
              data-testid="holding-operation-timing-after-3pm"
              type="button"
              onClick={() => onTimingChange('AFTER_3PM')}
              className={`rounded-xl border px-3 py-2 text-xs transition ${
                timing === 'AFTER_3PM'
                  ? 'border-sky-300/50 bg-sky-400/16 text-sky-100'
                  : 'border-white/10 bg-white/5 text-slate-300 hover:border-white/20 hover:text-white'
              }`}
            >
              15:00 后
            </button>
          </div>
        </div>
        {isBuy ? (
          <label className="block">
            <span className="text-xs text-slate-400">买入金额</span>
            <input
              data-testid="holding-operation-amount"
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
              data-testid="holding-operation-shares"
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
            data-testid="holding-operation-fee-rate"
            type="text"
            inputMode="decimal"
            value={feeRate}
            onChange={(event) => onFeeRateChange(event.target.value)}
            placeholder="默认 0"
            className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none placeholder:text-slate-500 focus:border-sky-300/40"
          />
        </label>
        <div className="rounded-xl border border-white/8 bg-white/5 px-3 py-2 text-xs text-slate-400">
          手续费率会自动按百分比除以 100 后再传给后端；当前提交将按 {effectiveTradeDate} 的净值确认，确认中期间不会立刻改动持仓结果。
        </div>
      </div>
      <div className="mt-3 flex justify-end gap-2">
        <button
          data-testid="holding-operation-cancel"
          type="button"
          onClick={onCancel}
          className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-slate-300 transition hover:border-white/20 hover:text-white"
        >
          取消
        </button>
        <button
          data-testid="holding-operation-confirm"
          type="button"
          onClick={onConfirm}
          className="rounded-full border border-sky-300/40 bg-sky-400/16 px-3 py-1.5 text-xs text-sky-100 transition hover:border-sky-200/60 hover:bg-sky-400/22"
        >
          {isBuy ? '确认买入' : '确认卖出'}
        </button>
      </div>
    </div>
  )
}
