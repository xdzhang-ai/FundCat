package com.winter.fund.modules.portfolio.repository;

import com.winter.fund.modules.portfolio.model.HoldingLotEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingLotRepository extends JpaRepository<HoldingLotEntity, String> {

    List<HoldingLotEntity> findByPortfolioIdOrderByAllocationDesc(String portfolioId);

    List<HoldingLotEntity> findByPortfolioIdIn(List<String> portfolioIds);

    Optional<HoldingLotEntity> findByPortfolioIdAndFundCode(String portfolioId, String fundCode);
}
