package com.winter.fund.modules.portfolio.repository;

/**
 * 组合与交易模块仓储接口，负责定义该模块的持久化访问能力。
 */

import com.winter.fund.modules.portfolio.model.HoldingLotEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingLotRepository extends JpaRepository<HoldingLotEntity, String> {

    List<HoldingLotEntity> findByPortfolioIdOrderByAllocationDesc(String portfolioId);

    List<HoldingLotEntity> findByPortfolioIdIn(List<String> portfolioIds);

    Optional<HoldingLotEntity> findByPortfolioIdAndFundCode(String portfolioId, String fundCode);
}
