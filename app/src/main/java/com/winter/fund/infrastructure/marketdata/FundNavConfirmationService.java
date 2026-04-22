package com.winter.fund.infrastructure.marketdata;

import com.winter.fund.modules.holding.service.HoldingRebuildTarget;
import com.winter.fund.modules.holding.service.HoldingService;
import com.winter.fund.modules.sip.service.SipService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FundNavConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(FundNavConfirmationService.class);

    private final HoldingService holdingService;
    private final SipService sipService;

    public FundNavConfirmationService(HoldingService holdingService, SipService sipService) {
        this.holdingService = holdingService;
        this.sipService = sipService;
    }

    /**
     * 处理一批基金净值 ready 消息。
     * RocketMQ 批量消息落地后可以直接调用这里，业务上仍按基金粒度逐个确认，避免把不同基金的事务耦合在一起。
     */
    public void handleConfirmedNavBatch(FundNavReadyBatchMessage message) {
        for (String fundCode : message.fundCodes()) {
            handleConfirmedNav(fundCode, message.navDate());
        }
    }

    @Transactional
    public void handleConfirmedNav(String fundCode, java.time.LocalDate navDate) {
        List<HoldingRebuildTarget> rebuildTargets = new ArrayList<>();
        List<HoldingRebuildTarget> manualTargets = holdingService.confirmPendingManualOperations(fundCode, navDate);
        List<HoldingRebuildTarget> sipTargets = sipService.confirmPendingSipOperations(fundCode, navDate);
        rebuildTargets.addAll(manualTargets);
        rebuildTargets.addAll(sipTargets);

        List<HoldingRebuildTarget> mergedTargets = HoldingRebuildTarget.merge(rebuildTargets);
        for (HoldingRebuildTarget target : mergedTargets) {
            holdingService.rebuildSnapshotsFrom(target.userId(), target.fundCode(), target.earliestTradeDate());
        }
        // 白天的 intraday 缓存只服务盘中展示；当晚正式净值落仓完成后，应让第二天重新从日终快照起算。
        holdingService.evictIntradayStates(fundCode, navDate);

        log.info(
            "Fund nav confirmation handled, fundCode={}, navDate={}, confirmedManualRecords={}, confirmedSipRecords={}, rebuildTargets={}",
            fundCode,
            navDate,
            manualTargets.size(),
            sipTargets.size(),
            mergedTargets.size()
        );
    }
}
