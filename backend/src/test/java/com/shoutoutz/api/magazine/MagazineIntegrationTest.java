package com.shoutoutz.api.magazine;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.request;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.epages.restdocs.apispec.SimpleType;
import com.shoutoutz.api.auth.presentation.security.CsrfTokenManager;
import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.InvalidInputException;
import com.shoutoutz.api.common.restdocs.RestDocsFields;
import com.shoutoutz.api.magazine.application.MagazineReactionService;
import com.shoutoutz.api.magazine.application.MagazineService;
import com.shoutoutz.api.magazine.presentation.dto.request.MagazineSaveRequest;
import com.shoutoutz.api.magazine.presentation.dto.response.MagazineAdminResponse;
import com.shoutoutz.api.magazine.presentation.dto.response.MagazineAuthorResponse;
import com.shoutoutz.api.user.domain.account.UserRole;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Transactional
class MagazineIntegrationTest {
    private static final VisitorKey VISITOR = new VisitorKey("a".repeat(64));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthSessionAccessor authSessionAccessor;

    @Autowired
    private CsrfTokenManager csrfTokenManager;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private MagazineService magazineService;

    @Autowired
    private MagazineReactionService magazineReactionService;

    private long adminId;
    private long featuredUserId;
    private long thumbnailId;
    private long imageId;

    @BeforeEach
    void setUp() {
        adminId = user("ADMIN");
        jdbcTemplate.update("UPDATE user_profiles SET display_name = '편집자' WHERE user_id = ?", adminId);
        featuredUserId = user("USER");
        thumbnailId = image(adminId, "MAGAZINE_THUMBNAIL", "READY");
        imageId = image(adminId, "MAGAZINE_CONTENT", "READY");
    }

