/** 基金详情页操作条，集中承接自选、持仓、买卖补记和定投等交互入口。 */
import { ArrowLeft, BellRing, BriefcaseBusiness, Plus, Radar, TrendingDown, WalletCards } from 'lucide-react'
import { QuickActionButton } from '../../../common/components/WebUi'
import type { WatchlistGroup } from '../../../common/appTypes'
import { HoldingOperationPanel } from './HoldingOperationPanel'
import type { FundDetailPageProps } from '../model/types'

type Props = Pick<
  FundDetailPageProps,
  | 'selectedFund'
  | 'onBack'
  | 'onQuickAction'
  | 'watchlistPickerOpen'
  | 'watchlistPickerGroups'
  | 'watchlistGroupOptions'
  | 'onToggleWatchlistGroup'
  | 'onCancelWatchlistPicker'
  | 'onConfirmWatchlistPicker'
  | 'holdingInputOpen'
  | 'holdingFormMode'
  | 'holdingAmount'
  | 'holdingPnl'
  | 'onHoldingAmountChange'
  | 'onHoldingPnlChange'
  | 'onCancelHoldingInput'
  | 'onConfirmHoldingInput'
  | 'holdingOperationInputOpen'
  | 'holdingOperationType'
  | 'holdingOperationAmount'
  | 'holdingOperationFeeRate'
  | 'holdingOperationShares'
  | 'holdingOperationTiming'
  | 'holdingOperationTradeDate'
  | 'onHoldingOperationAmountChange'
  | 'onHoldingOperationFeeRateChange'
  | 'onHoldingOperationSharesChange'
  | 'onHoldingOperationTimingChange'
  | 'onHoldingOperationTradeDateChange'
  | 'onCancelHoldingOperation'
  | 'onConfirmHoldingOperation'
  | 'sipPlanExists'
  | 'sipInputOpen'
  | 'sipCadence'
  | 'sipWeekday'
  | 'sipMonthDay'
  | 'sipAmount'
  | 'onSipCadenceChange'
  | 'onSipWeekdayChange'
  | 'onSipMonthDayChange'
  | 'onSipAmountChange'
  | 'onCancelSipInput'
  | 'onConfirmSipInput'
  | 'onOpenSipPlan'
  | 'onJumpToHoldings'
>

