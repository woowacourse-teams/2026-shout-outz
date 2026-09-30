package com.shoutoutz.api.notification.presentation;

import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.notification.application.NotificationService;
import com.shoutoutz.api.notification.presentation.dto.response.NotificationFindAllResponse;
import com.shoutoutz.api.notification.presentation.dto.response.NotificationReadResponse;
import com.shoutoutz.api.notification.presentation.dto.response.NotificationResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationHttpApi {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<NotificationResponse>>> findAll(
            @LoginUser AuthenticatedUser loginUser,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        NotificationFindAllResponse response = notificationService.findAll(
                loginUser.userId(),
                cursor,
                size
        );
        return ResponseEntity.ok(SuccessResponse.success(response.items(), response.meta()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<SuccessResponse<UnreadCountResponse>> countUnread(
            @LoginUser AuthenticatedUser loginUser
    ) {
        return ResponseEntity.ok(SuccessResponse.success(
                new UnreadCountResponse(notificationService.countUnread(loginUser.userId()))
        ));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<SuccessResponse<NotificationReadResponse>> markRead(
            @LoginUser AuthenticatedUser loginUser,
            @PathVariable long notificationId
    ) {
        NotificationReadResponse response = notificationService.markRead(
                loginUser.userId(),
                notificationId
        );
        return ResponseEntity.ok(SuccessResponse.success(response));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @LoginUser AuthenticatedUser loginUser
    ) {
        notificationService.markAllRead(loginUser.userId());
        return ResponseEntity.noContent().build();
    }

    public record UnreadCountResponse(long unreadCount) {
    }
}
