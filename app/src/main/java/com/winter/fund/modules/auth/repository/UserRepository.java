package com.winter.fund.modules.auth.repository;

/**
 * 认证模块仓储接口，负责定义该模块的持久化访问能力。
 */

import com.winter.fund.modules.auth.model.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByUsername(String username);
}
