/** 后台总览页，展示控制台核心指标并提供快速跳转入口。 */
import { Activity, DatabaseZap, ListTodo, ShieldCheck } from 'lucide-react'
import type { FeatureFlag } from '@fundcat/contracts'
import type { ProviderStatus } from '../../../common/appTypes'
import { MetricCard } from '../../../common/components/OpsUi'

export function OverviewPage({
  featureFlags,
  providers,
  importJobCount,
  onNavigate,
}: {
  featureFlags: FeatureFlag[]
  providers: ProviderStatus[]
  importJobCount: number
  onNavigate: (path: string) => void
}) {
  return (
    <section className="mt-6 grid gap-4 md:grid-cols-4">
      <MetricCard icon={ShieldCheck} label="开关" value={`${featureFlags.filter((flag) => flag.enabled).length}/${featureFlags.length}`} onClick={() => onNavigate('/flags')} />
      <MetricCard icon={DatabaseZap} label="数据源" value={String(providers.length)} onClick={() => onNavigate('/providers')} />
      <MetricCard icon={Activity} label="健康状态" value={providers.every((provider) => provider.status === 'healthy') ? 'Healthy' : 'Check'} onClick={() => onNavigate('/providers')} />
      <MetricCard icon={ListTodo} label="任务队列" value={String(importJobCount)} onClick={() => onNavigate('/queue')} />
    </section>
  )
}

