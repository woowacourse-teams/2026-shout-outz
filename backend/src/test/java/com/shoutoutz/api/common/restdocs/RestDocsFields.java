package com.shoutoutz.api.common.restdocs;

import org.springframework.restdocs.payload.FieldDescriptor;

import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

/**
 * 여러 API에서 공통으로 사용하는 REST Docs 응답 필드 정의.
 */
public final class RestDocsFields {

    private RestDocsFields() {
    }

    public static FieldDescriptor[] errorResponse() {
        return new FieldDescriptor[]{
                fieldWithPath("status")
                        .type(STRING)
                        .description("응답 상태"),
                fieldWithPath("code")
                        .type(STRING)
                        .description("오류 코드"),
                fieldWithPath("message")
                        .type(STRING)
                        .description("오류 메시지"),
                fieldWithPath("details")
                        .type(ARRAY)
                        .description("필드별 오류 상세")
                        .optional(),
                fieldWithPath("details[].field")
                        .type(STRING)
                        .description("오류 필드명")
                        .optional(),
                fieldWithPath("details[].message")
                        .type(STRING)
                        .description("필드 오류 메시지")
                        .optional()
        };
    }
}
