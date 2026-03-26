package com.winter.fund.modules.fund.repository;

import com.winter.fund.modules.fund.model.NavHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavHistoryRepository extends JpaRepository<NavHistoryEntity, String> {

    List<NavHistoryEntity> findTop30ByFundCodeOrderByTradeDateDesc(String fundCode);

    List<NavHistoryEntity> findByFundCodeOrderByTradeDateAsc(String fundCode);
}
