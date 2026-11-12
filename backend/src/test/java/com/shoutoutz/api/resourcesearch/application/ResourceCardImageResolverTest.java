package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.shoutoutz.api.media.application.MediaUrlResolver;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCardImages;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceCardImageResolverTest {

    private static final URI LOGO_31 = URI.create("https://cdn.example.com/media/project/31/thumbnail");
    private static final String TISTORY_ICON = "https://t1.daumcdn.net/tistory_admin/top_v2/bi-tistory-favicon.svg";

    @Mock
    private MediaUrlResolver mediaUrlResolver;

    @Mock
    private ResourceFaviconQueryRepository resourceFaviconQueryRepository;

    @InjectMocks
    private ResourceCardImageResolver resourceCardImageResolver;

    @Test
    @DisplayName("카드들의 로고 미디어 ID를 중복 없이 모아 썸네일 크기 주소로 한 번에 바꾼다")
    void resolvesLogoUrlsAtOnce() {
        given(mediaUrlResolver.resolveAll(Set.of(31L, 44L), MediaVariant.THUMBNAIL)).willReturn(Map.of(31L, LOGO_31));

        ResourceCardImages images = resourceCardImageResolver.resolve(List.of(
                pr(1L, new ResourceCard.Team("샤라웃", "shout-outz", 31L)),
                pr(2L, new ResourceCard.Team("샤라웃", "shout-outz", 31L)),
                pr(3L, new ResourceCard.Team("Rilog.", "rilog", 44L)),
                pr(4L, new ResourceCard.Team("등록 전 팀", null, null)),
                pr(5L, null)
        ));

        assertThat(images.logoUrlOf(31L)).isEqualTo(LOGO_31.toString());
        assertThat(images.logoUrlOf(44L)).as("준비되지 않은 이미지").isNull();
        assertThat(images.logoUrlOf(null)).isNull();
    }

    @Test
    @DisplayName("로고도, 수집한 파비콘이 필요한 자료도 없으면 아무것도 조회하지 않는다")
    void skipsLookupWhenNothingToResolve() {
        ResourceCardImages images = resourceCardImageResolver.resolve(List.of(
                blog(1L, "velog", "https://velog.io/@jun/post"),
                blog(2L, "tistory", "https://example.tistory.com/1")
        ));

        assertThat(images).isEqualTo(ResourceCardImages.NONE);
        verify(mediaUrlResolver, never()).resolveAll(anyCollection(), any());
        verify(resourceFaviconQueryRepository, never()).findFaviconUrls(anyCollection());
    }

    @Test
    @DisplayName("블로그 플랫폼은 공식 아이콘을 쓰고, 그 밖의 블로그와 블로그가 아닌 자료는 수집한 파비콘을 쓴다")
    void resolvesFavicons() {
        ResourceCard personalBlog = blog(11L, "generic", "https://jun.dev/posts/1");
        ResourceCard notCollected = blog(12L, null, "https://another.blog/posts/2");
        ResourceCard tistory = blog(13L, "tistory", "https://example.tistory.com/3");
        ResourceCard velogWithoutPlatform = blog(14L, null, "https://velog.io/@jun/post");
        ResourceCard pr = pr(15L, null);
        ResourceCard talk = new ResourceCard(16L, "youtube-tekotalk", "제목", "https://www.youtube.com/watch?v=abcdefghijk",
                null, null, null, null);
        given(resourceFaviconQueryRepository.findFaviconUrls(List.of(11L, 12L, 15L, 16L)))
                .willReturn(Map.of(
                        11L, "https://jun.dev/favicon.png",
                        15L, "https://github.githubassets.com/favicons/favicon.svg"
                ));

        ResourceCardImages images = resourceCardImageResolver.resolve(
                List.of(personalBlog, notCollected, tistory, velogWithoutPlatform, pr, talk));

        assertThat(images.faviconUrlOf(personalBlog)).isEqualTo("https://jun.dev/favicon.png");
        assertThat(images.faviconUrlOf(notCollected)).as("아직 수집 전").isNull();
        assertThat(images.faviconUrlOf(tistory)).isEqualTo(TISTORY_ICON);
        assertThat(images.faviconUrlOf(velogWithoutPlatform)).as("플랫폼 값이 없어도 주소로 판별")
                .isEqualTo("https://velog.io/favicon.ico");
        assertThat(images.faviconUrlOf(pr)).as("블로그가 아니어도 수집한 파비콘")
                .isEqualTo("https://github.githubassets.com/favicons/favicon.svg");
        assertThat(images.faviconUrlOf(talk)).as("수집한 파비콘이 없으면 null").isNull();
    }

    private ResourceCard pr(long id, ResourceCard.Team team) {
        return new ResourceCard(id, "github-pr", "제목", "https://github.com/example/pull/" + id, null, null, null, team);
    }

    private ResourceCard blog(long id, String platform, String url) {
        return new ResourceCard(id, "blog-post", "제목", url, null, null, platform, null);
    }
}
