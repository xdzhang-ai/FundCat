package com.winter.fund.modules.watchlist.repository;

/**
 * 自选模块仓储接口，负责定义该模块的持久化访问能力。
 */

import com.winter.fund.modules.watchlist.model.WatchlistEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository extends JpaRepository<WatchlistEntity, String> {

    List<WatchlistEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<WatchlistEntity> findByUserIdAndFundCode(String userId, String fundCode);
}