    @Test
    void 초안_등록과_관리자_원본_조회() throws Exception {
        String body = requestBody("첫 매거진", "![매거진](media://" + imageId + ")");
        mockMvc.perform(post("/api/v1/admin/magazines").with(authenticated(adminId, UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.author.displayName").value("편집자"))
                .andDo(adminDocument("magazine-create", "매거진 초안 등록",
                        "활성 관리자가 초안을 등록합니다. 요약, 대표 이미지, 본문은 초안에서는 생략할 수 있습니다.", true, false));
        MagazineAdminResponse draft = create();
        mockMvc.perform(get("/api/v1/admin/magazines/{magazineId}", draft.id())
                        .with(authenticated(adminId, UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentMd").value(draft.contentMd()))
                .andExpect(jsonPath("$.data.author.displayName").value("편집자"))
                .andExpect(jsonPath("$.data.previewContentMd").value(Matchers.containsString("https://cdn.example.com/")))
                .andDo(adminDocument("magazine-admin-detail", "매거진 원본과 초안 조회",
                        "활성 관리자가 초안을 포함한 매거진 원본을 조회합니다. 본문의 media:// 참조와 이미지 URL로 변환한 미리보기를 함께 반환합니다.", false, true));
        mockMvc.perform(get("/api/v1/admin/magazines").with(authenticated(adminId, UserRole.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].author.displayName").value("편집자"))
                .andDo(listDocument("magazine-admin-list", "관리자 매거진 목록", "Magazine Admin"));
    }

    @Test
    void 공개_목록과_상세는_발행된_매거진만_노출한다() throws Exception {
        MagazineAdminResponse draft = create();
        mockMvc.perform(get("/api/v1/magazines/{magazineId}", draft.id()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MAGAZINE_NOT_FOUND"))
                .andDo(errorDocument("magazine-detail-not-found", "Magazine", true));
        mockMvc.perform(get("/api/v1/magazines")).andExpect(jsonPath("$.data.length()").value(0));
        magazineService.publish(draft.id(), adminId);
        mockMvc.perform(get("/api/v1/magazines"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].author.displayName").value("편집자"))
                .andDo(listDocument("magazine-list", "공개 매거진 목록", "Magazine"));
        mockMvc.perform(get("/api/v1/magazines/{magazineId}", draft.id()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.featuredUser.displayName").value("소개 회원"))
                .andExpect(jsonPath("$.data.author.displayName").value("편집자"))
                .andExpect(jsonPath("$.data.contentMd").value(Matchers.containsString("https://cdn.example.com/")))
                .andDo(detailDocument());
    }

    @Test
    void 매거진_ID는_int_범위를_넘는_정수도_받는다() throws Exception {
        long id = 3_000_000_000L;
        jdbcTemplate.update("""
                INSERT INTO magazines(id, title, featured_user_id, author_id, status, published_at)
                VALUES (?, '큰 ID 매거진', ?, ?, 'PUBLISHED', now())
                """, id, featuredUserId, adminId);

        mockMvc.perform(get("/api/v1/magazines/{magazineId}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
        mockMvc.perform(get("/api/v1/admin/magazines/{magazineId}", id)
                        .with(authenticated(adminId, UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
        mockMvc.perform(get("/api/v1/magazines/{magazineId}", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 수정_발행취소_재발행_삭제() throws Exception {
        MagazineAdminResponse draft = create();
        mockMvc.perform(post("/api/v1/admin/magazines/{magazineId}/publish", draft.id())
                        .with(authenticated(adminId, UserRole.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andDo(adminDocument("magazine-publish", "매거진 발행",
                        "요약, 대표 이미지, 본문이 있는 매거진을 공개합니다. 재발행해도 최초 발행 시각은 유지됩니다.", false, true));
        var publishedAt = magazineService.find(draft.id(), null, VISITOR).publishedAt();
        mockMvc.perform(put("/api/v1/admin/magazines/{magazineId}", draft.id())
                        .with(authenticated(adminId, UserRole.ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("수정된 매거진", "수정 본문")))
                .andExpect(status().isOk()).andDo(adminDocument("magazine-update", "매거진 전체 수정",
                        "편집 가능한 필드를 전체 교체하며 생략한 선택 필드는 null로 처리합니다. 발행된 글의 수정은 즉시 공개되며 요약, 대표 이미지, 본문을 비울 수 없습니다.", true, true));
        assertThat(magazineService.find(draft.id(), null, VISITOR).title()).isEqualTo("수정된 매거진");
        assertThat(magazineService.find(draft.id(), null, VISITOR).publishedAt()).isEqualTo(publishedAt);
        mockMvc.perform(post("/api/v1/admin/magazines/{magazineId}/unpublish", draft.id())
                        .with(authenticated(adminId, UserRole.ADMIN)))
                .andExpect(status().isOk()).andDo(adminDocument("magazine-unpublish", "매거진 발행 취소",
                        "초안으로 전환해 공개 조회와 댓글, 반응을 차단합니다. 최초 발행 시각은 유지됩니다.", false, true));
        assertThatThrownBy(() -> magazineService.find(draft.id(), null, VISITOR)).isInstanceOf(EntityNotFoundException.class);
        magazineService.publish(draft.id(), adminId);
        assertThat(magazineService.find(draft.id(), null, VISITOR).publishedAt()).isEqualTo(publishedAt);
        mockMvc.perform(delete("/api/v1/admin/magazines/{magazineId}", draft.id())
                        .with(authenticated(adminId, UserRole.ADMIN)))
                .andExpect(status().isNoContent())
                .andDo(document("magazine-delete", resource(ResourceSnippetParameters.builder()
                        .tag("Magazine Admin").summary("매거진 삭제")
                        .description("활성 관리자가 매거진을 soft delete합니다. 삭제 후 관리자와 공개 API에서 조회할 수 없으며 응답 본문은 없습니다.")
                        .requestHeaders(
                                headerWithName(HttpHeaders.COOKIE).description("활성 관리자 세션의 JSESSIONID"),
                                headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰"))
                        .pathParameters(parameterWithName("magazineId").type(SimpleType.INTEGER).description("매거진 ID")).build())));
        assertThatThrownBy(() -> magazineService.findAdmin(draft.id(), adminId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 세션_역할이_관리자여도_현재_DB_상태와_권한을_검사한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/magazines").contentType(MediaType.APPLICATION_JSON)
                .content(requestBody("제목", "본문"))).andExpect(status().isUnauthorized());
        assertThatThrownBy(() -> magazineService.create(featuredUserId, saveRequest("제목", "본문")))
                .isInstanceOf(ForbiddenException.class);
        jdbcTemplate.update("UPDATE users SET status = 'BANNED' WHERE id = ?", adminId);
        mockMvc.perform(post("/api/v1/admin/magazines").with(authenticated(adminId, UserRole.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(requestBody("제목", "본문")))
                .andExpect(status().isForbidden());
    }

    @Test
    void 필수_정보가_없는_초안은_저장되지만_발행되지_않는다() throws Exception {
        MagazineAdminResponse draft = magazineService.create(adminId, new MagazineSaveRequest("제목", null, featuredUserId, null, null));
        mockMvc.perform(post("/api/v1/admin/magazines/{magazineId}/publish", draft.id())
                        .with(authenticated(adminId, UserRole.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MAGAZINE_PUBLISH_INCOMPLETE"))
                .andDo(errorDocument("magazine-publish-incomplete", "Magazine Admin", true));
    }

    @ParameterizedTest
    @MethodSource("adminEndpoints")
    void 관리자_API는_로그인하지_않으면_401을_반환한다(String operation, String method, String path) throws Exception {
        mockMvc.perform(adminRequest(method, path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andDo(errorDocument(operation + "-unauthorized",
                        "Magazine Admin", path.contains("{magazineId}")));
    }

    @ParameterizedTest
    @MethodSource("adminEndpoints")
    void 관리자_API는_일반_회원에게_403을_반환한다(String operation, String method, String path) throws Exception {
        mockMvc.perform(adminRequest(method, path).with(authenticated(featuredUserId, UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MAGAZINE_ADMIN_FORBIDDEN"))
                .andDo(errorDocument(operation + "-forbidden",
                        "Magazine Admin", path.contains("{magazineId}")));
    }

    @ParameterizedTest
    @MethodSource("adminWriteEndpoints")
    void 관리자_변경_API는_CSRF_토큰이_없으면_403을_반환한다(String operation, String method, String path) throws Exception {
        var session = new MockHttpSession();
        authSessionAccessor.saveAuthentication(session, adminId, UserRole.ADMIN);
        csrfTokenManager.getOrCreate(session);
        mockMvc.perform(adminRequest(method, path).session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"))
                .andDo(errorDocument(operation + "-csrf-missing",
                        "Magazine Admin", path.contains("{magazineId}")));
    }

    @Test
    void 관리자라도_다른_CSRF_토큰을_보내면_403을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/magazines")
                        .with(authenticated(adminId, UserRole.ADMIN))
                        .with(request -> {
                            request.removeHeader("X-CSRF-Token");
                            request.addHeader("X-CSRF-Token", "invalid-token");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody("제목", "본문")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"))
                .andDo(errorDocument("magazine-create-csrf-invalid", "Magazine Admin", false));
    }

    @ParameterizedTest
    @MethodSource("adminResourceEndpoints")
    void 없는_매거진을_관리하면_404를_반환한다(String operation, String method, String path) throws Exception {
        mockMvc.perform(adminRequest(method, path).with(authenticated(adminId, UserRole.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MAGAZINE_NOT_FOUND"))
                .andDo(errorDocument(operation + "-not-found", "Magazine Admin", true));
    }

    @Test
    void 제목이_빈_매거진_요청은_400과_필드_오류를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/magazines").with(authenticated(adminId, UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody("", "본문")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("title"))
                .andDo(errorDocument("magazine-create-validation-failed", "Magazine Admin", false));
        mockMvc.perform(put("/api/v1/admin/magazines/{magazineId}", create().id())
                        .with(authenticated(adminId, UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody("", "본문")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andDo(errorDocument("magazine-update-validation-failed", "Magazine Admin", true));
    }

    @Test
    void 등록할_소개_회원이_없으면_404를_반환한다() throws Exception {
        String body = requestBody("제목", "본문")
                .replace("\"featuredUserId\":" + featuredUserId, "\"featuredUserId\":" + Long.MAX_VALUE);
        mockMvc.perform(post("/api/v1/admin/magazines").with(authenticated(adminId, UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FEATURED_USER_NOT_FOUND"))
                .andDo(errorDocument("magazine-create-featuredUser-not-found", "Magazine Admin", false));
    }

    @Test
    void 초안_전체_수정에서_생략한_선택_필드는_비워진다() throws Exception {
        long id = create().id();
        mockMvc.perform(put("/api/v1/admin/magazines/{magazineId}", id)
                        .with(authenticated(adminId, UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"새 제목\",\"featuredUserId\":" + featuredUserId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("새 제목"))
                .andExpect(jsonPath("$.data.summary").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.thumbnailMediaId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.contentMd").value(Matchers.nullValue()));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 51})
    void 매거진_목록_조회_크기가_범위를_벗어나면_400을_반환한다(int size) throws Exception {
        mockMvc.perform(get("/api/v1/magazines").queryParam("size", String.valueOf(size)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MAGAZINE_INVALID_SIZE"))
                .andDo(errorDocument("magazine-list-invalid-size-" + size, "Magazine", false));
        mockMvc.perform(get("/api/v1/admin/magazines").with(authenticated(adminId, UserRole.ADMIN))
                        .queryParam("size", String.valueOf(size)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MAGAZINE_INVALID_SIZE"))
                .andDo(errorDocument("magazine-admin-list-invalid-size-" + size, "Magazine Admin", false));
    }

    @Test
    void 잘못된_매거진_목록_커서는_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/magazines").queryParam("cursor", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MAGAZINE_INVALID_CURSOR"))
                .andDo(errorDocument("magazine-list-invalid-cursor", "Magazine", false));
    }

    private static Stream<Arguments> adminResourceEndpoints() {
        return Stream.of(
                Arguments.of("magazine-admin-detail", "GET", "/api/v1/admin/magazines/{magazineId}"),
                Arguments.of("magazine-update", "PUT", "/api/v1/admin/magazines/{magazineId}"),
                Arguments.of("magazine-delete", "DELETE", "/api/v1/admin/magazines/{magazineId}"),
                Arguments.of("magazine-publish", "POST", "/api/v1/admin/magazines/{magazineId}/publish"),
                Arguments.of("magazine-unpublish", "POST", "/api/v1/admin/magazines/{magazineId}/unpublish")
        );
    }

    private static Stream<Arguments> adminWriteEndpoints() {
        return Stream.concat(
                Stream.of(Arguments.of("magazine-create", "POST", "/api/v1/admin/magazines")),
                adminResourceEndpoints().filter(endpoint -> !endpoint.get()[1].equals("GET"))
        );
    }

    private static Stream<Arguments> adminEndpoints() {
        return Stream.concat(
                Stream.of(Arguments.of("magazine-create", "POST", "/api/v1/admin/magazines"),
                        Arguments.of("magazine-admin-list", "GET", "/api/v1/admin/magazines")),
                adminResourceEndpoints()
        );
    }


    @Test
    void 이미지_소유자_용도_READY_상태를_검증한다() throws Exception {
        long otherImage = image(featuredUserId, "MAGAZINE_CONTENT", "READY");
        long processingImage = image(adminId, "MAGAZINE_CONTENT", "PROCESSING");
        for (long id : new long[]{otherImage, processingImage, thumbnailId}) {
            assertThatThrownBy(() -> magazineService.create(adminId, saveRequest("제목", "![사진](media://" + id + ")")))
                    .isInstanceOf(InvalidInputException.class);
        }
        assertThatThrownBy(() -> magazineService.create(adminId, saveRequest("제목", "![사진](https://example.com/a.png)")))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> magazineService.create(adminId, saveRequest("제목", "<img src='https://example.com/a.png'>")))
                .isInstanceOf(InvalidInputException.class);
        mockMvc.perform(post("/api/v1/admin/magazines").with(authenticated(adminId, UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("제목", "![사진](https://example.com/a.png)")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MAGAZINE_INVALID_MEDIA"))
                .andDo(errorDocument("magazine-create-invalid-media", "Magazine Admin", false));
    }

    @Test
    void 다른_관리자는_기존_이미지를_유지하며_수정할_수_있다() {
        MagazineAdminResponse draft = create();
        long secondAdmin = user("ADMIN");
        magazineService.update(draft.id(), secondAdmin, saveRequest("수정", draft.contentMd()));
        assertThat(magazineService.findAdmin(draft.id(), secondAdmin).title()).isEqualTo("수정");
    }

    @Test
    void 동일_발행일도_커서로_중복없이_조회한다() {
        for (int i = 0; i < 3; i++) {
            magazineService.publish(create().id(), adminId);
        }
        jdbcTemplate.update("UPDATE magazines SET published_at = '2026-10-15T00:00:00Z' WHERE author_id = ?", adminId);
        var first = magazineService.findAll(null, 2);
        var second = magazineService.findAll(first.meta().nextCursor(), 2);
        assertThat(first.meta().hasNext()).isTrue();
        assertThat(second.items()).hasSize(1);
        assertThat(second.items().getFirst().id()).isLessThan(first.items().getLast().id());
        assertThatThrownBy(() -> magazineService.findAllAdmin(adminId, first.meta().nextCursor(), 2))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void 프로필_변경이_매거진에_반영된다() {
        long id = magazineService.publish(create().id(), adminId).id();
        jdbcTemplate.update("UPDATE user_profiles SET display_name = '새 이름' WHERE user_id = ?", featuredUserId);
        assertThat(magazineService.find(id, null, VISITOR).featuredUser().displayName()).isEqualTo("새 이름");
    }

    @Test
    void 좋아요는_중복되지_않고_취소와_내_반응이_조회에_반영된다() {
        long id = magazineService.publish(create().id(), adminId).id();
        magazineReactionService.add(id, featuredUserId, "LIKE");
        magazineReactionService.add(id, featuredUserId, "LIKE");
        assertThat(magazineService.find(id, featuredUserId, VISITOR).likeCount()).isEqualTo(1);
        assertThat(magazineService.find(id, featuredUserId, VISITOR).likedByMe()).isTrue();
        assertThat(magazineService.find(id, null, VISITOR).likedByMe()).isFalse();
        assertThat(magazineService.findAll(null, 20, featuredUserId).items().getFirst().likedByMe()).isTrue();
        magazineReactionService.remove(id, featuredUserId, "LIKE");
        assertThat(magazineService.find(id, featuredUserId, VISITOR).likeCount()).isZero();
        assertThatThrownBy(() -> magazineReactionService.remove(id, featuredUserId, "LIKE"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 초안과_발행취소_삭제된_매거진에는_좋아요를_남길_수_없다() {
        long id = create().id();
        assertThatThrownBy(() -> magazineReactionService.add(id, featuredUserId, "LIKE"))
                .isInstanceOf(EntityNotFoundException.class);
        magazineService.publish(id, adminId);
        magazineService.unpublish(id, adminId);
        assertThatThrownBy(() -> magazineReactionService.add(id, featuredUserId, "LIKE"))
                .isInstanceOf(EntityNotFoundException.class);
        magazineService.delete(id, adminId);
        assertThatThrownBy(() -> magazineReactionService.add(id, featuredUserId, "LIKE"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 상세_GET에서_조회수를_집계하고_같은_방문자는_하루에_한번만_반영한다() throws Exception {
        long id = create().id();
        var cookie = new Cookie("VISITOR_ID", "3f2a1b4c-5d6e-4f70-8a9b-0c1d2e3f4a5b");
        mockMvc.perform(get("/api/v1/magazines/{magazineId}", id).cookie(cookie)).andExpect(status().isNotFound());
        magazineService.publish(id, adminId);
        var updatedAt = magazineService.findAdmin(id, adminId).updatedAt();
        mockMvc.perform(get("/api/v1/magazines/{magazineId}", id).cookie(cookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.viewCount").value(1));
        mockMvc.perform(get("/api/v1/magazines/{magazineId}", id).cookie(cookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.viewCount").value(1));
        assertThat(magazineService.findAdmin(id, adminId).updatedAt()).isEqualTo(updatedAt);
        assertThat(jdbcTemplate.queryForObject("SELECT view_count FROM magazines WHERE id = ?", Long.class, id))
                .isEqualTo(1);
        magazineService.unpublish(id, adminId);
        mockMvc.perform(get("/api/v1/magazines/{magazineId}", id).cookie(cookie)).andExpect(status().isNotFound());
        assertThat(jdbcTemplate.queryForObject("SELECT view_count FROM magazines WHERE id = ?", Long.class, id))
                .isEqualTo(1);
    }

    @Test
    void 소개_회원_아바타도_기존_GitHub_대체_규칙을_따른다() {
        jdbcTemplate.update("""
                INSERT INTO oauth_accounts(user_id, provider, provider_account_id, provider_avatar_url)
                VALUES (?, 'GITHUB', ?, 'https://avatars.githubusercontent.com/u/123')
                """, featuredUserId, UUID.randomUUID().toString());
        long id = magazineService.publish(create().id(), adminId).id();
        assertThat(magazineService.find(id, null, VISITOR).featuredUser().avatarUrl())
                .isEqualTo("https://avatars.githubusercontent.com/u/123");
        assertThat(magazineService.findAll(null, 20).items().getFirst().featuredUser().avatarUrl())
                .isEqualTo("https://avatars.githubusercontent.com/u/123");
    }

    @Test
    void 저장된_이미지_URL을_다시_보내도_원본_미디어_참조를_유지한다() {
        long id = magazineService.publish(create().id(), adminId).id();
        String publicContent = magazineService.find(id, null, VISITOR).contentMd();
        magazineService.update(id, adminId, saveRequest("수정", publicContent));
        var adminView = magazineService.findAdmin(id, adminId);
        assertThat(adminView.contentMd()).contains("media://" + imageId);
        assertThat(adminView.previewContentMd()).contains("https://cdn.example.com/");
        assertThat(adminView.thumbnailUrl()).isNotNull();
    }

    @Test
    void 다른_관리자가_수정해도_최초_등록_편집자를_유지한다() {
        MagazineAdminResponse draft = create();
        long otherAdminId = user("ADMIN");

        MagazineAdminResponse updated = magazineService.update(
                draft.id(), otherAdminId, saveRequest("수정", draft.contentMd()));

        assertThat(updated.author()).isEqualTo(draft.author());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT author_id FROM magazines WHERE id = ?", Long.class, draft.id())).isEqualTo(adminId);
    }

    @Test
    void 편집자_프로필_변경은_공개와_관리자_조회에_반영한다() {
        long id = magazineService.publish(create().id(), adminId).id();
        long avatarId = image(adminId, "USER_AVATAR", "READY");
        jdbcTemplate.update("UPDATE users SET handle = '@magazine-editor' WHERE id = ?", adminId);
        jdbcTemplate.update("""
                UPDATE user_profiles SET display_name = '바뀐 편집자', avatar_image_id = ? WHERE user_id = ?
                """, avatarId, adminId);
        entityManager.clear();

        var detail = magazineService.find(id, null, VISITOR);
        var expected = new MagazineAuthorResponse("@magazine-editor", "바뀐 편집자", detail.author().avatarUrl());
        assertThat(expected.avatarUrl()).startsWith("https://cdn.example.com/");
        assertThat(List.of(
                detail.author(),
                magazineService.findAll(null, 20).items().getFirst().author(),
                magazineService.findAdmin(id, adminId).author(),
                magazineService.findAllAdmin(adminId, null, 20).items().getFirst().author()
        )).containsOnly(expected);
    }

    @Test
    void 편집자_아바타도_기존_GitHub_대체_규칙을_따른다() {
        jdbcTemplate.update("""
                INSERT INTO oauth_accounts(user_id, provider, provider_account_id, provider_avatar_url)
                VALUES (?, 'GITHUB', ?, 'https://avatars.githubusercontent.com/u/456')
                """, adminId, UUID.randomUUID().toString());
        long id = magazineService.publish(create().id(), adminId).id();

        assertThat(magazineService.find(id, null, VISITOR).author().avatarUrl())
                .isEqualTo("https://avatars.githubusercontent.com/u/456");
        assertThat(magazineService.findAll(null, 20).items().getFirst().author().avatarUrl())
                .isEqualTo("https://avatars.githubusercontent.com/u/456");
        assertThat(magazineService.findAdmin(id, adminId).author().avatarUrl())
                .isEqualTo("https://avatars.githubusercontent.com/u/456");
    }

    @Test
    void 탈퇴한_편집자의_정보는_공개와_관리자_조회에서_숨긴다() {
        long id = magazineService.publish(create().id(), adminId).id();
        long otherAdminId = user("ADMIN");
        jdbcTemplate.update("UPDATE users SET status = 'DELETED', deleted_at = now() WHERE id = ?", adminId);
        entityManager.clear();
        var expected = new MagazineAuthorResponse(null, "탈퇴한 사용자", null);

        assertThat(List.of(
                magazineService.find(id, null, VISITOR).author(),
                magazineService.findAll(null, 20).items().getFirst().author(),
                magazineService.findAdmin(id, otherAdminId).author(),
                magazineService.findAllAdmin(otherAdminId, null, 20).items().getFirst().author()
        )).containsOnly(expected);
    }

    @Test
    void 매거진_API는_관리자와_공개_경로_모두_magazines를_사용한다() {
        var paths = requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().getBeanType().getSimpleName().startsWith("Magazine"))
                .flatMap(entry -> entry.getKey().getPatternValues().stream())
                .toList();

        assertThat(paths).isNotEmpty().allSatisfy(path ->
                assertThat(path).matches("/api/v1/(admin/)?magazines(/.*)?"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"BANNED", "DELETED"})
    void 정지와_탈퇴_후_남은_세션으로_좋아요를_변경할_수_없다(String userStatus) throws Exception {
        long id = magazineService.publish(create().id(), adminId).id();
        long actorId = user("USER");
        jdbcTemplate.update("INSERT INTO magazine_reactions(magazine_id, user_id, reaction_type) VALUES (?, ?, 'LIKE')",
                id, actorId);
        jdbcTemplate.update("""
                UPDATE users SET status = ?,
                    deleted_at = CASE WHEN ? = 'DELETED' THEN now() ELSE NULL END
                WHERE id = ?
                """, userStatus, userStatus, actorId);
        entityManager.clear();
        mockMvc.perform(put("/api/v1/magazines/{magazineId}/reactions/{type}", id, "LIKE")
                        .with(authenticated(actorId, UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MAGAZINE_USER_FORBIDDEN"))
                .andDo(document("magazine-reaction-add-inactive-user", resource(ResourceSnippetParameters.builder()
                        .tag("Magazine Reaction").summary("매거진 반응 추가")
                        .pathParameters(parameterWithName("magazineId").type(SimpleType.INTEGER).description("매거진 ID"),
                                parameterWithName("type").description("LIKE"))
                        .responseSchema(Schema.schema("ErrorResponse"))
                        .responseFields(RestDocsFields.errorResponse())
                        .build())));
        mockMvc.perform(delete("/api/v1/magazines/{magazineId}/reactions/{type}", id, "LIKE")
                        .with(authenticated(actorId, UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MAGAZINE_USER_FORBIDDEN"))
                .andDo(document("magazine-reaction-remove-inactive-user", resource(ResourceSnippetParameters.builder()
                        .tag("Magazine Reaction")
                        .pathParameters(parameterWithName("magazineId").type(SimpleType.INTEGER).description("매거진 ID"),
                                parameterWithName("type").description("LIKE"))
                        .responseSchema(Schema.schema("ErrorResponse"))
                        .responseFields(RestDocsFields.errorResponse())
                        .build())));
        assertThat(magazineService.find(id, null, VISITOR).likeCount()).isEqualTo(1);
    }

    private MagazineAdminResponse create() {
        return magazineService.create(adminId, saveRequest("첫 매거진", "![사진](media://" + imageId + ")"));
    }

    private MagazineSaveRequest saveRequest(String title, String content) {
        return new MagazineSaveRequest(title, "매거진 요약", featuredUserId, thumbnailId, content);
    }

    private String requestBody(String title, String content) {
        return "{\"title\":\"" + title + "\",\"summary\":\"매거진 요약\",\"featuredUserId\":" + featuredUserId
                + ",\"thumbnailMediaId\":" + thumbnailId + ",\"contentMd\":\"" + content + "\"}";
    }

    private long user(String role) {
        long id = jdbcTemplate.queryForObject("INSERT INTO users(handle, role) VALUES (?, ?) RETURNING id", Long.class,
                "@i" + UUID.randomUUID().toString().replace("-", "").substring(0, 20), role);
        jdbcTemplate.update("INSERT INTO user_profiles(user_id, display_name) VALUES (?, '소개 회원')", id);
        return id;
    }

    private long image(long userId, String purpose, String state) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO media_metadata(uploaded_by, purpose, s3_key, mime_type, size_bytes, status, expires_at)
                VALUES (?, ?, ?, 'image/webp', 1024, ?, now() + interval '1 hour') RETURNING id
                """, Long.class, userId, purpose, "media/magazine/" + UUID.randomUUID(), state);
    }

    private MockHttpServletRequestBuilder adminRequest(String method, String path) {
        var builder = request(HttpMethod.valueOf(method), path, Long.MAX_VALUE);
        if (method.equals("PUT") || (method.equals("POST") && path.endsWith("/magazines"))) {
            builder.contentType(MediaType.APPLICATION_JSON).content(requestBody("제목", "본문"));
        }
        return builder;
    }

    private RequestPostProcessor authenticated(long userId, UserRole role) {
        return request -> {
            var session = new MockHttpSession();
            authSessionAccessor.saveAuthentication(session, userId, role);
            String csrfToken = csrfTokenManager.getOrCreate(session);
            request.setSession(session);
            request.addHeader(HttpHeaders.COOKIE, "JSESSIONID=" + session.getId());
            if (!"GET".equals(request.getMethod())) {
                request.addHeader("X-CSRF-Token", csrfToken);
            }
            return request;
        };
    }

    private ResultHandler errorDocument(String name, String tag, boolean path) {
        var builder = ResourceSnippetParameters.builder().tag(tag)
                .responseSchema(Schema.schema("ErrorResponse"))
                .responseFields(RestDocsFields.errorResponse());
        if (path) {
            builder.pathParameters(parameterWithName("magazineId").type(SimpleType.INTEGER).description("매거진 ID"));
        }
        if (name.startsWith("magazine-create-") || name.startsWith("magazine-update-")) {
            builder.requestSchema(Schema.schema("MagazineSaveRequest"));
        }
        return document(name, resource(builder.build()));
    }

    private ResultHandler adminDocument(String name, String summary, String description, boolean request, boolean path) {
        var builder = ResourceSnippetParameters.builder().tag("Magazine Admin").summary(summary).description(description)
                .requestHeaders(headerWithName(HttpHeaders.COOKIE).description("활성 관리자 세션의 JSESSIONID"))
                .responseSchema(Schema.schema("MagazineAdminResponse")).responseFields(adminFields());
        if (!name.equals("magazine-admin-detail")) {
            builder.requestHeaders(
                    headerWithName(HttpHeaders.COOKIE).description("활성 관리자 세션의 JSESSIONID"),
                    headerWithName("X-CSRF-Token").description("세션 조회로 발급받은 CSRF 토큰")
            );
        }
        if (path) {
            builder.pathParameters(parameterWithName("magazineId").type(SimpleType.INTEGER).description("매거진 ID"));
        }
        if (request) {
            builder.requestSchema(Schema.schema("MagazineSaveRequest")).requestFields(
                    fieldWithPath("title").type(STRING).description("제목, 최대 200자"),
                    fieldWithPath("summary").type(STRING).optional().description("요약, 최대 500자"),
                    fieldWithPath("featuredUserId").type(NUMBER).description("매거진 대상 사용자 ID"),
                    fieldWithPath("thumbnailMediaId").type(NUMBER).optional().description("MAGAZINE_THUMBNAIL 용도의 READY 미디어 ID"),
                    fieldWithPath("contentMd").type(STRING).optional().description("원본 Markdown, 최대 100000자. 본문 이미지는 MAGAZINE_CONTENT 용도의 media://ID로 참조"));
        }
        return document(name, resource(builder.build()));
    }

    private FieldDescriptor[] adminFields() {
        return new FieldDescriptor[]{
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath("data.id").type(NUMBER).description("매거진 ID"),
                fieldWithPath("data.title").type(STRING).description("제목"),
                fieldWithPath("data.summary").type(STRING).optional().description("요약"),
                fieldWithPath("data.featuredUserId").type(NUMBER).description("매거진 대상 사용자 ID"),
                fieldWithPath("data.author").type(OBJECT).description("최초 등록 편집자, 다른 관리자가 수정해도 유지"),
                fieldWithPath("data.author.handle").type(STRING).optional().description("최초 등록 편집자의 현재 핸들, 탈퇴 시 null"),
                fieldWithPath("data.author.displayName").type(STRING).optional().description("편집자의 현재 표시 이름, 탈퇴 시 탈퇴한 사용자"),
                fieldWithPath("data.author.avatarUrl").type(STRING).optional().description("편집자 프로필 이미지 URL, 없으면 GitHub 아바타"),
                fieldWithPath("data.thumbnailUrl").type(STRING).optional().description("대표 이미지 미리보기 URL"),
                fieldWithPath("data.previewContentMd").type(STRING).optional().description("이미지 URL로 변환한 미리보기 Markdown"),
                fieldWithPath("data.thumbnailMediaId").type(NUMBER).optional().description("MAGAZINE_THUMBNAIL 용도의 READY 미디어 ID"),
                fieldWithPath("data.contentMd").type(STRING).optional().description("이미지 원본 참조가 포함된 Markdown"),
                fieldWithPath("data.status").type(STRING).description("DRAFT 또는 PUBLISHED"),
                fieldWithPath("data.publishedAt").type(STRING).optional().description("최초 발행 시각"),
                fieldWithPath("data.createdAt").type(STRING).description("생성 시각"),
                fieldWithPath("data.updatedAt").type(STRING).description("수정 시각")};
    }

    private ResultHandler listDocument(String name, String summary, String tag) {
        List<FieldDescriptor> fields = commonFields("data[]");
        fields.add(fieldWithPath("data").type(ARRAY).description("매거진 목록"));
        fields.add(fieldWithPath("data[].status").type(STRING).description("발행 상태"));
        fields.add(fieldWithPath("meta.nextCursor").type(STRING).optional().description("다음 커서"));
        fields.add(fieldWithPath("meta.hasNext").type(BOOLEAN).description("다음 페이지 여부"));
        fields.add(fieldWithPath("meta.totalCount").type(NUMBER).description("전체 개수"));
        var builder = ResourceSnippetParameters.builder().tag(tag).summary(summary)
                .description("발행된 매거진을 최초 발행 시각 내림차순으로 조회합니다. 비회원도 전문을 읽을 수 있습니다.");
        if (tag.equals("Magazine Admin")) {
            builder.description("활성 관리자가 초안을 포함한 매거진을 생성 시각 내림차순으로 조회합니다.")
                    .requestHeaders(headerWithName(HttpHeaders.COOKIE).description("활성 관리자 세션의 JSESSIONID"));
        }
        return document(name, resource(builder.queryParameters(parameterWithName("size").type(SimpleType.INTEGER).optional().description("1~50, 기본 20"),
                        parameterWithName("cursor").optional().description("다음 커서"))
                .responseSchema(Schema.schema("MagazineFindAllSuccessResponse"))
                .responseFields(fields).build()));
    }

    private ResultHandler detailDocument() {
        List<FieldDescriptor> fields = commonFields("data");
        fields.add(fieldWithPath("data.contentMd").type(STRING).description("이미지 URL로 변환한 Markdown"));
        return document("magazine-detail", resource(ResourceSnippetParameters.builder().tag("Magazine")
                .summary("발행된 매거진 상세 조회")
                .description("상세 조회 시 VISITOR_ID 쿠키의 방문자별로 한국 날짜 기준 하루 한 번 조회수를 집계합니다. 응답의 viewCount에 이번 조회가 반영됩니다.")
                .pathParameters(parameterWithName("magazineId").type(SimpleType.INTEGER).description("매거진 ID"))
                .responseSchema(Schema.schema("MagazineFindSuccessResponse"))
                .responseFields(fields).build()));
    }

    private List<FieldDescriptor> commonFields(String prefix) {
        return new ArrayList<>(List.of(
                fieldWithPath("status").type(STRING).description("응답 상태"),
                fieldWithPath(prefix + ".viewCount").type(NUMBER).description("누적 조회수"),
                fieldWithPath(prefix + ".commentCount").type(NUMBER).description("삭제되지 않은 댓글과 대댓글 수"),
                fieldWithPath(prefix + ".likeCount").type(NUMBER).description("좋아요 수"),
                fieldWithPath(prefix + ".likedByMe").type(BOOLEAN).description("내 좋아요 여부"),
                fieldWithPath(prefix + ".id").type(NUMBER).description("매거진 ID"),
                fieldWithPath(prefix + ".title").type(STRING).description("제목"),
                fieldWithPath(prefix + ".summary").type(STRING).optional().description("요약"),
                fieldWithPath(prefix + ".thumbnailUrl").type(STRING).optional().description("대표 이미지 URL"),
                fieldWithPath(prefix + ".author").type(OBJECT).description("최초 등록 편집자, 다른 관리자가 수정해도 유지"),
                fieldWithPath(prefix + ".author.handle").type(STRING).optional().description("최초 등록 편집자의 현재 핸들, 탈퇴 시 null"),
                fieldWithPath(prefix + ".author.displayName").type(STRING).optional().description("편집자의 현재 표시 이름, 탈퇴 시 탈퇴한 사용자"),
                fieldWithPath(prefix + ".author.avatarUrl").type(STRING).optional().description("편집자 프로필 이미지 URL, 없으면 GitHub 아바타"),
                fieldWithPath(prefix + ".featuredUser.handle").type(STRING).optional().description("사용자 핸들"),
                fieldWithPath(prefix + ".featuredUser.displayName").type(STRING).description("표시 이름"),
                fieldWithPath(prefix + ".featuredUser.bio").type(STRING).optional().description("현재 사용자 소개"),
                fieldWithPath(prefix + ".featuredUser.avatarUrl").type(STRING).optional().description("프로필 이미지 URL"),
                fieldWithPath(prefix + ".publishedAt").type(STRING).optional().description("최초 발행 시각"),
                fieldWithPath(prefix + ".createdAt").type(STRING).description("생성 시각"),
                fieldWithPath(prefix + ".updatedAt").type(STRING).description("수정 시각")));
    }
}
