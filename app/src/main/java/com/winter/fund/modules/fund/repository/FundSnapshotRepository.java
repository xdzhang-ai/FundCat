package com.winter.fund.modules.fund.repository;

import com.winter.fund.modules.fund.model.FundSnapshotEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundSnapshotRepository extends JpaRepository<FundSnapshotEntity, String> {

    Optional<FundSnapshotEntity> findTopByFundCodeOrderByUpdatedAtDesc(String fundCode);
}
