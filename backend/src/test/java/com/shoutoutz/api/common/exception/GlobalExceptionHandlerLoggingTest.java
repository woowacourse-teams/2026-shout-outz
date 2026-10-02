package com.shoutoutz.api.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.shoutoutz.api.common.exception.code.CommonErrorCode;
import com.shoutoutz.api.common.exception.custom.CustomException;
import com.shoutoutz.api.common.exception.custom.DomainValidationException;
import com.shoutoutz.api.common.exception.custom.PersistenceException;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

class GlobalExceptionHandlerLoggingTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private Logger logger;
    private Level previousLevel;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.ALL);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logger.setLevel(previousLevel);
        logAppender.stop();
    }

    @ParameterizedTest
    @MethodSource("customExceptionCases")
    @DisplayName("커스텀 예외의 HTTP 상태에 따라 4xx는 WARN, 5xx는 ERROR로 기록한다")
    void logsCustomExceptionsUsingTheirHttpStatus(CustomException exception, Level expectedLevel) {
        handler.handleCustomException(exception);

        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent event = logAppender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(expectedLevel);
        assertThat(event.getFormattedMessage())
                .contains("error_code=" + exception.getErrorCode().name())
                .contains("exception_type=" + exception.getClass().getName())
                .doesNotContain("database password", "private input");
        assertThat(event.getThrowableProxy()).isNull();
        if (expectedLevel == Level.ERROR) {
            assertThat(event.getFormattedMessage()).contains("stack_trace=");
        }
    }

    private static Stream<Arguments> customExceptionCases() {
        return Stream.of(
                Arguments.of(new DomainValidationException(
                        CommonErrorCode.VALIDATION_FAILED, new RuntimeException("private input")), Level.ERROR),
                Arguments.of(new PersistenceException(
                        CommonErrorCode.INTERNAL_SERVER_ERROR, new RuntimeException("database password")), Level.ERROR));
    }
}
