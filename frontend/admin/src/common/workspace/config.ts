/** 后台控制台全局配置，维护导航项和各标签页标题文案。 */
import { Activity, BellDot, DatabaseZap, ListTodo, ShieldCheck } from 'lucide-react'
import type { AdminTab } from '../appTypes'

export const tabs: Array<{
  id: AdminTab
  label: string
  icon: typeof Activity
  helper: string
  path: string
}> = [
  { id: 'overview', label: '总览', icon: Activity, helper: '查看运行状态与关键指标', path: '/' },
  { id: 'flags', label: '功能开关', icon: ShieldCheck, helper: '筛选、查看并切换能力开关', path: '/flags' },
  { id: 'providers', label: '数据源', icon: DatabaseZap, helper: '查看 provider 健康状态和备注', path: '/providers' },
  { id: 'queue', label: '任务队列', icon: ListTodo, helper: '查看 OCR 导入与异步任务样例', path: '/queue' },
  { id: 'reports', label: '周报提醒', icon: BellDot, helper: '查看周报与提醒规则', path: '/reports' },
]

export function currentTab(pathname: string): AdminTab {
  if (pathname.startsWith('/flags')) return 'flags'
  if (pathname.startsWith('/providers')) return 'providers'
  if (pathname.startsWith('/queue')) return 'queue'
  if (pathname.startsWith('/reports')) return 'reports'
  return 'overview'
}

