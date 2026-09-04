package com.shoutoutz.api.common.exception;

import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.code.ErrorCode;
import com.shoutoutz.api.common.exception.custom.CustomException;
import com.shoutoutz.api.common.exception.event.DiscordGeneralErrorEvent;
import com.shoutoutz.api.common.exception.event.DiscordInternalServerErrorEvent;
import com.shoutoutz.api.common.response.JSendResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * IllegalArgumentException나 NoSuchElementException 같은 예외의
     * message를 응답에 담지 않도록, 최대한 예외는 원인을 표시한 커스텀 예외를 사용한다.
     */
    @Deprecated
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {
        ErrorCode errorCode = CommonErrorCode.INVALID_PARAMETER;
        return handleExceptionInternal(errorCode, e.getMessage(), request);
    }

    @Deprecated
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Object> handleNoSuchElementException(
            NoSuchElementException e, HttpServletRequest request) {
        ErrorCode errorCode = CommonErrorCode.RESOURCE_NOT_FOUND;
        return handleExceptionInternal(errorCode, e.getMessage(), request);
    }

    /**
     * @Validated 기반, 메서드 파라미터나 반환값 검증에 실패했을 때 발생하는 예외인,
     * ConstraintViolationException 처리 핸들러
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {
        ErrorCode errorCode = CommonErrorCode.INVALID_PARAMETER;
        return handleExceptionInternal(e, errorCode, request);
    }

    /**
     * @RequestBody DTO에 Jakarta Bean Validation의 @Valid는
     * 검증 실패 시 MethodArgumentNotValidException이 발생한다.
     *
     * 해당 예외를 처리하는 핸들러는 이미 ResponseEntityExceptionHandler에 구현되어 있다.
     * 따라서, handleMethodArgumentNotValid 메서드를 재정의하여,
     * JSend 형식의 응답으로 커스텀 한다.
     */
    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        ErrorCode errorCode = CommonErrorCode.INVALID_PARAMETER;
        return handleExceptionInternal(e, errorCode, request);
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
        ErrorCode errorCode = CommonErrorCode.INVALID_PARAMETER;
        return handleExceptionInternal(e, errorCode, request);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Object> handleCustomException(
            CustomException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        /**
         * 500 서버 에러인 경우 ErrorResponse로 반환
         */
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error(errorCode.name(), e);
            return ResponseEntity.status(errorCode.getHttpStatus())
                    .body(makeErrorResponse(errorCode, request));
        }
        /**
         * 500 이외인 경우, FailErrorResponse를 생성하는 하위 메서드로 이동
         */
        return handleExceptionInternal(errorCode, request);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleAllExceptions(Exception e, HttpServletRequest request) {
        ErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
        // TODO: 예기치 못한 모든 예외는 해당 진입점으로 들어옴.
        //  현재 e.message는 errorCode의 메시지로 응답하는 형태. 예외 자체 메시지를 로그에 추가
        // DB에러와 같이 노출되면 안되는 값이 노출될 가능성. 절대 예외자체의 메시지를 응답값으로 넘기면 안됨. (errorCode의 메시지를 응답하기로 함.)

        // TODO: 삭제 예정
        log.error(errorCode.name(), e);
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(makeErrorResponse(errorCode, request));
    }

    // handleExceptionInternal Method
    private ResponseEntity<Object> handleExceptionInternal(
            ErrorCode errorCode, HttpServletRequest request) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(makeFailResponse(errorCode, request));
    }

    private ResponseEntity<Object> handleExceptionInternal(
            ErrorCode errorCode, String message, HttpServletRequest request) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(makeFailResponse(errorCode, message, request));
    }

    /**
     * ConstraintViolationException 이 발생한 경우의 예외 응답 생성 메서드
     */
    private ResponseEntity<Object> handleExceptionInternal(
            ConstraintViolationException e, ErrorCode errorCode, HttpServletRequest request) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(makeFailResponse(e, errorCode, request));
    }

    /**
     * MethodArgumentNotValidException이 발생한 경우의 예외 응답 생성 메서드
     */
    private ResponseEntity<Object> handleExceptionInternal(
            BindException e, ErrorCode errorCode, WebRequest request) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(makeFailResponse(e, errorCode, request));
    }

    /**
     * HandlerMethodValidationException이 발생한 경우의 예외 응답 생성 메서드
     */
    private ResponseEntity<Object> handleExceptionInternal(
            HandlerMethodValidationException e, ErrorCode errorCode, WebRequest request) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(makeFailResponse(e, errorCode, request));
    }

    /**
     * ResponseEntityExceptionHandler가 기본으로 생성한 ProblemDetail을 사용하지 않고,
     * 모든 Spring MVC 예외를 JSend 형식으로 변환하도록 통일
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        ErrorCode errorCode = resolveErrorCode(statusCode);
        JSendResponse<Object> response = statusCode.is5xxServerError()
                ? JSendResponse.error(errorCode.name(), errorCode.getMessage(), null)
                : JSendResponse.fail(errorCode.name(), errorCode.getMessage(), null);

        if (statusCode.is5xxServerError()) {
            log.error(errorCode.name(), e);
        }

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
        return CommonErrorCode.INVALID_PARAMETER;
    }

    /**
     * 예외 응답을 만드는 메서드
     * JSend 형식을 따르기 때문에, 요청이 예외의 원인인 경우는 Fail 응답, 요청은 올바른데 발생한 예외인 경우는 Error Response를 반환한다.
     */
    private JSendResponse<Object> makeErrorResponse(ErrorCode errorCode, HttpServletRequest request) {
        publishDiscordErrorEvent(errorCode, request);
        return JSendResponse.error(errorCode.name(), errorCode.getMessage(), null);
    }

    private JSendResponse<Object> makeFailResponse(ErrorCode errorCode, HttpServletRequest request) {
        publishDiscordErrorEvent(errorCode, request);
        return JSendResponse.fail(errorCode.name(), errorCode.getMessage(), null);
    }

    private JSendResponse<Object> makeFailResponse(
            ErrorCode errorCode, String message, HttpServletRequest request) {
        publishDiscordErrorEvent(errorCode, message, request);
        return JSendResponse.fail(errorCode.name(), message, null);
    }

    private JSendResponse<Object> makeFailResponse(
            ConstraintViolationException e, ErrorCode errorCode, HttpServletRequest request) {
        List<JSendResponse.ValidationField> validationFields =
                e.getConstraintViolations().stream()
                        .map(violation -> new JSendResponse.ValidationField(
                                violation.getPropertyPath().toString(), violation.getMessage()))
                        .collect(Collectors.toList());

        JSendResponse.ValidationData data = new JSendResponse.ValidationData(validationFields);
        publishDiscordErrorEvent(errorCode, data, request);
        return JSendResponse.fail(errorCode.name(), errorCode.getMessage(), data);
    }

    private JSendResponse<Object> makeFailResponse(
            BindException e, ErrorCode errorCode, WebRequest request) {
        List<JSendResponse.ValidationField> validationFields =
                e.getBindingResult().getFieldErrors().stream()
                        .map(fieldError -> new JSendResponse.ValidationField(
                                fieldError.getField(), fieldError.getDefaultMessage()))
                        .collect(Collectors.toList());

        JSendResponse.ValidationData data = new JSendResponse.ValidationData(validationFields);
        publishDiscordErrorEvent(errorCode, data, request);
        return JSendResponse.fail(errorCode.name(), errorCode.getMessage(), data);
    }

    private JSendResponse<Object> makeFailResponse(
            HandlerMethodValidationException e, ErrorCode errorCode, WebRequest request) {
        List<JSendResponse.ValidationField> validationFields = new ArrayList<>();

        for (ParameterValidationResult result : e.getParameterValidationResults()) {
            String parameterName = result.getMethodParameter().getParameterName();
            if (parameterName == null) {
                parameterName = "parameter" + result.getMethodParameter().getParameterIndex();
            }
            String resolvedParameterName = parameterName;

            if (result instanceof ParameterErrors errors) {
                for (ObjectError validationError : errors.getAllErrors()) {
                    String field = validationError instanceof FieldError fieldError
                            ? fieldError.getField()
                            : resolvedParameterName;
                    validationFields.add(new JSendResponse.ValidationField(
                            field, validationError.getDefaultMessage()));
                }
                continue;
            }

            result.getResolvableErrors().forEach(validationError -> validationFields.add(
                    new JSendResponse.ValidationField(
                            resolvedParameterName, validationError.getDefaultMessage())));
        }

        e.getCrossParameterValidationResults().forEach(validationError -> validationFields.add(
                new JSendResponse.ValidationField(
                        "parameters", validationError.getDefaultMessage())));

        JSendResponse.ValidationData data = new JSendResponse.ValidationData(validationFields);
        publishDiscordErrorEvent(errorCode, data, request);
        return JSendResponse.fail(errorCode.name(), errorCode.getMessage(), data);
    }

    /**
     * 예외 발생시 Discord 전송 이벤트
     */
    private void publishDiscordErrorEvent(ErrorCode errorCode, HttpServletRequest request) {
        publishDiscordErrorEvent(errorCode, errorCode.getMessage(), request.getRequestURI());
    }

    private void publishDiscordErrorEvent(
            ErrorCode errorCode, String message, HttpServletRequest request) {
        publishDiscordErrorEvent(errorCode, message, request.getRequestURI());
    }

    private void publishDiscordErrorEvent(
            ErrorCode errorCode, JSendResponse.ValidationData response, HttpServletRequest request) {
        String message = response.fields().stream()
                .map(JSendResponse.ValidationField::reason)
                .collect(Collectors.joining(", "));
        publishDiscordErrorEvent(errorCode, message, request);
    }

    private void publishDiscordErrorEvent(
            ErrorCode errorCode, JSendResponse.ValidationData response, WebRequest request) {
        String requestUri = request.getDescription(false).replace("uri=", "");
        String message = response.fields().stream()
                .map(JSendResponse.ValidationField::reason)
                .collect(Collectors.joining(", "));
        publishDiscordErrorEvent(errorCode, message, requestUri);
    }

    private void publishDiscordErrorEvent(ErrorCode errorCode, String message, String requestUri) {
        if (errorCode.getHttpStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
            eventPublisher.publishEvent(new DiscordInternalServerErrorEvent(requestUri, errorCode, message));
            return;
        }
        eventPublisher.publishEvent(new DiscordGeneralErrorEvent(requestUri, errorCode, message));
    }
}
