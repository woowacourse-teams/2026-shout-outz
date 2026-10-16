package com.shoutoutz.api.interview;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import com.shoutoutz.api.auth.presentation.session.AuthenticatedSession;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.interview.application.InterviewReactionService;
import com.shoutoutz.api.interview.application.InterviewService;
import com.shoutoutz.api.interview.presentation.dto.request.InterviewSaveRequest;
import com.shoutoutz.api.interview.presentation.dto.response.InterviewAdminResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Transactional
class InterviewIntegrationTest {
    private static final VisitorKey VISITOR = new VisitorKey("a".repeat(64));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private InterviewReactionService interviewReactionService;

    private long adminId;
    private long intervieweeId;
    private long thumbnailId;
    private long imageId;

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
        mockMvc.perform(post("/api/v1/admin/interviews").requestAttr(sessionKey(), admin())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andDo(adminDocument("interview-create", "인터뷰 초안 등록", true, false));
        InterviewAdminResponse draft = create();
        mockMvc.perform(get("/api/v1/admin/interviews/{interviewId}", draft.id())
                        .requestAttr(sessionKey(), admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentMd").value(draft.contentMd()))
                .andExpect(jsonPath("$.data.previewContentMd").value(Matchers.containsString("https://cdn.example.com/")))
                .andDo(adminDocument("interview-admin-detail", "인터뷰 원본과 초안 조회", false, true));
        mockMvc.perform(get("/api/v1/admin/interviews").requestAttr(sessionKey(), admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2))
                .andDo(listDocument("interview-admin-list", "관리자 인터뷰 목록", "Interview Admin"));
    }

    @Test
    void 공개_목록과_상세는_발행된_인터뷰만_노출한다() throws Exception {
        InterviewAdminResponse draft = create();
        mockMvc.perform(get("/api/v1/interviews/{interviewId}", draft.id())).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/interviews")).andExpect(jsonPath("$.data.length()").value(0));
        interviewService.publish(draft.id(), adminId);
        mockMvc.perform(get("/api/v1/interviews"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andDo(listDocument("interview-list", "공개 인터뷰 목록", "Interview"));
        mockMvc.perform(get("/api/v1/interviews/{interviewId}", draft.id()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.interviewee.displayName").value("인터뷰이"))
                .andExpect(jsonPath("$.data.contentMd").value(Matchers.containsString("https://cdn.example.com/")))
                .andDo(detailDocument());
    }

    @Test
    void 수정_발행취소_재발행_삭제() throws Exception {
        InterviewAdminResponse draft = create();
        mockMvc.perform(post("/api/v1/admin/interviews/{interviewId}/publish", draft.id())
                        .requestAttr(sessionKey(), admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andDo(adminDocument("interview-publish", "인터뷰 발행", false, true));
        var publishedAt = interviewService.find(draft.id(), null, VISITOR).publishedAt();
        mockMvc.perform(put("/api/v1/admin/interviews/{interviewId}", draft.id())
                        .requestAttr(sessionKey(), admin()).contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("수정된 인터뷰", "수정 본문")))
                .andExpect(status().isOk()).andDo(adminDocument("interview-update", "인터뷰 전체 수정", true, true));
        assertThat(interviewService.find(draft.id(), null, VISITOR).title()).isEqualTo("수정된 인터뷰");
        assertThat(interviewService.find(draft.id(), null, VISITOR).publishedAt()).isEqualTo(publishedAt);
        mockMvc.perform(post("/api/v1/admin/interviews/{interviewId}/unpublish", draft.id())
                        .requestAttr(sessionKey(), admin()))
                .andExpect(status().isOk()).andDo(adminDocument("interview-unpublish", "인터뷰 발행 취소", false, true));
        assertThatThrownBy(() -> interviewService.find(draft.id(), null, VISITOR)).isInstanceOf(EntityNotFoundException.class);
        interviewService.publish(draft.id(), adminId);
        assertThat(interviewService.find(draft.id(), null, VISITOR).publishedAt()).isEqualTo(publishedAt);
        mockMvc.perform(delete("/api/v1/admin/interviews/{interviewId}", draft.id())
                        .requestAttr(sessionKey(), admin()))
                .andExpect(status().isNoContent())
                .andDo(document("interview-delete", resource(ResourceSnippetParameters.builder()
                        .tag("Interview Admin").summary("인터뷰 삭제")
                        .pathParameters(parameterWithName("interviewId").description("인터뷰 ID")).build())));
        assertThatThrownBy(() -> interviewService.findAdmin(draft.id(), adminId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 세션_역할이_관리자여도_현재_DB_상태와_권한을_검사한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/interviews").contentType(MediaType.APPLICATION_JSON)
                .content(requestBody("제목", "본문"))).andExpect(status().isUnauthorized());
        assertThatThrownBy(() -> interviewService.create(intervieweeId, request("제목", "본문")))
                .isInstanceOf(ForbiddenException.class);
        jdbcTemplate.update("UPDATE users SET status = 'BANNED' WHERE id = ?", adminId);
        mockMvc.perform(post("/api/v1/admin/interviews").requestAttr(sessionKey(), admin())
                .contentType(MediaType.APPLICATION_JSON).content(requestBody("제목", "본문")))
                .andExpect(status().isForbidden());
    }

    @Test
    void 필수_정보가_없는_초안은_저장되지만_발행되지_않는다() {
        InterviewAdminResponse draft = interviewService.create(adminId, new InterviewSaveRequest("제목", null, intervieweeId, null, null));
        assertThatThrownBy(() -> interviewService.publish(draft.id(), adminId)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void 잘못된_입력은_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/interviews").requestAttr(sessionKey(), admin())
                .contentType(MediaType.APPLICATION_JSON).content(requestBody(" ", "본문")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/interviews").param("size", "51")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/interviews").param("cursor", "invalid")).andExpect(status().isBadRequest());
    }

    @Test
    void 이미지_소유자_용도_READY_상태를_검증한다() {
        long otherImage = image(intervieweeId, "INTERVIEW_CONTENT", "READY");
        long processingImage = image(adminId, "INTERVIEW_CONTENT", "PROCESSING");
        for (long id : new long[]{otherImage, processingImage, thumbnailId}) {
            assertThatThrownBy(() -> interviewService.create(adminId, request("제목", "![사진](media://" + id + ")")))
                    .isInstanceOf(InvalidInputException.class);
        }
        assertThatThrownBy(() -> interviewService.create(adminId, request("제목", "![사진](https://example.com/a.png)")))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> interviewService.create(adminId, request("제목", "<img src='https://example.com/a.png'>")))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void 다른_관리자는_기존_이미지를_유지하며_수정할_수_있다() {
        InterviewAdminResponse draft = create();
        long secondAdmin = user("ADMIN");
        interviewService.update(draft.id(), secondAdmin, request("수정", draft.contentMd()));
        assertThat(interviewService.findAdmin(draft.id(), secondAdmin).title()).isEqualTo("수정");
    }

    @Test
    void 동일_발행일도_커서로_중복없이_조회한다() {
        for (int i = 0; i < 3; i++) {
            interviewService.publish(create().id(), adminId);
        }
        jdbcTemplate.update("UPDATE interviews SET published_at = '2026-10-15T00:00:00Z' WHERE author_id = ?", adminId);
        var first = interviewService.findAll(null, 2);
        var second = interviewService.findAll(first.meta().nextCursor(), 2);
        assertThat(first.meta().hasNext()).isTrue();
        assertThat(second.items()).hasSize(1);
        assertThat(second.items().getFirst().id()).isLessThan(first.items().getLast().id());
        assertThatThrownBy(() -> interviewService.findAllAdmin(adminId, first.meta().nextCursor(), 2))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void 프로필_변경이_인터뷰에_반영된다() {
        long id = interviewService.publish(create().id(), adminId).id();
        jdbcTemplate.update("UPDATE user_profiles SET display_name = '새 이름' WHERE user_id = ?", intervieweeId);
        assertThat(interviewService.find(id, null, VISITOR).interviewee().displayName()).isEqualTo("새 이름");
    }

    @Test
    void 좋아요는_중복되지_않고_취소와_내_반응이_조회에_반영된다() {
        long id = interviewService.publish(create().id(), adminId).id();
        interviewReactionService.add(id, intervieweeId, "LIKE");
        interviewReactionService.add(id, intervieweeId, "LIKE");
        assertThat(interviewService.find(id, intervieweeId, VISITOR).likeCount()).isEqualTo(1);
        assertThat(interviewService.find(id, intervieweeId, VISITOR).likedByMe()).isTrue();
        assertThat(interviewService.find(id, null, VISITOR).likedByMe()).isFalse();
        assertThat(interviewService.findAll(null, 20, intervieweeId).items().getFirst().likedByMe()).isTrue();
        interviewReactionService.remove(id, intervieweeId, "LIKE");
        assertThat(interviewService.find(id, intervieweeId, VISITOR).likeCount()).isZero();
        assertThatThrownBy(() -> interviewReactionService.remove(id, intervieweeId, "LIKE"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 초안과_발행취소_삭제된_인터뷰에는_좋아요를_남길_수_없다() {
        long id = create().id();
        assertThatThrownBy(() -> interviewReactionService.add(id, intervieweeId, "LIKE"))
                .isInstanceOf(EntityNotFoundException.class);
        interviewService.publish(id, adminId);
        interviewService.unpublish(id, adminId);
        assertThatThrownBy(() -> interviewReactionService.add(id, intervieweeId, "LIKE"))
                .isInstanceOf(EntityNotFoundException.class);
        interviewService.delete(id, adminId);
        assertThatThrownBy(() -> interviewReactionService.add(id, intervieweeId, "LIKE"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 상세_GET에서_조회수를_집계하고_같은_방문자는_하루에_한번만_반영한다() throws Exception {
        long id = create().id();
        var cookie = new Cookie("VISITOR_ID", "3f2a1b4c-5d6e-4f70-8a9b-0c1d2e3f4a5b");
        mockMvc.perform(get("/api/v1/interviews/{interviewId}", id).cookie(cookie)).andExpect(status().isNotFound());
        interviewService.publish(id, adminId);
        var updatedAt = interviewService.findAdmin(id, adminId).updatedAt();
        mockMvc.perform(get("/api/v1/interviews/{interviewId}", id).cookie(cookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.viewCount").value(1));
        mockMvc.perform(get("/api/v1/interviews/{interviewId}", id).cookie(cookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.viewCount").value(1));
        assertThat(interviewService.findAdmin(id, adminId).updatedAt()).isEqualTo(updatedAt);
        assertThat(jdbcTemplate.queryForObject("SELECT view_count FROM interviews WHERE id = ?", Long.class, id))
                .isEqualTo(1);
        interviewService.unpublish(id, adminId);
        mockMvc.perform(get("/api/v1/interviews/{interviewId}", id).cookie(cookie)).andExpect(status().isNotFound());
        assertThat(jdbcTemplate.queryForObject("SELECT view_count FROM interviews WHERE id = ?", Long.class, id))
                .isEqualTo(1);
    }

    @Test
    void 인터뷰이_아바타도_기존_GitHub_대체_규칙을_따른다() {
        jdbcTemplate.update("""
                INSERT INTO oauth_accounts(user_id, provider, provider_account_id, provider_avatar_url)
                VALUES (?, 'GITHUB', ?, 'https://avatars.githubusercontent.com/u/123')
                """, intervieweeId, UUID.randomUUID().toString());
        long id = interviewService.publish(create().id(), adminId).id();
        assertThat(interviewService.find(id, null, VISITOR).interviewee().avatarUrl())
                .isEqualTo("https://avatars.githubusercontent.com/u/123");
        assertThat(interviewService.findAll(null, 20).items().getFirst().interviewee().avatarUrl())
                .isEqualTo("https://avatars.githubusercontent.com/u/123");
    }

    @Test
    void 저장된_이미지_URL을_다시_보내도_원본_미디어_참조를_유지한다() {
        long id = interviewService.publish(create().id(), adminId).id();
        String publicContent = interviewService.find(id, null, VISITOR).contentMd();
        interviewService.update(id, adminId, request("수정", publicContent));
        var adminView = interviewService.findAdmin(id, adminId);
        assertThat(adminView.contentMd()).contains("media://" + imageId);
        assertThat(adminView.previewContentMd()).contains("https://cdn.example.com/");
        assertThat(adminView.thumbnailUrl()).isNotNull();
    }

    @Test
    void 인터뷰_API는_관리자와_공개_경로_모두_interviews를_사용한다() {
        var paths = requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().getBeanType().getSimpleName().startsWith("Interview"))
                .flatMap(entry -> entry.getKey().getPatternValues().stream())
                .toList();

        assertThat(paths).isNotEmpty().allSatisfy(path ->
                assertThat(path).matches("/api/v1/(admin/)?interviews(/.*)?"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"BANNED", "DELETED"})
    void 정지와_탈퇴_후_남은_세션으로_좋아요를_변경할_수_없다(String userStatus) throws Exception {
        long id = interviewService.publish(create().id(), adminId).id();
        long actorId = user("USER");
        jdbcTemplate.update("INSERT INTO interview_reactions(interview_id, user_id, reaction_type) VALUES (?, ?, 'LIKE')",
                id, actorId);
        jdbcTemplate.update("""
                UPDATE users SET status = ?,
                    deleted_at = CASE WHEN ? = 'DELETED' THEN now() ELSE NULL END
                WHERE id = ?
                """, userStatus, userStatus, actorId);
        entityManager.clear();
        var authentication = new AuthenticatedSession(actorId, UserRole.USER);
        mockMvc.perform(put("/api/v1/interviews/{interviewId}/reactions/{type}", id, "LIKE")
                        .requestAttr(sessionKey(), authentication))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INTERVIEW_USER_FORBIDDEN"))
                .andDo(document("interview-reaction-add-inactive-user", resource(ResourceSnippetParameters.builder()
                        .tag("Interview Reaction").summary("인터뷰 반응 추가")
                        .pathParameters(parameterWithName("interviewId").description("인터뷰 ID"),
                                parameterWithName("type").description("LIKE"))
                        .responseFields(RestDocsFields.errorResponse())
                        .build())));
        mockMvc.perform(delete("/api/v1/interviews/{interviewId}/reactions/{type}", id, "LIKE")
                        .requestAttr(sessionKey(), authentication))
                .andExpect(status().isForbidden());
        assertThat(interviewService.find(id, null, VISITOR).likeCount()).isEqualTo(1);
    }

    private InterviewAdminResponse create() {
        return interviewService.create(adminId, request("첫 인터뷰", "![사진](media://" + imageId + ")"));
    }

    private InterviewSaveRequest request(String title, String content) {
        return new InterviewSaveRequest(title, "인터뷰 요약", intervieweeId, thumbnailId, content);
    }

    private String requestBody(String title, String content) {
        return "{\"title\":\"" + title + "\",\"summary\":\"인터뷰 요약\",\"intervieweeId\":" + intervieweeId
                + ",\"thumbnailMediaId\":" + thumbnailId + ",\"contentMd\":\"" + content + "\"}";
    }

    private long user(String role) {
        long id = jdbcTemplate.queryForObject("INSERT INTO users(handle, role) VALUES (?, ?) RETURNING id", Long.class,
                "@i" + UUID.randomUUID().toString().replace("-", "").substring(0, 20), role);
        jdbcTemplate.update("INSERT INTO user_profiles(user_id, display_name) VALUES (?, '인터뷰이')", id);
        return id;
    }

    private long image(long userId, String purpose, String state) {
        return jdbcTemplate.queryForObject("""
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
                    fieldWithPath("thumbnailMediaId").type(NUMBER).optional().description("INTERVIEW_THUMBNAIL 용도의 READY 미디어 ID"),
                    fieldWithPath("contentMd").type(STRING).optional().description("원본 Markdown, 최대 100000자. 본문 이미지는 INTERVIEW_CONTENT 용도의 media://ID로 참조"));
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
                fieldWithPath("data.thumbnailUrl").type(STRING).optional().description("대표 이미지 미리보기 URL"),
                fieldWithPath("data.previewContentMd").type(STRING).optional().description("이미지 URL로 변환한 미리보기 Markdown"),
                fieldWithPath("data.thumbnailMediaId").type(NUMBER).optional().description("INTERVIEW_THUMBNAIL 용도의 READY 미디어 ID"),
                fieldWithPath("data.contentMd").type(STRING).optional().description("이미지 원본 참조가 포함된 Markdown"),
                fieldWithPath("data.status").type(STRING).description("DRAFT 또는 PUBLISHED"),
                fieldWithPath("data.publishedAt").type(STRING).optional().description("최초 발행 시각"),
                fieldWithPath("data.createdAt").type(STRING).description("생성 시각"),
                fieldWithPath("data.updatedAt").type(STRING).description("수정 시각")};
    }

    private ResultHandler listDocument(String name, String summary, String tag) {
        List<FieldDescriptor> fields = commonFields("data[]");
        fields.add(fieldWithPath("data").type(ARRAY).description("인터뷰 목록"));
        fields.add(fieldWithPath("data[].status").type(STRING).description("발행 상태"));
        fields.add(fieldWithPath("meta.nextCursor").type(STRING).optional().description("다음 커서"));
        fields.add(fieldWithPath("meta.hasNext").type(BOOLEAN).description("다음 페이지 여부"));
        fields.add(fieldWithPath("meta.totalCount").type(NUMBER).description("전체 개수"));
        return document(name, resource(ResourceSnippetParameters.builder().tag(tag).summary(summary)
                .queryParameters(parameterWithName("size").type(SimpleType.INTEGER).optional().description("1~50, 기본 20"),
                        parameterWithName("cursor").optional().description("다음 커서"))
                .responseFields(fields).build()));
    }

    private ResultHandler detailDocument() {
        List<FieldDescriptor> fields = commonFields("data");
        fields.add(fieldWithPath("data.contentMd").type(STRING).description("이미지 URL로 변환한 Markdown"));
        return document("interview-detail", resource(ResourceSnippetParameters.builder().tag("Interview")
                .summary("발행된 인터뷰 상세 조회")
                .description("상세 조회 시 VISITOR_ID 쿠키의 방문자별로 한국 날짜 기준 하루 한 번 조회수를 집계합니다. 응답의 viewCount에 이번 조회가 반영됩니다.")
                .pathParameters(parameterWithName("interviewId").description("인터뷰 ID"))
                .responseFields(fields).build()));
    }

    private List<FieldDescriptor> commonFields(String prefix) {
        return new ArrayList<>(List.of(
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath(prefix + ".viewCount").type(NUMBER).description("누적 조회수"),
                fieldWithPath(prefix + ".commentCount").type(NUMBER).description("삭제되지 않은 댓글과 대댓글 수"),
                fieldWithPath(prefix + ".likeCount").type(NUMBER).description("좋아요 수"),
                fieldWithPath(prefix + ".likedByMe").type(BOOLEAN).description("내 좋아요 여부"),
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
