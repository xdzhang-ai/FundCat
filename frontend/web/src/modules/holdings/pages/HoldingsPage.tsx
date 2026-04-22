/** 持仓页，展示当前组合持仓、收益和组合市值摘要。 */
import type { HoldingsOverviewResponse } from '@fundcat/contracts'
import {
  closestCenter,
  DndContext,
  type DragEndEvent,
  type DragOverEvent,
  type DragStartEvent,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core'
import { arrayMove, SortableContext, useSortable, horizontalListSortingStrategy } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { motion } from 'framer-motion'
import { GripVertical } from 'lucide-react'
import { useMemo, useState } from 'react'
import { SectionCard } from '../../../common/components/SectionCard'
import { formatCompactPercent, formatCurrency } from '../../../common/utils/fundInsights'

type FixedColumnKey = 'name'
type MovableColumnKey = 'dayGrowth' | 'todayPnl' | 'marketValue' | 'holdingPnl'
type ColumnKey = FixedColumnKey | MovableColumnKey

const fixedColumn: FixedColumnKey = 'name'
const initialMovableColumns: MovableColumnKey[] = ['dayGrowth', 'todayPnl', 'marketValue', 'holdingPnl']
const columnTemplate = 'minmax(260px,2.2fr) repeat(4,minmax(0,1fr))'

const columnMeta: Record<ColumnKey, { label: string; align?: 'left' | 'right' }> = {
  name: { label: '基金名称 + 代码', align: 'left' },
  dayGrowth: { label: '当日涨幅', align: 'right' },
  todayPnl: { label: '今日收益', align: 'right' },
  marketValue: { label: '持有金额', align: 'right' },
  holdingPnl: { label: '持有收益', align: 'right' },
}

type HoldingRow = {
  fundCode: string
  fundName: string
  dayGrowth: number
  todayPnl: number
  marketValue: number
  holdingPnl: number
}

export function HoldingsPage({
  overview,
  onOpenFund,
}: {
  overview: HoldingsOverviewResponse
  onOpenFund: (code: string) => void
}) {
  const [movableColumns, setMovableColumns] = useState<MovableColumnKey[]>(initialMovableColumns)
  const [activeColumn, setActiveColumn] = useState<MovableColumnKey | null>(null)
  const [overColumn, setOverColumn] = useState<MovableColumnKey | null>(null)

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 6,
      },
    }),
  )

  const rows = useMemo<HoldingRow[]>(() => {
    return overview.items.map((item) => ({
      fundCode: item.fundCode,
      fundName: item.fundName,
      dayGrowth: item.dayGrowth,
      todayPnl: item.todayPnl,
      marketValue: item.marketValue,
      holdingPnl: item.holdingPnl,
    }))
  }, [overview.items])

  const previewMovableColumns = useMemo(() => {
    if (!activeColumn || !overColumn || activeColumn === overColumn) {
      return movableColumns
    }
    const activeIndex = movableColumns.indexOf(activeColumn)
    const overIndex = movableColumns.indexOf(overColumn)
    if (activeIndex === -1 || overIndex === -1) {
      return movableColumns
    }
    return arrayMove(movableColumns, activeIndex, overIndex)
  }, [activeColumn, movableColumns, overColumn])

  function handleDragStart(event: DragStartEvent) {
    setActiveColumn(event.active.id as MovableColumnKey)
    setOverColumn(event.active.id as MovableColumnKey)
  }

  function handleDragOver(event: DragOverEvent) {
    if (!event.over) return
    setOverColumn(event.over.id as MovableColumnKey)
  }

  function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event
    if (over && active.id !== over.id) {
      setMovableColumns((current) => {
        const activeIndex = current.indexOf(active.id as MovableColumnKey)
        const overIndex = current.indexOf(over.id as MovableColumnKey)
        if (activeIndex === -1 || overIndex === -1) {
          return current
        }
        return arrayMove(current, activeIndex, overIndex)
      })
    }
    setActiveColumn(null)
    setOverColumn(null)
  }

  function handleDragCancel() {
    setActiveColumn(null)
    setOverColumn(null)
  }

  return (
    <SectionCard title="当前持仓" eyebrow="Holdings page">
      <div className="mb-4 flex items-center justify-between gap-3 text-xs text-slate-500">
        <span>点击任意行进入基金详情</span>
        <span>拖拽后 4 列可调整顺序，基金名称保持固定</span>
      </div>

      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragStart={handleDragStart}
        onDragOver={handleDragOver}
        onDragEnd={handleDragEnd}
        onDragCancel={handleDragCancel}
      >
        <div data-testid="holdings-table" className="overflow-hidden rounded-[1.75rem] border border-white/8 bg-white/5">
          <div
            className="grid gap-3 border-b border-white/8 px-4 py-3 text-xs text-slate-400"
            style={{ gridTemplateColumns: columnTemplate }}
          >
            <HeaderCell column={fixedColumn} />
            <SortableContext items={movableColumns} strategy={horizontalListSortingStrategy}>
              {movableColumns.map((column) => (
                <SortableHeaderCell
                  key={column}
                  column={column}
                  previewOrder={previewMovableColumns.indexOf(column)}
                  isPreviewTarget={Boolean(activeColumn && overColumn === column && activeColumn !== column)}
                />
              ))}
            </SortableContext>
          </div>

          <div className="divide-y divide-white/8">
            {rows.map((row) => (
              <button
                data-testid={`holding-row-${row.fundCode}`}
                key={row.fundCode}
                onClick={() => onOpenFund(row.fundCode)}
                className="grid w-full gap-3 px-4 py-4 text-left transition hover:bg-white/5"
                style={{ gridTemplateColumns: columnTemplate }}
              >
                <div className="min-w-0">{renderCell(fixedColumn, row)}</div>
                {previewMovableColumns.map((column) => (
                  <motion.div
                    key={`${row.fundCode}:${column}`}
                    layout
                    transition={{ type: 'spring', stiffness: 420, damping: 34, mass: 0.85 }}
                    className={`relative flex min-w-0 items-center ${columnMeta[column].align === 'right' ? 'justify-end text-right' : ''}`}
                  >
                    {renderCell(column, row)}
                  </motion.div>
                ))}
              </button>
            ))}
          </div>
        </div>
      </DndContext>
    </SectionCard>
  )
}

