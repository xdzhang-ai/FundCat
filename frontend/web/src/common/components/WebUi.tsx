/** 工作台通用 UI 小组件，集中放置按钮、加载态和轻量展示部件。 */
import { Sparkles } from 'lucide-react'
import type { ComponentType, PropsWithChildren } from 'react'

export function formatSignedPercent(value: number) {
  return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`
}

export function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl bg-slate-950/50 px-3 py-3">
      <p className="text-xs uppercase tracking-[0.16em] text-slate-500">{label}</p>
      <p className="mt-2 font-medium text-white">{value}</p>
    </div>
  )
}

export function Panel({ title, children }: PropsWithChildren<{ title: string }>) {
  return (
    <div className="rounded-[1.75rem] border border-white/8 bg-slate-950/35 p-4">
      {title ? <p className="mb-4 text-sm text-slate-400">{title}</p> : null}
      <div className="space-y-3">{children}</div>
    </div>
  )
}

export function Row({ title, meta, value }: { title: string; meta: string; value: string }) {
  return (
    <div className="flex items-start justify-between gap-4 rounded-2xl bg-white/5 px-4 py-3">
      <div>
        <p className="font-medium text-white">{title}</p>
        <p className="mt-1 text-sm text-slate-500">{meta}</p>
      </div>
      <p className="text-sm text-slate-200">{value}</p>
    </div>
  )
}

export function QuickActionButton({
  icon: Icon,
  label,
  onClick,
}: {
  icon: ComponentType<{ className?: string }>
  label: string
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-200 transition hover:border-[color:var(--fc-color-accent)]/50 hover:text-white"
    >
      <Icon className="h-4 w-4" />
      {label}
    </button>
  )
}

export function LoadingScreen() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <div className="rounded-[2rem] border border-white/8 bg-white/5 px-8 py-6 text-center text-slate-300 shadow-[var(--fc-shadow-card)] backdrop-blur-xl">
        <Sparkles className="mx-auto h-8 w-8 text-[color:var(--fc-color-accent)]" />
        <p className="mt-4 font-[var(--fc-font-display)] text-xl text-white">正在载入 FundCat 工作台…</p>
      </div>
    </div>
  )
}
