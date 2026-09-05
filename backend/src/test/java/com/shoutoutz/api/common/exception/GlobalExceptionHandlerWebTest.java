package com.shoutoutz.api.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerWebTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("요청 본문 검증 오류를 클라이언트용 오류 응답 형식으로 반환한다")
    void handlesRequestBodyValidationWithTheClientErrorFormat() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.VALIDATION_FAILED.name()))
                .andExpect(jsonPath("$.message").value(CommonErrorCode.VALIDATION_FAILED.getMessage()))
                .andExpect(jsonPath("$.details[0].field").value("email"))
                .andExpect(jsonPath("$.details[0].message").value("이메일을 입력해주세요."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.meta").doesNotExist());
    }

    @Test
    @DisplayName("IllegalArgumentException을 클라이언트용 오류 응답 형식으로 반환한다")
    void handlesIllegalArgumentExceptionWithTheClientErrorFormat() throws Exception {
        mockMvc.perform(get("/test/illegal"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.VALIDATION_FAILED.name()))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("커스텀 예외를 클라이언트용 오류 응답 형식으로 반환한다")
    void handlesCustomExceptionWithTheClientErrorFormat() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.RESOURCE_NOT_FOUND.name()))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("처리되지 않은 예외의 상세 메시지를 노출하지 않는다")
    void handlesUnexpectedExceptionWithoutExposingItsMessage() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.INTERNAL_SERVER_ERROR.name()))
                .andExpect(jsonPath("$.message").value(CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("database password"))))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("잘못된 JSON 요청 본문을 클라이언트용 오류 응답 형식으로 반환한다")
    void convertsMalformedRequestBodyToTheClientErrorFormat() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.VALIDATION_FAILED.name()))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @PostMapping("/validation")
        void validate(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/illegal")
        void illegal() {
            throw new IllegalArgumentException("internal detail");
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new com.shoutoutz.api.common.exception.custom.EntityNotFoundException(
                    CommonErrorCode.RESOURCE_NOT_FOUND);
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("database password");
        }
    }

    record TestRequest(@NotBlank(message = "이메일을 입력해주세요.") String email) {
    }
}
