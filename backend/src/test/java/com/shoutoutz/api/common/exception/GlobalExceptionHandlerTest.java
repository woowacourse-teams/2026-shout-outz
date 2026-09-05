package com.shoutoutz.api.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.code.ErrorCode;
import com.shoutoutz.api.common.exception.custom.CustomException;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.PersistenceException;
import com.shoutoutz.api.common.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("IllegalArgumentException을 검증 실패 응답으로 변환하고 예외 메시지를 노출하지 않는다")
    void handlesIllegalArgumentExceptionWithoutExposingExceptionMessage() {
        ResponseEntity<Object> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("internal detail"));

        ErrorResponse error = assertError(response, HttpStatus.BAD_REQUEST, CommonErrorCode.VALIDATION_FAILED);

        assertThat(error.message()).doesNotContain("internal detail");
        assertThat(error.details()).isNull();
    }

    @Test
    @DisplayName("NoSuchElementException을 리소스 없음 응답으로 변환하고 예외 메시지를 노출하지 않는다")
    void handlesNoSuchElementExceptionWithoutExposingExceptionMessage() {
        ResponseEntity<Object> response = handler.handleNoSuchElementException(
                new java.util.NoSuchElementException("internal detail"));

        ErrorResponse error = assertError(response, HttpStatus.NOT_FOUND, CommonErrorCode.RESOURCE_NOT_FOUND);

        assertThat(error.message()).doesNotContain("internal detail");
        assertThat(error.details()).isNull();
    }

    @Test
    @DisplayName("ConstraintViolationException을 검증 실패 응답으로 변환한다")
    void handlesConstraintViolationExceptionWithoutBodyDetails() {
        ResponseEntity<Object> response = handler.handleConstraintViolationException(
                new ConstraintViolationException(Set.of()));

        ErrorResponse error = assertError(response, HttpStatus.BAD_REQUEST, CommonErrorCode.VALIDATION_FAILED);

        assertThat(error.details()).isNull();
    }

    @Test
    @DisplayName("요청 본문 검증 오류의 필드 정보를 details에 포함한다")
    void handlesRequestBodyValidationWithFieldDetails() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                new TestRequest(), "request");
        bindingResult.addError(new FieldError(
                "request", "email", null, false, null, null, "올바른 이메일 형식이 아닙니다."));
        bindingResult.addError(new FieldError(
                "request", "profile.email", null, false, null, null, "중첩 이메일이 올바르지 않습니다."));
        bindingResult.addError(new FieldError(
                "request", "items[0].name", null, false, null, null, "상품명이 필요합니다."));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                requestBodyMethodParameter(), bindingResult);

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, mock(WebRequest.class));

        ErrorResponse error = assertError(response, HttpStatus.BAD_REQUEST, CommonErrorCode.VALIDATION_FAILED);

        assertThat(error.details()).containsExactly(
                new ErrorResponse.ErrorDetail("email", "올바른 이메일 형식이 아닙니다."),
                new ErrorResponse.ErrorDetail("profile", "중첩 이메일이 올바르지 않습니다."),
                new ErrorResponse.ErrorDetail("items", "상품명이 필요합니다."));
    }

    @Test
    @DisplayName("요청 본문에 매핑되는 필드 오류가 없으면 details를 생략한다")
    void omitsDetailsWhenRequestBodyValidationHasNoFieldError() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                new TestRequest(), "request");
        bindingResult.addError(new ObjectError("request", "요청 본문이 올바르지 않습니다."));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                requestBodyMethodParameter(), bindingResult);

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, mock(WebRequest.class));

        ErrorResponse error = assertError(response, HttpStatus.BAD_REQUEST, CommonErrorCode.VALIDATION_FAILED);

        assertThat(error.details()).isNull();
    }

    @Test
    @DisplayName("컨트롤러 메서드 파라미터 검증 오류를 검증 실패 응답으로 변환한다")
    void handlesHandlerMethodValidationWithoutBodyDetails() {
        ResponseEntity<Object> response = handler.handleHandlerMethodValidationException(
                mock(HandlerMethodValidationException.class),
                new HttpHeaders(), HttpStatus.BAD_REQUEST, mock(WebRequest.class));

        ErrorResponse error = assertError(response, HttpStatus.BAD_REQUEST, CommonErrorCode.VALIDATION_FAILED);

        assertThat(error.details()).isNull();
    }

    @ParameterizedTest
    @MethodSource("customExceptionCases")
    @DisplayName("커스텀 예외의 에러 코드에 맞는 응답을 생성한다")
    void handlesCustomExceptionsUsingTheirErrorCode(
            CustomException exception, ErrorCode expectedErrorCode) {
        ResponseEntity<Object> response = handler.handleCustomException(exception);

        ErrorResponse error = assertError(response, expectedErrorCode.getHttpStatus(), expectedErrorCode);

        assertThat(error.message()).isEqualTo(expectedErrorCode.getMessage());
        assertThat(error.details()).isNull();
    }

    @Test
    @DisplayName("처리되지 않은 예외를 내부 서버 오류 응답으로 변환하고 예외 메시지를 노출하지 않는다")
    void handlesUnexpectedExceptionAsInternalServerErrorWithoutExposingExceptionMessage() {
        ResponseEntity<Object> response = handler.handleAllExceptions(
                new IllegalStateException("database password"));

        ErrorResponse error = assertError(
                response, HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorCode.INTERNAL_SERVER_ERROR);

        assertThat(error.message()).doesNotContain("database password");
        assertThat(error.details()).isNull();
    }

    @ParameterizedTest
    @MethodSource("springMvcExceptionCases")
    @DisplayName("Spring MVC 예외를 공통 오류 응답으로 변환하고 응답 헤더를 보존한다")
    void convertsSpringMvcExceptionsToErrorResponse(
            HttpStatus status, ErrorCode expectedErrorCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Test", "preserved");

        ResponseEntity<Object> response = handler.handleExceptionInternal(
                new RuntimeException("internal detail"),
                null,
                headers,
                status,
                mock(WebRequest.class));

        ErrorResponse error = assertError(response, status, expectedErrorCode);

        assertThat(response.getHeaders().getFirst("X-Test")).isEqualTo("preserved");
        assertThat(error.message()).doesNotContain("internal detail");
        assertThat(error.details()).isNull();
    }

    private static Stream<Arguments> customExceptionCases() {
        return Stream.of(
                Arguments.of(
                        new DomainValidationException(CommonErrorCode.VALIDATION_FAILED),
                        CommonErrorCode.VALIDATION_FAILED),
                Arguments.of(
                        new EntityNotFoundException(CommonErrorCode.RESOURCE_NOT_FOUND),
                        CommonErrorCode.RESOURCE_NOT_FOUND),
                Arguments.of(
                        new DuplicateEntityException(CommonErrorCode.DUPLICATE_RESOURCE),
                        CommonErrorCode.DUPLICATE_RESOURCE),
                Arguments.of(
                        new PersistenceException(
                                CommonErrorCode.INTERNAL_SERVER_ERROR, new RuntimeException("database detail")),
                        CommonErrorCode.INTERNAL_SERVER_ERROR));
    }

    private static Stream<Arguments> springMvcExceptionCases() {
        return Stream.of(
                Arguments.of(HttpStatus.BAD_REQUEST, CommonErrorCode.VALIDATION_FAILED),
                Arguments.of(HttpStatus.NOT_FOUND, CommonErrorCode.RESOURCE_NOT_FOUND),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorCode.INTERNAL_SERVER_ERROR));
    }

    private static MethodParameter requestBodyMethodParameter() throws Exception {
        Method method = TestController.class.getDeclaredMethod("create", TestRequest.class);
        return new MethodParameter(method, 0);
    }

    private static ErrorResponse assertError(
            ResponseEntity<Object> response, HttpStatus expectedStatus, ErrorCode expectedErrorCode) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);

        ErrorResponse error = (ErrorResponse) response.getBody();
        assertThat(error.status()).isEqualTo("error");
        assertThat(error.code()).isEqualTo(expectedErrorCode.name());
        assertThat(error.message()).isEqualTo(expectedErrorCode.getMessage());
        return error;
    }

    private static class TestController {

        private void create(@Valid @RequestBody TestRequest request) {
        }
    }

    private record TestRequest() {
    }
}
