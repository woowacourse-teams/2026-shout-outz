package com.shoutoutz.api.user.domain.account;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(long id);

    /**
     * 여러 사용자를 한 번에 읽는다. 없는 사용자는 결과에서 빠지고, 순서는 보장하지 않는다.
     */
    List<User> findAllByIds(Collection<Long> ids);

    Optional<User> findByHandle(String handle);

    boolean existsByHandle(String handle);
}
