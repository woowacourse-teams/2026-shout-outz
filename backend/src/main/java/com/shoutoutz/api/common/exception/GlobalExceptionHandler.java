package com.shoutoutz.api.common.exception;

import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.code.ErrorCode;
import com.shoutoutz.api.common.exception.custom.CustomException;
import com.shoutoutz.api.common.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * IllegalArgumentException나 NoSuchElementException 같은 예외의
     * message를 응답에 담지 않도록, 최대한 예외는 원인을 표시한 커스텀 예외를 사용한다.
     */
    @Deprecated
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorCode errorCode = CommonErrorCode.VALIDATION_FAILED;
        logException(errorCode, e);
        return createErrorResponse(errorCode);
    }

    @Deprecated
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Object> handleNoSuchElementException(NoSuchElementException e) {
        ErrorCode errorCode = CommonErrorCode.RESOURCE_NOT_FOUND;
        logException(errorCode, e);
        return createErrorResponse(errorCode);
    }

    /**
     * @Validated 기반, 메서드 파라미터나 반환값 검증에 실패했을 때 발생하는 예외인,
     * ConstraintViolationException 처리 핸들러
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(
            ConstraintViolationException e) {
        ErrorCode errorCode = CommonErrorCode.VALIDATION_FAILED;
        logException(errorCode, e);
        return createErrorResponse(errorCode);
    }

    /**
     * @RequestBody DTO에 Jakarta Bean Validation의 @Valid는
     * 검증 실패 시 MethodArgumentNotValidException이 발생한다.
     *
     * 해당 예외를 처리하는 핸들러는 이미 ResponseEntityExceptionHandler에 구현되어 있다.
     * 따라서, handleMethodArgumentNotValid 메서드를 재정의하여,
     * 공통 오류 응답 형식으로 커스텀 한다.
     */
    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        ErrorCode errorCode = CommonErrorCode.VALIDATION_FAILED;
        logException(errorCode, e);
        return createErrorResponse(e, errorCode);
    }

    /**
     * Spring MVC 내장 메서드 검증에서 발생하는 검증 예외 처리 핸들러
     * 예: 컨트롤러 파라미터에 @Min, @NotBlank 등을 직접 선언하는 방식에서 검증 얘외가 발생하면,
     * HandlerMethodValidationException가 발생한다.
     *
     * 해당 예외를 처리하는 핸들러는 이미 ResponseEntityExceptionHandler에 구현되어 있다.
     * 따라서 handleHandlerMethodValidationException만 재정의 한다.
     */
    @Override
    public ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException e,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        ErrorCode errorCode = CommonErrorCode.VALIDATION_FAILED;
        logException(errorCode, e);
        return createErrorResponse(errorCode);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Object> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        logException(errorCode, e);
        return createErrorResponse(errorCode);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleAllExceptions(Exception e) {
        ErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
        logException(errorCode, e);
        return createErrorResponse(errorCode);
    }

    /**
     * MethodArgumentNotValidException이 발생한 경우의 예외 응답 생성 메서드
     */
    private ResponseEntity<Object> createErrorResponse(
            BindException e, ErrorCode errorCode) {
        // request body 필드에 매핑할 수 있는 오류만 details에 포함한다.
        List<ErrorResponse.ErrorDetail> details = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ErrorResponse.ErrorDetail(
                        toRequestBodyFieldName(fieldError.getField()), fieldError.getDefaultMessage()))
                .collect(Collectors.toList());
        return createErrorResponse(errorCode, details);
    }

    /**
     * ResponseEntityExceptionHandler가 기본으로 생성한 ProblemDetail을 사용하지 않고,
     * 모든 Spring MVC 예외를 공통 오류 응답 형식으로 변환하도록 통일
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        ErrorCode errorCode = resolveErrorCode(statusCode);
        ErrorResponse response = ErrorResponse.error(errorCode.name(), errorCode.getMessage());

        logException(errorCode, e);

        return ResponseEntity.status(statusCode)
                .headers(headers)
                .body(response);
    }

    private ErrorCode resolveErrorCode(HttpStatusCode statusCode) {
        if (statusCode.value() == CommonErrorCode.RESOURCE_NOT_FOUND.getHttpStatus().value()) {
            return CommonErrorCode.RESOURCE_NOT_FOUND;
        }
        if (statusCode.is5xxServerError()) {
            return CommonErrorCode.INTERNAL_SERVER_ERROR;
        }
        return CommonErrorCode.VALIDATION_FAILED;
    }

    private ResponseEntity<Object> createErrorResponse(ErrorCode errorCode) {
        return createErrorResponse(errorCode, null);
    }

    private ResponseEntity<Object> createErrorResponse(
            ErrorCode errorCode, List<ErrorResponse.ErrorDetail> details) {
        ErrorResponse response = details == null || details.isEmpty()
                ? ErrorResponse.error(errorCode.name(), errorCode.getMessage())
                : ErrorResponse.error(errorCode.name(), errorCode.getMessage(), details);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    /**
     * 예외 메시지는 사용자 입력값이나 외부 시스템 응답을 포함할 수 있으므로 기록하지 않는다.
     * 5xx 예외는 메시지를 제외한 stack trace를 함께 남겨 장애 원인을 추적한다.
     */
    private void logException(ErrorCode errorCode, Exception exception) {
        String exceptionType = exception.getClass().getName();
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error(
                    "api_exception error_code={} exception_type={} stack_trace={}",
                    errorCode.name(),
                    exceptionType,
                    Arrays.toString(exception.getStackTrace()));
            return;
        }
        log.warn(
                "api_exception error_code={} exception_type={}",
                errorCode.name(),
                exceptionType);
    }

    /**
     * request body의 중첩 필드는 최상위 필드명만 오류 응답에 포함한다.
     */
    private String toRequestBodyFieldName(String fieldName) {
        int dotIndex = fieldName.indexOf('.');
        int bracketIndex = fieldName.indexOf('[');
        int endIndex = fieldName.length();

        if (dotIndex >= 0) {
            endIndex = Math.min(endIndex, dotIndex);
        }
        if (bracketIndex >= 0) {
            endIndex = Math.min(endIndex, bracketIndex);
        }
        return fieldName.substring(0, endIndex);
    }
}
