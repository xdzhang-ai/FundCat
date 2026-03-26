/** 工作台全局配置，维护导航项与各页面头部元信息。 */
import { BellRing, ChartCandlestick, LayoutDashboard, WalletCards } from 'lucide-react'
import type { PageId, WatchlistGroup } from '../appTypes'

export const navItems: Array<{
  id: PageId
  label: string
  icon: typeof LayoutDashboard
  path: string
}> = [
  { id: 'overview', label: '仪表盘', icon: LayoutDashboard, path: '/' },
  { id: 'funds', label: '基金中心', icon: ChartCandlestick, path: '/funds' },
  { id: 'holdings', label: '持仓页', icon: WalletCards, path: '/holdings' },
  { id: 'portfolio', label: '自选基金', icon: WalletCards, path: '/portfolio' },
  { id: 'automation', label: '基金定投', icon: BellRing, path: '/automation' },
]

export const watchlistGroupOrder: WatchlistGroup[] = ['全部', '成长进攻', '稳健配置', '行业主题']

export const pageMeta: Record<PageId, { eyebrow: string; title: string; description: string }> = {
  overview: {
    eyebrow: 'Research Workspace',
    title: '仪表盘',
    description: '查看研究版的核心指标、关注基金和最近动作，快速进入今天的工作上下文。',
  },
  funds: {
    eyebrow: 'Fund Center',
    title: '基金中心',
    description: '先看基金列表与搜索结果，再进入独立基金详情页做更深的走势和持仓分析。',
  },
  holdings: {
    eyebrow: 'Holding Book',
    title: '持仓页',
    description: '聚合当前持有的基金，快速查看仓位、收益和成本，并直接进入对应基金详情。',
  },
  portfolio: {
    eyebrow: 'Watchlist Book',
    title: '自选基金',
    description: '使用与持仓页一致的列表结构聚合当前自选基金，快速查看涨跌并进入基金详情。',
  },
  automation: {
    eyebrow: 'Fund SIP',
    title: '基金定投',
    description: '查看当前基金定投计划与下次执行时间，集中管理持续买入节奏。',
  },
}
