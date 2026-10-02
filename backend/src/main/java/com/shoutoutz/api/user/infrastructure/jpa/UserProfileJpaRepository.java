package com.shoutoutz.api.user.infrastructure.jpa;

import com.shoutoutz.api.user.infrastructure.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileJpaRepository extends JpaRepository<UserProfileEntity, Long> {
}
