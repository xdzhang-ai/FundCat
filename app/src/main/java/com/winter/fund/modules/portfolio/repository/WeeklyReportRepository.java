package com.winter.fund.modules.portfolio.repository;

import com.winter.fund.modules.portfolio.model.WeeklyReportEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReportEntity, String> {

    List<WeeklyReportEntity> findTop8ByUserIdOrderByCreatedAtDesc(String userId);
}
