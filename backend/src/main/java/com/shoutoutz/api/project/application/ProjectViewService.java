package com.shoutoutz.api.project.application;

import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_NOT_FOUND;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.project.domain.ProjectViewRepository;
import com.shoutoutz.api.project.presentation.dto.response.ProjectViewRecordResponse;
import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로젝트 조회를 방문자, 날짜별로 한 번씩 기록한다.
 */
@Service
@RequiredArgsConstructor
public class ProjectViewService {

    /**
     * 사용자가 체감하는 하루에 맞춰 한국 날짜로 집계한다. Clock 빈은 UTC 이므로 시간대를 명시한다.
     */
    private static final ZoneId VIEW_DATE_ZONE = ZoneId.of("Asia/Seoul");

    private final ProjectViewRepository projectViewRepository;
    private final Clock clock;

    /**
     * 없거나 삭제됐거나 승인되지 않은 프로젝트는 404 다. 승인 전 프로젝트는 등록자 본인이 조회해도 기록하지 않는다.
     * 같은 날 이미 기록된 조회는 예외 없이 넘어가고, 현재 조회수를 돌려준다.
     */
    @Transactional
    public ProjectViewRecordResponse record(long projectId, VisitorKey visitorKey) {
        if (!projectViewRepository.existsViewableProject(projectId)) {
            throw new EntityNotFoundException(PROJECT_NOT_FOUND);
        }

        Instant viewedAt = clock.instant();
        LocalDate viewedOn = LocalDate.ofInstant(viewedAt, VIEW_DATE_ZONE);
        long viewCount = projectViewRepository.record(projectId, visitorKey, viewedOn, viewedAt);
        return new ProjectViewRecordResponse(viewCount);
    }
}
