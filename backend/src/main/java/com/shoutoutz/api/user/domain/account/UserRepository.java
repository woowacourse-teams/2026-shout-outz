package com.shoutoutz.api.user.domain.account;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(long id);

    Optional<User> findByHandle(String handle);
}
