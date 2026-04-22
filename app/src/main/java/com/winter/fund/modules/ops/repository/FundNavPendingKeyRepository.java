package com.winter.fund.modules.ops.repository;

import com.winter.fund.modules.ops.model.FundNavPendingKeyEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundNavPendingKeyRepository extends JpaRepository<FundNavPendingKeyEntity, String> {

    Optional<FundNavPendingKeyEntity> findByFundCodeAndNavDate(String fundCode, LocalDate navDate);

    List<FundNavPendingKeyEntity> findTop500ByNavDateAndStatusOrderByFundCodeAsc(LocalDate navDate, String status);

    void deleteByNavDateBefore(LocalDate navDate);
}
