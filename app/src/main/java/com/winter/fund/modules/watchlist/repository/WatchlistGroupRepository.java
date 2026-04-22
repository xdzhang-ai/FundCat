package com.winter.fund.modules.watchlist.repository;

/**
 * 自选分组仓储接口，负责定义分组选项的持久化访问能力。
 */

import com.winter.fund.modules.watchlist.model.WatchlistGroupEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistGroupRepository extends JpaRepository<WatchlistGroupEntity, String> {

    List<WatchlistGroupEntity> findByUserIdOrderByCreatedAtAsc(String userId);

    Optional<WatchlistGroupEntity> findByUserIdAndGroupName(String userId, String groupName);

    List<WatchlistGroupEntity> findByIdIn(List<String> ids);

    void deleteByUserId(String userId);
}
