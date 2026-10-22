package com.shoutoutz.api.feed.presentation;

import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.applyPathPrefix;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

import com.epages.restdocs.apispec.EnumFields;
import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.feed.domain.FeedType;
import com.shoutoutz.api.feed.recommendation.domain.QuestionRecommendationStatus;
import com.shoutoutz.api.user.domain.profile.Track;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.restdocs.payload.FieldDescriptor;

final class FeedRestDocsFields {

    private FeedRestDocsFields() {
    }

    static List<FieldDescriptor> feedFields(String prefix) {
        return applyPathPrefix(prefix, List.of(
                fieldWithPath("feedId").type(NUMBER).description("피드 ID"),
                new EnumFields(FeedType.class).withPath("feedType").description("피드 유형"),
                fieldWithPath("title").type(STRING).description("피드 제목"),
                fieldWithPath("content").type(STRING).description("Markdown 본문"),
                fieldWithPath("isAnonymous").type(BOOLEAN).description("작성자 정보를 익명으로 공개할지 여부"),
                fieldWithPath("author").type(OBJECT).description("현재 작성자 프로필"),
                fieldWithPath("author.userId").type(NUMBER).description("작성자 ID. 타인의 익명 글이면 null").optional(),
                fieldWithPath("author.handle").type(STRING).description("작성자 핸들. 타인의 익명 글이면 null").optional(),
                fieldWithPath("author.displayName").type(STRING).description("작성자 이름. 타인의 익명 글이면 null").optional(),
                new EnumFields(UserType.class).withPath("author.userType")
                        .description("작성자 유형. 크루 배지 구분을 위해 타인의 익명 글에도 포함"),
                new EnumFields(Track.class).withPath("author.track")
                        .description("작성자 트랙. 크루가 아니거나 타인의 익명 글이면 null").optional(),
                fieldWithPath("author.cohort").type(NUMBER)
                        .description("작성자 정보가 공개되는 크루의 기수. 타인의 익명 글이면 제외").optional(),
                fieldWithPath("author.isCurrent").type(BOOLEAN)
                        .description("작성자가 현재 기수 크루인지 여부. 크루가 아니면 null").optional(),
                fieldWithPath("author.avatarImageId").type(NUMBER).description("현재 프로필 이미지 미디어 ID. 타인의 익명 글이면 null").optional(),
                fieldWithPath("author.avatarUrl").type(STRING).description("직접 업로드한 이미지가 없으면 GitHub 아바타를 사용하는 현재 프로필 이미지 공개 URL. 타인의 익명 글이면 null").optional(),
                fieldWithPath("categories").type(ARRAY).description("카테고리 목록"),
                fieldWithPath("categories[].categoryId").type(NUMBER).description("카테고리 ID"),
                fieldWithPath("categories[].slug").type(STRING).description("카테고리 slug"),
                fieldWithPath("categories[].displayName").type(STRING).description("카테고리 표시 이름"),
                new EnumFields(FeedType.class).withPath("categories[].feedType")
                        .description("카테고리가 연결된 피드 유형"),
                new EnumFields(CategoryType.class).withPath("categories[].type").description("카테고리 유형"),
                fieldWithPath("media").type(ARRAY).description("본문 미디어 목록"),
                fieldWithPath("media[].mediaId").type(NUMBER).description("본문 미디어 ID"),
                fieldWithPath("media[].url").type(STRING).description("본문 미디어 공개 URL"),
                fieldWithPath("media[].displayOrder").type(NUMBER).description("미디어 표시 순서"),
                fieldWithPath("linkPreview").type(OBJECT).description("본문 첫 URL의 링크 미리보기. URL이 없으면 null").optional(),
                fieldWithPath("linkPreview.url").type(STRING).description("본문에서 추출한 첫 URL").optional(),
                fieldWithPath("linkPreview.title").type(STRING).description("외부 페이지 제목. 수집 전·실패 시 null").optional(),
                fieldWithPath("linkPreview.description").type(STRING).description("외부 페이지 설명").optional(),
                fieldWithPath("linkPreview.imageUrl").type(STRING).description("외부 페이지 이미지 URL").optional(),
                fieldWithPath("linkPreview.siteName").type(STRING).description("외부 사이트 이름").optional(),
                fieldWithPath("likeCount").type(NUMBER).description("좋아요 수"),
                fieldWithPath("bookmarkCount").type(NUMBER).description("북마크 수"),
                fieldWithPath("likedByMe").type(BOOLEAN).description("요청자의 좋아요 여부. 비로그인이면 false"),
                fieldWithPath("bookmarkedByMe").type(BOOLEAN).description("요청자의 북마크 여부. 비로그인이면 false"),
                fieldWithPath("commentCount").type(NUMBER).description("삭제되지 않은 댓글 수"),
                fieldWithPath("createdAt").type(STRING).description("ISO-8601 생성 시각"),
                fieldWithPath("updatedAt").type(STRING).description("ISO-8601 수정 시각")
        ));
    }

