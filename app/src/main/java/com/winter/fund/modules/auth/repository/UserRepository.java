package com.winter.fund.modules.auth.repository;

import com.winter.fund.modules.auth.model.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByUsername(String username);
}
