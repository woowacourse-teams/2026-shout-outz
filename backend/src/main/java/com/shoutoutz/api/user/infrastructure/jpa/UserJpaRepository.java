package com.shoutoutz.api.user.infrastructure.jpa;

import com.shoutoutz.api.user.infrastructure.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
}
