package com.winter.fund.modules.portfolio.repository;

/**
 * 组合与交易模块仓储接口，负责定义该模块的持久化访问能力。
 */

import com.winter.fund.modules.portfolio.model.PaperOrderEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperOrderRepository extends JpaRepository<PaperOrderEntity, String> {

    List<PaperOrderEntity> findTop12ByPortfolioIdInOrderByExecutedAtDesc(List<String> portfolioIds);
}
