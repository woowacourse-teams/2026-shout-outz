package com.shoutoutz.api.resourcesearch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.resourcesearch.application.ResourceTopicQueryRepository;
import com.shoutoutz.api.resourcesearch.application.dto.CatalogTopic;
import com.shoutoutz.api.resourcesearch.application.dto.PopularTopic;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceTopic;
import com.shoutoutz.api.resourcesearch.domain.ResourceTopicSide;
import com.shoutoutz.api.resourcesearch.support.ResourceDocumentFixture;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ResourceTopicQueryRepositoryIntegrationTest {

    @Autowired
    private ResourceTopicQueryRepository resourceTopicQueryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ResourceDocumentFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ResourceDocumentFixture(jdbcTemplate);
        jdbcTemplate.update("UPDATE resource_documents SET deleted_at = now() WHERE deleted_at IS NULL");
    }

    @Test
    @DisplayName("카탈로그의 주제 77개를 분야별 개수 그대로 준다")
    void findsAllCatalogTopics() {
        List<CatalogTopic> topics = resourceTopicQueryRepository.findAllOrderByName();

        Map<ResourceTopicSide, Long> countsBySide = topics.stream()
                .collect(Collectors.groupingBy(CatalogTopic::side, Collectors.counting()));
        assertThat(topics).hasSize(77);
        assertThat(countsBySide).containsExactlyInAnyOrderEntriesOf(Map.of(
                ResourceTopicSide.BACKEND, 23L,
                ResourceTopicSide.FRONTEND, 15L,
                ResourceTopicSide.COMMON, 37L,
                ResourceTopicSide.MOBILE, 2L));
    }

    @Test
    @DisplayName("주제는 영문 대소문자를 가리지 않는 가나다순이다")
    void ordersTopicsByNameIgnoringCase() {
        List<String> names = resourceTopicQueryRepository.findAllOrderByName().stream()
                .map(topic -> topic.topic().name())
                .toList();

        assertThat(names).isSortedAccordingTo((left, right) -> left.toLowerCase().compareTo(right.toLowerCase()));
        assertThat(names.indexOf("Android")).isLessThan(names.indexOf("API 문서화"));
        assertThat(names.indexOf("Spring 동작 원리(빈, AOP, 프록시)")).isLessThan(names.indexOf("동시성 제어/락"));
        assertThat(names.indexOf("동시성 제어/락")).isLessThan(names.indexOf("무중단 배포"));
    }

    @Test
    @DisplayName("많이 다룬 주제는 삭제되지 않은 자료 수가 많은 순이고, 같으면 이름순이다")
    void ordersPopularTopicsByResourceCountThenName() {
        saveDocuments("concurrency", 3);
        saveDocuments("cicd", 2);
        saveDocuments("auth", 2);
        fixture.document("blog-post").topic("concurrency").deleted().save();
        fixture.document("blog-post").topic("zero_downtime").deleted().save();

        List<PopularTopic> topics = resourceTopicQueryRepository.findMostCovered(10);

        assertThat(topics).containsExactly(
                new PopularTopic(new ResourceTopic("concurrency", "동시성 제어/락"), 3),
                new PopularTopic(new ResourceTopic("cicd", "CI/CD"), 2),
                new PopularTopic(new ResourceTopic("auth", "인증/인가"), 2));
    }

    @Test
    @DisplayName("많이 다룬 주제는 요청한 개수까지만 준다")
    void limitsPopularTopics() {
        saveDocuments("concurrency", 2);
        saveDocuments("cicd", 1);

        List<PopularTopic> topics = resourceTopicQueryRepository.findMostCovered(1);

        assertThat(topics).extracting(topic -> topic.topic().topicId()).containsExactly("concurrency");
    }

    @Test
    @DisplayName("자료가 없는 주제는 많이 다룬 주제에 넣지 않는다")
    void excludesTopicsWithoutResources() {
        List<PopularTopic> topics = resourceTopicQueryRepository.findMostCovered(10);

        assertThat(topics).isEmpty();
    }

    private void saveDocuments(String topic, int count) {
        for (int index = 0; index < count; index++) {
            fixture.document("blog-post").topic(topic).save();
        }
    }
}
