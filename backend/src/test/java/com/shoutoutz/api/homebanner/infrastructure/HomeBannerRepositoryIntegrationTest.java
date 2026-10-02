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
    private long projectId;
    private String projectSlug;

    @BeforeEach
    void setUp() {
        adminId = insertAdmin();
        mediaId = insertReadyMedia(adminId);
        projectSlug = "banner-" + UUID.randomUUID().toString().substring(0, 12);
        projectId = insertProject(adminId, projectSlug);
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
    void 프로젝트_배너는_저장_결과와_조회_결과에_프로젝트_slug를_함께_담는다() {
        HomeBanner saved = homeBannerRepository.save(targetBanner(0, true));

        assertThat(saved.getTargetSlug()).isEqualTo(projectSlug);
        assertThat(homeBannerRepository.findById(saved.getId()).orElseThrow().getTargetSlug()).isEqualTo(projectSlug);
        assertThat(homeBannerRepository.findAll()).extracting(HomeBanner::getTargetSlug).containsOnly(projectSlug);
        assertThat(homeBannerRepository.findAllActive()).extracting(HomeBanner::getTargetSlug)
                .containsOnly(projectSlug);
    }

    @Test
    void 뉴스_배너는_같은_ID의_프로젝트가_있어도_slug를_담지_않는다() {
        HomeBanner saved = homeBannerRepository.save(HomeBanner.create(
                mediaId,
                BannerDestinationType.TARGET,
                BannerTargetType.NEWS,
                projectId,
                null,
                null,
                0,
                true,
                adminId
        ));

        assertThat(saved.getTargetSlug()).isNull();
        assertThat(homeBannerRepository.findById(saved.getId()).orElseThrow().getTargetSlug()).isNull();
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
    void 대상_프로젝트를_바꿔_수정하면_바뀐_프로젝트의_slug를_돌려준다() {
        String otherSlug = "banner-" + UUID.randomUUID().toString().substring(0, 12);
        long otherProjectId = insertProject(adminId, otherSlug);
        HomeBanner saved = homeBannerRepository.save(targetBanner(0, true));
        HomeBanner changed = saved.update(
                mediaId,
                BannerDestinationType.TARGET,
                BannerTargetType.PROJECT,
                otherProjectId,
                null,
                null,
                0,
                true
        );

        HomeBanner updated = homeBannerRepository.update(changed).orElseThrow();

        assertThat(updated.getTargetId()).isEqualTo(otherProjectId);
        assertThat(updated.getTargetSlug()).isEqualTo(otherSlug);
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
        assertThat(updated.getTargetSlug()).isNull();
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
                projectId,
                null,
                null,
                displayOrder,
                active,
                adminId
        );
    }

    private long insertAdmin() {
        String handle = "@admin-" + UUID.randomUUID().toString().substring(0, 12);
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (handle, role) VALUES (?, 'ADMIN') RETURNING id",
                Long.class,
                handle
        );
    }

    private long insertProject(long registeredBy, String slug) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO projects (
                            cohort, registered_by, team_name, slug, title, tagline,
                            service_status, approval_status, github_repository_url
                        ) VALUES (8, ?, '테스트 팀', ?, '테스트 프로젝트', '프로젝트 소개', 'CLOSED', 'APPROVED', ?)
                        RETURNING id
                        """,
                Long.class,
                registeredBy,
                slug,
                "https://github.com/test/" + slug
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
