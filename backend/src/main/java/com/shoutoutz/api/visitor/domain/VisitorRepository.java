package com.shoutoutz.api.visitor.domain;

import java.util.Optional;

public interface VisitorRepository {
    Visitor save(Visitor visitor);

    //TODO: 지울것. 조회시 대상이 없는 경우(optional인 경우)는 find
    Optional<Visitor> findById(long id);

    //TODO: 지울것. 조회시 대상이 무조건 있는 경우((optional이 아닌 경우)는 get
    Visitor getById(long id);
}
