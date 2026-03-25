import type { FormEvent } from 'react'
import { LoginShell } from '../components/ops/OpsUi'

export function LoginPage({
  username,
  password,
  message,
  onUsernameChange,
  onPasswordChange,
  onSubmit,
}: {
  username: string
  password: string
  message: string
  onUsernameChange: (value: string) => void
  onPasswordChange: (value: string) => void
  onSubmit: (event: FormEvent) => void
}) {
  return (
    <LoginShell>
      <div className="grid w-full gap-8 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="rounded-[2rem] border border-white/8 bg-white/5 p-8 shadow-[var(--fc-shadow-card)] backdrop-blur-xl">
          <p className="text-sm uppercase tracking-[0.28em] text-[color:var(--fc-color-accent)]">FundCat Ops</p>
          <h1 className="mt-5 font-[var(--fc-font-display)] text-4xl font-semibold text-white">后台联调入口</h1>
          <p className="mt-4 max-w-xl text-slate-300">
            后台已支持独立登录，并提供总览、功能开关、数据源、任务队列和周报提醒等可切换工作区。
          </p>
        </div>

        <form
          onSubmit={onSubmit}
          className="rounded-[2rem] border border-white/8 bg-white/5 p-8 shadow-[var(--fc-shadow-card)] backdrop-blur-xl"
        >
          <h2 className="font-[var(--fc-font-display)] text-3xl font-semibold text-white">登录后台</h2>
          <p className="mt-3 text-slate-400">演示账号与主站一致。</p>
          <label className="mt-8 block">
            <span className="mb-2 block text-sm text-slate-400">用户名</span>
            <input
              value={username}
              onChange={(event) => onUsernameChange(event.target.value)}
              autoComplete="username"
              className="w-full rounded-2xl border border-white/10 bg-slate-950/60 px-4 py-3 text-white outline-none transition focus:border-[color:var(--fc-color-accent)]"
            />
          </label>
          <label className="mt-5 block">
            <span className="mb-2 block text-sm text-slate-400">密码</span>
            <input
              type="password"
              value={password}
              onChange={(event) => onPasswordChange(event.target.value)}
              autoComplete="current-password"
              className="w-full rounded-2xl border border-white/10 bg-slate-950/60 px-4 py-3 text-white outline-none transition focus:border-[color:var(--fc-color-accent)]"
            />
          </label>
          {message ? <p className="mt-4 text-sm text-orange-300">{message}</p> : null}
          <button className="mt-8 inline-flex w-full items-center justify-center rounded-2xl bg-[color:var(--fc-color-accent)] px-4 py-3 font-semibold text-slate-950 transition hover:brightness-105">
            进入后台
          </button>
        </form>
      </div>
    </LoginShell>
  )
}
