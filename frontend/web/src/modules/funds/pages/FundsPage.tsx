/** 基金中心页，负责基金搜索、候选列表和快捷加入自选交互。 */
import type { FundCard } from '@fundcat/contracts'
import { Search } from 'lucide-react'
import { SectionCard } from '../../../common/components/SectionCard'
import { formatSignedPercent } from '../../../common/components/WebUi'

type WatchlistGroup = '全部' | '成长进攻' | '稳健配置' | '行业主题'

export function FundsPage({
  search,
  funds,
  suggestions,
  onSearchChange,
  onAddToWatchlist,
  watchlistPickerFundCode,
  watchlistPickerGroups,
  watchlistGroupOptions,
  onToggleWatchlistGroup,
  onCancelWatchlistPicker,
  onConfirmWatchlistPicker,
  onSelectFund,
}: {
  search: string
  funds: FundCard[]
  suggestions: FundCard[]
  onSearchChange: (value: string) => void
  onAddToWatchlist: (fund: FundCard) => void
  watchlistPickerFundCode: string | null
  watchlistPickerGroups: WatchlistGroup[]
  watchlistGroupOptions: WatchlistGroup[]
  onToggleWatchlistGroup: (group: WatchlistGroup) => void
  onCancelWatchlistPicker: () => void
  onConfirmWatchlistPicker: () => void
  onSelectFund: (code: string) => void
}) {
  const hasKeyword = search.trim().length > 0

  return (
    <SectionCard
      title="基金雷达"
      eyebrow="Fund center"
      action={
        <div className="relative">
          <label className="flex items-center gap-2 rounded-full border border-white/10 bg-slate-950/50 px-4 py-2 text-sm text-slate-300">
            <Search className="h-4 w-4" />
            <input
              value={search}
              onChange={(event) => onSearchChange(event.target.value)}
              placeholder="输入基金代码 / 名称搜索"
              className="w-40 bg-transparent outline-none placeholder:text-slate-500 md:w-64"
            />
          </label>

          {hasKeyword ? (
            <div className="absolute left-0 right-0 top-[calc(100%+0.65rem)] z-20 overflow-hidden rounded-[1.25rem] border border-white/10 bg-slate-950/95 shadow-[0_24px_60px_rgba(2,6,23,0.45)] backdrop-blur-xl">
              {suggestions.length > 0 ? (
                <div className="max-h-80 overflow-y-auto py-2">
                  {suggestions.map((fund) => (
                    <div key={fund.code} className="flex items-start gap-2 px-2 py-1">
                      <div className="relative shrink-0">
                        <button
                          type="button"
                          onClick={() => onAddToWatchlist(fund)}
                          disabled={fund.watchlisted}
                          aria-label={fund.watchlisted ? `${fund.name} 已在自选基金中` : `将 ${fund.name} 加入自选基金`}
                          className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full border text-lg leading-none transition ${
                            fund.watchlisted
                              ? 'cursor-not-allowed border-white/10 bg-white/5 text-slate-600'
                              : 'border-[color:var(--fc-color-accent)]/35 bg-[color:var(--fc-color-accent)]/10 text-[color:var(--fc-color-accent)] hover:border-[color:var(--fc-color-accent)]/60 hover:bg-[color:var(--fc-color-accent)]/20'
                          }`}
                        >
                          +
                        </button>
                        {watchlistPickerFundCode === fund.code ? (
                          <div className="absolute left-[calc(100%+0.6rem)] top-0 z-30 w-72 rounded-[1.25rem] border border-sky-400/20 bg-slate-950/98 p-3 shadow-[0_18px_40px_rgba(15,23,42,0.45)] backdrop-blur-xl">
                            <p className="text-xs uppercase tracking-[0.2em] text-sky-300">加入自选分组</p>
                            <p className="mt-1 text-sm text-white">{fund.name}</p>
                            <p className="mt-1 text-xs text-slate-400">默认勾选“全部”，最多只能勾选 2 个。</p>
                            <div className="mt-3 space-y-2">
                              {watchlistGroupOptions.map((group) => {
                                const checked = watchlistPickerGroups.includes(group)
                                const disabled = group !== '全部' && !checked && watchlistPickerGroups.length >= 2
                                return (
                                  <button
                                    key={group}
                                    type="button"
                                    disabled={disabled}
                                    onClick={() => onToggleWatchlistGroup(group)}
                                    className={`flex w-full items-center justify-between rounded-xl border px-3 py-2 text-sm transition ${
                                      checked
                                        ? 'border-sky-300/45 bg-sky-400/14 text-white'
                                        : disabled
                                          ? 'cursor-not-allowed border-white/8 bg-white/5 text-slate-500'
                                          : 'border-white/10 bg-white/5 text-slate-200 hover:border-white/20 hover:text-white'
                                    }`}
                                  >
                                    <span>{group}</span>
                                    <span
                                      className={`inline-flex h-4 w-4 items-center justify-center rounded border text-[0.72rem] ${
                                        checked ? 'border-sky-200/70 bg-sky-300/20 text-white' : 'border-white/20 text-transparent'
                                      }`}
                                    >
                                      ✓
                                    </span>
                                  </button>
                                )
                              })}
                            </div>
                            <div className="mt-3 flex justify-end gap-2">
                              <button
                                type="button"
                                onClick={onCancelWatchlistPicker}
                                className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-slate-300 transition hover:border-white/20 hover:text-white"
                              >
                                取消
                              </button>
                              <button
                                type="button"
                                onClick={onConfirmWatchlistPicker}
                                className="rounded-full border border-sky-300/40 bg-sky-400/16 px-3 py-1.5 text-xs text-sky-100 transition hover:border-sky-200/60 hover:bg-sky-400/22"
                              >
                                确认
                              </button>
                            </div>
                          </div>
                        ) : null}
                      </div>
                      <button
                        type="button"
                        onClick={() => {
                          onSearchChange('')
                          onSelectFund(fund.code)
                        }}
                        className="flex min-w-0 w-full items-center justify-between gap-4 px-4 py-3 text-left transition hover:bg-white/5"
                      >
                        <div className="min-w-0 flex-1">
                          <p
                            title={fund.name}
                            className="truncate whitespace-nowrap font-medium text-white [font-size:clamp(0.8rem,1vw,1rem)]"
                          >
                            {fund.name}
                          </p>
                          <p
                            title={`${fund.code} · ${fund.category}`}
                            className="mt-1 truncate whitespace-nowrap text-slate-500 [font-size:clamp(0.65rem,0.85vw,0.75rem)]"
                          >
                            {fund.code} · {fund.category}
                          </p>
                        </div>
                        <p
                          className={`shrink-0 whitespace-nowrap [font-size:clamp(0.75rem,0.9vw,0.875rem)] ${
                            fund.estimatedGrowth >= 0 ? 'text-emerald-300' : 'text-orange-300'
                          }`}
                        >
                          {formatSignedPercent(fund.estimatedGrowth)}
                        </p>
                      </button>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="px-4 py-4 text-sm text-slate-500">未找到匹配基金</div>
              )}
            </div>
          ) : null}
        </div>
      }
    >
      <div className="mb-4 flex items-center justify-between gap-3 text-xs text-slate-500">
        <span>当前热门基金 {funds.length} 条</span>
        <span>输入后 0.5 秒自动搜索，点击候选项进入详情页</span>
      </div>
      <div className="grid gap-4 xl:grid-cols-2 2xl:grid-cols-3">
        {funds.map((fund) => {
          const isWatchlisted = fund.watchlisted
          const isHeld = fund.held

          return (
            <button
              key={fund.code}
              onClick={() => onSelectFund(fund.code)}
              className="rounded-[1.5rem] border border-white/8 bg-white/5 px-4 py-4 text-left transition hover:border-white/20"
            >
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="font-[var(--fc-font-display)] text-xl text-white">{fund.name}</p>
                  <p className="mt-2 text-sm text-slate-400">
                    {fund.code} · {fund.category}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">跟踪基准 {fund.benchmark}</p>
                </div>
                <div className="text-right">
                  <p className={`font-[var(--fc-font-display)] text-2xl ${fund.estimatedGrowth >= 0 ? 'text-emerald-300' : 'text-orange-300'}`}>
                    {formatSignedPercent(fund.estimatedGrowth)}
                  </p>
                  <p className="mt-2 text-xs text-slate-500">{fund.referenceOnly ? '盘中参考' : '前一交易日'}</p>
                </div>
              </div>

              <div className="mt-4 flex items-start justify-between gap-3">
                <div className="flex flex-1 flex-wrap gap-2">
                  {fund.tags.map((tag) => (
                    <span key={tag} className="rounded-full border border-white/10 bg-slate-950/45 px-3 py-1.5 text-xs text-slate-300">
                      {tag}
                    </span>
                  ))}
                </div>
                <div className="flex shrink-0 flex-wrap justify-end gap-2">
                  {isWatchlisted ? (
                    <span className="rounded-full border border-[color:var(--fc-color-accent)]/30 bg-[color:var(--fc-color-accent)]/10 px-3 py-1.5 text-xs text-[color:var(--fc-color-accent)]">
                      已自选
                    </span>
                  ) : null}
                  {isHeld ? (
                    <span className="rounded-full border border-emerald-300/25 bg-emerald-400/10 px-3 py-1.5 text-xs text-emerald-200">
                      已持仓
                    </span>
                  ) : null}
                </div>
              </div>
            </button>
          )
        })}
      </div>
    </SectionCard>
  )
}
