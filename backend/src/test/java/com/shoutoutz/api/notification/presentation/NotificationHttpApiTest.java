package com.shoutoutz.api.notification.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.SimpleType.INTEGER;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.notification.application.NotificationService;
import com.shoutoutz.api.notification.domain.NotificationType;
import com.shoutoutz.api.notification.presentation.dto.response.NotificationFindAllResponse;
import com.shoutoutz.api.notification.presentation.dto.response.NotificationReadResponse;
import com.shoutoutz.api.notification.presentation.dto.response.NotificationResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = NotificationHttpApi.class)
@AutoConfigureRestDocs
class NotificationHttpApiTest {

    private static final long USER_ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void 알림_목록을_조회한다() throws Exception {
        NotificationResponse questionNotification = new NotificationResponse(
                100L,
                NotificationType.QUESTION_ACTIVITY,
                "내 질문에 새로운 답변이 달렸어요.",
                10L,
                "질문 제목",
                20L,
                new NotificationResponse.Actor(30L, "@actor", "작성자", null),
                false,
                Instant.parse("2026-09-30T00:00:00Z")
        );
        NotificationResponse postNotification = new NotificationResponse(
                99L,
                NotificationType.POST_ACTIVITY,
                "내 피드에 새로운 댓글이 달렸어요.",
                11L,
                "피드 제목",
                21L,
                new NotificationResponse.Actor(30L, "@actor", "작성자", null),
                false,
                Instant.parse("2026-09-29T00:00:00Z")
        );
        given(notificationService.findAll(USER_ID, null, 20))
                .willReturn(new NotificationFindAllResponse(
                        List.of(questionNotification, postNotification),
                        new SliceMetaResponse(null, false, 2L)
                ));

        mockMvc.perform(get("/api/v1/notifications").with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].notificationId").value(100))
                .andExpect(jsonPath("$.data[0].notificationType")
                        .value("QUESTION_ACTIVITY"))
                .andExpect(jsonPath("$.data[0].message")
                        .value("내 질문에 새로운 답변이 달렸어요."))
                .andExpect(jsonPath("$.data[1].notificationType").value("POST_ACTIVITY"))
                .andExpect(jsonPath("$.data[1].message")
                        .value("내 피드에 새로운 댓글이 달렸어요."))
                .andExpect(jsonPath("$.data[0].isRead").value(false))
                .andExpect(jsonPath("$.meta.hasNext").value(false))
                .andDo(document(
                        "notification-list",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Notification")
                                .summary("알림 목록 조회")
                                .description("현재 로그인 사용자의 알림을 최신순으로 조회한다.")
                                .queryParameters(
                                        parameterWithName("cursor")
                                                .description("다음 페이지 조회용 커서. 첫 요청은 생략")
                                                .optional(),
                                        parameterWithName("size")
                                                .type(INTEGER)
                                                .description("조회 개수. 기본값 20, 1~50")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("NotificationListSuccessResponse"))
                                .responseFields(notificationListFields())
                                .build())
                ));

