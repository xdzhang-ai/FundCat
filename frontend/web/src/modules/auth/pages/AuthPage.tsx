/** 登录页，处理研究工作台的账号密码输入、错误提示和入口文案。 */
import { BellRing, DatabaseZap, ScanSearch } from 'lucide-react'
import type { FormEvent } from 'react'

const quickSignalItems = [
  {
    label: '数据源',
    value: 'demo-aggregator',
    helper: '可替换 provider 适配层',
    icon: DatabaseZap,
  },
  {
    label: 'OCR 状态',
    value: '排队导入',
    helper: '截图上传后由后台异步识别',
    icon: ScanSearch,
  },
  {
    label: '提醒中心',
    value: 'Inbox',
    helper: '默认走站内消息',
    icon: BellRing,
  },
] as const

export function AuthPage({
  username,
  password,
  errorMessage,
  onUsernameChange,
  onPasswordChange,
  onSubmit,
}: {
  username: string
  password: string
  errorMessage?: string
  onUsernameChange: (value: string) => void
  onPasswordChange: (value: string) => void
  onSubmit: (event: FormEvent) => void
}) {
  return (
    <div className="relative mx-auto flex min-h-screen max-w-6xl items-center px-6 py-12">
      <div className="grid w-full gap-8 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="rounded-[2rem] border border-white/8 bg-white/5 p-8 shadow-[var(--fc-shadow-card)] backdrop-blur-xl">
          <div className="inline-flex items-center gap-2 rounded-full border border-[color:var(--fc-color-accent)]/30 bg-[color:var(--fc-color-accent)]/10 px-4 py-2 text-xs uppercase tracking-[0.24em] text-[color:var(--fc-color-accent)]">
            FundCat Demo Workspace
          </div>
          <h1 className="mt-6 max-w-xl font-[var(--fc-font-display)] text-5xl font-semibold text-white">
            面向基金观察、模拟决策与运营控制的演示工作台
          </h1>
          <p className="mt-6 max-w-2xl text-lg text-slate-300">
            这个首版实现了登录、基金搜索与详情、持仓看板、模拟买卖、模拟定投、OCR 导入占位、周报与高风险功能开关。
          </p>
          <div className="mt-10 grid gap-4 sm:grid-cols-3">
            {quickSignalItems.map((item) => (
              <div key={item.label} className="rounded-3xl border border-white/8 bg-slate-950/40 p-4">
                <item.icon className="h-5 w-5 text-[color:var(--fc-color-accent)]" />
                <p className="mt-4 text-sm text-slate-400">{item.label}</p>
                <p className="mt-1 font-[var(--fc-font-display)] text-lg text-white">{item.value}</p>
                <p className="mt-1 text-sm text-slate-500">{item.helper}</p>
              </div>
            ))}
          </div>
        </div>

        <form
          onSubmit={onSubmit}
          className="rounded-[2rem] border border-white/8 bg-[color:var(--fc-color-surface-glass)] p-8 shadow-[var(--fc-shadow-card)] backdrop-blur-xl"
        >
          <p className="text-sm uppercase tracking-[0.28em] text-[color:var(--fc-color-accent)]">Demo Access</p>
          <h2 className="mt-4 font-[var(--fc-font-display)] text-3xl font-semibold text-white">登录研究版</h2>
          <p className="mt-3 text-slate-400">默认演示账号已预置，你也可以直接拿这些凭证启动前后端联调。</p>
          <label className="mt-8 block">
            <span className="mb-2 block text-sm text-slate-400">用户名</span>
            <input
              value={username}
              onChange={(event) => onUsernameChange(event.target.value)}
              className="w-full rounded-2xl border border-white/10 bg-slate-950/60 px-4 py-3 text-white outline-none transition focus:border-[color:var(--fc-color-accent)]"
            />
          </label>
          <label className="mt-5 block">
            <span className="mb-2 block text-sm text-slate-400">密码</span>
            <input
              type="password"
              value={password}
              onChange={(event) => onPasswordChange(event.target.value)}
              className="w-full rounded-2xl border border-white/10 bg-slate-950/60 px-4 py-3 text-white outline-none transition focus:border-[color:var(--fc-color-accent)]"
            />
          </label>
          {errorMessage ? (
            <p className="mt-4 text-sm text-orange-300">{errorMessage}</p>
          ) : null}
          <button className="mt-8 inline-flex w-full items-center justify-center rounded-2xl bg-[color:var(--fc-color-accent)] px-4 py-3 font-semibold text-slate-950 transition hover:brightness-105">
            进入研究工作台
          </button>
          <p className="mt-4 text-xs text-slate-500">Demo credentials: demo_analyst / ChangeMe123!</p>
        </form>
      </div>
    </div>
  )
}
