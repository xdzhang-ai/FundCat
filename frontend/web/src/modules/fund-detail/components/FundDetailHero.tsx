/** 基金详情页顶部 Hero 区块，展示基金基本信息、标签和净值摘要。 */
import type { FundDetail } from '@fundcat/contracts'
import { formatSignedPercent } from '../../../common/components/WebUi'
import { formatAmount } from '../../../common/utils/fundInsights'

export function FundDetailHero({ selectedFund }: { selectedFund: FundDetail }) {
  return (
    <div className="rounded-[1.8rem] border border-white/8 bg-[linear-gradient(135deg,rgba(255,255,255,0.05),rgba(8,12,20,0.16)_55%,rgba(243,186,47,0.1))] p-5">
      <div className="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
        <div className="min-w-0">
          <p className="text-xs uppercase tracking-[0.28em] text-[color:var(--fc-color-accent)]">{selectedFund.code}</p>
          <h3 className="mt-3 font-[var(--fc-font-display)] text-3xl font-semibold text-white xl:text-4xl">{selectedFund.name}</h3>
          <p className="mt-3 text-sm text-slate-400">
            {selectedFund.category} · {selectedFund.riskLevel} · 跟踪基准 {selectedFund.benchmark}
          </p>
          <div className="mt-4 flex flex-wrap gap-2">
            {selectedFund.tags.map((tag) => (
              <span key={tag} className="rounded-full border border-white/10 bg-slate-950/45 px-3 py-1.5 text-xs text-slate-200">
                {tag}
              </span>
            ))}
          </div>
        </div>

        <div className="grid gap-3 xl:min-w-[240px] xl:justify-items-end">
          <p className="font-[var(--fc-font-display)] text-4xl text-white">{formatAmount(selectedFund.estimatedNav)}</p>
          <div className="flex flex-wrap gap-2 xl:justify-end">
            <span
              className={`rounded-full px-3 py-1 text-sm ${
                selectedFund.estimatedGrowth >= 0 ? 'bg-emerald-400/10 text-emerald-300' : 'bg-orange-400/10 text-orange-300'
              }`}
            >
              {formatSignedPercent(selectedFund.estimatedGrowth)}
            </span>
            {selectedFund.referenceOnly ? (
              <span className="rounded-full border border-[color:var(--fc-color-accent)]/25 bg-[color:var(--fc-color-accent)]/10 px-3 py-1 text-xs uppercase tracking-[0.24em] text-[color:var(--fc-color-accent)]">
                reference only
              </span>
            ) : null}
          </div>
          <p className="text-sm text-slate-400">单位净值 {formatAmount(selectedFund.unitNav)}</p>
        </div>
      </div>
    </div>
  )
}
