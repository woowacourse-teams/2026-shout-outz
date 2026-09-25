package com.shoutoutz.api.news.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsErrorCode;
import com.shoutoutz.api.news.domain.NewsReactionRepository;
import com.shoutoutz.api.news.domain.NewsReactionType;
import com.shoutoutz.api.news.domain.NewsRepository;
import com.shoutoutz.api.news.presentation.dto.response.NewsReactionResponse;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewsReactionServiceTest {

    private static final long NEWS_ID = 100L;
    private static final long USER_ID = 1L;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NewsReactionRepository newsReactionRepository;

    private NewsReactionService newsReactionService;

    @BeforeEach
    void setUp() {
        newsReactionService = new NewsReactionService(newsRepository, newsReactionRepository);
    }

    @Test
    void 소식에_좋아요를_멱등하게_추가하고_반응_수를_반환한다() {
        givenActiveNews();
        when(newsReactionRepository.countByNewsId(NEWS_ID)).thenReturn(21L);

        NewsReactionResponse response = newsReactionService.add(NEWS_ID, USER_ID, "LIKE");

        assertThat(response).isEqualTo(
                new NewsReactionResponse(NEWS_ID, NewsReactionType.LIKE, true, 21L)
        );
        verify(newsReactionRepository).add(NEWS_ID, USER_ID, NewsReactionType.LIKE);
    }

    @Test
    void 소식에서_좋아요를_멱등하게_제거하고_반응_수를_반환한다() {
        givenActiveNews();
        when(newsReactionRepository.countByNewsId(NEWS_ID)).thenReturn(20L);

        NewsReactionResponse response = newsReactionService.remove(NEWS_ID, USER_ID, "LIKE");

        assertThat(response).isEqualTo(
                new NewsReactionResponse(NEWS_ID, NewsReactionType.LIKE, false, 20L)
        );
        verify(newsReactionRepository).remove(NEWS_ID, USER_ID, NewsReactionType.LIKE);
    }

    @Test
    void 존재하지_않거나_삭제된_소식에는_반응할_수_없다() {
        assertThatThrownBy(() -> newsReactionService.add(NEWS_ID, USER_ID, "LIKE"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", NewsErrorCode.NEWS_NOT_FOUND);

        verifyNoInteractions(newsReactionRepository);
    }

    @Test
    void 지원하지_않는_반응_타입은_400_오류로_처리한다() {
        assertThatThrownBy(() -> newsReactionService.add(NEWS_ID, USER_ID, "BOOKMARK"))
                .isInstanceOf(InvalidInputException.class)
                .hasFieldOrPropertyWithValue("errorCode", NewsErrorCode.REACTION_TYPE_INVALID);

        verify(newsRepository, never()).findActiveById(NEWS_ID);
        verifyNoInteractions(newsReactionRepository);
    }

    private void givenActiveNews() {
        when(newsRepository.findActiveById(NEWS_ID))
                .thenReturn(Optional.of(News.createNotice(
                        "공지 제목",
                        "공지 요약",
                        "공지 본문",
                        99L,
                        "작성자",
                        null,
                        Instant.parse("2026-09-25T00:00:00Z")
                )));
    }
}