function HeaderCell({ column }: { column: ColumnKey }) {
  const meta = columnMeta[column]
  return (
    <div
      className={`relative inline-flex min-w-0 rounded-xl py-1.5 text-left text-[0.88rem] text-slate-400 ${
        meta.align === 'right' ? 'justify-end pr-2' : 'pr-2'
      }`}
    >
      <span className={`block w-full truncate ${meta.align === 'right' ? 'text-right' : ''}`}>{meta.label}</span>
    </div>
  )
}

function SortableHeaderCell({
  column,
  previewOrder,
  isPreviewTarget,
}: {
  column: MovableColumnKey
  previewOrder: number
  isPreviewTarget: boolean
}) {
  const meta = columnMeta[column]
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: column,
  })

  return (
    <motion.button
      ref={setNodeRef}
      layout
      transition={{ type: 'spring', stiffness: 420, damping: 34, mass: 0.85 }}
      style={{
        transform: CSS.Transform.toString(transform),
        transition,
        order: previewOrder,
        zIndex: isDragging ? 4 : isPreviewTarget ? 2 : 1,
      }}
      className={`relative inline-flex min-w-0 rounded-xl py-1.5 text-left text-[0.88rem] ${
        meta.align === 'right' ? 'justify-end pr-2 pl-7' : 'pl-7 pr-2'
      } ${
        isDragging
          ? 'bg-[color:var(--fc-color-accent)]/14 text-[color:var(--fc-color-accent)] ring-1 ring-[color:var(--fc-color-accent)]/35 shadow-[0_12px_30px_rgba(0,0,0,0.28)]'
          : isPreviewTarget
            ? 'text-[color:var(--fc-color-accent)]'
            : 'text-slate-400'
      }`}
      {...attributes}
      {...listeners}
    >
      <GripVertical
        className={`absolute left-2 top-1/2 h-4 w-4 -translate-y-1/2 ${
          isDragging || isPreviewTarget ? 'text-[color:var(--fc-color-accent)]' : 'text-slate-600'
        }`}
      />
      {isPreviewTarget ? (
        <span className="absolute -left-1 top-2 bottom-2 w-[3px] rounded-full bg-[color:var(--fc-color-accent)] shadow-[0_0_12px_rgba(250,191,62,0.4)]" />
      ) : null}
      <span className={`block w-full truncate ${meta.align === 'right' ? 'text-right' : ''}`}>{meta.label}</span>
    </motion.button>
  )
}

function renderCell(column: ColumnKey, row: HoldingRow) {
  switch (column) {
    case 'name':
      return (
        <>
          <p className="truncate font-[var(--fc-font-display)] text-[0.92rem] text-white xl:text-[1rem]">{row.fundName}</p>
          <p className="mt-1 text-[0.8rem] text-slate-500">{row.fundCode}</p>
        </>
      )
    case 'dayGrowth':
      return <ValueText value={formatCompactPercent(row.dayGrowth)} tone={row.dayGrowth >= 0 ? 'positive' : 'negative'} />
    case 'todayPnl':
      return <ValueText value={formatCurrency(row.todayPnl)} tone={row.todayPnl >= 0 ? 'positive' : 'negative'} />
    case 'marketValue':
      return <ValueText value={formatCurrency(row.marketValue)} />
    case 'holdingPnl':
      return <ValueText value={formatCurrency(row.holdingPnl)} tone={row.holdingPnl >= 0 ? 'positive' : 'negative'} />
  }
}

function ValueText({
  value,
  tone = 'neutral',
}: {
  value: string
  tone?: 'positive' | 'negative' | 'neutral'
}) {
  return (
    <p
      className={`truncate font-[var(--fc-font-display)] ${valueSizeClass(value)} ${
        tone === 'positive' ? 'text-emerald-300' : tone === 'negative' ? 'text-orange-300' : 'text-white'
      }`}
    >
      {value}
    </p>
  )
}

function valueSizeClass(value: string) {
  const length = value.length
  if (length >= 12) return 'text-[0.94rem] xl:text-[1rem]'
  if (length >= 10) return 'text-[1rem] xl:text-[1.06rem]'
  if (length >= 8) return 'text-[1.06rem] xl:text-[1.12rem]'
  return 'text-[1.12rem] xl:text-[1.18rem]'
}