export function FundDetailActionBar(props: Props) {
  const {
    selectedFund,
    onBack,
    onQuickAction,
    watchlistPickerOpen,
    watchlistPickerGroups,
    watchlistGroupOptions,
    onToggleWatchlistGroup,
    onCancelWatchlistPicker,
    onConfirmWatchlistPicker,
    holdingInputOpen,
    holdingFormMode,
    holdingAmount,
    holdingPnl,
    onHoldingAmountChange,
    onHoldingPnlChange,
    onCancelHoldingInput,
    onConfirmHoldingInput,
    holdingOperationInputOpen,
    holdingOperationType,
    holdingOperationAmount,
    holdingOperationFeeRate,
    holdingOperationShares,
    holdingOperationTiming,
    holdingOperationTradeDate,
    onHoldingOperationAmountChange,
    onHoldingOperationFeeRateChange,
    onHoldingOperationSharesChange,
    onHoldingOperationTimingChange,
    onHoldingOperationTradeDateChange,
    onCancelHoldingOperation,
    onConfirmHoldingOperation,
    sipPlanExists,
    sipInputOpen,
    sipCadence,
    sipWeekday,
    sipMonthDay,
    sipAmount,
    onSipCadenceChange,
    onSipWeekdayChange,
    onSipMonthDayChange,
    onSipAmountChange,
    onCancelSipInput,
    onConfirmSipInput,
    onOpenSipPlan,
    onJumpToHoldings,
  } = props

  return (
    <div className="flex flex-wrap gap-2">
      <button
        onClick={onBack}
        className="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-[color:var(--fc-color-accent)]/55 bg-[color:var(--fc-color-accent)] text-slate-950 shadow-[0_12px_30px_rgba(243,186,47,0.24)] transition hover:-translate-y-0.5 hover:shadow-[0_16px_36px_rgba(243,186,47,0.32)]"
        aria-label="返回上一页"
      >
        <ArrowLeft className="h-5 w-5" />
      </button>
      <div className="relative">
        <button
          type="button"
          disabled={selectedFund.watchlisted}
          onClick={() => onQuickAction('watchlist')}
          className={`inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm transition ${
            selectedFund.watchlisted
              ? 'cursor-not-allowed border-[color:var(--fc-color-accent)]/20 bg-[color:var(--fc-color-accent)]/10 text-[color:var(--fc-color-accent)]/65'
              : 'border-white/10 bg-white/5 text-slate-200 hover:border-[color:var(--fc-color-accent)]/50 hover:text-white'
          }`}
        >
          <Radar className="h-4 w-4" />
          {selectedFund.watchlisted ? '已自选' : '加自选'}
        </button>
        {watchlistPickerOpen ? (
          <WatchlistPicker
            fundName={selectedFund.name}
            groups={watchlistPickerGroups}
            options={watchlistGroupOptions}
            onToggle={onToggleWatchlistGroup}
            onCancel={onCancelWatchlistPicker}
            onConfirm={onConfirmWatchlistPicker}
          />
        ) : null}
      </div>
      <div className="relative">
        {selectedFund.held ? (
          <button
            type="button"
            onClick={onJumpToHoldings}
            className="inline-flex items-center gap-2 rounded-full border border-emerald-300/30 bg-emerald-400/12 px-4 py-2 text-sm text-emerald-100 transition hover:border-emerald-200/55 hover:bg-emerald-400/18"
          >
            <BriefcaseBusiness className="h-4 w-4" />
            已持仓
          </button>
        ) : (
          <QuickActionButton icon={BriefcaseBusiness} label="加持仓" onClick={() => onQuickAction('holding')} />
        )}
        {holdingInputOpen ? (
          <HoldingInputPanel
            fundName={selectedFund.name}
            mode={holdingFormMode}
            amount={holdingAmount}
            pnl={holdingPnl}
            onAmountChange={onHoldingAmountChange}
            onPnlChange={onHoldingPnlChange}
            onCancel={onCancelHoldingInput}
            onConfirm={onConfirmHoldingInput}
          />
        ) : null}
      </div>
      <button
        type="button"
        disabled={!selectedFund.held}
        onClick={() => onQuickAction('editHolding')}
        className={`inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm transition ${
          selectedFund.held
            ? 'border-white/10 bg-white/5 text-slate-200 hover:border-[color:var(--fc-color-accent)]/50 hover:text-white'
            : 'cursor-not-allowed border-white/8 bg-white/5 text-slate-500'
        }`}
      >
        <WalletCards className="h-4 w-4" />
        修改持仓
      </button>
      <div className="relative">
        <QuickActionButton icon={Plus} label="买入" onClick={() => onQuickAction('buy')} />
        {holdingOperationInputOpen && holdingOperationType === 'BUY' ? (
          <HoldingOperationPanel
            fundName={selectedFund.name}
            operation={holdingOperationType}
            tradeDate={holdingOperationTradeDate}
            amount={holdingOperationAmount}
            feeRate={holdingOperationFeeRate}
            shares={holdingOperationShares}
            timing={holdingOperationTiming}
            onTradeDateChange={onHoldingOperationTradeDateChange}
            onAmountChange={onHoldingOperationAmountChange}
            onFeeRateChange={onHoldingOperationFeeRateChange}
            onSharesChange={onHoldingOperationSharesChange}
            onTimingChange={onHoldingOperationTimingChange}
            onCancel={onCancelHoldingOperation}
            onConfirm={onConfirmHoldingOperation}
          />
        ) : null}
      </div>
      <div className="relative">
        <button
          type="button"
          disabled={!selectedFund.held}
          onClick={() => onQuickAction('sell')}
          className={`inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm transition ${
            selectedFund.held
              ? 'border-white/10 bg-white/5 text-slate-200 hover:border-[color:var(--fc-color-accent)]/50 hover:text-white'
              : 'cursor-not-allowed border-white/8 bg-white/5 text-slate-500'
          }`}
        >
          <TrendingDown className="h-4 w-4" />
          卖出
        </button>
        {holdingOperationInputOpen && holdingOperationType === 'SELL' ? (
          <HoldingOperationPanel
            fundName={selectedFund.name}
            operation={holdingOperationType}
            tradeDate={holdingOperationTradeDate}
            amount={holdingOperationAmount}
            feeRate={holdingOperationFeeRate}
            shares={holdingOperationShares}
            timing={holdingOperationTiming}
            onTradeDateChange={onHoldingOperationTradeDateChange}
            onAmountChange={onHoldingOperationAmountChange}
            onFeeRateChange={onHoldingOperationFeeRateChange}
            onSharesChange={onHoldingOperationSharesChange}
            onTimingChange={onHoldingOperationTimingChange}
            onCancel={onCancelHoldingOperation}
            onConfirm={onConfirmHoldingOperation}
          />
        ) : null}
      </div>
      <div className="relative">
        <button
          type="button"
          onClick={() => (sipPlanExists ? onOpenSipPlan() : onQuickAction('sip'))}
          className={`inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm transition ${
            sipPlanExists
              ? 'border-[color:var(--fc-color-accent)]/30 bg-[color:var(--fc-color-accent)]/12 text-[color:var(--fc-color-accent)] hover:border-[color:var(--fc-color-accent)]/55 hover:bg-[color:var(--fc-color-accent)]/18'
              : 'border-white/10 bg-white/5 text-slate-200 hover:border-[color:var(--fc-color-accent)]/50 hover:text-white'
          }`}
        >
          <BellRing className="h-4 w-4" />
          {sipPlanExists ? '已设定投' : '设定投'}
        </button>
        {sipInputOpen ? (
          <SipInputPanel
            fundName={selectedFund.name}
            cadence={sipCadence}
            weekday={sipWeekday}
            monthDay={sipMonthDay}
            amount={sipAmount}
            onCadenceChange={onSipCadenceChange}
            onWeekdayChange={onSipWeekdayChange}
            onMonthDayChange={onSipMonthDayChange}
            onAmountChange={onSipAmountChange}
            onCancel={onCancelSipInput}
            onConfirm={onConfirmSipInput}
          />
        ) : null}
      </div>
    </div>
  )
}

