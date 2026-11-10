package com.shoutoutz.api.projectresource.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.shoutoutz.api.projectresource.domain.ProjectTopicGroup;
import com.shoutoutz.api.projectresource.domain.ResourceTopic;
import com.shoutoutz.api.projectresource.domain.TopicProject;
import com.shoutoutz.api.projectresource.presentation.dto.response.ProjectTopicsResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectTopicServiceTest {

    @Mock
    private ProjectResourceQueryRepository projectResourceQueryRepository;

    @InjectMocks
    private ProjectTopicService projectTopicService;

    @Test
    @DisplayName("주제별 프로젝트 수를 프론트 목업과 같은 직군 묶음 모양으로 준다")
    void findsTopics() {
        given(projectResourceQueryRepository.findTopics()).willReturn(List.of(
                new ResourceTopic("auth", "인증/인가", "common", "아키텍처, 설계"),
                new ResourceTopic("concurrency", "동시성 제어/락", "backend", "동시성, 성능")
        ));
        given(projectResourceQueryRepository.findTopicProjects()).willReturn(List.of(
                new TopicProject("auth", 1L),
                new TopicProject("concurrency", 1L),
                new TopicProject("concurrency", 2L)
        ));

        ProjectTopicsResponse response = projectTopicService.findTopics();

        assertThat(response).isEqualTo(new ProjectTopicsResponse(2, List.of(
                new ProjectTopicsResponse.Group(ProjectTopicGroup.COMMON, "공통", "공통", 1, List.of(
                        new ProjectTopicsResponse.Topic("auth", "인증/인가", 1)
                )),
                new ProjectTopicsResponse.Group(ProjectTopicGroup.BE, "백엔드", "BE", 2, List.of(
                        new ProjectTopicsResponse.Topic("concurrency", "동시성 제어/락", 2)
                ))
        )));
    }
}
