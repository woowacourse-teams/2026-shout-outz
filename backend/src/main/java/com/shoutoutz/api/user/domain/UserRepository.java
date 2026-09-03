package com.shoutoutz.api.user.domain;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(long id);
}
