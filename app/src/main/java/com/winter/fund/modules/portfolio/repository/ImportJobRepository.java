package com.winter.fund.modules.portfolio.repository;

import com.winter.fund.modules.portfolio.model.ImportJobEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJobEntity, String> {

    List<ImportJobEntity> findTop10ByUserIdOrderByCreatedAtDesc(String userId);
}
