package com.fundcat.api.portfolio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository extends JpaRepository<WatchlistEntity, String> {

    List<WatchlistEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<WatchlistEntity> findByUserIdAndFundCode(String userId, String fundCode);
}
