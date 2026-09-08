package com.shoutoutz.api.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.code.ErrorCode;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.common.exception.custom.CustomException;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.common.exception.custom.InternalServerErrorException;
import com.shoutoutz.api.common.exception.custom.NotFoundException;
import com.shoutoutz.api.common.exception.custom.PersistenceException;
import com.shoutoutz.api.common.exception.custom.UnauthorizedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
    private TestController controller;

    @BeforeEach
    void setUp() {
        controller = new TestController();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
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

    @ParameterizedTest
    @MethodSource("customExceptionCases")
    @DisplayName("상태별 공통 예외와 하위 예외를 하나의 핸들러로 처리해 HTTP 오류 응답을 반환한다")
    void handlesCustomExceptionWithItsStatusAndErrorCode(
            CustomException exception, int expectedStatus, ErrorCode expectedErrorCode) throws Exception {
        controller.exceptionToThrow = exception;

        mockMvc.perform(get("/test/custom"))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(expectedErrorCode.name()))
                .andExpect(jsonPath("$.message").value(expectedErrorCode.getMessage()))
                .andExpect(jsonPath("$.cause").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
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

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드는 405 상태와 Allow 헤더를 보존한다")
    void preservesMethodNotAllowedStatusAndAllowHeader() throws Exception {
        mockMvc.perform(get("/test/validation"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("POST")))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.VALIDATION_FAILED.name()));
    }

    private static Stream<Arguments> customExceptionCases() {
        return Stream.of(
                Arguments.of(new BadRequestException(CommonErrorCode.VALIDATION_FAILED),
                        400, CommonErrorCode.VALIDATION_FAILED),
                Arguments.of(new UnauthorizedException(CommonErrorCode.UNAUTHORIZED),
                        401, CommonErrorCode.UNAUTHORIZED),
                Arguments.of(new ForbiddenException(CommonErrorCode.FORBIDDEN),
                        403, CommonErrorCode.FORBIDDEN),
                Arguments.of(new NotFoundException(CommonErrorCode.RESOURCE_NOT_FOUND),
                        404, CommonErrorCode.RESOURCE_NOT_FOUND),
                Arguments.of(new ConflictException(CommonErrorCode.DUPLICATE_RESOURCE),
                        409, CommonErrorCode.DUPLICATE_RESOURCE),
                Arguments.of(new InternalServerErrorException(CommonErrorCode.INTERNAL_SERVER_ERROR),
                        500, CommonErrorCode.INTERNAL_SERVER_ERROR),
                Arguments.of(new DomainValidationException(CommonErrorCode.VALIDATION_FAILED),
                        500, CommonErrorCode.VALIDATION_FAILED),
                Arguments.of(new EntityNotFoundException(CommonErrorCode.RESOURCE_NOT_FOUND),
                        404, CommonErrorCode.RESOURCE_NOT_FOUND),
                Arguments.of(new DuplicateEntityException(CommonErrorCode.DUPLICATE_RESOURCE),
                        409, CommonErrorCode.DUPLICATE_RESOURCE),
                Arguments.of(new PersistenceException(
                                CommonErrorCode.INTERNAL_SERVER_ERROR, new RuntimeException("database password")),
                        500, CommonErrorCode.INTERNAL_SERVER_ERROR));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        private CustomException exceptionToThrow;

        @PostMapping("/validation")
        void validate(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/illegal")
        void illegal() {
            throw new IllegalArgumentException("internal detail");
        }

        @GetMapping("/custom")
        void custom() {
            throw exceptionToThrow;
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("database password");
        }
    }

    record TestRequest(@NotBlank(message = "이메일을 입력해주세요.") String email) {
    }
}
