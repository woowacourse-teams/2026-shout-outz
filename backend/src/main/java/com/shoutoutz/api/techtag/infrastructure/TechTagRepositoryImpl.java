package com.shoutoutz.api.techtag.infrastructure;

import com.shoutoutz.api.techtag.domain.TechTag;
import com.shoutoutz.api.techtag.domain.TechTagRepository;
import com.shoutoutz.api.techtag.infrastructure.jpa.TechTagJpaRepository;
import com.shoutoutz.api.techtag.infrastructure.mapper.TechTagMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TechTagRepositoryImpl implements TechTagRepository {

    private final TechTagJpaRepository techTagJpaRepository;

    @Override
    public List<TechTag> findAllActive() {
        return TechTagMapper.toDomains(techTagJpaRepository.findAllActive());
    }

    @Override
    public List<TechTag> findAllActiveByKeyword(String keyword) {
        return TechTagMapper.toDomains(techTagJpaRepository.findAllActiveByKeyword(keyword));
    }

    @Override
    public List<TechTag> findAllActiveByIds(List<Long> ids) {
        return TechTagMapper.toDomains(techTagJpaRepository.findAllByActiveTrueAndIdIn(ids));
    }
}
