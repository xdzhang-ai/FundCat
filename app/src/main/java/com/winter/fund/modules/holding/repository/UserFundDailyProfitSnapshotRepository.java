package com.winter.fund.modules.holding.repository;

/**
 * 持仓模块仓储接口，负责定义该模块的持久化访问能力。
 */

import com.winter.fund.modules.holding.model.UserFundDailyProfitSnapshotEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFundDailyProfitSnapshotRepository extends JpaRepository<UserFundDailyProfitSnapshotEntity, String> {

    List<UserFundDailyProfitSnapshotEntity> findByUserIdOrderByTradeDateDesc(String userId);

    List<UserFundDailyProfitSnapshotEntity> findByTradeDateLessThanEqualOrderByTradeDateDesc(LocalDate tradeDate);

    Optional<UserFundDailyProfitSnapshotEntity> findTopByUserIdAndFundCodeOrderByTradeDateDesc(String userId, String fundCode);

    Optional<UserFundDailyProfitSnapshotEntity> findTopByUserIdAndFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(
        String userId,
        String fundCode,
        LocalDate tradeDate
    );

    List<UserFundDailyProfitSnapshotEntity> findByUserIdAndFundCodeAndTradeDateBetweenOrderByTradeDateAsc(
        String userId,
        String fundCode,
        LocalDate start,
        LocalDate end
    );

    List<UserFundDailyProfitSnapshotEntity> findByUserIdAndFundCodeOrderByTradeDateAsc(String userId, String fundCode);
}
