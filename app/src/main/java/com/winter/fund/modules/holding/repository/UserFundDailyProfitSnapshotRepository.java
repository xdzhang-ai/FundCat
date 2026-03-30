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
}
