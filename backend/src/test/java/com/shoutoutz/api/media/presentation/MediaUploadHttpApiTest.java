package com.shoutoutz.api.media.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.media.application.MediaUploadCompletionService;
import com.shoutoutz.api.media.application.MediaUploadService;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.presentation.dto.request.MediaUploadStartRequest;
import com.shoutoutz.api.media.presentation.dto.response.MediaUploadCompleteResponse;
import com.shoutoutz.api.media.presentation.dto.response.MediaUploadStartResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MediaUploadHttpApi.class)
class MediaUploadHttpApiTest {

    private static final long USER_ID = 7L;
    private static final Instant EXPIRES_AT = Instant.parse("2026-08-31T00:05:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaUploadService mediaUploadService;

    @MockitoBean
    private MediaUploadCompletionService mediaUploadCompletionService;

    @Test
    void 인증된_사용자의_업로드_시작을_서비스에_전달하고_CREATED를_반환한다() throws Exception {
        MediaUploadStartResponse response = new MediaUploadStartResponse(
                10L,
                MediaStatus.PENDING_UPLOAD,
                URI.create("https://s3.example.com/upload"),
                EXPIRES_AT,
                "image/webp"
        );
        given(mediaUploadService.startUpload(eq(USER_ID), any(MediaUploadStartRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/media/uploads")
                        .requestAttr(AuthenticatedSession.class.getName(), authenticatedSession())
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(uploadStartRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mediaId").value(10L))
                .andExpect(jsonPath("$.status").value("PENDING_UPLOAD"));

        verify(mediaUploadService).startUpload(eq(USER_ID), any(MediaUploadStartRequest.class));
    }

    @Test
    void 인증된_사용자의_업로드_완료를_서비스에_전달하고_OK를_반환한다() throws Exception {
        MediaUploadCompleteResponse response = new MediaUploadCompleteResponse(
                10L,
                MediaStatus.PROCESSING,
                1024L,
                "image/webp",
                EXPIRES_AT
        );
        given(mediaUploadCompletionService.completeUpload(USER_ID, 10L)).willReturn(response);

        mockMvc.perform(post("/api/v1/media/{mediaId}/complete", 10L)
                        .requestAttr(AuthenticatedSession.class.getName(), authenticatedSession())
                        .header("X-CSRF-Token", "csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaId").value(10L))
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        verify(mediaUploadCompletionService).completeUpload(USER_ID, 10L);
    }

    @Test
    void 인증_정보가_없으면_업로드_서비스를_호출하지_않고_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/media/uploads")
                        .header("X-CSRF-Token", "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(uploadStartRequest()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(mediaUploadService, mediaUploadCompletionService);
    }

    private AuthenticatedSession authenticatedSession() {
        return new AuthenticatedSession(USER_ID, UserRole.USER);
    }

    private String uploadStartRequest() {
        return """
                {
                  "purpose": "FEED_CONTENT",
                  "originalFileName": "feed-image.webp",
                  "contentType": "image/webp",
                  "sizeBytes": 1024
                }
                """;
    }
}
