package com.winter.fund.modules.portfolio.repository;

import com.winter.fund.modules.portfolio.model.SipPlanEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SipPlanRepository extends JpaRepository<SipPlanEntity, String> {

    List<SipPlanEntity> findByPortfolioIdInOrderByNextRunAtAsc(List<String> portfolioIds);
}
