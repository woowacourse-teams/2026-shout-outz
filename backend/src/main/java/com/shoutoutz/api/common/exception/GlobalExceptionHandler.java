package com.shoutoutz.api.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ApplicationEventPublisher eventPublisher;

    // DataIntegrityViolationException은 JPA에서 데이터 무결성 제약 조건을 위반했을 때 발생하는 에러
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolationException(
            DataIntegrityViolationException e, HttpServletRequest request) {
        ErrorCode errorCode = CommonErrorCode.DUPLICATE_RESOURCE;
        return handleExceptionInternal(errorCode, request);
    }

    // EmptyResultDataAccessException은 JPA에서 존재하지 않는 데이터를 삭제하려고 할 때 발생하는 예외
    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Object> handleEmptyResult(
            EmptyResultDataAccessException e, HttpServletRequest request) {
        ErrorCode errorCode = CommonErrorCode.RESOURCE_NOT_FOUND;
        return handleExceptionInternal(errorCode, e.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {
        ErrorCode errorCode = CommonErrorCode.INVALID_PARAMETER;
        return handleExceptionInternal(errorCode, e.getMessage(), request);
    }

    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<Object> handleInvalidFormatException(
            InvalidFormatException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        return handleExceptionInternal(errorCode, request);
    }

    /**
     * Spring Validation 어노테이션 @Validated 사용 시, MethodValidationInterceptor 에서 발생하는 검증 예외 주의. 우리
     * 서버는 @RequestBody의 직렬화 과정에서는 JSP 표준 @Valid 어노테이션을 사용한다. 따라서 ConstraintViolationException 예외의
     * 진입점인 해당 핸들러는, @PathVariable , @RequestParam 검증 실패시만 작동한다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {
        ErrorCode errorCode = CommonErrorCode.INVALID_PARAMETER;
        return handleExceptionInternal(e, errorCode, request);
    }

    /** JSP @Valid 어노테이션 이용시 발생 예외처리 */
    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        ErrorCode errorCode = CommonErrorCode.INVALID_PARAMETER;
        return handleExceptionInternal(e, errorCode, request);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleAllExceptions(Exception e, HttpServletRequest request) {
        ErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
        // TODO: 예기치 못한 모든 예외는 해당 진입점으로 들어옴.
        //  현재 e.message는 errorCode의 메시지로 응답하는 형태. 예외 자체 메시지를 로그에 추가
        // DB에러와 같이 노출되면 안되는 값이 노출될 가능성. 절대 예외자체의 메시지를 응답값으로 넘기면 안됨. (errorCode의 메시지를 응답하기로 함.)

        // TODO: 삭제 예정
        log.error(errorCode.name(), e);
        return handleExceptionInternal(errorCode, request);
    }

    // handleExceptionInternal Method
    private ResponseEntity<Object> handleExceptionInternal(
            ErrorCode errorCode, HttpServletRequest request) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(makeErrorResponse(errorCode, request));
    }

    private ResponseEntity<Object> handleExceptionInternal(
            ErrorCode errorCode, String message, HttpServletRequest request) {
        publishDiscordErrorEvent(errorCode, message, request);
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(makeErrorResponse(errorCode, message, request));
    }

    private ResponseEntity<Object> handleExceptionInternal(
            ConstraintViolationException e, ErrorCode errorCode, HttpServletRequest request) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(makeErrorResponse(e, errorCode, request));
    }

    private ResponseEntity<Object> handleExceptionInternal(
            BindException e, ErrorCode errorCode, WebRequest request) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(makeErrorResponse(e, errorCode, request));
    }

    // makeErrorResponse Method
    private ErrorResponse makeErrorResponse(ErrorCode errorCode, HttpServletRequest request) {
        publishDiscordErrorEvent(errorCode, request);
        return ErrorResponse.builder().code(errorCode.name()).message(errorCode.getMessage()).build();
    }

    private ErrorResponse makeErrorResponse(
            ErrorCode errorCode, String message, HttpServletRequest request) {
        publishDiscordErrorEvent(errorCode, message, request);
        return ErrorResponse.builder().code(errorCode.name()).message(message).build();
    }

    private ErrorResponse makeErrorResponse(
            ConstraintViolationException e, ErrorCode errorCode, HttpServletRequest request) {
        List<ErrorResponse.ValidationError> validationErrorList =
                e.getConstraintViolations().stream()
                        .map(violation -> ErrorResponse.ValidationError.of(violation, errorCode))
                        .collect(Collectors.toList());

        ErrorResponse response = ErrorResponse.builder().errors(validationErrorList).build();
        publishDiscordErrorEvent(errorCode, response, request);
        return response;
    }

    private ErrorResponse makeErrorResponse(
            BindException e, ErrorCode errorCode, WebRequest request) {
        List<ErrorResponse.ValidationError> validationErrorList =
                e.getBindingResult().getFieldErrors().stream()
                        .map(fieldError -> ErrorResponse.ValidationError.of(fieldError, errorCode))
                        .collect(Collectors.toList());

        ErrorResponse response = ErrorResponse.builder().errors(validationErrorList).build();
        publishDiscordErrorEvent(errorCode, response, request);
        return response;
    }

    private void publishDiscordErrorEvent(ErrorCode errorCode, HttpServletRequest request) {
        if (errorCode.getHttpStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
            eventPublisher.publishEvent(
                    new DiscordInternalServerErrorEvent(
                            request.getRequestURI(), errorCode, errorCode.getMessage()));
            return;
        }
        eventPublisher.publishEvent(
                new DiscordGeneralErrorEvent(request.getRequestURI(), errorCode, errorCode.getMessage()));
    }

    private void publishDiscordErrorEvent(
            ErrorCode errorCode, String message, HttpServletRequest request) {
        if (errorCode.getHttpStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
            eventPublisher.publishEvent(
                    new DiscordInternalServerErrorEvent(request.getRequestURI(), errorCode, message));
            return;
        }
        eventPublisher.publishEvent(
                new DiscordGeneralErrorEvent(request.getRequestURI(), errorCode, message));
    }

    private void publishDiscordErrorEvent(
            ErrorCode errorCode, ErrorResponse response, HttpServletRequest request) {
        if (errorCode.getHttpStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
            eventPublisher.publishEvent(
                    new DiscordInternalServerErrorEvent(
                            request.getRequestURI(),
                            errorCode,
                            response.getErrors().stream()
                                    .map(ErrorResponse.ValidationError::message)
                                    .collect(Collectors.joining(", "))));
            return;
        }
        eventPublisher.publishEvent(
                new DiscordGeneralErrorEvent(
                        request.getRequestURI(),
                        errorCode,
                        response.getErrors().stream()
                                .map(ErrorResponse.ValidationError::message)
                                .collect(Collectors.joining(", "))));
    }

    private void publishDiscordErrorEvent(
            ErrorCode errorCode, ErrorResponse response, WebRequest request) {

        String requestUri = request.getDescription(false).replace("uri=", "");
        if (errorCode.getHttpStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
            eventPublisher.publishEvent(
                    new DiscordInternalServerErrorEvent(
                            requestUri,
                            errorCode,
                            response.getErrors().stream()
                                    .map(ErrorResponse.ValidationError::message)
                                    .collect(Collectors.joining(", "))));
            return;
        }
        eventPublisher.publishEvent(
                new DiscordGeneralErrorEvent(
                        requestUri,
                        errorCode,
                        response.getErrors().stream()
                                .map(ErrorResponse.ValidationError::message)
                                .collect(Collectors.joining(", "))));
    }
}
