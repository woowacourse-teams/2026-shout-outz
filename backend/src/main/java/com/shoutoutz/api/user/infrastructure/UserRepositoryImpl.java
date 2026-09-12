package com.shoutoutz.api.user.infrastructure;

import com.shoutoutz.api.user.domain.User;
import com.shoutoutz.api.user.domain.UserRepository;
import com.shoutoutz.api.user.infrastructure.jpa.UserJpaRepository;
import com.shoutoutz.api.user.infrastructure.mapper.UserMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        UserEntity userEntity = UserMapper.toEntity(user);
        UserEntity savedUserEntity = userJpaRepository.save(userEntity);

        return UserMapper.toDomain(savedUserEntity);
    }

    @Override
    public Optional<User> findById(long id) {
        return userJpaRepository.findById(id)
                .map(UserMapper::toDomain);
    }
}
