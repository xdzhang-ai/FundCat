/** 自选基金页，按分组展示自选列表并提供移除、调整分组等能力。 */
import type { WatchlistItem } from '@fundcat/contracts'
import { Plus } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { SectionCard } from '../../../common/components/SectionCard'
import { formatCompactPercent } from '../../../common/utils/fundInsights'

type WatchlistRow = {
  code: string
  name: string
  dayGrowth: number
  held: boolean
  group: string
}

export function PortfolioPage({
  watchlist,
  groupOptions,
  groupSelections,
  onAssignGroup,
  onCreateGroup,
  onOpenFund,
  onRemoveFund,
}: {
  watchlist: WatchlistItem[]
  groupOptions: string[]
  groupSelections: Record<string, string>
  onAssignGroup: (codes: string[], group: string) => void
  onCreateGroup: (name: string) => Promise<unknown> | unknown
  onOpenFund: (code: string) => void
  onRemoveFund: (code: string) => Promise<void> | void
}) {
  const [activeGroup, setActiveGroup] = useState<string>('全部')
  const [movingFundCode, setMovingFundCode] = useState<string | null>(null)
  const [confirmingDeleteCode, setConfirmingDeleteCode] = useState<string | null>(null)
  const [selectedCodes, setSelectedCodes] = useState<string[]>([])
  const [batchMovingOpen, setBatchMovingOpen] = useState(false)
  const [batchDeleteOpen, setBatchDeleteOpen] = useState(false)
  const [createGroupOpen, setCreateGroupOpen] = useState(false)
  const [newGroupName, setNewGroupName] = useState('')
  const movingMenuRef = useRef<HTMLDivElement | null>(null)
  const deleteConfirmRef = useRef<HTMLDivElement | null>(null)
  const batchMoveMenuRef = useRef<HTMLDivElement | null>(null)
  const batchDeleteRef = useRef<HTMLDivElement | null>(null)
  const createGroupRef = useRef<HTMLDivElement | null>(null)

  const rows = useMemo<WatchlistRow[]>(() => {
    const deduped = new Map<string, WatchlistRow>()

    watchlist.forEach((item) => {
      if (deduped.has(item.code)) return
      const assignedGroup = groupSelections[item.code] ?? item.group ?? '全部'
      deduped.set(item.code, {
        code: item.code,
        name: item.name,
        dayGrowth: item.estimatedGrowth,
        held: item.held ?? false,
        group: assignedGroup,
      })
    })

    return Array.from(deduped.values())
  }, [groupSelections, watchlist])

  const visibleRows = useMemo(
    () => (activeGroup === '全部' ? rows : rows.filter((row) => row.group === activeGroup)),
    [activeGroup, rows],
  )

  const tableColumns = activeGroup === '全部'
    ? '52px minmax(260px,2.2fr) 120px 120px 140px 84px'
    : '52px minmax(260px,2.2fr) 120px 140px 84px'
  const selectedCount = selectedCodes.length
  const allVisibleSelected = visibleRows.length > 0 && selectedCodes.length === visibleRows.length

  useEffect(() => {
    if (!groupOptions.includes(activeGroup)) {
      setActiveGroup('全部')
    }
  }, [activeGroup, groupOptions])

  useEffect(() => {
    if (!movingFundCode && !confirmingDeleteCode && !batchMovingOpen && !batchDeleteOpen && !createGroupOpen) return

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
      if (!createGroupRef.current?.contains(event.target as Node)) {
        setCreateGroupOpen(false)
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setMovingFundCode(null)
        setConfirmingDeleteCode(null)
        setBatchMovingOpen(false)
        setBatchDeleteOpen(false)
        setCreateGroupOpen(false)
      }
    }

    document.addEventListener('mousedown', handlePointerDown)
    document.addEventListener('keydown', handleEscape)
    return () => {
      document.removeEventListener('mousedown', handlePointerDown)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [batchDeleteOpen, batchMovingOpen, confirmingDeleteCode, createGroupOpen, movingFundCode])

  useEffect(() => {
    const visibleCodeSet = new Set(visibleRows.map((row) => row.code))
    setSelectedCodes((current) => current.filter((code) => visibleCodeSet.has(code)))
  }, [visibleRows])

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
    onAssignGroup(selectedCodes, targetGroup)
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

  async function handleCreateGroup() {
    const normalizedName = newGroupName.trim()
    if (!normalizedName) {
      return
    }
    await onCreateGroup(normalizedName)
    setNewGroupName('')
    setCreateGroupOpen(false)
  }

  return (
    <SectionCard title="自选基金" eyebrow="Watchlist page">
      <div className="grid gap-5 xl:grid-cols-[220px_minmax(0,1fr)]">
        <div className="rounded-[1.5rem] border border-white/8 bg-white/5 p-3">
          <div className="mb-3 flex items-center justify-between gap-2 px-2 text-[0.72rem] uppercase tracking-[0.28em] text-[color:var(--fc-color-accent)]/80">
            <span>Watch groups</span>
            <div ref={createGroupOpen ? createGroupRef : null} className="relative">
              <button
                data-testid="watchlist-create-group-button"
                type="button"
                onClick={() => setCreateGroupOpen((current) => !current)}
                className="inline-flex h-8 w-8 items-center justify-center rounded-full border border-[color:var(--fc-color-accent)]/30 bg-[color:var(--fc-color-accent)]/10 text-[color:var(--fc-color-accent)] transition hover:border-[color:var(--fc-color-accent)]/55 hover:bg-[color:var(--fc-color-accent)]/18"
              >
                <Plus className="h-4 w-4" />
              </button>
              {createGroupOpen ? (
                <div className="absolute right-0 top-full z-20 mt-2 w-64 rounded-2xl border border-white/10 bg-slate-950/95 p-3 shadow-[0_18px_40px_rgba(15,23,42,0.45)] backdrop-blur-xl">
                  <p className="text-xs uppercase tracking-[0.2em] text-[color:var(--fc-color-accent)]">新增分组</p>
                  <input
                    data-testid="watchlist-create-group-input"
                    value={newGroupName}
                    onChange={(event) => setNewGroupName(event.target.value)}
                    placeholder="输入分组名称"
                    className="mt-3 w-full rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none placeholder:text-slate-500"
                  />
                  <div className="mt-3 flex justify-end gap-2">
                    <button
                      type="button"
                      onClick={() => setCreateGroupOpen(false)}
                      className="rounded-lg border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-slate-300 transition hover:border-white/20 hover:text-white"
                    >
                      取消
                    </button>
                    <button
                      data-testid="watchlist-create-group-confirm"
                      type="button"
                      onClick={() => void handleCreateGroup()}
                      className="rounded-lg border border-[color:var(--fc-color-accent)]/35 bg-[color:var(--fc-color-accent)]/14 px-3 py-1.5 text-xs text-[color:var(--fc-color-accent)] transition hover:border-[color:var(--fc-color-accent)]/55 hover:bg-[color:var(--fc-color-accent)]/22"
                    >
                      新增
                    </button>
                  </div>
                </div>
              ) : null}
            </div>
          </div>
          <div className="space-y-2">
            {groupOptions.map((group) => {
              const active = activeGroup === group
              const count = group === '全部' ? rows.length : rows.filter((row) => row.group === group).length
              return (
                <button
                  data-testid={`watchlist-group-tab-${group}`}
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
              {selectedCount > 0 ? (
                <>
                  <div ref={batchMovingOpen ? batchMoveMenuRef : null} className="relative">
                    <button
                      data-testid="watchlist-batch-assign-button"
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
                      <div className="absolute right-0 top-full z-20 mt-2 w-40 overflow-hidden rounded-2xl border border-white/10 bg-slate-950/95 p-1 shadow-[0_18px_40px_rgba(15,23,42,0.45)] backdrop-blur-xl">
                        {groupOptions
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
                      data-testid="watchlist-batch-delete-button"
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
                <div className="inline-flex min-w-0 rounded-xl py-1.5 pr-2 text-left text-[0.88rem] text-slate-400">
                  <span className="block w-full truncate">基金名称 + 代码</span>
                </div>
                <div className="inline-flex min-w-0 justify-center rounded-xl py-1.5 text-[0.88rem] text-slate-400">
                  <span className="block truncate text-center">当日涨幅</span>
                </div>
                {activeGroup === '全部' ? (
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
                    {activeGroup === '全部' ? (
                      <div className="flex items-center justify-center">
                        {row.group && row.group !== '全部' ? (
                          <button
                            type="button"
                            onClick={(event) => {
                              event.stopPropagation()
                              setActiveGroup(row.group)
                            }}
                            className="truncate rounded-full border border-white/8 bg-white/5 px-3 py-1 text-xs text-slate-300 transition hover:border-[color:var(--fc-color-accent)]/35 hover:bg-[color:var(--fc-color-accent)]/8 hover:text-[color:var(--fc-color-accent)]"
                          >
                            {row.group}
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
                          className="absolute left-1/2 top-full z-20 mt-2 w-40 overflow-hidden rounded-2xl border border-white/10 bg-slate-950/95 p-1 shadow-[0_18px_40px_rgba(15,23,42,0.45)] backdrop-blur-xl"
                          onClick={(event) => event.stopPropagation()}
                        >
                          {groupOptions
                            .filter((group) => group !== '全部')
                            .map((group) => (
                              <button
                                key={group}
                                type="button"
                                onClick={() => {
                                  onAssignGroup([row.code], group)
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