function WatchlistPicker({
  fundName,
  groups,
  options,
  onToggle,
  onCancel,
  onConfirm,
}: {
  fundName: string
  groups: WatchlistGroup[]
  options: WatchlistGroup[]
  onToggle: (group: WatchlistGroup) => void
  onCancel: () => void
  onConfirm: () => void
}) {
  return (
    <div className="absolute left-0 top-[calc(100%+0.6rem)] z-30 w-72 rounded-[1.25rem] border border-sky-400/20 bg-slate-950/98 p-3 shadow-[0_18px_40px_rgba(15,23,42,0.45)] backdrop-blur-xl">
      <p className="text-xs uppercase tracking-[0.2em] text-sky-300">加入自选分组</p>
      <p className="mt-1 text-sm text-white">{fundName}</p>
      <p className="mt-1 text-xs text-slate-400">默认勾选“全部”，最多只能勾选 2 个。</p>
      <div className="mt-3 space-y-2">
        {options.map((group) => {
          const checked = groups.includes(group)
          const disabled = group !== '全部' && !checked && groups.length >= 2
          return (
            <button
              key={group}
              type="button"
              disabled={disabled}
              onClick={() => onToggle(group)}
              className={`flex w-full items-center justify-between rounded-xl border px-3 py-2 text-sm transition ${
                checked
                  ? 'border-sky-300/45 bg-sky-400/14 text-white'
                  : disabled
                    ? 'cursor-not-allowed border-white/8 bg-white/5 text-slate-500'
                    : 'border-white/10 bg-white/5 text-slate-200 hover:border-white/20 hover:text-white'
              }`}
            >
              <span>{group}</span>
              <span className={`inline-flex h-4 w-4 items-center justify-center rounded border text-[0.72rem] ${checked ? 'border-sky-200/70 bg-sky-300/20 text-white' : 'border-white/20 text-transparent'}`}>
                ✓
              </span>
            </button>
          )
        })}
      </div>
      <div className="mt-3 flex justify-end gap-2">
        <button type="button" onClick={onCancel} className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-slate-300 transition hover:border-white/20 hover:text-white">
          取消
        </button>
        <button type="button" onClick={onConfirm} className="rounded-full border border-sky-300/40 bg-sky-400/16 px-3 py-1.5 text-xs text-sky-100 transition hover:border-sky-200/60 hover:bg-sky-400/22">
          确认
        </button>
      </div>
    </div>
  )
}

