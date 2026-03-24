package com.fundcat.api.portfolio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReportEntity, String> {

    List<WeeklyReportEntity> findTop8ByUserIdOrderByCreatedAtDesc(String userId);
}
