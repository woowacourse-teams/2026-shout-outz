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
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCardLogoUrls;
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
class ResourceCardLogoResolverTest {

    private static final URI LOGO_31 = URI.create("https://cdn.example.com/media/project/31/thumbnail");

    @Mock
    private MediaUrlResolver mediaUrlResolver;

    @InjectMocks
    private ResourceCardLogoResolver resourceCardLogoResolver;

    @Test
    @DisplayName("카드들의 로고 미디어 ID를 중복 없이 모아 썸네일 크기 주소로 한 번에 바꾼다")
    void resolvesLogoUrlsAtOnce() {
        given(mediaUrlResolver.resolveAll(Set.of(31L, 44L), MediaVariant.THUMBNAIL)).willReturn(Map.of(31L, LOGO_31));

        ResourceCardLogoUrls logoUrls = resourceCardLogoResolver.resolve(List.of(
                card(1L, new ResourceCard.Team("샤라웃", "shout-outz", 31L)),
                card(2L, new ResourceCard.Team("샤라웃", "shout-outz", 31L)),
                card(3L, new ResourceCard.Team("Rilog.", "rilog", 44L)),
                card(4L, new ResourceCard.Team("등록 전 팀", null, null)),
                card(5L, null)
        ));

        assertThat(logoUrls.urlOf(31L)).isEqualTo(LOGO_31.toString());
        assertThat(logoUrls.urlOf(44L)).as("준비되지 않은 이미지").isNull();
        assertThat(logoUrls.urlOf(null)).isNull();
    }

    @Test
    @DisplayName("로고가 있는 카드가 없으면 미디어를 조회하지 않는다")
    void skipsLookupWithoutLogo() {
        ResourceCardLogoUrls logoUrls = resourceCardLogoResolver.resolve(List.of(
                card(1L, null),
                card(2L, new ResourceCard.Team("등록 전 팀", null, null))
        ));

        assertThat(logoUrls).isEqualTo(ResourceCardLogoUrls.NONE);
        verify(mediaUrlResolver, never()).resolveAll(anyCollection(), any());
    }

    private ResourceCard card(long id, ResourceCard.Team team) {
        return new ResourceCard(id, "blog-post", "제목", "https://example.com/" + id, null, null, null, team);
    }
}
