package com.fundcat.api.fund;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundEstimateRepository extends JpaRepository<FundEstimateEntity, String> {

    Optional<FundEstimateEntity> findTopByFundCodeOrderByEstimatedAtDesc(String fundCode);
}
