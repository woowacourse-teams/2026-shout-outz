package com.shoutoutz.api.homebanner.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.homebanner.domain.BannerDestinationType;
import com.shoutoutz.api.homebanner.domain.BannerLinkType;
import com.shoutoutz.api.homebanner.domain.BannerTargetType;
import com.shoutoutz.api.homebanner.domain.HomeBanner;
import com.shoutoutz.api.homebanner.domain.HomeBannerRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class HomeBannerRepositoryIntegrationTest {

    @Autowired
    private HomeBannerRepository homeBannerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long adminId;
    private long mediaId;

    @BeforeEach
    void setUp() {
        adminId = insertAdmin();
        mediaId = insertReadyMedia(adminId);
    }

    @Test
    void 배너를_저장하고_조회한다() {
        HomeBanner saved = homeBannerRepository.save(targetBanner(2, true));

        HomeBanner found = homeBannerRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getMediaId()).isEqualTo(mediaId);
        assertThat(found.getTargetType()).isEqualTo(BannerTargetType.PROJECT);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void 활성_배너를_표시_순서와_ID순으로_전체_조회한다() {
        HomeBanner first = homeBannerRepository.save(targetBanner(1, true));
        HomeBanner second = homeBannerRepository.save(targetBanner(1, true));
        HomeBanner third = homeBannerRepository.save(targetBanner(2, true));
        homeBannerRepository.save(targetBanner(0, false));

        List<HomeBanner> banners = homeBannerRepository.findAllActive();

        assertThat(banners).extracting(HomeBanner::getId)
                .containsExactly(first.getId(), second.getId(), third.getId());
    }

    @Test
    void 배너를_URL_방식으로_수정하고_삭제한다() {
        HomeBanner saved = homeBannerRepository.save(targetBanner(0, true));
        HomeBanner changed = saved.update(
                mediaId,
                BannerDestinationType.URL,
                null,
                null,
                BannerLinkType.EXTERNAL_URL,
                "https://example.com/promotion",
                3,
                false
        );

        HomeBanner updated = homeBannerRepository.update(changed).orElseThrow();
        boolean deleted = homeBannerRepository.deleteById(saved.getId());

        assertThat(updated.getDestinationType()).isEqualTo(BannerDestinationType.URL);
        assertThat(updated.getDisplayOrder()).isEqualTo(3);
        assertThat(updated.isActive()).isFalse();
        assertThat(deleted).isTrue();
        assertThat(homeBannerRepository.findById(saved.getId())).isEmpty();
    }

    private HomeBanner targetBanner(int displayOrder, boolean active) {
        return HomeBanner.create(
                mediaId,
                BannerDestinationType.TARGET,
                BannerTargetType.PROJECT,
                10L,
                null,
                null,
                displayOrder,
                active,
                adminId
        );
    }

    private long insertAdmin() {
        String handle = "admin-" + UUID.randomUUID().toString().substring(0, 12);
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (handle, role) VALUES (?, 'ADMIN') RETURNING id",
                Long.class,
                handle
        );
    }

    private long insertReadyMedia(long uploadedBy) {
        String key = "media/home-banner/" + UUID.randomUUID();
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO media_metadata (
                            uploaded_by, purpose, s3_key, mime_type,
                            size_bytes, status, expires_at, uploaded_at
                        ) VALUES (?, 'HOME_BANNER', ?, 'image/webp', 1024, 'READY', now(), now())
                        RETURNING id
                        """,
                Long.class,
                uploadedBy,
                key
        );
    }
}