    static List<FieldDescriptor> successResponseFields(String feedPrefix) {
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(fieldWithPath("status").type(STRING).description("응답 상태"));
        fields.add(fieldWithPath("data").type(OBJECT).description("피드"));
        fields.addAll(feedFields(feedPrefix));
        fields.addAll(applyPathPrefix(feedPrefix, List.of(
                fieldWithPath("relatedQuestionRecommendations").type(OBJECT)
                        .description("질문 상세에 포함되는 연관 질문 추천. 추천 작업 생성 전이거나 질문이 아니면 생략")
                        .optional(),
                new EnumFields(QuestionRecommendationStatus.class).withPath("relatedQuestionRecommendations.status")
                        .description("추천 생성 상태. PENDING, PROCESSING, READY, FAILED, SKIPPED"),
                fieldWithPath("relatedQuestionRecommendations.items").type(ARRAY)
                        .description("READY 상태에서 제공되는 연관 질문 목록(기본 최대 5개). 그 외 상태에서는 빈 배열"),
                fieldWithPath("relatedQuestionRecommendations.items[].recommendationId").type(NUMBER)
                        .description("추천 질문 ID"),
                fieldWithPath("relatedQuestionRecommendations.items[].title").type(STRING)
                        .description("추천 질문 제목"),
                fieldWithPath("relatedQuestionRecommendations.items[].content").type(STRING)
                        .description("추천 질문 본문")
        )));
        return fields;
    }

    static List<FieldDescriptor> commandSuccessResponseFields(String prefix) {
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(fieldWithPath("status").type(STRING).description("응답 상태"));
        fields.add(fieldWithPath("data").type(OBJECT).description("피드"));
        fields.addAll(applyPathPrefix(prefix, List.of(
                fieldWithPath("feedId").type(NUMBER).description("피드 ID"),
                new EnumFields(FeedType.class).withPath("feedType").description("피드 유형"),
                fieldWithPath("title").type(STRING).description("피드 제목"),
                fieldWithPath("content").type(STRING).description("Markdown 본문"),
                fieldWithPath("isAnonymous").type(BOOLEAN).description("작성자 정보를 익명으로 공개할지 여부"),
                fieldWithPath("author").type(OBJECT).description("현재 작성자 프로필"),
                fieldWithPath("author.userId").type(NUMBER).description("작성자 ID"),
                fieldWithPath("author.handle").type(STRING).description("작성자 핸들"),
                fieldWithPath("author.displayName").type(STRING).description("작성자 이름"),
                new EnumFields(UserType.class).withPath("author.userType").description("작성자 유형"),
                new EnumFields(Track.class).withPath("author.track").description("작성자 트랙").optional(),
                fieldWithPath("author.cohort").type(NUMBER)
                        .description("작성자 정보가 공개되는 크루의 기수. 타인의 익명 글이면 제외").optional(),
                fieldWithPath("author.isCurrent").type(BOOLEAN)
                        .description("작성자가 현재 기수 크루인지 여부. 크루가 아니면 null").optional(),
                fieldWithPath("author.avatarUrl").type(STRING).description("직접 업로드한 이미지가 없으면 GitHub 아바타를 사용하는 현재 프로필 이미지 공개 URL").optional(),
                fieldWithPath("categories").type(ARRAY).description("카테고리 목록"),
                fieldWithPath("categories[].categoryId").type(NUMBER).description("카테고리 ID"),
                fieldWithPath("categories[].slug").type(STRING).description("카테고리 slug"),
                fieldWithPath("categories[].displayName").type(STRING).description("카테고리 표시 이름"),
                new EnumFields(FeedType.class).withPath("categories[].feedType")
                        .description("카테고리가 연결된 피드 유형"),
                new EnumFields(CategoryType.class).withPath("categories[].type").description("카테고리 유형"),
                fieldWithPath("media").type(ARRAY).description("본문 미디어 목록"),
                fieldWithPath("media[].url").type(STRING).description("본문 미디어 공개 URL"),
                fieldWithPath("media[].displayOrder").type(NUMBER).description("미디어 표시 순서"),
                fieldWithPath("linkPreview").type(OBJECT).description("본문 첫 URL의 링크 미리보기. URL이 없으면 null").optional(),
                fieldWithPath("linkPreview.url").type(STRING).description("본문에서 추출한 첫 URL").optional(),
                fieldWithPath("linkPreview.title").type(STRING).description("외부 페이지 제목. 수집 전·실패 시 null").optional(),
                fieldWithPath("linkPreview.description").type(STRING).description("외부 페이지 설명").optional(),
                fieldWithPath("linkPreview.imageUrl").type(STRING).description("외부 페이지 이미지 URL").optional(),
                fieldWithPath("linkPreview.siteName").type(STRING).description("외부 사이트 이름").optional(),
                fieldWithPath("createdAt").type(STRING).description("ISO-8601 생성 시각"),
                fieldWithPath("updatedAt").type(STRING).description("ISO-8601 수정 시각"),
                new EnumFields(QuestionRecommendationStatus.class).withPath("recommendationStatus")
                        .description("비동기 질문 추천 생성 상태. 생성 또는 제목·본문 수정 직후에는 PENDING. 일반 피드 또는 추천 기능 비활성화 시 생략")
                        .optional()
        )));
        return fields;
    }

