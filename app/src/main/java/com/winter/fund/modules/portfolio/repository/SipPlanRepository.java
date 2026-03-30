package com.winter.fund.modules.portfolio.repository;

/**
 * 组合与交易模块仓储接口，负责定义该模块的持久化访问能力。
 */

import com.winter.fund.modules.portfolio.model.SipPlanEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SipPlanRepository extends JpaRepository<SipPlanEntity, String> {

    List<SipPlanEntity> findByPortfolioIdInOrderByNextRunAtAsc(List<String> portfolioIds);

    List<SipPlanEntity> findByUserIdOrderByNextRunAtAsc(String userId);

    Optional<SipPlanEntity> findByIdAndUserId(String id, String userId);

    Optional<SipPlanEntity> findByUserIdAndFundCodeAndStatusNot(String userId, String fundCode, String status);
}
