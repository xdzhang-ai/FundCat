package com.fundcat.api.portfolio;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperOrderRepository extends JpaRepository<PaperOrderEntity, String> {

    List<PaperOrderEntity> findTop12ByPortfolioIdInOrderByExecutedAtDesc(List<String> portfolioIds);
}
