package com.shoutoutz.api.project.presentation;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.EnumFields;
import com.epages.restdocs.apispec.SimpleType;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.project.application.AdminProjectService;
import com.shoutoutz.api.project.domain.ApprovalStatus;
import com.shoutoutz.api.project.domain.ServiceStatus;
import com.shoutoutz.api.project.presentation.dto.request.AdminProjectFindAllRequest;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectApproveResponse;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectDecisionActor;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectDetailResponse;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectFindAllResponse;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectHistoryResponse;
import com.shoutoutz.api.project.presentation.dto.response.AdminProjectRejectResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = {
        AdminProjectHttpApi.class,
        AdminProjectDecisionHttpApi.class,
        AdminProjectHistoryHttpApi.class
})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs
class AdminProjectHttpApiTest {

    private static final long PROJECT_ID = 100L;
    private static final long ADMIN_ID = 7L;
    private static final Instant NOW = Instant.parse("2026-09-20T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminProjectService adminProjectService;

    @Test
    void findsAdminProjectList() throws Exception {
        given(adminProjectService.findAll(
                eq(UserRole.ADMIN),
                any(AdminProjectFindAllRequest.class)
        )).willReturn(new AdminProjectFindAllResponse(
                List.of(projectItem()),
                new SliceMetaResponse("next-cursor", true, 3L)
        ));

        mockMvc.perform(get("/api/v1/admin/projects")
                        .with(authenticated(UserRole.ADMIN))
                        .header(HttpHeaders.COOKIE, "JSESSIONID=admin-session")
                        .queryParam("status", "PENDING")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(PROJECT_ID))
                .andExpect(jsonPath("$.data[0].approvalStatus").value("PENDING"))
                .andExpect(jsonPath("$.meta.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andDo(document(
                        "admin-project-find-all",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Admin Project Approval")
                                .summary("관리자의 프로젝트 심사 목록 조회")
                                .description("관리자가 상태별 프로젝트 심사 목록을 최신 등록순으로 조회한다.")
                                .requestHeaders(
                                        headerWithName(HttpHeaders.COOKIE)
                                                .description("인증된 관리자의 JSESSIONID")
                                )
                                .queryParameters(
                                        parameterWithName("status")
                                                .type(SimpleType.STRING)
                                                .defaultValue("PENDING")
                                                .description("조회할 상태(PENDING, APPROVED, REJECTED)")
                                                .optional(),
                                        parameterWithName("size")
                                                .type(SimpleType.INTEGER)
                                                .defaultValue(20)
                                                .description("조회 개수(1~100), 기본값 20")
                                                .optional(),
                                        parameterWithName("cursor")
                                                .type(SimpleType.STRING)
                                                .description("다음 페이지 조회용 커서")
                                                .optional()
                                )
                                .responseSchema(Schema.schema("AdminProjectFindAllSuccessResponse"))
                                .responseFields(listResponseFields())
                                .build())
                ));

        verify(adminProjectService).findAll(eq(UserRole.ADMIN), any(AdminProjectFindAllRequest.class));
    }

    @Test
    void approvesProject() throws Exception {
        given(adminProjectService.approve(PROJECT_ID, ADMIN_ID, UserRole.ADMIN))
                .willReturn(new AdminProjectApproveResponse(
                        PROJECT_ID,
                        ApprovalStatus.APPROVED,
                        new AdminProjectDecisionActor(ADMIN_ID, "@admin"),
                        NOW
                ));

        mockMvc.perform(post("/api/v1/admin/projects/{projectId}/approve", PROJECT_ID)
                        .with(authenticated(UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID))
                .andExpect(jsonPath("$.data.approvalStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.decidedBy.handle").value("@admin"))
                .andDo(document(
                        "admin-project-approve",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Admin Project Approval")
                                .summary("프로젝트 승인")
                                .description("관리자가 PENDING 프로젝트를 승인한다.")
                                .pathParameters(parameterWithName("projectId")
                                        .type(SimpleType.INTEGER)
                                        .description("프로젝트 ID"))
                                .responseSchema(Schema.schema("AdminProjectApproveSuccessResponse"))
                                .responseFields(approveResponseFields())
                                .build())
                ));

        verify(adminProjectService).approve(PROJECT_ID, ADMIN_ID, UserRole.ADMIN);
    }

