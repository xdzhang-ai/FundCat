package com.winter.fund.modules.portfolio.repository;

import com.winter.fund.modules.portfolio.model.WatchlistEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository extends JpaRepository<WatchlistEntity, String> {

    List<WatchlistEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<WatchlistEntity> findByUserIdAndFundCode(String userId, String fundCode);
}