function HoldingInputPanel({
  fundName,
  mode,
  amount,
  pnl,
  onAmountChange,
  onPnlChange,
  onCancel,
  onConfirm,
}: {
  fundName: string
  mode: 'add' | 'edit'
  amount: string
  pnl: string
  onAmountChange: (value: string) => void
  onPnlChange: (value: string) => void
  onCancel: () => void
  onConfirm: () => void
}) {
  return (
    <div className="absolute left-0 top-[calc(100%+0.6rem)] z-30 w-80 rounded-[1.25rem] border border-emerald-400/20 bg-slate-950/98 p-3 shadow-[0_18px_40px_rgba(15,23,42,0.45)] backdrop-blur-xl">
      <p className="text-xs uppercase tracking-[0.2em] text-emerald-300">{mode === 'edit' ? '修改持仓' : '加入持仓'}</p>
      <p className="mt-1 text-sm text-white">{fundName}</p>
      <div className="mt-3 space-y-3">
        <label className="block">
          <span className="text-xs text-slate-400">持有金额</span>
          <input type="number" min="0" step="0.01" value={amount} onChange={(event) => onAmountChange(event.target.value)} placeholder="例如 5000" className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none placeholder:text-slate-500 focus:border-emerald-300/40" />
        </label>
        <label className="block">
          <span className="text-xs text-slate-400">持有收益</span>
          <input type="number" step="0.01" value={pnl} onChange={(event) => onPnlChange(event.target.value)} placeholder="例如 320 或 -120" className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none placeholder:text-slate-500 focus:border-emerald-300/40" />
        </label>
      </div>
      <div className="mt-3 flex justify-end gap-2">
        <button type="button" onClick={onCancel} className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-slate-300 transition hover:border-white/20 hover:text-white">
          取消
        </button>
        <button type="button" onClick={onConfirm} className="rounded-full border border-emerald-300/40 bg-emerald-400/16 px-3 py-1.5 text-xs text-emerald-100 transition hover:border-emerald-200/60 hover:bg-emerald-400/22">
          {mode === 'edit' ? '确认修改' : '加入持仓'}
        </button>
      </div>
    </div>
  )
}

function SipInputPanel({
  fundName,
  cadence,
  weekday,
  monthDay,
  amount,
  onCadenceChange,
  onWeekdayChange,
  onMonthDayChange,
  onAmountChange,
  onCancel,
  onConfirm,
}: {
  fundName: string
  cadence: 'DAILY' | 'WEEKLY' | 'MONTHLY'
  weekday: '1' | '2' | '3' | '4' | '5' | '6' | '0'
  monthDay: string
  amount: string
  onCadenceChange: (value: 'DAILY' | 'WEEKLY' | 'MONTHLY') => void
  onWeekdayChange: (value: '1' | '2' | '3' | '4' | '5' | '6' | '0') => void
  onMonthDayChange: (value: string) => void
  onAmountChange: (value: string) => void
  onCancel: () => void
  onConfirm: () => void
}) {
  return (
    <div className="absolute left-0 top-[calc(100%+0.6rem)] z-30 w-80 rounded-[1.25rem] border border-[color:var(--fc-color-accent)]/20 bg-slate-950/98 p-3 shadow-[0_18px_40px_rgba(15,23,42,0.45)] backdrop-blur-xl">
      <p className="text-xs uppercase tracking-[0.2em] text-[color:var(--fc-color-accent)]/80">创建定投计划</p>
      <p className="mt-1 text-sm text-white">{fundName}</p>
      <div className="mt-3 space-y-3">
        <label className="block">
          <span className="text-xs text-slate-400">定投周期</span>
          <select value={cadence} onChange={(event) => onCadenceChange(event.target.value as 'DAILY' | 'WEEKLY' | 'MONTHLY')} className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none focus:border-[color:var(--fc-color-accent)]/40">
            <option value="DAILY">每日</option>
            <option value="WEEKLY">每周</option>
            <option value="MONTHLY">每月</option>
          </select>
        </label>
        {cadence === 'WEEKLY' ? (
          <label className="block">
            <span className="text-xs text-slate-400">定投时间</span>
            <select value={weekday} onChange={(event) => onWeekdayChange(event.target.value as '1' | '2' | '3' | '4' | '5' | '6' | '0')} className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none focus:border-[color:var(--fc-color-accent)]/40">
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
          <input type="number" min="0" step="0.01" value={amount} onChange={(event) => onAmountChange(event.target.value)} placeholder="例如 500" className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none placeholder:text-slate-500 focus:border-[color:var(--fc-color-accent)]/40" />
        </label>
      </div>
      <div className="mt-3 flex justify-end gap-2">
        <button type="button" onClick={onCancel} className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-slate-300 transition hover:border-white/20 hover:text-white">
          取消
        </button>
        <button type="button" onClick={onConfirm} className="rounded-full border border-[color:var(--fc-color-accent)]/35 bg-[color:var(--fc-color-accent)]/14 px-3 py-1.5 text-xs text-[color:var(--fc-color-accent)] transition hover:border-[color:var(--fc-color-accent)]/55 hover:bg-[color:var(--fc-color-accent)]/20">
          确认设定
        </button>
      </div>
    </div>
  )
}
