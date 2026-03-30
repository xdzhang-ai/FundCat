package com.winter.fund.modules.holding.repository;

/**
 * 持仓模块仓储接口，负责定义该模块的持久化访问能力。
 */

import com.winter.fund.modules.holding.model.UserFundHoldingEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFundHoldingRepository extends JpaRepository<UserFundHoldingEntity, String> {

    List<UserFundHoldingEntity> findByUserIdOrderByMarketValueDesc(String userId);

    Optional<UserFundHoldingEntity> findByUserIdAndFundCode(String userId, String fundCode);
}
