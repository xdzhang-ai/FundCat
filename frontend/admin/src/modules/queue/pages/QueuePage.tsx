/** 任务队列页，展示 OCR 导入与异步任务样例。 */
import type { ImportJob } from '@fundcat/contracts'
import { DetailPill, formatDate } from '../../../common/components/OpsUi'

export function QueuePage({ importJobs }: { importJobs: ImportJob[] }) {
  return (
    <section className="mt-6 rounded-[2rem] border border-white/8 bg-white/5 p-6 shadow-[var(--fc-shadow-card)]">
      <div className="flex items-end justify-between gap-4">
        <div>
          <h2 className="font-[var(--fc-font-display)] text-2xl text-white">任务队列</h2>
          <p className="mt-2 text-sm text-slate-400">当前以 OCR 导入任务为主，可继续扩展成统一异步任务中心。</p>
        </div>
        <p className="text-sm text-slate-500">共 {importJobs.length} 条</p>
      </div>
      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        {importJobs.map((job) => (
          <div key={job.id} className="rounded-[1.6rem] border border-white/8 bg-slate-950/35 p-5">
            <div className="flex items-center justify-between gap-4">
              <p className="font-medium text-white">{job.fileName}</p>
              <span className="rounded-full border border-white/10 px-3 py-1 text-xs uppercase tracking-[0.18em] text-slate-200">{job.status}</span>
            </div>
            <p className="mt-3 text-sm text-slate-400">{job.sourcePlatform}</p>
            <div className="mt-5 grid gap-3 sm:grid-cols-2">
              <DetailPill label="识别持仓数" value={String(job.recognizedHoldings)} />
              <DetailPill label="创建时间" value={formatDate(job.createdAt)} />
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}

