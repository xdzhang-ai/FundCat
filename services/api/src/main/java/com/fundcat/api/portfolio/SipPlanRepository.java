package com.fundcat.api.portfolio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SipPlanRepository extends JpaRepository<SipPlanEntity, String> {

    List<SipPlanEntity> findByPortfolioIdInOrderByNextRunAtAsc(List<String> portfolioIds);
}
