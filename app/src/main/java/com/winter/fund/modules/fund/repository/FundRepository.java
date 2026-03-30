package com.winter.fund.modules.fund.repository;

/**
 * 基金模块仓储接口，负责定义该模块的持久化访问能力。
 */

import com.winter.fund.modules.fund.model.FundEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundRepository extends JpaRepository<FundEntity, String> {

    List<FundEntity> findTop12ByOrderByCreatedAtDesc();

    List<FundEntity> findTop8ByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code);

    List<FundEntity> findByCodeIn(List<String> codes);

    Optional<FundEntity> findByCode(String code);
}
