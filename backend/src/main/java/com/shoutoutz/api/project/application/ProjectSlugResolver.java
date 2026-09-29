package com.shoutoutz.api.project.application;

import static com.shoutoutz.api.project.domain.ProjectErrorCode.PROJECT_NOT_FOUND;

import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.project.domain.ProjectRepository;
import com.shoutoutz.api.project.domain.Slug;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 주소로 받은 프로젝트 slug 를 내부 id 로 바꾼다.
 * 외부에는 slug 만 드러내고, 서비스 내부 로직과 다른 테이블과의 연결은 id 로 처리한다.
 */
@Component
@RequiredArgsConstructor
public class ProjectSlugResolver {

    private final ProjectRepository projectRepository;

    /**
     * 형식이 틀린 slug 와 없는 slug 는 모두 404로 응답한다.
     * 삭제 여부와 승인 상태는 보지 않으므로, 호출한 쪽에서 기존 규칙대로 확인한다.
     */
    public long resolveId(String slug) {
        return Slug.parse(slug)
                .flatMap(projectRepository::findIdBySlug)
                .orElseThrow(() -> new EntityNotFoundException(PROJECT_NOT_FOUND));
    }
}
