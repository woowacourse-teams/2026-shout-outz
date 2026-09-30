package com.shoutoutz.api.notification.application;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.notification.application.dto.NotificationCursor;
import com.shoutoutz.api.notification.application.dto.NotificationItem;
import com.shoutoutz.api.notification.application.dto.NotificationPage;
import com.shoutoutz.api.notification.domain.NotificationErrorCode;
import com.shoutoutz.api.notification.domain.NotificationRepository;
import com.shoutoutz.api.notification.presentation.dto.response.NotificationFindAllResponse;
import com.shoutoutz.api.notification.presentation.dto.response.NotificationReadResponse;
import com.shoutoutz.api.notification.presentation.dto.response.NotificationResponse;
import com.shoutoutz.api.user.application.UserAvatarUrlResolver;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final NotificationQueryRepository notificationQueryRepository;
    private final NotificationCursorCodec notificationCursorCodec;
    private final UserAvatarUrlResolver userAvatarUrlResolver;

    @Transactional(readOnly = true)
    public NotificationFindAllResponse findAll(
            long recipientId,
            String encodedCursor,
            int size
    ) {
        validateSize(size);
        NotificationCursor cursor = notificationCursorCodec.decode(encodedCursor);
        NotificationPage page = notificationQueryRepository.findAll(recipientId, cursor, size);

        String nextCursor = null;
        if (page.hasNext() && !page.items().isEmpty()) {
            NotificationItem lastItem = page.items().getLast();
            nextCursor = notificationCursorCodec.encode(
                    new NotificationCursor(lastItem.createdAt(), lastItem.notificationId())
            );
        }

        Map<Long, String> avatarUrls = resolveAvatarUrls(page.items());
        List<NotificationResponse> items = page.items().stream()
                .map(item -> toResponse(item, avatarUrls))
                .toList();
        return new NotificationFindAllResponse(
                items,
                new SliceMetaResponse(nextCursor, page.hasNext(), page.totalCount())
        );
    }

    @Transactional(readOnly = true)
    public long countUnread(long recipientId) {
        return notificationQueryRepository.countUnread(recipientId);
    }

    @Transactional
    public NotificationReadResponse markRead(long recipientId, long notificationId) {
        if (!notificationRepository.markRead(recipientId, notificationId)) {
            throw new EntityNotFoundException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        }
        return new NotificationReadResponse(notificationId, true);
    }

    @Transactional
    public void markAllRead(long recipientId) {
        notificationRepository.markAllRead(recipientId);
    }

    /**
     * 댓글 저장이 완료된 뒤 호출되어 해당 댓글의 알림 수신자를 만든다.
     * FeedCommentService의 트랜잭션 안에서 실행되므로 댓글과 알림은 함께 커밋된다.
     */
    @Transactional
    public void createForFeedComment(long feedId, long commentId, long actorId) {
        notificationRepository.createForFeedComment(feedId, commentId, actorId);
    }

    private void validateSize(int size) {
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new InvalidInputException(NotificationErrorCode.INVALID_NOTIFICATION_SIZE);
        }
    }

    private Map<Long, String> resolveAvatarUrls(List<NotificationItem> items) {
        return userAvatarUrlResolver.resolveAll(items.stream()
                .map(item -> new UserAvatarUrlResolver.AvatarReference(
                        item.actorId(),
                        item.actorAvatarImageId()
                ))
                .toList());
    }

    private NotificationResponse toResponse(
            NotificationItem item,
            Map<Long, String> avatarUrls
    ) {
        NotificationResponse.Actor actor = item.actorId() == null
                ? null
                : new NotificationResponse.Actor(
                        item.actorId(),
                        item.actorHandle(),
                        item.actorDisplayName(),
                        avatarUrls.get(item.actorId())
                );
        return new NotificationResponse(
                item.notificationId(),
                item.notificationType(),
                item.message(),
                item.feedId(),
                item.feedTitle(),
                item.commentId(),
                actor,
                item.read(),
                item.createdAt()
        );
    }

}