        verify(notificationService).findAll(USER_ID, null, 20);
    }

    @Test
    void 읽지_않은_알림_수를_조회한다() throws Exception {
        given(notificationService.countUnread(USER_ID)).willReturn(3L);

        mockMvc.perform(get("/api/v1/notifications/unread-count").with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(3))
                .andDo(document(
                        "notification-unread-count",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Notification")
                                .summary("읽지 않은 알림 수 조회")
                                .description("현재 로그인 사용자의 읽지 않은 알림 수를 조회한다.")
                                .responseSchema(Schema.schema("NotificationUnreadCountSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status")
                                                .type(JsonFieldType.STRING)
                                                .description("응답 상태"),
                                        fieldWithPath("data")
                                                .type(JsonFieldType.OBJECT)
                                                .description("읽지 않은 알림 수"),
                                        fieldWithPath("data.unreadCount")
                                                .type(JsonFieldType.NUMBER)
                                                .description("읽지 않은 알림 수")
                                )
                                .build())
                ));

        verify(notificationService).countUnread(USER_ID);
    }

    @Test
    void 알림을_읽음_처리한다() throws Exception {
        given(notificationService.markRead(USER_ID, 100L))
                .willReturn(new NotificationReadResponse(100L, true));

        mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", 100L)
                        .with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notificationId").value(100))
                .andExpect(jsonPath("$.data.isRead").value(true))
                .andDo(document(
                        "notification-read",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Notification")
                                .summary("알림 읽음 처리")
                                .description("현재 로그인 사용자의 특정 알림을 읽음 상태로 변경한다.")
                                .pathParameters(
                                        parameterWithName("notificationId")
                                                .type(INTEGER)
                                                .description("알림 ID")
                                )
                                .responseSchema(Schema.schema("NotificationReadSuccessResponse"))
                                .responseFields(
                                        fieldWithPath("status")
                                                .type(JsonFieldType.STRING)
                                                .description("응답 상태"),
                                        fieldWithPath("data")
                                                .type(JsonFieldType.OBJECT)
                                                .description("읽음 처리 결과"),
                                        fieldWithPath("data.notificationId")
                                                .type(JsonFieldType.NUMBER)
                                                .description("알림 ID"),
                                        fieldWithPath("data.isRead")
                                                .type(JsonFieldType.BOOLEAN)
                                                .description("읽음 상태")
                                )
                                .build())
                ));

        verify(notificationService).markRead(USER_ID, 100L);
    }

    @Test
    void 모든_알림을_읽음_처리한다() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/read-all").with(authenticated()))
                .andExpect(status().isNoContent())
                .andDo(document(
                        "notification-read-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Notification")
                                .summary("전체 알림 읽음 처리")
                                .description("현재 로그인 사용자의 모든 알림을 읽음 상태로 변경한다.")
                                .build())
                ));

        verify(notificationService).markAllRead(USER_ID);
    }

    @Test
    void 로그인하지_않으면_알림을_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(notificationService);
    }

    private RequestPostProcessor authenticated() {
        return request -> {
            request.setAttribute(
                    AuthenticatedSession.class.getName(),
                    new AuthenticatedSession(USER_ID, UserRole.USER)
            );
            return request;
        };
    }

    private FieldDescriptor[] notificationListFields() {
        return new FieldDescriptor[]{
                fieldWithPath("status").type(JsonFieldType.STRING).description("응답 상태"),
                fieldWithPath("data").type(JsonFieldType.ARRAY).description("알림 목록"),
                fieldWithPath("data[].notificationId")
                        .type(JsonFieldType.NUMBER).description("알림 ID"),
                fieldWithPath("data[].notificationType")
                        .type(JsonFieldType.STRING).description(
                                "알림 유형. POST_ACTIVITY(내 피드), COMMENTED_POST_ACTIVITY(댓글을 남긴 피드), "
                                        + "QUESTION_ACTIVITY(내 질문), INTERESTED_QUESTION_ACTIVITY(궁금해요를 누른 질문), "
                                        + "COMMENTED_QUESTION_ACTIVITY(댓글을 남긴 질문)"
                        ),
                fieldWithPath("data[].message")
                        .type(JsonFieldType.STRING).description("알림 메시지"),
                fieldWithPath("data[].feedId")
                        .type(JsonFieldType.NUMBER).description("대상 피드 ID").optional(),
                fieldWithPath("data[].feedTitle")
                        .type(JsonFieldType.STRING).description("대상 피드 제목").optional(),
                fieldWithPath("data[].commentId")
                        .type(JsonFieldType.NUMBER).description("대상 댓글 ID").optional(),
                fieldWithPath("data[].actor")
                        .type(JsonFieldType.OBJECT).description("알림을 발생시킨 사용자").optional(),
                fieldWithPath("data[].actor.userId")
                        .type(JsonFieldType.NUMBER).description("사용자 ID").optional(),
                fieldWithPath("data[].actor.handle")
                        .type(JsonFieldType.STRING).description("사용자 handle").optional(),
                fieldWithPath("data[].actor.displayName")
                        .type(JsonFieldType.STRING).description("사용자 표시 이름").optional(),
                fieldWithPath("data[].actor.avatarUrl")
                        .type(JsonFieldType.STRING)
                        .description("직접 업로드한 이미지가 없으면 GitHub 아바타를 사용하는 사용자 아바타 URL")
                        .optional(),
                fieldWithPath("data[].isRead")
                        .type(JsonFieldType.BOOLEAN).description("읽음 상태"),
                fieldWithPath("data[].createdAt")
                        .type(JsonFieldType.STRING).description("알림 생성 시각"),
                fieldWithPath("meta").type(JsonFieldType.OBJECT).description("페이지 정보"),
                fieldWithPath("meta.nextCursor")
                        .type(JsonFieldType.STRING).description("다음 페이지 커서").optional(),
                fieldWithPath("meta.hasNext")
                        .type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
                fieldWithPath("meta.totalCount")
                        .type(JsonFieldType.NUMBER).description("전체 알림 수")
        };
    }
}
