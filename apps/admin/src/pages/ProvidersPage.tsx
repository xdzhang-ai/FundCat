import { useEffect, useState } from 'react'
import type { ProviderStatus } from '../api/client'
import { DetailPill, EmptyPanel } from '../components/ops/OpsUi'

export function ProvidersPage({ providers }: { providers: ProviderStatus[] }) {
  const [selectedProviderKey, setSelectedProviderKey] = useState<string | null>(null)

  useEffect(() => {
    setSelectedProviderKey((current) => {
      if (current && providers.some((provider) => provider.providerKey === current)) {
        return current
      }
      return providers[0]?.providerKey ?? null
    })
  }, [providers])

  const selectedProvider =
    providers.find((provider) => provider.providerKey === selectedProviderKey) ??
    providers[0] ??
    null

  return (
    <section className="mt-6 grid gap-6 xl:grid-cols-[0.48fr_0.52fr]">
      <div className="rounded-[2rem] border border-white/8 bg-white/5 p-6 shadow-[var(--fc-shadow-card)]">
        <h2 className="font-[var(--fc-font-display)] text-2xl text-white">Provider Status</h2>
        <div className="mt-5 space-y-3">
          {providers.map((provider) => (
            <button
              key={provider.providerKey}
              onClick={() => setSelectedProviderKey(provider.providerKey)}
              className={`w-full rounded-[1.6rem] border p-4 text-left transition ${
                selectedProvider?.providerKey === provider.providerKey
                  ? 'border-[color:var(--fc-color-accent)]/45 bg-[color:var(--fc-color-accent)]/8'
                  : 'border-white/8 bg-slate-950/35 hover:border-white/20'
              }`}
            >
              <div className="flex items-center justify-between gap-4">
                <p className="font-medium text-white">{provider.providerKey}</p>
                <span className="rounded-full bg-emerald-400/10 px-3 py-1 text-xs uppercase tracking-[0.18em] text-emerald-300">
                  {provider.status}
                </span>
              </div>
              <p className="mt-3 text-sm leading-6 text-slate-400">{provider.notes}</p>
            </button>
          ))}
        </div>
      </div>

      <div className="rounded-[2rem] border border-white/8 bg-white/5 p-6 shadow-[var(--fc-shadow-card)]">
        {selectedProvider ? (
          <>
            <p className="text-sm uppercase tracking-[0.22em] text-[color:var(--fc-color-accent)]">Provider detail</p>
            <h2 className="mt-4 font-[var(--fc-font-display)] text-3xl text-white">{selectedProvider.providerKey}</h2>
            <div className="mt-6 grid gap-3 sm:grid-cols-2">
              <DetailPill label="Status" value={selectedProvider.status} />
              <DetailPill label="Scope" value="research adapter" />
            </div>
            <div className="mt-6 rounded-[1.6rem] border border-white/8 bg-slate-950/35 p-4">
              <p className="text-sm text-slate-400">备注</p>
              <p className="mt-3 leading-7 text-slate-200">{selectedProvider.notes}</p>
            </div>
            <div className="mt-6 rounded-[1.6rem] border border-white/8 bg-slate-950/35 p-4">
              <p className="text-sm text-slate-400">下一步建议</p>
              <ul className="mt-3 space-y-2 text-sm text-slate-300">
                <li>检查最近一次同步时间与失败重试记录</li>
                <li>补充 provider 详情页与任务重试操作</li>
                <li>将健康状态和开关策略联动展示</li>
              </ul>
            </div>
          </>
        ) : (
          <EmptyPanel title="暂无数据源" body="当前环境没有可显示的数据源状态。" />
        )}
      </div>
    </section>
  )
}
