package com.shoutoutz.api.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.auth.domain.OAuthAccountRepository;
import com.shoutoutz.api.media.application.MediaUrlResolver;
import com.shoutoutz.api.notification.application.dto.NotificationItem;
import com.shoutoutz.api.notification.application.dto.NotificationPage;
import com.shoutoutz.api.notification.domain.NotificationRepository;
import com.shoutoutz.api.notification.domain.NotificationType;
import com.shoutoutz.api.notification.presentation.dto.response.NotificationFindAllResponse;
import com.shoutoutz.api.user.application.UserAvatarUrlResolver;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final long USER_ID = 7L;
    private static final Instant CREATED_AT = Instant.parse("2026-09-30T00:00:00Z");

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationQueryRepository notificationQueryRepository;

    @Mock
    private MediaUrlResolver mediaUrlResolver;

    @Mock
    private OAuthAccountRepository oauthAccountRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                notificationQueryRepository,
                new NotificationCursorCodec(),
                new UserAvatarUrlResolver(mediaUrlResolver, oauthAccountRepository)
        );
    }

    @Test
    void 알림_목록을_조회하고_아바타_URL을_변환한다() {
        NotificationItem item = new NotificationItem(
                100L,
                NotificationType.QUESTION_ACTIVITY,
                "내 질문에 새로운 답변이 달렸어요.",
                10L,
                "질문 제목",
                20L,
                30L,
                "@actor",
                "작성자",
                40L,
                false,
                CREATED_AT
        );
        when(notificationQueryRepository.findAll(USER_ID, null, 20))
                .thenReturn(new NotificationPage(List.of(item), false, 1L));
        when(mediaUrlResolver.resolveAll(Set.of(40L)))
                .thenReturn(Map.of(40L, URI.create("https://cdn.example.com/avatar")));

        NotificationFindAllResponse response = notificationService.findAll(USER_ID, null, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().notificationId()).isEqualTo(100L);
        assertThat(response.items().getFirst().actor().avatarUrl())
                .isEqualTo("https://cdn.example.com/avatar");
        assertThat(response.meta().hasNext()).isFalse();
        assertThat(response.meta().totalCount()).isEqualTo(1L);
    }

    @Test
    void 조회_개수가_범위를_벗어나면_거부한다() {
        assertThatThrownBy(() -> notificationService.findAll(USER_ID, null, 51))
                .isInstanceOf(InvalidInputException.class);

        verify(notificationQueryRepository, never()).findAll(any(Long.class), any(), any(Integer.class));
    }

    @Test
    void 존재하지_않거나_다른_사용자의_알림은_읽음_처리할_수_없다() {
        when(notificationRepository.markRead(USER_ID, 100L)).thenReturn(false);

        assertThatThrownBy(() -> notificationService.markRead(USER_ID, 100L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 댓글_알림_생성을_저장소에_위임한다() {
        notificationService.createForFeedComment(10L, 20L, 30L);

        verify(notificationRepository).createForFeedComment(10L, 20L, 30L);
    }
}
