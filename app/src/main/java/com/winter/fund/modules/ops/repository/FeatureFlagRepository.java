package com.winter.fund.modules.ops.repository;

/**
 * 运维模块仓储接口，负责定义该模块的持久化访问能力。
 */

import com.winter.fund.modules.ops.model.FeatureFlagEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlagEntity, String> {

    List<FeatureFlagEntity> findAllByOrderByCreatedAtAsc();

    Optional<FeatureFlagEntity> findByCode(String code);
}
