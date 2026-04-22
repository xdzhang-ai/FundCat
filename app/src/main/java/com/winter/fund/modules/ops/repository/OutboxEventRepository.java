package com.winter.fund.modules.ops.repository;

import com.winter.fund.modules.ops.model.OutboxEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, String> {

    List<OutboxEventEntity> findTop200ByStatusOrderByCreatedAtAsc(String status);
}

