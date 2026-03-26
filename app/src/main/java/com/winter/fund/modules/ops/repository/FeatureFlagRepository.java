package com.winter.fund.modules.ops.repository;

import com.winter.fund.modules.ops.model.FeatureFlagEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlagEntity, String> {

    List<FeatureFlagEntity> findAllByOrderByCreatedAtAsc();

    Optional<FeatureFlagEntity> findByCode(String code);
}
