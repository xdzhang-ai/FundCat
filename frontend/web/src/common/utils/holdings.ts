/** 持仓领域纯函数，负责持仓查找、草稿合并和组合市值重算。 */
import type { HoldingLot, PortfolioSummary } from '@fundcat/contracts'
import type { LocalHoldingDraft } from '../appTypes'

export function holdingCodesFromPortfolios(portfolios: PortfolioSummary[] | null) {
  return new Set(portfolios?.flatMap((portfolio) => portfolio.holdings.map((holding) => holding.fundCode)) ?? [])
}

export function dedupeHoldingDrafts(drafts: LocalHoldingDraft[]) {
  const seen = new Set<string>()
  return drafts.filter((draft) => {
    const key = `${draft.portfolioId}:${draft.fundCode}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

export function findHoldingTarget(code: string, portfolios: PortfolioSummary[] | null) {
  if (!portfolios) return null
  for (const portfolio of portfolios) {
    const holding = portfolio.holdings.find((item) => item.fundCode === code)
    if (holding) {
      return { portfolioId: portfolio.id, holding }
    }
  }
  return null
}

export function mergeHoldingDraftsIntoPortfolios(portfolios: PortfolioSummary[] | null, drafts: LocalHoldingDraft[]) {
  if (!portfolios) return portfolios
  if (drafts.length === 0) return portfolios
  return portfolios.map((portfolio) => {
    const portfolioDrafts = dedupeHoldingDrafts(drafts.filter((draft) => draft.portfolioId === portfolio.id))
    if (portfolioDrafts.length === 0) return portfolio
    const overriddenCodes = new Set(portfolioDrafts.map((draft) => draft.fundCode))
    return recalculatePortfolio(portfolio, [
      ...portfolioDrafts,
      ...portfolio.holdings.filter((holding) => !overriddenCodes.has(holding.fundCode)),
    ])
  })
}

export function recalculatePortfolio(portfolio: PortfolioSummary, nextHoldings: HoldingLot[]): PortfolioSummary {
  const marketValue = nextHoldings.reduce((sum, holding) => sum + holding.currentValue, 0)
  const totalPnl = nextHoldings.reduce((sum, holding) => sum + holding.pnl, 0)
  return {
    ...portfolio,
    marketValue: Number(marketValue.toFixed(4)),
    totalPnl: Number(totalPnl.toFixed(4)),
    holdings: nextHoldings.map((holding) => ({
      ...holding,
      allocation: marketValue === 0 ? 0 : Number((holding.currentValue / marketValue).toFixed(4)),
    })),
  }
}
