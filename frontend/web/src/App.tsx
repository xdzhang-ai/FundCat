/** 应用壳层，负责承接工作台布局、全局提示和页面路由装配。 */
import { LogOut, Sparkles } from 'lucide-react'
import type { ReactNode } from 'react'
import { Navigate, NavLink, Route, Routes } from 'react-router-dom'
import { LoadingScreen } from './common/components/WebUi'
import { navItems } from './common/workspace/config'
import { useWorkspaceApp } from './common/workspace/useWorkspaceApp'
import { formatCurrency } from './common/utils/fundInsights'
import { AuthPage } from './modules/auth/pages/AuthPage'
import { AutomationPage } from './modules/automation/pages/AutomationPage'
import { SipPlanDetailPage } from './modules/automation/pages/SipPlanDetailPage'
import { FundDetailPage } from './modules/fund-detail/pages/FundDetailPage'
import { FundsPage } from './modules/funds/pages/FundsPage'
import { HoldingsPage } from './modules/holdings/pages/HoldingsPage'
import { OverviewPage } from './modules/overview/pages/OverviewPage'
import { PortfolioPage } from './modules/portfolio/pages/PortfolioPage'

function renderRouteContent(isReady: boolean, isRouteLoading: boolean, showRouteLoading: boolean, element: ReactNode) {
  if (isReady) return element
  if (isRouteLoading && showRouteLoading) return <LoadingScreen />
  return null
}

