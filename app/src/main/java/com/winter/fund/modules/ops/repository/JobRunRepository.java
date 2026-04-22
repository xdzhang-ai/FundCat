package com.winter.fund.modules.ops.repository;

import com.winter.fund.modules.ops.model.JobRunEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRunRepository extends JpaRepository<JobRunEntity, String> {

    Optional<JobRunEntity> findByJobCodeAndRunKey(String jobCode, String runKey);

    List<JobRunEntity> findTop50ByOrderByStartedAtDesc();
}

