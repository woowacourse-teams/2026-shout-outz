package com.shoutoutz.api.verification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.DuplicateEntityException;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.account.UserRepository;
import com.shoutoutz.api.verification.domain.UserVerificationRequest;
import com.shoutoutz.api.verification.domain.UserVerificationRequestHistory;
import com.shoutoutz.api.verification.domain.UserVerificationRequestHistoryRepository;
import com.shoutoutz.api.verification.domain.UserVerificationRequestRepository;
import com.shoutoutz.api.verification.domain.VerificationRequestStatus;
import com.shoutoutz.api.user.domain.profile.UserType;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UserVerificationRequestRepositoryIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-16T00:00:00Z");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserVerificationRequestRepository requestRepository;

    @Autowired
    private UserVerificationRequestHistoryRepository historyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 인증_신청과_NULL에서_PENDING으로의_이력을_저장하고_조회한다() {
        User user = userRepository.save(User.initialize(uniqueHandle()));
        UserVerificationRequest request = requestRepository.save(
                UserVerificationRequest.create(
                        user.getId(),
                        UserType.WOOWACOURSE_CREW,
                        "  샤를  ",
                        8,
                        "BACKEND",
                        NOW
                )
        );
        historyRepository.save(UserVerificationRequestHistory.initial(request.getId(), NOW));
        entityManager.flush();

        UserVerificationRequest found = requestRepository.findPendingByUserId(user.getId())
                .orElseThrow();
        long historyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_verification_request_histories WHERE request_id = ?",
                Long.class,
                request.getId()
        );
        Integer nullFromStatusCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM user_verification_request_histories
                        WHERE request_id = ?
                          AND from_status IS NULL
                          AND to_status = 'PENDING'
                        """,
                Integer.class,
                request.getId()
        );

        assertThat(found.getId()).isEqualTo(request.getId());
        assertThat(found.getUserId()).isEqualTo(user.getId());
        assertThat(found.getNickname()).isEqualTo("샤를");
        assertThat(found.getCohort()).isEqualTo(8);
        assertThat(found.getTrack()).isEqualTo("BACKEND");
        assertThat(found.getStatus()).isEqualTo(VerificationRequestStatus.PENDING);
        assertThat(found.getRequestedAt()).isEqualTo(NOW);
        assertThat(historyCount).isEqualTo(1L);
        assertThat(nullFromStatusCount).isEqualTo(1);
    }

    @Test
    void 사용자별_PENDING_신청은_유니크_인덱스로_중복을_막는다() {
        User user = userRepository.save(User.initialize(uniqueHandle()));
        requestRepository.save(request(user.getId(), "샤를"));

        assertThatThrownBy(() -> requestRepository.save(request(user.getId(), "다른 닉네임")))
                .isInstanceOf(DuplicateEntityException.class);
    }

    @Test
    void REJECTED_신청이_있으면_같은_사용자가_새_PENDING_신청을_저장할_수_있다() {
        User user = userRepository.save(User.initialize(uniqueHandle()));
        requestRepository.save(UserVerificationRequest.reconstitute(
                null,
                user.getId(),
                UserType.WOOWACOURSE_CREW,
                "기존 신청",
                8,
                "BACKEND",
                VerificationRequestStatus.REJECTED,
                NOW,
                NOW
        ));

        UserVerificationRequest pending = requestRepository.save(request(user.getId(), "새 신청"));

        assertThat(requestRepository.findPendingByUserId(user.getId()))
                .map(UserVerificationRequest::getId)
                .contains(pending.getId());
    }

    @Test
    void 사용자의_가장_최근_신청을_신청시각_내림차순으로_조회한다() {
        User user = userRepository.save(User.initialize(uniqueHandle()));
        requestRepository.save(
                UserVerificationRequest.reconstitute(
                        null,
                        user.getId(),
                        UserType.WOOWACOURSE_CREW,
                        "이전 신청",
                        8,
                        "BACKEND",
                        VerificationRequestStatus.REJECTED,
                        NOW.minusSeconds(60),
                        NOW.minusSeconds(30)
                )
        );
        UserVerificationRequest latestRequest = requestRepository.save(
                request(user.getId(), "최신 신청")
        );

        assertThat(requestRepository.findLatestByUserId(user.getId()))
                .map(UserVerificationRequest::getId)
                .contains(latestRequest.getId());
    }

    @Test
    void 신청의_최신_승인_또는_반려_이력을_조회한다() {
        User user = userRepository.save(User.initialize(uniqueHandle()));
        UserVerificationRequest request = requestRepository.save(
                UserVerificationRequest.reconstitute(
                        null,
                        user.getId(),
                        UserType.WOOWACOURSE_CREW,
                        "샤를",
                        8,
                        "BACKEND",
                        VerificationRequestStatus.REJECTED,
                        NOW,
                        NOW
                )
        );
        historyRepository.save(UserVerificationRequestHistory.initial(
                request.getId(),
                NOW.minusSeconds(60)
        ));
        historyRepository.save(UserVerificationRequestHistory.reconstitute(
                null,
                request.getId(),
                user.getId(),
                VerificationRequestStatus.PENDING,
                VerificationRequestStatus.REJECTED,
                "Slack 정보와 일치하지 않습니다.",
                NOW
        ));
        entityManager.flush();

        UserVerificationRequestHistory history = historyRepository
                .findLatestDecisionByRequestId(request.getId())
                .orElseThrow();

        assertThat(history.getToStatus()).isEqualTo(VerificationRequestStatus.REJECTED);
        assertThat(history.getReason()).isEqualTo("Slack 정보와 일치하지 않습니다.");
        assertThat(history.getChangedAt()).isEqualTo(NOW);
    }

    private UserVerificationRequest request(long userId, String nickname) {
        return UserVerificationRequest.create(
                userId,
                UserType.WOOWACOURSE_CREW,
                nickname,
                8,
                "BACKEND",
                NOW
        );
    }

    private String uniqueHandle() {
        return "verify-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }
}
