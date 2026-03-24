package com.fundcat.api.portfolio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJobEntity, String> {

    List<ImportJobEntity> findTop10ByUserIdOrderByCreatedAtDesc(String userId);
}
