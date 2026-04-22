package com.winter.fund.modules.fund.repository;

/**
 * 基金模块仓储接口，负责定义该模块的持久化访问能力。
 */

import com.winter.fund.modules.fund.model.NavHistoryEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavHistoryRepository extends JpaRepository<NavHistoryEntity, String> {

    List<NavHistoryEntity> findTop30ByFundCodeOrderByTradeDateDesc(String fundCode);

    Optional<NavHistoryEntity> findTopByFundCodeOrderByTradeDateDesc(String fundCode);

    List<NavHistoryEntity> findByFundCodeOrderByTradeDateAsc(String fundCode);

    List<NavHistoryEntity> findByFundCodeAndTradeDateBetweenOrderByTradeDateAsc(String fundCode, LocalDate start, LocalDate end);

    List<NavHistoryEntity> findByTradeDateOrderByFundCodeAsc(LocalDate tradeDate);

    Optional<NavHistoryEntity> findTopByFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(String fundCode, LocalDate tradeDate);

    Optional<NavHistoryEntity> findByFundCodeAndTradeDate(String fundCode, LocalDate tradeDate);
}
