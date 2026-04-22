package com.winter.fund.modules.fund.repository;

/**
 * 基金区间涨幅仓储接口，负责读取基金在某个净值日的涨幅摘要。
 */

import com.winter.fund.modules.fund.model.FundNavGrowthEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundNavGrowthRepository extends JpaRepository<FundNavGrowthEntity, String> {

    Optional<FundNavGrowthEntity> findTopByFundCodeOrderByNavDateDesc(String fundCode);
}
