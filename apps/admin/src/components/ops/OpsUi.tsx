import type { FeatureFlag } from '@fundcat/contracts'
import type { ComponentType, PropsWithChildren } from 'react'

export function FlagBadge({ flag }: { flag: FeatureFlag }) {
  return (
    <span
      className={`rounded-full px-3 py-1 text-xs uppercase tracking-[0.18em] ${
        flag.enabled
          ? 'bg-emerald-400/10 text-emerald-300'
          : 'bg-slate-500/20 text-slate-300'
      }`}
    >
      {flag.environment}
    </span>
  )
}

export function DetailPill({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[1.4rem] border border-white/8 bg-slate-950/35 px-4 py-4">
      <p className="text-xs uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className="mt-3 text-sm leading-6 text-slate-200">{value}</p>
    </div>
  )
}

export function EmptyPanel({ title, body }: { title: string; body: string }) {
  return (
    <div className="flex h-full min-h-72 items-center justify-center rounded-[1.6rem] border border-dashed border-white/10 bg-slate-950/30 p-6 text-center">
      <div>
        <p className="font-medium text-white">{title}</p>
        <p className="mt-3 text-sm leading-6 text-slate-400">{body}</p>
      </div>
    </div>
  )
}

export function MetricCard({
  icon: Icon,
  label,
  value,
  onClick,
}: {
  icon: ComponentType<{ className?: string }>
  label: string
  value: string
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      className="rounded-[1.8rem] border border-white/8 bg-white/5 p-5 text-left shadow-[var(--fc-shadow-card)] backdrop-blur-xl transition hover:border-white/20"
    >
      <Icon className="h-5 w-5 text-[color:var(--fc-color-accent)]" />
      <p className="mt-4 text-sm text-slate-400">{label}</p>
      <p className="mt-2 font-[var(--fc-font-display)] text-3xl text-white">{value}</p>
    </button>
  )
}

export function formatDate(value: string) {
  return value.replace('T', ' ').slice(0, 16)
}

export function LoginShell({ children }: PropsWithChildren) {
  return (
    <div className="mx-auto flex min-h-screen max-w-6xl items-center px-6 py-8">
      {children}
    </div>
  )
}
