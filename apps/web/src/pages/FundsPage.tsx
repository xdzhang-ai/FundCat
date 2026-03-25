import type { FundCard } from '@fundcat/contracts'
import { ArrowRight, Search } from 'lucide-react'
import { SectionCard } from '../components/SectionCard'
import { formatSignedPercent } from '../components/workspace/WebUi'

export function FundsPage({
  search,
  funds,
  selectedCode,
  onSearchChange,
  onSelectFund,
}: {
  search: string
  funds: FundCard[]
  selectedCode: string
  onSearchChange: (value: string) => void
  onSelectFund: (code: string) => void
}) {
  return (
    <SectionCard
      title="基金雷达"
      eyebrow="Fund center"
      action={
        <label className="flex items-center gap-2 rounded-full border border-white/10 bg-slate-950/50 px-4 py-2 text-sm text-slate-300">
          <Search className="h-4 w-4" />
          <input
            value={search}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="基金代码 / 名称"
            className="w-32 bg-transparent outline-none placeholder:text-slate-500 md:w-52"
          />
        </label>
      }
    >
      <div className="mb-4 flex items-center justify-between gap-3 text-xs text-slate-500">
        <span>共 {funds.length} 条结果</span>
        <span>点击基金卡片进入独立详情页</span>
      </div>
      <div className="grid gap-4 xl:grid-cols-2 2xl:grid-cols-3">
        {funds.map((fund) => (
          <button
            key={fund.code}
            onClick={() => onSelectFund(fund.code)}
            className={`rounded-[1.5rem] border px-4 py-4 text-left transition ${
              fund.code === selectedCode
                ? 'border-[color:var(--fc-color-accent)]/60 bg-[color:var(--fc-color-accent)]/10'
                : 'border-white/8 bg-white/5 hover:border-white/20'
            }`}
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

            <div className="mt-4 flex flex-wrap gap-2">
              {fund.tags.map((tag) => (
                <span key={tag} className="rounded-full border border-white/10 bg-slate-950/45 px-3 py-1.5 text-xs text-slate-300">
                  {tag}
                </span>
              ))}
            </div>

            <div className="mt-5 flex items-center justify-between border-t border-white/8 pt-4 text-sm text-slate-300">
              <span>{fund.riskLevel}</span>
              <span className="inline-flex items-center gap-1 text-[color:var(--fc-color-accent)]">
                查看详情
                <ArrowRight className="h-4 w-4" />
              </span>
            </div>
          </button>
        ))}
      </div>
    </SectionCard>
  )
}
