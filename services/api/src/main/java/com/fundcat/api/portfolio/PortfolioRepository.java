package com.fundcat.api.portfolio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioRepository extends JpaRepository<PortfolioEntity, String> {

    List<PortfolioEntity> findByUserIdOrderByCreatedAtAsc(String userId);
}
