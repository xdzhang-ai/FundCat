package com.fundcat.api.fund;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundRepository extends JpaRepository<FundEntity, String> {

    List<FundEntity> findTop12ByOrderByCreatedAtDesc();

    List<FundEntity> findTop12ByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code);

    Optional<FundEntity> findByCode(String code);
}
