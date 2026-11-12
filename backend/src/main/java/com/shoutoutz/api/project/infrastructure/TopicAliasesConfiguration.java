package com.shoutoutz.api.project.infrastructure;

import com.shoutoutz.api.project.domain.TopicAliases;
import com.shoutoutz.api.projectresource.application.ProjectResourceQueryRepository;
import com.shoutoutz.api.projectresource.domain.ResourceTopic;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 별칭은 사람이 검수해 배포와 함께 바꾸는 값이라 DB가 아닌 파일로 둔다.
 * 카탈로그에서 주제가 빠졌는데 별칭이 남거나 한 별칭이 두 주제를 가리키면 서버 시작에 실패시켜, 잘못된 해석이 배포되지 않게 한다.
 */
@Configuration
public class TopicAliasesConfiguration {

    private static final String ALIASES_PATH = "search/topic-aliases.json";

    @Bean
    public TopicAliases topicAliases(
            ObjectMapper objectMapper,
            ProjectResourceQueryRepository projectResourceQueryRepository
    ) {
        Set<String> catalogTopicIds = projectResourceQueryRepository.findTopics().stream()
                .map(ResourceTopic::topicId)
                .collect(Collectors.toUnmodifiableSet());
        return TopicAliases.of(readAliases(objectMapper), catalogTopicIds);
    }

    private static Map<String, List<String>> readAliases(ObjectMapper objectMapper) {
        try (InputStream input = new ClassPathResource(ALIASES_PATH).getInputStream()) {
            return objectMapper.readValue(input, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new UncheckedIOException("기술 주제 별칭 파일을 읽지 못했습니다: " + ALIASES_PATH, exception);
        }
    }
}
