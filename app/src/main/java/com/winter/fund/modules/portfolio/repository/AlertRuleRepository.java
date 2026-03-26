package com.winter.fund.modules.portfolio.repository;

import com.winter.fund.modules.portfolio.model.AlertRuleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRuleRepository extends JpaRepository<AlertRuleEntity, String> {

    List<AlertRuleEntity> findByUserIdOrderByFundCodeAsc(String userId);
}
