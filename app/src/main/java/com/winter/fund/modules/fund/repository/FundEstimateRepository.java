package com.winter.fund.modules.fund.repository;

import com.winter.fund.modules.fund.model.FundEstimateEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundEstimateRepository extends JpaRepository<FundEstimateEntity, String> {

    Optional<FundEstimateEntity> findTopByFundCodeOrderByEstimatedAtDesc(String fundCode);
}
