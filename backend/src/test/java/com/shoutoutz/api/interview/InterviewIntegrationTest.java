package com.shoutoutz.api.interview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.interview.application.InterviewQueryService;
import com.shoutoutz.api.interview.application.InterviewService;
import com.shoutoutz.api.interview.domain.Interview;
import com.shoutoutz.api.interview.presentation.dto.request.InterviewSaveRequest;
import com.shoutoutz.api.user.domain.account.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.transaction.annotation.Transactional;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import org.springframework.restdocs.payload.FieldDescriptor;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Transactional
class InterviewIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired InterviewService service;
    @Autowired InterviewQueryService queryService;
    long adminId;
    long intervieweeId;
    long thumbnailId;
    long imageId;

    @BeforeEach
    void setUp() {
        adminId = user("ADMIN");
        intervieweeId = user("USER");
        thumbnailId = image(adminId, "INTERVIEW_THUMBNAIL", "READY");
        imageId = image(adminId, "INTERVIEW_CONTENT", "READY");
    }

    @Test
    void 초안_등록과_관리자_원본_조회() throws Exception {
        String body = requestBody("첫 인터뷰", "![인터뷰](media://" + imageId + ")");
        mvc.perform(post("/api/v1/admin/interviews").requestAttr(sessionKey(), admin())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andDo(adminDocument("interview-create", "인터뷰 초안 등록", true, false));
        Interview draft = create();
        mvc.perform(get("/api/v1/admin/interviews/{interviewId}", draft.getId())
                        .requestAttr(sessionKey(), admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentMd").value(draft.getContentMd()))
                .andDo(adminDocument("interview-admin-detail", "인터뷰 원본과 초안 조회", false, true));
        mvc.perform(get("/api/v1/admin/interviews").requestAttr(sessionKey(), admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2))
                .andDo(listDocument("interview-admin-list", "관리자 인터뷰 목록"));
    }

    @Test
    void 공개_목록과_상세는_발행된_인터뷰만_노출한다() throws Exception {
        Interview draft = create();
        mvc.perform(get("/api/v1/interviews/{interviewId}", draft.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/interviews")).andExpect(jsonPath("$.data.length()").value(0));
        service.publish(draft.getId(), adminId);
        mvc.perform(get("/api/v1/interviews"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andDo(listDocument("interview-list", "공개 인터뷰 목록"));
        mvc.perform(get("/api/v1/interviews/{interviewId}", draft.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.interviewee.displayName").value("인터뷰이"))
                .andExpect(jsonPath("$.data.contentMd").value(org.hamcrest.Matchers.containsString("https://cdn.example.com/")))
                .andDo(detailDocument());
    }

    @Test
    void 수정_발행취소_재발행_삭제() throws Exception {
        Interview draft = create();
        mvc.perform(post("/api/v1/admin/interviews/{interviewId}/publish", draft.getId())
                        .requestAttr(sessionKey(), admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andDo(adminDocument("interview-publish", "인터뷰 발행", false, true));
        var publishedAt = queryService.find(draft.getId()).publishedAt();
        mvc.perform(put("/api/v1/admin/interviews/{interviewId}", draft.getId())
                        .requestAttr(sessionKey(), admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("수정된 인터뷰", "수정 본문")))
                .andExpect(status().isOk()).andDo(adminDocument("interview-update", "인터뷰 전체 수정", true, true));
        assertThat(queryService.find(draft.getId()).title()).isEqualTo("수정된 인터뷰");
        assertThat(queryService.find(draft.getId()).publishedAt()).isEqualTo(publishedAt);
        mvc.perform(post("/api/v1/admin/interviews/{interviewId}/unpublish", draft.getId())
                        .requestAttr(sessionKey(), admin()))
                .andExpect(status().isOk()).andDo(adminDocument("interview-unpublish", "인터뷰 발행 취소", false, true));
        assertThatThrownBy(() -> queryService.find(draft.getId())).isInstanceOf(EntityNotFoundException.class);
        service.publish(draft.getId(), adminId);
        assertThat(queryService.find(draft.getId()).publishedAt()).isEqualTo(publishedAt);
        mvc.perform(delete("/api/v1/admin/interviews/{interviewId}", draft.getId())
                        .requestAttr(sessionKey(), admin()))
                .andExpect(status().isNoContent())
                .andDo(document("interview-delete", resource(ResourceSnippetParameters.builder()
                        .tag("Interview Admin").summary("인터뷰 삭제")
                        .pathParameters(parameterWithName("interviewId").description("인터뷰 ID")).build())));
        assertThatThrownBy(() -> queryService.findAdmin(draft.getId(), adminId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 세션_역할이_관리자여도_현재_DB_상태와_권한을_검사한다() throws Exception {
        mvc.perform(post("/api/v1/admin/interviews").contentType(MediaType.APPLICATION_JSON)
                .content(requestBody("제목", "본문"))).andExpect(status().isUnauthorized());
        assertThatThrownBy(() -> service.create(intervieweeId, request("제목", "본문")))
                .isInstanceOf(ForbiddenException.class);
        jdbc.update("UPDATE users SET status = 'BANNED' WHERE id = ?", adminId);
        mvc.perform(post("/api/v1/admin/interviews").requestAttr(sessionKey(), admin())
                .contentType(MediaType.APPLICATION_JSON).content(requestBody("제목", "본문")))
                .andExpect(status().isForbidden());
    }

    @Test
    void 필수_정보가_없는_초안은_저장되지만_발행되지_않는다() {
        Interview draft = service.create(adminId, new InterviewSaveRequest("제목", null, intervieweeId, null, null));
        assertThatThrownBy(() -> service.publish(draft.getId(), adminId)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void 잘못된_입력은_400을_반환한다() throws Exception {
        mvc.perform(post("/api/v1/admin/interviews").requestAttr(sessionKey(), admin())
                .contentType(MediaType.APPLICATION_JSON).content(requestBody(" ", "본문")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/interviews").param("size", "51")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/interviews").param("cursor", "invalid")).andExpect(status().isBadRequest());
    }

    @Test
    void 이미지_소유자_용도_READY_상태를_검증한다() {
        long otherImage = image(intervieweeId, "INTERVIEW_CONTENT", "READY");
        long processingImage = image(adminId, "INTERVIEW_CONTENT", "PROCESSING");
        for (long id : new long[]{otherImage, processingImage, thumbnailId}) {
            assertThatThrownBy(() -> service.create(adminId, request("제목", "![사진](media://" + id + ")")))
                    .isInstanceOf(InvalidInputException.class);
        }
        assertThatThrownBy(() -> service.create(adminId, request("제목", "![사진](https://example.com/a.png)")))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> service.create(adminId, request("제목", "<img src='https://example.com/a.png'>")))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void 다른_관리자는_기존_이미지를_유지하며_수정할_수_있다() {
        Interview draft = create();
        long secondAdmin = user("ADMIN");
        service.update(draft.getId(), secondAdmin, request("수정", draft.getContentMd()));
        assertThat(queryService.findAdmin(draft.getId(), secondAdmin).title()).isEqualTo("수정");
    }

    @Test
    void 동일_발행일도_커서로_중복없이_조회한다() {
        for (int i = 0; i < 3; i++) {
            service.publish(create().getId(), adminId);
        }
        jdbc.update("UPDATE interviews SET published_at = '2026-10-15T00:00:00Z' WHERE author_id = ?", adminId);
        var first = queryService.findAll(null, 2);
        var second = queryService.findAll(first.meta().nextCursor(), 2);
        assertThat(first.meta().hasNext()).isTrue();
        assertThat(second.items()).hasSize(1);
        assertThat(second.items().getFirst().id()).isLessThan(first.items().getLast().id());
        assertThatThrownBy(() -> queryService.findAllAdmin(adminId, first.meta().nextCursor(), 2))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void 프로필_변경이_인터뷰에_반영된다() {
        long id = service.publish(create().getId(), adminId).getId();
        jdbc.update("UPDATE user_profiles SET display_name = '새 이름' WHERE user_id = ?", intervieweeId);
        assertThat(queryService.find(id).interviewee().displayName()).isEqualTo("새 이름");
    }

    private Interview create() {
        return service.create(adminId, request("첫 인터뷰", "![사진](media://" + imageId + ")"));
    }

    private InterviewSaveRequest request(String title, String content) {
        return new InterviewSaveRequest(title, "인터뷰 요약", intervieweeId, thumbnailId, content);
    }

    private String requestBody(String title, String content) {
        return "{\"title\":\"" + title + "\",\"summary\":\"인터뷰 요약\",\"intervieweeId\":" + intervieweeId
                + ",\"thumbnailMediaId\":" + thumbnailId + ",\"contentMd\":\"" + content + "\"}";
    }

    long user(String role) {
        long id = jdbc.queryForObject("INSERT INTO users(handle, role) VALUES (?, ?) RETURNING id", Long.class,
                "@i" + UUID.randomUUID().toString().replace("-", "").substring(0, 20), role);
        jdbc.update("INSERT INTO user_profiles(user_id, display_name) VALUES (?, '인터뷰이')", id);
        return id;
    }

    long image(long userId, String purpose, String state) {
        return jdbc.queryForObject("""
                INSERT INTO media_metadata(uploaded_by, purpose, s3_key, mime_type, size_bytes, status, expires_at)
                VALUES (?, ?, ?, 'image/webp', 1024, ?, now() + interval '1 hour') RETURNING id
                """, Long.class, userId, purpose, "media/interview/" + UUID.randomUUID(), state);
    }

    private String sessionKey() { return AuthenticatedSession.class.getName(); }
    private AuthenticatedSession admin() { return new AuthenticatedSession(adminId, UserRole.ADMIN); }

    private ResultHandler adminDocument(String name, String summary, boolean request, boolean path) {
        var builder = ResourceSnippetParameters.builder().tag("Interview Admin").summary(summary)
                .responseSchema(Schema.schema("InterviewAdminResponse")).responseFields(adminFields());
        if (path) {
            builder.pathParameters(parameterWithName("interviewId").description("인터뷰 ID"));
        }
        if (request) {
            builder.requestSchema(Schema.schema("InterviewSaveRequest")).requestFields(
                    fieldWithPath("title").type(STRING).description("제목, 최대 200자"),
                    fieldWithPath("summary").type(STRING).optional().description("요약, 최대 500자"),
                    fieldWithPath("intervieweeId").type(NUMBER).description("인터뷰 대상 사용자 ID"),
                    fieldWithPath("thumbnailMediaId").type(NUMBER).optional().description("대표 이미지 ID"),
                    fieldWithPath("contentMd").type(STRING).optional().description("원본 Markdown, 최대 100000자"));
        }
        return document(name, resource(builder.build()));
    }

    private FieldDescriptor[] adminFields() {
        return new FieldDescriptor[]{
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data.id").type(NUMBER).description("인터뷰 ID"),
                fieldWithPath("data.title").type(STRING).description("제목"),
                fieldWithPath("data.summary").type(STRING).optional().description("요약"),
                fieldWithPath("data.intervieweeId").type(NUMBER).description("인터뷰 대상 사용자 ID"),
                fieldWithPath("data.thumbnailMediaId").type(NUMBER).optional().description("대표 이미지 ID"),
                fieldWithPath("data.contentMd").type(STRING).optional().description("이미지 원본 참조가 포함된 Markdown"),
                fieldWithPath("data.status").type(STRING).description("DRAFT 또는 PUBLISHED"),
                fieldWithPath("data.publishedAt").type(STRING).optional().description("최초 발행 시각"),
                fieldWithPath("data.createdAt").type(STRING).description("생성 시각"),
                fieldWithPath("data.updatedAt").type(STRING).description("수정 시각")};
    }

    private ResultHandler listDocument(String name, String summary) {
        List<FieldDescriptor> fields = commonFields("data[]");
        fields.add(fieldWithPath("data").type(ARRAY).description("인터뷰 목록"));
        fields.add(fieldWithPath("data[].status").type(STRING).description("발행 상태"));
        fields.add(fieldWithPath("meta.nextCursor").type(STRING).optional().description("다음 커서"));
        fields.add(fieldWithPath("meta.hasNext").type(BOOLEAN).description("다음 페이지 여부"));
        fields.add(fieldWithPath("meta.totalCount").type(NUMBER).description("전체 개수"));
        return document(name, resource(ResourceSnippetParameters.builder().tag("Interview").summary(summary)
                .queryParameters(parameterWithName("size").optional().description("1~50, 기본 20"),
                        parameterWithName("cursor").optional().description("다음 커서"))
                .responseFields(fields).build()));
    }

    private ResultHandler detailDocument() {
        List<FieldDescriptor> fields = commonFields("data");
        fields.add(fieldWithPath("data.contentMd").type(STRING).description("이미지 URL로 변환한 Markdown"));
        return document("interview-detail", resource(ResourceSnippetParameters.builder().tag("Interview")
                .summary("발행된 인터뷰 상세 조회")
                .pathParameters(parameterWithName("interviewId").description("인터뷰 ID"))
                .responseFields(fields).build()));
    }

    private List<FieldDescriptor> commonFields(String prefix) {
        return new ArrayList<>(List.of(
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath(prefix + ".id").type(NUMBER).description("인터뷰 ID"),
                fieldWithPath(prefix + ".title").type(STRING).description("제목"),
                fieldWithPath(prefix + ".summary").type(STRING).optional().description("요약"),
                fieldWithPath(prefix + ".thumbnailUrl").type(STRING).optional().description("대표 이미지 URL"),
                fieldWithPath(prefix + ".interviewee.handle").type(STRING).optional().description("사용자 핸들"),
                fieldWithPath(prefix + ".interviewee.displayName").type(STRING).description("표시 이름"),
                fieldWithPath(prefix + ".interviewee.bio").type(STRING).optional().description("현재 사용자 소개"),
                fieldWithPath(prefix + ".interviewee.avatarUrl").type(STRING).optional().description("프로필 이미지 URL"),
                fieldWithPath(prefix + ".publishedAt").type(STRING).optional().description("최초 발행 시각"),
                fieldWithPath(prefix + ".createdAt").type(STRING).description("생성 시각"),
                fieldWithPath(prefix + ".updatedAt").type(STRING).description("수정 시각")));
    }
}
