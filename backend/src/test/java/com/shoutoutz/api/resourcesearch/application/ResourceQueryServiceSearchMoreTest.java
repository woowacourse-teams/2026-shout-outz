package com.shoutoutz.api.resourcesearch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedProject;
import com.shoutoutz.api.resourcesearch.application.dto.ProjectResourceCursor;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCardImages;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import com.shoutoutz.api.resourcesearch.presentation.dto.request.ResourceSearchMoreRequest;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceCardResponse;
import com.shoutoutz.api.resourcesearch.presentation.dto.response.ResourceSearchMoreResponse;
import java.time.Instant;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ResourceQueryServiceSearchMoreTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-10-20T01:00:00Z");
    private static final int LIMIT = ResourceSection.DOCUMENT.limit();

    @Mock
    private SectionedResourceSearchService sectionedResourceSearchService;

    @Mock
    private ResourceCardQueryRepository resourceCardQueryRepository;

    @Mock
    private SearchKeywordService searchKeywordService;

    @Mock
    private ResourceCardImageResolver resourceCardImageResolver;

    @Mock
    private ProjectNameFinder projectNameFinder;

    @Spy
    private ProjectResourceCursorCodec projectResourceCursorCodec =
            new ProjectResourceCursorCodec(JsonMapper.builder().build());

    @InjectMocks
    private ResourceQueryService resourceQueryService;

    @Test
    @DisplayName("커서 다음의 프로젝트 자료를 섹션 상한만큼 주고, 뒤에 더 있으면 다음 커서와 전체 수를 준다")
    void continuesAfterCursor() {
        ProjectResourceCursor cursor = new ProjectResourceCursor(PUBLISHED_AT, 500L);
        given(projectNameFinder.findNamed("Rilog")).willReturn(List.of(new MatchedProject(7L, "Rilog.", "rilog")));
        given(resourceCardQueryRepository.findLatestByProjects(
                List.of(7L), ResourceSection.DOCUMENT, ResourceTrack.BE, cursor, LIMIT + 1))
                .willReturn(LongStream.rangeClosed(1, LIMIT + 1).map(id -> 100 - id).mapToObj(this::card).toList());
        given(resourceCardQueryRepository.countByProjects(List.of(7L), ResourceSection.DOCUMENT, ResourceTrack.BE))
                .willReturn(84L);
        given(resourceCardImageResolver.resolve(any())).willReturn(ResourceCardImages.NONE);

        ResourceSearchMoreResponse response = resourceQueryService.searchMore(new ResourceSearchMoreRequest(
                "Rilog", ResourceTrack.BE, ResourceSection.DOCUMENT, projectResourceCursorCodec.encode(cursor)));

        assertThat(response.items()).extracting(ResourceCardResponse::resourceId)
                .hasSize(LIMIT)
                .startsWith(99L)
                .endsWith(100L - LIMIT);
        assertThat(projectResourceCursorCodec.decode(response.nextCursor()))
                .isEqualTo(new ProjectResourceCursor(PUBLISHED_AT, 100L - LIMIT));
        assertThat(response.totalCount()).isEqualTo(84L);
        verifyNoInteractions(searchKeywordService, sectionedResourceSearchService);
    }

    @Test
    @DisplayName("마지막 페이지면 다음 커서가 없다")
    void hasNoCursorOnLastPage() {
        given(projectNameFinder.findNamed("Rilog")).willReturn(List.of(new MatchedProject(7L, "Rilog.", "rilog")));
        given(resourceCardQueryRepository.findLatestByProjects(List.of(7L), ResourceSection.BLOG, null, null, LIMIT + 1))
                .willReturn(List.of(card(3), card(2)));
        given(resourceCardQueryRepository.countByProjects(List.of(7L), ResourceSection.BLOG, null)).willReturn(2L);
        given(resourceCardImageResolver.resolve(any())).willReturn(ResourceCardImages.NONE);

        ResourceSearchMoreResponse response = resourceQueryService.searchMore(
                new ResourceSearchMoreRequest("Rilog", null, ResourceSection.BLOG, null));

        assertThat(response.items()).extracting(ResourceCardResponse::resourceId).containsExactly(3L, 2L);
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("검색어가 프로젝트 이름이 아니면 이어 읽을 자료가 없다")
    void returnsNothingWithoutNamedProject() {
        given(projectNameFinder.findNamed("동시성")).willReturn(List.of());

        ResourceSearchMoreResponse response = resourceQueryService.searchMore(
                new ResourceSearchMoreRequest("동시성", null, ResourceSection.BLOG, null));

        assertThat(response.items()).isEmpty();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.totalCount()).isZero();
        verifyNoInteractions(resourceCardQueryRepository);
    }

    @Test
    @DisplayName("풀 수 없는 커서는 잘못된 요청이다")
    void rejectsInvalidCursor() {
        assertThatThrownBy(() -> resourceQueryService.searchMore(
                new ResourceSearchMoreRequest("Rilog", null, ResourceSection.BLOG, "!!!")))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(projectNameFinder, resourceCardQueryRepository);
    }

    private ResourceCard card(long id) {
        return new ResourceCard(id, "github-pr", "제목", "https://example.com/" + id, "소개", PUBLISHED_AT, null, null);
    }
}