    @Test
    void rejectsProject() throws Exception {
        given(adminProjectService.reject(PROJECT_ID, ADMIN_ID, UserRole.ADMIN, "설명을 보완해주세요."))
                .willReturn(new AdminProjectRejectResponse(
                        PROJECT_ID,
                        ApprovalStatus.REJECTED,
                        "설명을 보완해주세요.",
                        new AdminProjectDecisionActor(ADMIN_ID, "@admin"),
                        NOW
                ));

        mockMvc.perform(post("/api/v1/admin/projects/{projectId}/reject", PROJECT_ID)
                        .with(authenticated(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"  설명을 보완해주세요.  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID))
                .andExpect(jsonPath("$.data.approvalStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.reason").value("설명을 보완해주세요."))
                .andDo(document(
                        "admin-project-reject",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Admin Project Approval")
                                .summary("프로젝트 반려")
                                .description("관리자가 PENDING 프로젝트를 반려한다.")
                                .pathParameters(parameterWithName("projectId")
                                        .type(SimpleType.INTEGER)
                                        .description("프로젝트 ID"))
                                .requestSchema(Schema.schema("AdminProjectRejectRequest"))
                                .requestFields(fieldWithPath("reason").type(STRING)
                                        .description("반려 사유(앞뒤 공백 제거 후 1~100자)"))
                                .responseSchema(Schema.schema("AdminProjectRejectSuccessResponse"))
                                .responseFields(rejectResponseFields())
                                .build())
                ));

        verify(adminProjectService).reject(
                PROJECT_ID,
                ADMIN_ID,
                UserRole.ADMIN,
                "설명을 보완해주세요."
        );
    }

    @Test
    void findsProjectHistory() throws Exception {
        given(adminProjectService.findHistory(PROJECT_ID, UserRole.ADMIN))
                .willReturn(new AdminProjectHistoryResponse(
                        PROJECT_ID,
                        List.of(new AdminProjectHistoryResponse.Item(
                                1L,
                                null,
                                ApprovalStatus.PENDING,
                                null,
                                null,
                                NOW
                        ))
                ));

        mockMvc.perform(get("/api/v1/admin/projects/{projectId}/history", PROJECT_ID)
                        .with(authenticated(UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID))
                .andExpect(jsonPath("$.data.items[0].toStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.items[0].changedBy").doesNotExist())
                .andDo(document(
                        "admin-project-history",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Admin Project Approval")
                                .summary("프로젝트 심사 이력 조회")
                                .description("관리자가 프로젝트의 승인 상태 변경 이력을 최신순으로 조회한다.")
                                .pathParameters(parameterWithName("projectId")
                                        .type(SimpleType.INTEGER)
                                        .description("프로젝트 ID"))
                                .responseSchema(Schema.schema("AdminProjectHistorySuccessResponse"))
                                .responseFields(historyResponseFields())
                                .build())
                ));

        verify(adminProjectService).findHistory(PROJECT_ID, UserRole.ADMIN);
    }

    @Test
    void findsProjectDetailForAdmin() throws Exception {
        given(adminProjectService.findDetail(PROJECT_ID, UserRole.ADMIN))
                .willReturn(new AdminProjectDetailResponse(
                        PROJECT_ID,
                        42L,
                        "loop",
                        "루프",
                        "루프팀",
                        "한 줄 소개",
                        6,
                        null,
                        null,
                        null,
                        List.of(),
                        "https://github.com/woowacourse-teams/2026-loop",
                        null,
                        ServiceStatus.CLOSED,
                        ApprovalStatus.PENDING,
                        null,
                        0,
                        null,
                        0,
                        0,
                        false,
                        false,
                        0,
                        List.of(),
                        List.of(),
                        NOW,
                        NOW
                ));

        mockMvc.perform(get("/api/v1/admin/projects/{projectId}", PROJECT_ID)
                        .with(authenticated(UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(PROJECT_ID))
                .andExpect(jsonPath("$.data.registeredBy").value(42))
                .andExpect(jsonPath("$.data.approvalStatus").value("PENDING"))
                .andDo(document(
                        "admin-project-find-detail",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Admin Project Approval")
                                .summary("관리자의 프로젝트 상세 조회")
                                .description("관리자가 승인 상태와 관계없이 삭제되지 않은 프로젝트 상세를 조회한다.")
                                .pathParameters(parameterWithName("projectId")
                                        .type(SimpleType.INTEGER)
                                        .description("프로젝트 ID"))
                                .responseSchema(Schema.schema("AdminProjectDetailSuccessResponse"))
                                .responseFields(detailResponseFields())
                                .build())
                ));

        verify(adminProjectService).findDetail(PROJECT_ID, UserRole.ADMIN);
    }

    private static AdminProjectFindAllResponse.Item projectItem() {
        return new AdminProjectFindAllResponse.Item(
                PROJECT_ID,
                "loop",
                "루프",
                "루프팀",
                "한 줄 소개",
                6,
                null,
                null,
                null,
                0,
                0,
                0,
                false,
                false,
                ServiceStatus.CLOSED,
                ApprovalStatus.PENDING,
                null,
                42L,
                List.of(),
                List.of(),
                NOW,
                NOW
        );
    }

    private static RequestPostProcessor authenticated(UserRole role) {
        return request -> {
            request.setAttribute(
                    AuthenticatedSession.class.getName(),
                    new AuthenticatedSession(ADMIN_ID, role)
            );
            return request;
        };
    }

    private static List<FieldDescriptor> listResponseFields() {
        return List.of(
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(ARRAY).description("프로젝트 심사 목록"),
                fieldWithPath("data[].id").type(NUMBER).description("프로젝트 ID"),
                fieldWithPath("data[].slug").type(STRING).description("프로젝트 slug"),
                fieldWithPath("data[].title").type(STRING).description("프로젝트 이름"),
                fieldWithPath("data[].teamName").type(STRING).description("팀 이름"),
                fieldWithPath("data[].tagline").type(STRING).description("한 줄 소개"),
                fieldWithPath("data[].cohort").type(NUMBER).description("우아한테크코스 기수"),
                fieldWithPath("data[].thumbnailImageId").type(NUMBER)
                        .description("프로젝트 썸네일 이미지 ID").optional(),
                fieldWithPath("data[].thumbnailUrl").type(STRING)
                        .description("프로젝트 썸네일 URL").optional(),
                fieldWithPath("data[].starCount").type(NUMBER)
                        .description("GitHub star 수").optional(),
                fieldWithPath("data[].likeCount").type(NUMBER).description("좋아요 수"),
                fieldWithPath("data[].commentCount").type(NUMBER).description("댓글 수"),
                fieldWithPath("data[].bookmarkCount").type(NUMBER).description("북마크 수"),
                fieldWithPath("data[].likedByMe").type(BOOLEAN).description("요청자의 좋아요 여부"),
                fieldWithPath("data[].bookmarkedByMe").type(BOOLEAN).description("요청자의 북마크 여부"),
                new EnumFields(ServiceStatus.class).withPath("data[].serviceStatus")
                        .description("운영 상태"),
                new EnumFields(ApprovalStatus.class).withPath("data[].approvalStatus")
                        .description("승인 상태"),
                fieldWithPath("data[].rejectReason").type(STRING)
                        .description("반려 사유. REJECTED일 때만 값이 있다.").optional(),
                fieldWithPath("data[].registeredBy").type(NUMBER)
                        .description("프로젝트 등록자 ID").optional(),
                fieldWithPath("data[].techTags").type(ARRAY).description("기술 스택"),
                fieldWithPath("data[].members").type(ARRAY).description("프로젝트 팀원"),
                fieldWithPath("data[].createdAt").type(STRING).description("등록 시각"),
                fieldWithPath("data[].updatedAt").type(STRING).description("수정 시각"),
                fieldWithPath("meta").type(OBJECT).description("페이지네이션 정보"),
                fieldWithPath("meta.nextCursor").type(STRING).description("다음 페이지 커서").optional(),
                fieldWithPath("meta.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"),
                fieldWithPath("meta.totalCount").type(NUMBER).description("전체 프로젝트 수")
        );
    }

    private static List<FieldDescriptor> approveResponseFields() {
        return List.of(
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(OBJECT).description("승인 결과"),
                fieldWithPath("data.projectId").type(NUMBER).description("프로젝트 ID"),
                new EnumFields(ApprovalStatus.class).withPath("data.approvalStatus")
                        .description("변경된 승인 상태"),
                fieldWithPath("data.decidedBy").type(OBJECT).description("승인한 관리자"),
                fieldWithPath("data.decidedBy.userId").type(NUMBER).description("관리자 사용자 ID"),
                fieldWithPath("data.decidedBy.handle").type(STRING).description("관리자 handle"),
                fieldWithPath("data.decidedAt").type(STRING).description("승인 시각")
        );
    }

    private static List<FieldDescriptor> rejectResponseFields() {
        return List.of(
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(OBJECT).description("반려 결과"),
                fieldWithPath("data.projectId").type(NUMBER).description("프로젝트 ID"),
                new EnumFields(ApprovalStatus.class).withPath("data.approvalStatus")
                        .description("변경된 승인 상태"),
                fieldWithPath("data.reason").type(STRING).description("반려 사유"),
                fieldWithPath("data.decidedBy").type(OBJECT).description("반려한 관리자"),
                fieldWithPath("data.decidedBy.userId").type(NUMBER).description("관리자 사용자 ID"),
                fieldWithPath("data.decidedBy.handle").type(STRING).description("관리자 handle"),
                fieldWithPath("data.decidedAt").type(STRING).description("반려 시각")
        );
    }

    private static List<FieldDescriptor> historyResponseFields() {
        return List.of(
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(OBJECT).description("프로젝트 심사 이력"),
                fieldWithPath("data.projectId").type(NUMBER).description("프로젝트 ID"),
                fieldWithPath("data.items").type(ARRAY).description("상태 변경 이력"),
                fieldWithPath("data.items[].historyId").type(NUMBER).description("이력 ID"),
                fieldWithPath("data.items[].fromStatus").type(STRING)
                        .description("변경 전 상태. 최초 등록이면 null").optional(),
                new EnumFields(ApprovalStatus.class).withPath("data.items[].toStatus")
                        .description("변경 후 상태"),
                fieldWithPath("data.items[].changedBy").type(OBJECT)
                        .description("상태를 변경한 사용자. 최초 등록이면 null").optional(),
                fieldWithPath("data.items[].reason").type(STRING)
                        .description("반려 사유").optional(),
                fieldWithPath("data.items[].changedAt").type(STRING).description("상태 변경 시각")
        );
    }

    private static List<FieldDescriptor> detailResponseFields() {
        return List.of(
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data").type(OBJECT).description("프로젝트 상세"),
                fieldWithPath("data.id").type(NUMBER).description("프로젝트 ID"),
                fieldWithPath("data.registeredBy").type(NUMBER).description("프로젝트 등록자 ID").optional(),
                fieldWithPath("data.slug").type(STRING).description("프로젝트 slug"),
                fieldWithPath("data.title").type(STRING).description("프로젝트 이름"),
                fieldWithPath("data.teamName").type(STRING).description("팀 이름"),
                fieldWithPath("data.tagline").type(STRING).description("한 줄 소개"),
                fieldWithPath("data.cohort").type(NUMBER).description("우아한테크코스 기수"),
                fieldWithPath("data.thumbnailImageId").type(NUMBER)
                        .description("프로젝트 썸네일 이미지 ID").optional(),
                fieldWithPath("data.imageUrl").type(STRING).description("프로젝트 썸네일 URL").optional(),
                fieldWithPath("data.descriptionMd").type(STRING).description("프로젝트 설명").optional(),
                fieldWithPath("data.descriptionMedia").type(ARRAY).description("본문 이미지"),
                fieldWithPath("data.githubRepositoryUrl").type(STRING).description("GitHub 저장소 URL"),
                fieldWithPath("data.deploymentUrl").type(STRING).description("배포 URL").optional(),
                new EnumFields(ServiceStatus.class).withPath("data.serviceStatus")
                        .description("운영 상태"),
                new EnumFields(ApprovalStatus.class).withPath("data.approvalStatus")
                        .description("승인 상태"),
                fieldWithPath("data.rejectReason").type(STRING).description("반려 사유").optional(),
                fieldWithPath("data.viewCount").type(NUMBER).description("조회 수"),
                fieldWithPath("data.starCount").type(NUMBER).description("GitHub star 수").optional(),
                fieldWithPath("data.likeCount").type(NUMBER).description("좋아요 수"),
                fieldWithPath("data.bookmarkCount").type(NUMBER).description("북마크 수"),
                fieldWithPath("data.likedByMe").type(BOOLEAN).description("요청자의 좋아요 여부"),
                fieldWithPath("data.bookmarkedByMe").type(BOOLEAN).description("요청자의 북마크 여부"),
                fieldWithPath("data.commentCount").type(NUMBER).description("댓글 수"),
                fieldWithPath("data.techTags").type(ARRAY).description("기술 스택"),
                fieldWithPath("data.members").type(ARRAY).description("프로젝트 팀원"),
                fieldWithPath("data.createdAt").type(STRING).description("등록 시각"),
                fieldWithPath("data.updatedAt").type(STRING).description("수정 시각")
        );
    }
}
