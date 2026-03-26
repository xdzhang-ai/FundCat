/** 基金详情页折叠区块容器，统一展开/收起头部和内容布局。 */
import { ChevronDown, ChevronUp } from 'lucide-react'
import type { ReactNode } from 'react'

export function CollapsibleBlock({
  title,
  subtitle,
  expanded,
  onToggle,
  children,
}: {
  title: string
  subtitle?: string
  expanded: boolean
  onToggle: () => void
  children: ReactNode
}) {
  return (
    <div className="rounded-[1.75rem] border border-white/8 bg-white/5 p-4">
      <button onClick={onToggle} className="flex w-full items-start justify-between gap-4 text-left">
        <div>
          <p className="text-sm text-slate-300">{title}</p>
          {subtitle ? <p className="mt-1 text-xs text-slate-500">{subtitle}</p> : null}
        </div>
        <span className="rounded-full border border-white/10 bg-slate-950/45 p-2 text-slate-300">
          {expanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
        </span>
      </button>
      {expanded ? <div className="mt-4">{children}</div> : null}
    </div>
  )
}
