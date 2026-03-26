/** 自选基金页，按分组展示自选列表并提供移除、调整分组等能力。 */
import type { FundCard, WatchlistItem } from '@fundcat/contracts'
import { useEffect, useMemo, useRef, useState } from 'react'
import { SectionCard } from '../../../common/components/SectionCard'
import { formatCompactPercent } from '../../../common/utils/fundInsights'

type WatchlistRow = {
  code: string
  name: string
  dayGrowth: number
  held: boolean
  group: string
  assignedGroup: string
}

const groupOrder = ['全部', '成长进攻', '稳健配置', '行业主题'] as const
type WatchlistGroup = (typeof groupOrder)[number]

export function PortfolioPage({
  watchlist,
  funds,
  groupSelections,
  onAssignGroup,
  onOpenFund,
  onRemoveFund,
}: {
  watchlist: WatchlistItem[]
  funds: FundCard[]
  groupSelections: Record<string, WatchlistGroup[]>
  onAssignGroup: (codes: string[], group: Exclude<WatchlistGroup, '全部'>) => void
  onOpenFund: (code: string) => void
  onRemoveFund: (code: string) => Promise<void> | void
}) {
  const [activeGroup, setActiveGroup] = useState<(typeof groupOrder)[number]>('全部')
  const [movingFundCode, setMovingFundCode] = useState<string | null>(null)
  const [confirmingDeleteCode, setConfirmingDeleteCode] = useState<string | null>(null)
  const [selectedCodes, setSelectedCodes] = useState<string[]>([])
  const [batchMovingOpen, setBatchMovingOpen] = useState(false)
  const [batchDeleteOpen, setBatchDeleteOpen] = useState(false)
  const movingMenuRef = useRef<HTMLDivElement | null>(null)
  const deleteConfirmRef = useRef<HTMLDivElement | null>(null)
  const batchMoveMenuRef = useRef<HTMLDivElement | null>(null)
  const batchDeleteRef = useRef<HTMLDivElement | null>(null)

  const rows = useMemo<WatchlistRow[]>(() => {
    const deduped = new Map<string, WatchlistRow>()

    watchlist.forEach((item) => {
      if (deduped.has(item.code)) return

      const fund = funds.find((candidate) => candidate.code === item.code)
      const assignedGroup = resolveAssignedGroup(item.code, groupSelections[item.code])
      deduped.set(item.code, {
        code: item.code,
        name: item.name,
        dayGrowth: fund?.estimatedGrowth ?? item.estimatedGrowth,
        held: fund?.held ?? false,
        group: assignedGroup,
        assignedGroup,
      })
    })

    return Array.from(deduped.values())
  }, [funds, groupSelections, watchlist])

  const visibleRows = useMemo(
    () => (activeGroup === '全部' ? rows : rows.filter((row) => row.group === activeGroup)),
    [activeGroup, rows],
  )

  const showSelectionColumn = true
  const showAssignedGroupColumn = activeGroup === '全部'
  const tableColumns = showAssignedGroupColumn
    ? '52px minmax(260px,2.2fr) 120px 120px 140px 84px'
    : '52px minmax(260px,2.2fr) 120px 140px 84px'
  const selectedCount = selectedCodes.length
  const allVisibleSelected = visibleRows.length > 0 && selectedCodes.length === visibleRows.length

  useEffect(() => {
    if (!movingFundCode && !confirmingDeleteCode && !batchMovingOpen && !batchDeleteOpen) return

    function handlePointerDown(event: MouseEvent) {
      if (!movingMenuRef.current?.contains(event.target as Node)) {
        setMovingFundCode(null)
      }
      if (!deleteConfirmRef.current?.contains(event.target as Node)) {
        setConfirmingDeleteCode(null)
      }
      if (!batchMoveMenuRef.current?.contains(event.target as Node)) {
        setBatchMovingOpen(false)
      }
      if (!batchDeleteRef.current?.contains(event.target as Node)) {
        setBatchDeleteOpen(false)
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setMovingFundCode(null)
        setConfirmingDeleteCode(null)
        setBatchMovingOpen(false)
        setBatchDeleteOpen(false)
      }
    }

    document.addEventListener('mousedown', handlePointerDown)
    document.addEventListener('keydown', handleEscape)
    return () => {
      document.removeEventListener('mousedown', handlePointerDown)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [batchDeleteOpen, batchMovingOpen, confirmingDeleteCode, movingFundCode])

  useEffect(() => {
    if (!showSelectionColumn) {
      setSelectedCodes([])
      setBatchMovingOpen(false)
      setBatchDeleteOpen(false)
      return
    }

    const visibleCodeSet = new Set(visibleRows.map((row) => row.code))
    setSelectedCodes((current) => current.filter((code) => visibleCodeSet.has(code)))
  }, [showSelectionColumn, visibleRows])

  function toggleSelectedCode(code: string) {
    setSelectedCodes((current) => (current.includes(code) ? current.filter((item) => item !== code) : [...current, code]))
  }

  function toggleSelectAll() {
    if (allVisibleSelected) {
      setSelectedCodes([])
      return
    }
    setSelectedCodes(visibleRows.map((row) => row.code))
  }

  function handleBatchMove(targetGroup: string) {
    onAssignGroup(selectedCodes, targetGroup as Exclude<WatchlistGroup, '全部'>)
    setBatchMovingOpen(false)
    setSelectedCodes([])
  }

  async function handleBatchDelete() {
    const codes = [...selectedCodes]
    setBatchDeleteOpen(false)
    setSelectedCodes([])
    for (const code of codes) {
      await onRemoveFund(code)
    }
  }

  return (
    <SectionCard title="自选基金" eyebrow="Watchlist page">
      <div className="grid gap-5 xl:grid-cols-[220px_minmax(0,1fr)]">
        <div className="rounded-[1.5rem] border border-white/8 bg-white/5 p-3">
          <div className="mb-3 px-2 text-[0.72rem] uppercase tracking-[0.28em] text-[color:var(--fc-color-accent)]/80">
            Watch groups
          </div>
          <div className="space-y-2">
            {groupOrder.map((group) => {
              const active = activeGroup === group
              const count = group === '全部' ? rows.length : rows.filter((row) => row.group === group).length
              return (
                <button
                  key={group}
                  onClick={() => setActiveGroup(group)}
                  className={`group relative flex w-full items-center justify-between overflow-visible rounded-2xl border px-4 py-3 text-left text-sm transition ${
                    active
                      ? 'border-[color:var(--fc-color-accent)]/45 bg-[color:var(--fc-color-accent)]/10 text-[color:var(--fc-color-accent)] shadow-[0_0_0_1px_rgba(255,193,59,0.08)]'
                      : 'border-white/8 bg-white/5 text-slate-300 hover:border-white/15 hover:bg-white/[0.07] hover:text-white'
                  }`}
                >
                  <span className="truncate">{group}</span>
                  <span className={`text-[0.72rem] ${active ? 'text-[color:var(--fc-color-accent)]/70' : 'text-slate-500'}`}>
                    {count}
                  </span>
                  {active ? (
                    <span
                      aria-hidden="true"
                      className="absolute -right-5 top-1/2 h-0 w-0 -translate-y-1/2 border-y-[16px] border-l-[20px] border-y-transparent border-l-[color:var(--fc-color-accent)]/18 drop-shadow-[8px_0_14px_rgba(255,193,59,0.08)]"
                    />
                  ) : null}
                </button>
              )
            })}
          </div>
        </div>

        <div>
          <div className="mb-4 flex items-start justify-between gap-4">
            <div className="text-xs leading-6 text-slate-500">
              <p>点击任意行进入基金详情</p>
              <p>展示当前分组下的自选基金列表</p>
            </div>
            <div className="flex items-center gap-2">
              {showSelectionColumn && selectedCount > 0 ? (
                <>
                  <div ref={batchMovingOpen ? batchMoveMenuRef : null} className="relative">
                    <button
                      type="button"
                      onClick={() => {
                        setBatchMovingOpen((current) => !current)
                        setBatchDeleteOpen(false)
                      }}
                      className="rounded-full border border-sky-400/30 bg-sky-500/12 px-4 py-2 text-xs text-sky-200 transition hover:border-sky-300/55 hover:bg-sky-500/18"
                    >
                      加入分组
                    </button>
                    {batchMovingOpen ? (
                      <div className="absolute right-0 top-full z-20 mt-2 w-36 overflow-hidden rounded-2xl border border-white/10 bg-slate-950/95 p-1 shadow-[0_18px_40px_rgba(15,23,42,0.45)] backdrop-blur-xl">
                        {groupOrder
                          .filter((group) => group !== '全部')
                          .map((group) => (
                            <button
                              key={group}
                              type="button"
                              onClick={() => handleBatchMove(group)}
                              className="flex w-full items-center justify-between rounded-xl px-3 py-2 text-sm text-slate-300 transition hover:bg-white/6 hover:text-white"
                            >
                              <span>{group}</span>
                            </button>
                          ))}
                      </div>
                    ) : null}
                  </div>
                  <div ref={batchDeleteOpen ? batchDeleteRef : null} className="relative">
                    <button
                      type="button"
                      onClick={() => {
                        setBatchDeleteOpen((current) => !current)
                        setBatchMovingOpen(false)
                      }}
                      className="rounded-full border border-red-400/30 bg-red-500/12 px-4 py-2 text-xs text-red-200 transition hover:border-red-300/55 hover:bg-red-500/18"
                    >
                      批量删除
                    </button>
                    {batchDeleteOpen ? (
                      <div className="absolute right-0 top-full z-20 mt-2 w-64 rounded-2xl border border-red-400/20 bg-slate-950/96 p-3 text-left shadow-[0_18px_40px_rgba(15,23,42,0.45)] backdrop-blur-xl">
                        <p className="text-sm text-white">移出已勾选基金？</p>
                        <p className="mt-1 text-xs leading-5 text-slate-400">当前共选择 {selectedCount} 只基金。</p>
                        <div className="mt-3 flex justify-end gap-2">
                          <button
                            type="button"
                            onClick={() => setBatchDeleteOpen(false)}
                            className="rounded-lg border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-slate-300 transition hover:border-white/20 hover:text-white"
                          >
                            取消
                          </button>
                          <button
                            type="button"
                            onClick={() => void handleBatchDelete()}
                            className="rounded-lg border border-red-400/35 bg-red-500/16 px-3 py-1.5 text-xs text-red-200 transition hover:border-red-300/55 hover:bg-red-500/22 hover:text-red-100"
                          >
                            确认删除
                          </button>
                        </div>
                      </div>
                    ) : null}
                  </div>
                </>
              ) : null}
              <div className="rounded-full border border-white/8 bg-white/5 px-4 py-2 text-xs text-slate-400">
                当前分组：<span className="text-white">{activeGroup}</span>
              </div>
            </div>
          </div>

          <div className="relative rounded-[1.75rem] border border-white/8 bg-white/5">
            <div className="rounded-[1.75rem]">
              <div className="grid gap-3 border-b border-white/8 px-4 py-3 text-xs text-slate-400" style={{ gridTemplateColumns: tableColumns }}>
                {showSelectionColumn ? (
                  <div className="flex items-center justify-center">
                    <button
                      type="button"
                      aria-label="全选自选基金"
                      onClick={toggleSelectAll}
                      className={`inline-flex h-5 w-5 items-center justify-center rounded border text-[0.72rem] transition ${
                        allVisibleSelected
                          ? 'border-[color:var(--fc-color-accent)]/55 bg-[color:var(--fc-color-accent)]/15 text-[color:var(--fc-color-accent)]'
                          : 'border-white/15 bg-white/5 text-slate-400 hover:border-white/25 hover:text-white'
                      }`}
                    >
                      {allVisibleSelected ? '✓' : ''}
                    </button>
                  </div>
                ) : null}
                <div className="inline-flex min-w-0 rounded-xl py-1.5 pr-2 text-left text-[0.88rem] text-slate-400">
                  <span className="block w-full truncate">基金名称 + 代码</span>
                </div>
                <div className="inline-flex min-w-0 justify-center rounded-xl py-1.5 text-[0.88rem] text-slate-400">
                  <span className="block truncate text-center">当日涨幅</span>
                </div>
                {showAssignedGroupColumn ? (
                  <div className="inline-flex min-w-0 justify-center rounded-xl py-1.5 text-[0.88rem] text-slate-400">
                    <span className="block truncate text-center">所属分组</span>
                  </div>
                ) : null}
                <div className="inline-flex min-w-0 justify-center rounded-xl py-1.5 text-[0.88rem] text-slate-400">
                  <span className="block truncate text-center">移动分组</span>
                </div>
                <div className="inline-flex min-w-0 justify-center rounded-xl py-1.5 text-[0.88rem] text-slate-400">
                  <span className="block truncate text-center">删除</span>
                </div>
              </div>

              <div className="divide-y divide-white/8">
                {visibleRows.map((row) => (
                  <div
                    key={row.code}
                    onClick={() => onOpenFund(row.code)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault()
                        onOpenFund(row.code)
                      }
                    }}
                    role="button"
                    tabIndex={0}
                    className="grid w-full cursor-pointer gap-3 px-4 py-4 text-left transition hover:bg-white/5 focus:outline-none focus-visible:bg-white/5"
                    style={{ gridTemplateColumns: tableColumns }}
                  >
                    {showSelectionColumn ? (
                      <div className="flex items-center justify-center">
                        <button
                          type="button"
                          aria-label={`勾选 ${row.name}`}
                          onClick={(event) => {
                            event.stopPropagation()
                            toggleSelectedCode(row.code)
                          }}
                          className={`inline-flex h-5 w-5 items-center justify-center rounded border text-[0.72rem] transition ${
                            selectedCodes.includes(row.code)
                              ? 'border-[color:var(--fc-color-accent)]/55 bg-[color:var(--fc-color-accent)]/15 text-[color:var(--fc-color-accent)]'
                              : 'border-white/15 bg-white/5 text-slate-400 hover:border-white/25 hover:text-white'
                          }`}
                        >
                          {selectedCodes.includes(row.code) ? '✓' : ''}
                        </button>
                      </div>
                    ) : null}
                    <div className="min-w-0">
                      <p className="truncate font-[var(--fc-font-display)] text-[0.92rem] text-white xl:text-[1rem]">{row.name}</p>
                      <p className="mt-1 flex items-center gap-2 text-[0.8rem] text-slate-500">
                        <span>{row.code}</span>
                        {row.held ? (
                          <span className="rounded-full border border-emerald-400/25 bg-emerald-400/10 px-2 py-0.5 text-[0.68rem] text-emerald-200">
                            已持仓
                          </span>
                        ) : null}
                      </p>
                    </div>
                    <div className="flex items-center justify-center">
                      <p
                        className={`truncate text-center font-[var(--fc-font-display)] text-[1.06rem] xl:text-[1.12rem] ${
                          row.dayGrowth >= 0 ? 'text-emerald-300' : 'text-orange-300'
                        }`}
                      >
                        {formatCompactPercent(row.dayGrowth)}
                      </p>
                    </div>
                    {showAssignedGroupColumn ? (
                      <div className="flex items-center justify-center">
                        {row.assignedGroup && row.assignedGroup !== '全部' ? (
                          <button
                            type="button"
                            onClick={(event) => {
                              event.stopPropagation()
                              setActiveGroup(row.assignedGroup as (typeof groupOrder)[number])
                            }}
                            className="truncate rounded-full border border-white/8 bg-white/5 px-3 py-1 text-xs text-slate-300 transition hover:border-[color:var(--fc-color-accent)]/35 hover:bg-[color:var(--fc-color-accent)]/8 hover:text-[color:var(--fc-color-accent)]"
                          >
                            {row.assignedGroup}
                          </button>
                        ) : (
                          <span className="block h-6 w-full" />
                        )}
                      </div>
                    ) : null}
                    <div ref={movingFundCode === row.code ? movingMenuRef : null} className="relative flex items-center justify-center">
                      <button
                        type="button"
                        aria-label={`移动 ${row.name} 的分组`}
                        onClick={(event) => {
                          event.stopPropagation()
                          setMovingFundCode((current) => (current === row.code ? null : row.code))
                        }}
                        className="inline-flex h-9 min-w-9 items-center justify-center rounded-lg border border-sky-400/35 bg-sky-500/16 px-3 text-sm font-semibold text-sky-200 transition hover:border-sky-300/65 hover:bg-sky-500/26 hover:text-sky-100"
                      >
                        &gt;
                      </button>
                      {movingFundCode === row.code ? (
                        <div
                          className="absolute left-1/2 top-full z-20 mt-2 w-36 overflow-hidden rounded-2xl border border-white/10 bg-slate-950/95 p-1 shadow-[0_18px_40px_rgba(15,23,42,0.45)] backdrop-blur-xl"
                          onClick={(event) => event.stopPropagation()}
                        >
                          {groupOrder
                            .filter((group) => group !== '全部')
                            .map((group) => (
                              <button
                                key={group}
                                type="button"
                                onClick={() => {
                                  onAssignGroup([row.code], group as Exclude<WatchlistGroup, '全部'>)
                                  setMovingFundCode(null)
                                }}
                                className={`flex w-full items-center justify-between rounded-xl px-3 py-2 text-sm transition ${
                                  row.group === group
                                    ? 'bg-[color:var(--fc-color-accent)]/10 text-[color:var(--fc-color-accent)]'
                                    : 'text-slate-300 hover:bg-white/6 hover:text-white'
                                }`}
                              >
                                <span>{group}</span>
                                {row.group === group ? <span className="text-[0.72rem]">当前</span> : null}
                              </button>
                            ))}
                        </div>
                      ) : null}
                    </div>
                    <div className="flex items-center justify-center">
                      <div ref={confirmingDeleteCode === row.code ? deleteConfirmRef : null} className="relative flex items-center justify-center">
                        <button
                          type="button"
                          aria-label={`删除 ${row.name}`}
                          title={`移出自选：${row.name}`}
                          onClick={(event) => {
                            event.stopPropagation()
                            setConfirmingDeleteCode((current) => (current === row.code ? null : row.code))
                          }}
                          className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-red-400/40 bg-red-500/14 text-[0.92rem] font-semibold text-red-200 transition hover:border-red-300/65 hover:bg-red-500/22 hover:text-red-100"
                        >
                          ×
                        </button>
                        {confirmingDeleteCode === row.code ? (
                          <div
                            className="absolute right-10 top-1/2 z-20 w-56 -translate-y-1/2 rounded-2xl border border-red-400/20 bg-slate-950/96 p-3 text-left shadow-[0_18px_40px_rgba(15,23,42,0.45)] backdrop-blur-xl"
                            onClick={(event) => event.stopPropagation()}
                          >
                            <p className="text-sm text-white">移出自选基金？</p>
                            <p className="mt-1 text-xs leading-5 text-slate-400">{row.name}</p>
                            <div className="mt-3 flex justify-end gap-2">
                              <button
                                type="button"
                                onClick={() => setConfirmingDeleteCode(null)}
                                className="rounded-lg border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-slate-300 transition hover:border-white/20 hover:text-white"
                              >
                                取消
                              </button>
                              <button
                                type="button"
                                onClick={() => {
                                  setConfirmingDeleteCode(null)
                                  void onRemoveFund(row.code)
                                }}
                                className="rounded-lg border border-red-400/35 bg-red-500/16 px-3 py-1.5 text-xs text-red-200 transition hover:border-red-300/55 hover:bg-red-500/22 hover:text-red-100"
                              >
                                确认删除
                              </button>
                            </div>
                          </div>
                        ) : null}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </SectionCard>
  )
}

function resolveWatchlistGroup(code: string) {
  switch (code) {
    case '000001':
    case '519674':
      return '成长进攻'
    case '005827':
      return '稳健配置'
    case '161725':
      return '行业主题'
    case '012348':
    case '008552':
      return '成长进攻'
    case '002190':
    case '006113':
      return '稳健配置'
    case '001632':
    case '161039':
      return '行业主题'
    default:
      return ''
  }
}

function resolveAssignedGroup(code: string, selections?: WatchlistGroup[]) {
  const selectedSpecificGroup = selections?.find((group) => group !== '全部')
  if (selectedSpecificGroup) {
    return selectedSpecificGroup
  }
  const resolved = resolveWatchlistGroup(code)
  return resolved || '全部'
}
