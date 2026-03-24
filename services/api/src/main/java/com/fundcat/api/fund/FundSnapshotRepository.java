package com.fundcat.api.fund;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundSnapshotRepository extends JpaRepository<FundSnapshotEntity, String> {

    Optional<FundSnapshotEntity> findTopByFundCodeOrderByUpdatedAtDesc(String fundCode);
}