function App() {
  const workspace = useWorkspaceApp()

  if (workspace.screen.status === 'loading') {
    return <LoadingScreen />
  }

  if (workspace.screen.status === 'auth' || workspace.screen.status === 'error') {
    return (
      <AuthPage
        username={workspace.username}
        password={workspace.password}
        errorMessage={workspace.screen.status === 'error' ? workspace.screen.message : undefined}
        onUsernameChange={workspace.setUsername}
        onPasswordChange={workspace.setPassword}
        onSubmit={workspace.handleLogin}
      />
    )
  }

  return (
    <div className="mx-auto min-h-screen max-w-[1600px] px-4 py-4 lg:px-6">
      <header className="mb-6 border-b border-white/8 px-1 pb-4">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-[color:var(--fc-color-accent)] text-slate-950">
              <Sparkles className="h-7 w-7" />
            </div>
            <div>
              <p className="text-sm uppercase tracking-[0.28em] text-[color:var(--fc-color-accent)]">FundCat</p>
              <p className="mt-1 text-sm text-slate-400">Research workspace for simulated fund decisions</p>
            </div>
          </div>
        </div>
      </header>

      <div className="grid gap-5 lg:grid-cols-[236px_minmax(0,1fr)]">
        <aside className="rounded-[1.75rem] border border-white/8 bg-slate-950/70 p-4 shadow-[var(--fc-shadow-card)] backdrop-blur-xl lg:sticky lg:top-4 lg:h-[calc(100vh-7.5rem)]">
          <div className="flex h-full flex-col">
            <nav className="grid gap-2">
              {navItems.map(({ id, label, icon: Icon, path }) => (
                <NavLink
                  key={id}
                  to={path}
                  onClick={(event) => {
                    if (workspace.activePage === id) {
                      event.preventDefault()
                      workspace.navigateToPage(id)
                    }
                  }}
                  className={({ isActive }) =>
                    `inline-flex items-center gap-3 rounded-[1rem] border px-3 py-2.5 text-left text-sm transition ${
                      isActive
                        ? 'border-[color:var(--fc-color-accent)]/50 bg-[color:var(--fc-color-accent)]/10 text-white'
                        : 'border-white/8 bg-white/5 text-slate-300 hover:border-white/20 hover:text-white'
                    }`
                  }
                >
                  <Icon className="h-4 w-4" />
                  <span>{label}</span>
                </NavLink>
              ))}
            </nav>

            <button
              onClick={() => void workspace.handleLogout()}
              className="mt-auto inline-flex items-center justify-center gap-2 rounded-full border border-white/10 px-4 py-2.5 text-sm text-slate-300 transition hover:border-white/20 hover:text-white"
            >
              <LogOut className="h-4 w-4" />
              退出
            </button>
          </div>
        </aside>

        <main className="min-w-0 space-y-6">
          <header className="rounded-[2rem] border border-white/8 bg-[color:var(--fc-color-surface-glass)] px-5 py-6 shadow-[var(--fc-shadow-card)] backdrop-blur-xl">
            <p className="text-sm uppercase tracking-[0.24em] text-[color:var(--fc-color-accent)]">{workspace.currentPageMeta.eyebrow}</p>
            <div className="mt-4 flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
              <div>
                <h2 className="font-[var(--fc-font-display)] text-4xl font-semibold text-white">{workspace.currentPageMeta.title}</h2>
                <p className="mt-3 max-w-3xl text-slate-300">{workspace.currentPageMeta.description}</p>
                {workspace.activePage === 'holdings' ? (
                  <div className="mt-5 max-w-[340px] rounded-[1.45rem] border border-white/8 bg-[linear-gradient(135deg,rgba(243,186,47,0.12),rgba(255,255,255,0.04),rgba(8,12,20,0.2))] px-5 py-4">
                    <p className="text-xs uppercase tracking-[0.18em] text-[color:var(--fc-color-accent)]/80">组合市值</p>
                    <p className="mt-3 font-[var(--fc-font-display)] text-3xl text-white">{formatCurrency(workspace.holdingsMarketValue)}</p>
                    <p className="mt-2 text-xs text-slate-500">汇总当前组合下所有基金持仓市值</p>
                  </div>
                ) : null}
              </div>
              {workspace.data.selectedFund ? (
                <div className="flex flex-wrap gap-2">
                  <span className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-300">
                    焦点基金 {workspace.effectiveSelectedFund?.code ?? workspace.data.selectedFund.code}
                  </span>
                  <span className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-300">
                    {(workspace.effectiveSelectedFund ?? workspace.data.selectedFund).referenceOnly ? '实时涨跌参考' : '前一交易日涨跌'}
                  </span>
                </div>
              ) : null}
            </div>
          </header>

          {workspace.actionMessage ? (
            <div className={`rounded-3xl border px-4 py-3 text-sm ${workspace.actionMessageToneClass}`}>
              {workspace.actionMessage}
            </div>
          ) : null}

          <Routes>
            <Route
              path="/"
              element={renderRouteContent(
                Boolean(
                  workspace.data.overviewHeroMetrics &&
                    workspace.data.overviewWatchlistPulse &&
                    workspace.data.overviewRecentActions &&
                    workspace.data.overviewSipPlanDigests &&
                    workspace.data.selectedFund,
                ),
                workspace.isRouteLoading,
                workspace.showRouteLoading,
                <OverviewPage
                  heroMetrics={workspace.data.overviewHeroMetrics!.metrics}
                  watchlistPulse={workspace.data.overviewWatchlistPulse!.items}
                  recentActions={workspace.data.overviewRecentActions!.items}
                  sipPlanDigests={workspace.data.overviewSipPlanDigests!.items}
                />,
              )}
            />
            <Route
              path="/funds"
              element={renderRouteContent(
                Boolean(workspace.effectiveFunds),
                workspace.isRouteLoading,
                workspace.showRouteLoading,
                <FundsPage
                  search={workspace.search}
                  funds={workspace.effectiveFunds!}
                  suggestions={workspace.effectiveFundSuggestions}
                  onSearchChange={workspace.handleSearch}
                  onAddToWatchlist={(fund) => void workspace.handleAddWatchlistFromFunds(fund)}
                  watchlistPickerFundCode={workspace.pendingWatchlistSelection?.code ?? null}
                  watchlistPickerGroups={workspace.pendingWatchlistGroups}
                  watchlistGroupOptions={workspace.watchlistGroupOrder}
                  onToggleWatchlistGroup={workspace.togglePendingWatchlistGroup}
                  onCancelWatchlistPicker={() => {
                    workspace.setPendingWatchlistSelection(null)
                    workspace.setPendingWatchlistGroups(['全部'])
                  }}
                  onConfirmWatchlistPicker={() => void workspace.confirmAddWatchlist()}
                  onSelectFund={workspace.openFundDetail}
                />,
              )}
            />
            <Route
              path="/funds/:fundCode"
              element={renderRouteContent(
                Boolean(workspace.effectiveFunds && workspace.effectiveSelectedFund && workspace.effectivePortfolios && workspace.data.orders && workspace.effectiveSipPlans),
                workspace.isRouteLoading,
                workspace.showRouteLoading,
                workspace.effectiveFunds && workspace.effectiveSelectedFund ? (
                  <FundDetailPage
                    portfolios={workspace.effectivePortfolios ?? []}
                    orders={workspace.data.orders ?? []}
                    sipPlans={workspace.effectiveSipPlans ?? []}
                    funds={workspace.effectiveFunds}
                    selectedFund={workspace.effectiveSelectedFund}
                    isFlagEnabled={workspace.isFlagEnabled}
                    onBack={() => workspace.navigate(workspace.fundDetailBackPath)}
                    onQuickAction={(kind) => void workspace.runQuickAction(kind)}
                    watchlistPickerOpen={workspace.pendingWatchlistSelection?.code === workspace.effectiveSelectedFund.code}
                    watchlistPickerGroups={workspace.pendingWatchlistGroups}
                    watchlistGroupOptions={workspace.watchlistGroupOrder}
                    onToggleWatchlistGroup={workspace.togglePendingWatchlistGroup}
                    onCancelWatchlistPicker={() => {
                      workspace.setPendingWatchlistSelection(null)
                      workspace.setPendingWatchlistGroups(['全部'])
                    }}
                    onConfirmWatchlistPicker={() => void workspace.confirmAddWatchlist()}
                    holdingInputOpen={workspace.pendingHoldingInput?.code === workspace.effectiveSelectedFund.code}
                    holdingFormMode={workspace.pendingHoldingInput?.mode ?? 'add'}
                    holdingAmount={workspace.pendingHoldingAmount}
                    holdingPnl={workspace.pendingHoldingPnl}
                    onHoldingAmountChange={workspace.setPendingHoldingAmount}
                    onHoldingPnlChange={workspace.setPendingHoldingPnl}
                    onCancelHoldingInput={() => {
                      workspace.setPendingHoldingInput(null)
                      workspace.setPendingHoldingAmount('')
                      workspace.setPendingHoldingPnl('')
                    }}
                    onConfirmHoldingInput={workspace.confirmAddHolding}
                    sipPlanExists={workspace.sipPlanCodeSet.has(workspace.effectiveSelectedFund.code)}
                    sipInputOpen={workspace.pendingSipInput?.code === workspace.effectiveSelectedFund.code}
                    sipCadence={workspace.pendingSipCadence}
                    sipWeekday={workspace.pendingSipWeekday}
                    sipMonthDay={workspace.pendingSipMonthDay}
                    sipAmount={workspace.pendingSipAmount}
                    onSipCadenceChange={workspace.setPendingSipCadence}
                    onSipWeekdayChange={workspace.setPendingSipWeekday}
                    onSipMonthDayChange={workspace.setPendingSipMonthDay}
                    onSipAmountChange={workspace.setPendingSipAmount}
                    onCancelSipInput={() => {
                      workspace.setPendingSipInput(null)
                      workspace.setPendingSipCadence('DAILY')
                      workspace.setPendingSipWeekday('1')
                      workspace.setPendingSipMonthDay('1')
                      workspace.setPendingSipAmount('')
                    }}
                    onConfirmSipInput={() => void workspace.confirmAddSip()}
                    onOpenSipPlan={() => {
                      if (workspace.effectiveSelectedFund) {
                        workspace.handleOpenSipPlan(workspace.effectiveSelectedFund.code)
                      }
                    }}
                    onJumpToHoldings={() => workspace.navigate('/holdings')}
                  />
                ) : null,
              )}
            />
            <Route
              path="/holdings"
              element={renderRouteContent(
                Boolean(workspace.effectivePortfolios && workspace.effectiveFunds),
                workspace.isRouteLoading,
                workspace.showRouteLoading,
                <HoldingsPage portfolios={workspace.effectivePortfolios!} funds={workspace.effectiveFunds!} onOpenFund={workspace.openFundDetail} />,
              )}
            />
            <Route
              path="/portfolio"
              element={renderRouteContent(
                Boolean(workspace.data.watchlist && workspace.effectiveFunds),
                workspace.isRouteLoading,
                workspace.showRouteLoading,
                <PortfolioPage
                  watchlist={workspace.data.watchlist!}
                  funds={workspace.effectiveFunds!}
                  groupSelections={workspace.watchlistGroups}
                  onAssignGroup={workspace.assignWatchlistGroup}
                  onOpenFund={workspace.openFundDetail}
                  onRemoveFund={(code) => workspace.handleRemoveWatchlist(code)}
                />,
              )}
            />
            <Route
              path="/automation"
              element={renderRouteContent(
                Boolean(workspace.effectiveSipPlans),
                workspace.isRouteLoading,
                workspace.showRouteLoading,
                <AutomationPage sipPlans={workspace.effectiveSipPlans!} onOpenPlan={(sipPlanId) => workspace.navigate(`/automation/${sipPlanId}`)} />,
              )}
            />
            <Route
              path="/automation/:sipPlanId"
              element={renderRouteContent(
                Boolean(workspace.effectiveSipPlans && workspace.routeSipPlanId && workspace.effectiveSipPlans.find((plan) => plan.id === workspace.routeSipPlanId)),
                workspace.isRouteLoading,
                workspace.showRouteLoading,
                workspace.effectiveSipPlans && workspace.routeSipPlanId
                  ? (() => {
                      const matchedPlan = workspace.effectiveSipPlans.find((plan) => plan.id === workspace.routeSipPlanId)
                      return matchedPlan ? (
                        <SipPlanDetailPage
                          plan={matchedPlan}
                          onBack={() => workspace.navigate('/automation')}
                          onEditPlan={workspace.handleEditSipPlan}
                          onTogglePlan={workspace.handleToggleSipPlan}
                          onStopPlan={workspace.handleStopSipPlan}
                        />
                      ) : null
                    })()
                  : null,
              )}
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  )
}

export default App