    static List<FieldDescriptor> feedListResponseFields(String description) {
        return feedListResponseFields(
                description,
                "커서와 size를 제외한 조회 조건을 만족하는 전체 피드 수. 추천 질문 슬롯은 포함하지 않음",
                true
        );
    }

    static List<FieldDescriptor> userFeedListResponseFields(String description) {
        return feedListResponseFields(
                description,
                "커서와 size에 무관한 해당 사용자의 전체 공개 피드 수. 추천 질문 슬롯은 포함하지 않음",
                false
        );
    }

    private static List<FieldDescriptor> feedListResponseFields(
            String description,
            String totalCountDescription,
            boolean includeRecommendationSlots
    ) {
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(fieldWithPath("status").type(STRING).description("응답 상태"));
        fields.add(fieldWithPath("data").type(ARRAY).description(description));
        fields.addAll(feedFields("data[]."));
        fields.add(fieldWithPath("meta").type(OBJECT).description("페이지네이션 정보"));
        fields.add(fieldWithPath("meta.nextCursor").type(STRING).description("다음 페이지 커서").optional());
        fields.add(fieldWithPath("meta.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"));
        fields.add(fieldWithPath("meta.totalCount").type(NUMBER)
                .description(totalCountDescription));
        if (includeRecommendationSlots) {
            fields.add(fieldWithPath("meta.recommendationSlots").type(ARRAY)
                    .description("type=QUESTION일 때 실제 질문 5개마다 무작위 추천 질문을 최대 1개 제공. data·cursor·totalCount에는 포함되지 않는다. 페이지 요청마다 무작위 선택되므로 이미 표시한 페이지의 슬롯은 클라이언트가 유지한다. 일반 피드 목록이면 생략")
                    .optional());
            fields.add(fieldWithPath("meta.recommendationSlots[].afterFeedId").type(NUMBER)
                    .description("추천 질문을 삽입할 위치. 이 피드 바로 뒤에 표시"));
            fields.add(fieldWithPath("meta.recommendationSlots[].recommendation").type(OBJECT)
                    .description("무작위 추천 질문"));
            fields.add(fieldWithPath("meta.recommendationSlots[].recommendation.recommendationId").type(NUMBER)
                    .description("추천 질문 ID"));
            fields.add(fieldWithPath("meta.recommendationSlots[].recommendation.title").type(STRING)
                    .description("추천 질문 제목"));
            fields.add(fieldWithPath("meta.recommendationSlots[].recommendation.content").type(STRING)
                    .description("추천 질문 본문"));
        }
        return fields;
    }
}
