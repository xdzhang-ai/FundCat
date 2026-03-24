import type { PropsWithChildren, ReactNode } from 'react'
import { clsx } from 'clsx'

type SectionCardProps = PropsWithChildren<{
  title: string
  eyebrow?: string
  action?: ReactNode
  className?: string
}>

export function SectionCard({ title, eyebrow, action, className, children }: SectionCardProps) {
  return (
    <section
      className={clsx(
        'rounded-[var(--fc-radius-card)] border border-white/8 bg-[color:var(--fc-color-surface-glass)] p-6 shadow-[var(--fc-shadow-card)] backdrop-blur-xl',
        className,
      )}
    >
      <header className="mb-5 flex items-start justify-between gap-4">
        <div>
          {eyebrow ? (
            <p className="mb-2 text-xs uppercase tracking-[0.3em] text-[color:var(--fc-color-accent)]">
              {eyebrow}
            </p>
          ) : null}
          <h2 className="font-[var(--fc-font-display)] text-xl font-semibold text-white">{title}</h2>
        </div>
        {action}
      </header>
      {children}
    </section>
  )
}
