package com.fundcat.api.fund;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavHistoryRepository extends JpaRepository<NavHistoryEntity, String> {

    List<NavHistoryEntity> findTop30ByFundCodeOrderByTradeDateDesc(String fundCode);
}
