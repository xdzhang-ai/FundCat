package com.fundcat.api.fund;

import com.fundcat.api.common.NotFoundException;
import com.fundcat.api.ops.OpsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FundService {

    private final FundRepository fundRepository;
    private final FundSnapshotRepository fundSnapshotRepository;
    private final FundEstimateRepository fundEstimateRepository;
    private final NavHistoryRepository navHistoryRepository;
    private final OpsService opsService;

    public FundService(
        FundRepository fundRepository,
        FundSnapshotRepository fundSnapshotRepository,
        FundEstimateRepository fundEstimateRepository,
        NavHistoryRepository navHistoryRepository,
        OpsService opsService
    ) {
        this.fundRepository = fundRepository;
        this.fundSnapshotRepository = fundSnapshotRepository;
        this.fundEstimateRepository = fundEstimateRepository;
        this.navHistoryRepository = navHistoryRepository;
        this.opsService = opsService;
    }

    public List<FundDtos.FundCardResponse> search(String query) {
        List<FundEntity> funds = (query == null || query.isBlank())
            ? fundRepository.findTop12ByOrderByCreatedAtDesc()
            : fundRepository.findTop12ByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(query.trim(), query.trim());
        return funds.stream().map(this::toCard).toList();
    }

    public FundDtos.FundDetailResponse getDetail(String code) {
        FundEntity fund = fundRepository.findByCode(code)
            .orElseThrow(() -> new NotFoundException("Fund not found"));
        FundSnapshotEntity snapshot = fundSnapshotRepository.findTopByFundCodeOrderByUpdatedAtDesc(code)
            .orElseThrow(() -> new NotFoundException("Fund snapshot not found"));
        FundEstimateEntity estimate = fundEstimateRepository.findTopByFundCodeOrderByEstimatedAtDesc(code)
            .orElseThrow(() -> new NotFoundException("Fund estimate not found"));
        boolean estimateReferenceEnabled = opsService.isEnabled("estimate_reference");
        List<FundDtos.TrendPoint> navHistory = navHistoryRepository.findTop30ByFundCodeOrderByTradeDateDesc(code).stream()
            .sorted(java.util.Comparator.comparing(NavHistoryEntity::getTradeDate))
            .map(point -> new FundDtos.TrendPoint(point.getTradeDate().toString(), point.getUnitNav()))
            .toList();
        double displayedNav = estimateReferenceEnabled ? estimate.getEstimatedNav() : snapshot.getUnitNav();
        double displayedGrowth = estimateReferenceEnabled ? estimate.getEstimatedGrowth() : snapshot.getDayGrowth();
        List<FundDtos.TrendPoint> estimateHistory = estimateReferenceEnabled
            ? buildEstimateHistory(snapshot.getNavDate(), snapshot.getUnitNav(), estimate.getEstimatedNav())
            : navHistory;
        List<String> comparisonLabels = List.of(fund.getCategory(), fund.getRiskLevel(), fund.getTagLine());
        return new FundDtos.FundDetailResponse(
            fund.getCode(),
            fund.getName(),
            fund.getCategory(),
            fund.getRiskLevel(),
            splitPipe(fund.getTags()),
            fund.getBenchmark(),
            round(snapshot.getUnitNav()),
            round(snapshot.getDayGrowth()),
            round(displayedNav),
            round(displayedGrowth),
            estimateReferenceEnabled && estimate.isReferenceOnly(),
            round(fund.getManagementFee()),
            round(fund.getCustodyFee()),
            round(fund.getPurchaseFee()),
            round(snapshot.getAssetSize()),
            round(snapshot.getStockRatio()),
            round(snapshot.getBondRatio()),
            splitPipe(snapshot.getTopHoldings()),
            navHistory,
            estimateHistory,
            comparisonLabels
        );
    }

    private FundDtos.FundCardResponse toCard(FundEntity fund) {
        FundSnapshotEntity snapshot = fundSnapshotRepository.findTopByFundCodeOrderByUpdatedAtDesc(fund.getCode())
            .orElseThrow(() -> new NotFoundException("Fund snapshot not found"));
        FundEstimateEntity estimate = fundEstimateRepository.findTopByFundCodeOrderByEstimatedAtDesc(fund.getCode())
            .orElseThrow(() -> new NotFoundException("Fund estimate not found"));
        boolean estimateReferenceEnabled = opsService.isEnabled("estimate_reference");
        return new FundDtos.FundCardResponse(
            fund.getCode(),
            fund.getName(),
            fund.getCategory(),
            fund.getRiskLevel(),
            splitPipe(fund.getTags()),
            fund.getBenchmark(),
            round(snapshot.getUnitNav()),
            round(snapshot.getDayGrowth()),
            round(estimateReferenceEnabled ? estimate.getEstimatedNav() : snapshot.getUnitNav()),
            round(estimateReferenceEnabled ? estimate.getEstimatedGrowth() : snapshot.getDayGrowth()),
            estimateReferenceEnabled && estimate.isReferenceOnly()
        );
    }

    private List<FundDtos.TrendPoint> buildEstimateHistory(LocalDate navDate, double unitNav, double estimateNav) {
        List<FundDtos.TrendPoint> points = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            double ratio = (i + 1) / 6.0;
            double value = unitNav + (estimateNav - unitNav) * ratio;
            points.add(new FundDtos.TrendPoint(navDate.atTime(9 + i, 30).toString(), round(value)));
        }
        return points;
    }

    private List<String> splitPipe(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(raw.split("\\|"));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
