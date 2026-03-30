/** 基金详情页用户操作历史模块，展示当前基金最近的买入、卖出和定投记录。 */
import type { FundOperationHistoryItem, LocalHoldingHistoryItem } from '../../../common/appTypes'
import { formatCurrency } from '../../../common/utils/fundInsights'

export function FundOperationHistorySection({
  orders,
}: {
  orders: FundOperationHistoryItem[]
}) {
  return (
    <div className="rounded-[1.45rem] border border-white/8 bg-white/5 px-4 py-4">
      <div className="flex flex-col gap-1">
        <p className="text-sm uppercase tracking-[0.22em] text-[color:var(--fc-color-accent)]/80">Operation history</p>
        <h4 className="font-[var(--fc-font-display)] text-[1.75rem] leading-none text-white">用户操作历史</h4>
        <p className="text-sm text-slate-400">接入后台最近动作接口，按当前基金筛出买入、卖出和定投记录。</p>
      </div>

      <div className="mt-4 overflow-hidden rounded-[1.15rem] border border-white/8 bg-slate-950/45">
        <div className="grid grid-cols-[1.1fr_0.8fr_0.9fr_0.8fr] gap-3 border-b border-white/8 px-4 py-3 text-xs uppercase tracking-[0.16em] text-slate-500">
          <span>日期</span>
          <span>类型</span>
          <span className="text-right">金额 / 份额</span>
          <span className="text-right">状态</span>
        </div>
        <div className="divide-y divide-white/8">
          {orders.length > 0 ? (
            orders.map((order) => (
              <div key={order.id} className="grid grid-cols-[1.1fr_0.8fr_0.9fr_0.8fr] gap-3 px-4 py-4">
                <div>
                  <p className="text-sm text-white">{order.tradeDate}</p>
                  <p className="mt-1 text-xs text-slate-500">手续费 {formatCurrency(order.fee)}</p>
                </div>
                <div>
                  <p className={`inline-flex rounded-full border px-2.5 py-1 text-xs ${operationToneClass(order)}`}>
                    {operationLabel(order)}
                  </p>
                </div>
                <div className="text-right">
                  <p className="font-[var(--fc-font-display)] text-lg text-white">{formatCurrency(order.amount)}</p>
                  <p className="mt-1 text-xs text-slate-500">{formatShares(order.shares)}</p>
                </div>
                <div className="text-right">
                  <p className={`text-sm ${statusToneClass(order.status)}`}>{order.status}</p>
                  <p className="mt-1 text-xs text-slate-500">{order.source === 'SIP' ? '自动执行' : '手动操作'}</p>
                </div>
              </div>
            ))
          ) : (
            <div className="px-4 py-6 text-sm text-slate-400">当前基金还没有用户操作记录。</div>
          )}
        </div>
      </div>
    </div>
  )
}

function operationLabel(order: FundOperationHistoryItem) {
  if (isLocalHoldingHistory(order)) {
    return '建仓'
  }
  if (order.operation === 'SIP_BUY' || order.source === 'SIP') {
    return '定投'
  }
  return order.orderType === 'BUY' ? '买入' : '卖出'
}

function operationToneClass(order: FundOperationHistoryItem) {
  if (isLocalHoldingHistory(order)) {
    return 'border-[color:var(--fc-color-accent)]/30 bg-[color:var(--fc-color-accent)]/10 text-[color:var(--fc-color-accent)]'
  }
  if (order.operation === 'SIP_BUY' || order.source === 'SIP') {
    return 'border-sky-400/25 bg-sky-400/10 text-sky-200'
  }
  return order.orderType === 'BUY'
    ? 'border-emerald-400/25 bg-emerald-400/10 text-emerald-200'
    : 'border-orange-400/25 bg-orange-400/10 text-orange-200'
}

function statusToneClass(status: string) {
  return status === '已执行' ? 'text-emerald-300' : 'text-sky-300'
}

function formatShares(value: number) {
  return `${value.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })} 份`
}

function isLocalHoldingHistory(order: FundOperationHistoryItem): order is LocalHoldingHistoryItem {
  return 'historyType' in order
}
