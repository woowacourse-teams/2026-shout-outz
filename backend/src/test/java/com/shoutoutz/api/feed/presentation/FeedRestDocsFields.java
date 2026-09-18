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
                fieldWithPath("content").type(STRING).description("Markdown 본문"),
                fieldWithPath("author").type(OBJECT).description("현재 작성자 프로필"),
                fieldWithPath("author.handle").type(STRING).description("작성자 핸들"),
                fieldWithPath("author.displayName").type(STRING).description("작성자 이름"),
                new EnumFields(UserType.class).withPath("author.userType").description("작성자 유형"),
                new EnumFields(Track.class).withPath("author.track").description("작성자 트랙").optional(),
                fieldWithPath("author.cohort").type(NUMBER).description("작성자 기수").optional(),
                fieldWithPath("author.avatarUrl").type(STRING).description("현재 프로필 이미지 공개 URL").optional(),
                fieldWithPath("categories").type(ARRAY).description("카테고리 목록"),
                fieldWithPath("categories[].categoryId").type(NUMBER).description("카테고리 ID"),
                fieldWithPath("categories[].slug").type(STRING).description("카테고리 slug"),
                fieldWithPath("categories[].displayName").type(STRING).description("카테고리 표시 이름"),
                new EnumFields(CategoryType.class).withPath("categories[].type").description("카테고리 유형"),
                fieldWithPath("media").type(ARRAY).description("본문 미디어 목록"),
                fieldWithPath("media[].url").type(STRING).description("본문 미디어 공개 URL"),
                fieldWithPath("media[].displayOrder").type(NUMBER).description("미디어 표시 순서"),
                fieldWithPath("createdAt").type(STRING).description("ISO-8601 생성 시각"),
                fieldWithPath("updatedAt").type(STRING).description("ISO-8601 수정 시각")
        ));
    }

    static List<FieldDescriptor> successResponseFields(String feedPrefix) {
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(fieldWithPath("status").type(STRING).description("응답 상태"));
        fields.add(fieldWithPath("data").type(OBJECT).description("피드"));
        fields.addAll(feedFields(feedPrefix));
        return fields;
    }

    static List<FieldDescriptor> feedListResponseFields(String description) {
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(fieldWithPath("status").type(STRING).description("응답 상태"));
        fields.add(fieldWithPath("data").type(ARRAY).description(description));
        fields.addAll(feedFields("data[]."));
        fields.add(fieldWithPath("meta").type(OBJECT).description("페이지네이션 정보"));
        fields.add(fieldWithPath("meta.nextCursor").type(STRING).description("다음 페이지 커서").optional());
        fields.add(fieldWithPath("meta.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부"));
        return fields;
    }
}
