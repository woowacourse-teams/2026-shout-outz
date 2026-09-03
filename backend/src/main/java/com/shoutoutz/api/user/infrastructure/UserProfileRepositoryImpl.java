package com.shoutoutz.api.user.infrastructure;

import com.shoutoutz.api.user.domain.UserProfile;
import com.shoutoutz.api.user.domain.UserProfileRepository;
import com.shoutoutz.api.user.infrastructure.jpa.UserProfileJpaRepository;
import com.shoutoutz.api.user.infrastructure.mapper.UserProfileMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserProfileRepositoryImpl implements UserProfileRepository {

    private final UserProfileJpaRepository userProfileJpaRepository;

    @Override
    public UserProfile save(UserProfile userProfile) {
        UserProfileEntity userProfileEntity = UserProfileMapper.toEntity(userProfile);
        UserProfileEntity savedUserProfileEntity = userProfileJpaRepository.save(userProfileEntity);

        return UserProfileMapper.toDomain(savedUserProfileEntity);
    }

    @Override
    public Optional<UserProfile> findByUserId(long userId) {
        return userProfileJpaRepository.findById(userId)
                .map(UserProfileMapper::toDomain);
    }
}
