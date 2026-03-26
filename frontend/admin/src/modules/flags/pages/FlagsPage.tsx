/** 功能开关页，支持筛选、查看详情和切换 Feature Flag。 */
import type { FeatureFlag } from '@fundcat/contracts'
import { useDeferredValue, useEffect, useState } from 'react'
import { DetailPill, EmptyPanel, FlagBadge } from '../../../common/components/OpsUi'

export function FlagsPage({
  featureFlags,
  isSavingFlagCode,
  onToggleFlag,
}: {
  featureFlags: FeatureFlag[]
  isSavingFlagCode: string | null
  onToggleFlag: (flag: FeatureFlag) => void
}) {
  const [flagQuery, setFlagQuery] = useState('')
  const [selectedFlagCode, setSelectedFlagCode] = useState<string | null>(null)
  const deferredFlagQuery = useDeferredValue(flagQuery.trim().toLowerCase())

  const filteredFlags = featureFlags.filter((flag) => {
    if (!deferredFlagQuery) return true
    const haystack = `${flag.name} ${flag.code} ${flag.description} ${flag.environment} ${flag.riskLevel}`.toLowerCase()
    return haystack.includes(deferredFlagQuery)
  })

  useEffect(() => {
    setSelectedFlagCode((current) => {
      if (current && filteredFlags.some((flag) => flag.code === current)) {
        return current
      }
      return filteredFlags[0]?.code ?? null
    })
  }, [filteredFlags])

  const selectedFlag = filteredFlags.find((flag) => flag.code === selectedFlagCode) ?? filteredFlags[0] ?? null

  return (
    <section className="mt-6 grid gap-6 xl:grid-cols-[0.58fr_0.42fr]">
      <div className="rounded-[2rem] border border-white/8 bg-white/5 p-6 shadow-[var(--fc-shadow-card)]">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <h2 className="font-[var(--fc-font-display)] text-2xl text-white">Feature Flags</h2>
            <p className="mt-2 text-sm text-slate-400">支持搜索、切换与查看风险等级。</p>
          </div>
          <input value={flagQuery} onChange={(event) => setFlagQuery(event.target.value)} placeholder="搜索开关名 / code / 描述" className="rounded-full border border-white/10 bg-slate-950/50 px-4 py-2 text-sm text-white outline-none placeholder:text-slate-500" />
        </div>
        <div className="mt-5 space-y-3">
          {filteredFlags.map((flag) => (
            <button
              key={flag.code}
              onClick={() => setSelectedFlagCode(flag.code)}
              className={`w-full rounded-[1.6rem] border p-4 text-left transition ${
                selectedFlag?.code === flag.code ? 'border-[color:var(--fc-color-accent)]/45 bg-[color:var(--fc-color-accent)]/8' : 'border-white/8 bg-slate-950/35 hover:border-white/20'
              }`}
            >
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="font-medium text-white">{flag.name}</p>
                  <p className="mt-1 text-sm text-slate-400">{flag.description}</p>
                </div>
                <FlagBadge flag={flag} />
              </div>
              <div className="mt-4 flex items-center justify-between text-sm text-slate-500">
                <span>{flag.code}</span>
                <span className="rounded-full border border-white/10 px-3 py-1">{flag.riskLevel}</span>
              </div>
            </button>
          ))}
        </div>
      </div>

      <div className="rounded-[2rem] border border-white/8 bg-white/5 p-6 shadow-[var(--fc-shadow-card)]">
        {selectedFlag ? (
          <>
            <p className="text-sm uppercase tracking-[0.22em] text-[color:var(--fc-color-accent)]">Flag detail</p>
            <h2 className="mt-4 font-[var(--fc-font-display)] text-3xl text-white">{selectedFlag.name}</h2>
            <p className="mt-4 text-sm leading-7 text-slate-300">{selectedFlag.description}</p>
            <div className="mt-6 grid gap-3 sm:grid-cols-2">
              <DetailPill label="Code" value={selectedFlag.code} />
              <DetailPill label="Environment" value={selectedFlag.environment} />
              <DetailPill label="Risk" value={selectedFlag.riskLevel} />
              <DetailPill label="Status" value={selectedFlag.enabled ? 'enabled' : 'disabled'} />
            </div>
            <button
              onClick={() => onToggleFlag(selectedFlag)}
              disabled={isSavingFlagCode === selectedFlag.code}
              className={`mt-8 inline-flex items-center justify-center rounded-2xl px-4 py-3 font-semibold transition ${
                selectedFlag.enabled ? 'bg-orange-400/90 text-slate-950 hover:brightness-105' : 'bg-[color:var(--fc-color-accent)] text-slate-950 hover:brightness-105'
              } disabled:cursor-not-allowed disabled:opacity-70`}
            >
              {isSavingFlagCode === selectedFlag.code ? '提交中...' : selectedFlag.enabled ? '关闭该功能' : '启用该功能'}
            </button>
          </>
        ) : (
          <EmptyPanel title="没有匹配的功能开关" body="调整搜索词后再试，或先点击刷新重新获取后台数据。" />
        )}
      </div>
    </section>
  )
}

