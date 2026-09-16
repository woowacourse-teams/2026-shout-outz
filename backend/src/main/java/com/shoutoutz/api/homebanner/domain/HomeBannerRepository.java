package com.shoutoutz.api.homebanner.domain;

import java.util.List;
import java.util.Optional;

public interface HomeBannerRepository {

    HomeBanner save(HomeBanner homeBanner);

    Optional<HomeBanner> findById(long bannerId);

    List<HomeBanner> findAll();

    List<HomeBanner> findActive(int limit);

    Optional<HomeBanner> update(HomeBanner homeBanner);

    boolean deleteById(long bannerId);
}
