package com.shoutoutz.api.visitor.infrastructure;

import com.shoutoutz.api.visitor.domain.Visitor;
import com.shoutoutz.api.visitor.domain.VisitorRepository;
import com.shoutoutz.api.visitor.infrastructure.jpa.VisitorJpaRepository;
import com.shoutoutz.api.visitor.infrastructure.mapper.VisitorMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class VisitorRepositoryImpl implements VisitorRepository {

    private final VisitorJpaRepository visitorJpaRepository;

    @Override
    public Visitor save(Visitor visitor) {
        VisitorEntity visitorEntity = VisitorMapper.toEntity(visitor);
        final VisitorEntity savedVisitorEntity = visitorJpaRepository.save(visitorEntity);

        return  VisitorMapper.toDomain(savedVisitorEntity);
    }

    /**
     * TODO: 예시코드 필요하면 채우기
     * @param id
     * @return
     */
    @Override
    public Optional<Visitor> findById(long id) {
        return Optional.empty();
    }

    @Override
    public Visitor getById(long id) {
        return null;
    }
}
