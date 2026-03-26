/** 通用统计卡片组件，用于仪表盘和摘要区展示单个核心指标。 */
type StatCardProps = {
  label: string
  value: string
  delta: string
  tone: 'positive' | 'negative' | 'neutral'
}

const toneClassMap = {
  positive: 'text-emerald-400',
  negative: 'text-orange-400',
  neutral: 'text-slate-300',
}

export function StatCard({ label, value, delta, tone }: StatCardProps) {
  return (
    <div className="group relative overflow-hidden rounded-[2rem] border border-white/8 bg-white/5 p-5 transition duration-300 hover:-translate-y-1 hover:bg-white/7">
      <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-[color:var(--fc-color-accent)] to-transparent opacity-70" />
      <p className="text-sm text-slate-400">{label}</p>
      <div className="mt-4 flex items-end justify-between gap-4">
        <p className="font-[var(--fc-font-display)] text-3xl font-semibold text-white">{value}</p>
        <p className={`text-sm font-medium ${toneClassMap[tone]}`}>{delta}</p>
      </div>
    </div>
  )
}
