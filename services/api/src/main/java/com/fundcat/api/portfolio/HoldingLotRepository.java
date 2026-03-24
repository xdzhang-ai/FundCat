package com.fundcat.api.portfolio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingLotRepository extends JpaRepository<HoldingLotEntity, String> {

    List<HoldingLotEntity> findByPortfolioIdOrderByAllocationDesc(String portfolioId);
}
