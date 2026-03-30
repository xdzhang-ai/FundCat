package com.winter.fund.modules.fund.repository;

/**
 * 基金模块仓储接口，负责定义该模块的持久化访问能力。
 */

import com.winter.fund.modules.fund.model.FundSnapshotEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundSnapshotRepository extends JpaRepository<FundSnapshotEntity, String> {

    Optional<FundSnapshotEntity> findTopByFundCodeOrderByUpdatedAtDesc(String fundCode);
}
