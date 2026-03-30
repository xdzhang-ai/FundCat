package com.winter.fund.modules.holding.repository;

/**
 * 持仓模块仓储接口，负责定义该模块的持久化访问能力。
 */

import com.winter.fund.modules.holding.model.UserFundOperationRecordEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFundOperationRecordRepository extends JpaRepository<UserFundOperationRecordEntity, String> {

    List<UserFundOperationRecordEntity> findTop12ByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status);

    List<UserFundOperationRecordEntity> findByUserIdAndFundCodeAndStatusAndTradeDateBetweenOrderByTradeDateAscCreatedAtAsc(
        String userId,
        String fundCode,
        String status,
        LocalDate start,
        LocalDate end
    );

    List<UserFundOperationRecordEntity> findBySipPlanIdOrderByTradeDateDescCreatedAtDesc(String sipPlanId);

    Optional<UserFundOperationRecordEntity> findTopBySipPlanIdAndStatusOrderByCreatedAtDesc(String sipPlanId, String status);

    boolean existsBySipPlanIdAndTradeDate(String sipPlanId, LocalDate tradeDate);

    List<UserFundOperationRecordEntity> findBySourceAndStatusAndTradeDateOrderByCreatedAtAsc(
        String source,
        String status,
        LocalDate tradeDate
    );

    List<UserFundOperationRecordEntity> findBySourceAndStatusAndTradeDateLessThanEqualOrderByTradeDateAscCreatedAtAsc(
        String source,
        String status,
        LocalDate tradeDate
    );

    List<UserFundOperationRecordEntity> findByStatusAndTradeDateOrderByCreatedAtAsc(String status, LocalDate tradeDate);
}
