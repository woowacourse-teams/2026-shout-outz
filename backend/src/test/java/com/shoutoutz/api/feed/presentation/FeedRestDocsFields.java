package com.shoutoutz.api.feed.presentation;

import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.applyPathPrefix;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

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
                fieldWithPath("author.userType").type(STRING).description("작성자 유형"),
                fieldWithPath("author.track").type(STRING).description("작성자 트랙").optional(),
                fieldWithPath("author.cohort").type(NUMBER).description("작성자 기수").optional(),
                fieldWithPath("author.avatarImageId").type(NUMBER).description("현재 프로필 이미지 미디어 ID").optional(),
                fieldWithPath("categories").type(ARRAY).description("카테고리 목록"),
                fieldWithPath("categories[].categoryId").type(NUMBER).description("카테고리 ID"),
                fieldWithPath("categories[].slug").type(STRING).description("카테고리 slug"),
                fieldWithPath("categories[].displayName").type(STRING).description("카테고리 표시 이름"),
                fieldWithPath("categories[].type").type(STRING).description("GENERAL 또는 EVENT"),
                fieldWithPath("media").type(ARRAY).description("본문 미디어 목록"),
                fieldWithPath("media[].mediaId").type(NUMBER).description("미디어 ID"),
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
        fields.add(fieldWithPath("meta").type(OBJECT).description("Slice 메타데이터"));
        fields.add(fieldWithPath("meta.nextCursor").type(STRING).description("다음 Slice 커서").optional());
        fields.add(fieldWithPath("meta.hasNext").type(BOOLEAN).description("다음 Slice 존재 여부"));
        return fields;
    }
}
