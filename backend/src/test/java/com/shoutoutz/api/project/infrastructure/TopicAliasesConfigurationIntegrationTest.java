package com.shoutoutz.api.project.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.project.domain.NormalizedKeyword;
import com.shoutoutz.api.project.domain.TopicAliases;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 별칭 파일(search/topic-aliases.json)이 기술 주제 카탈로그와 맞아 서버가 뜨고, 대표 검색어가 의도한 주제로 해석되는지 본다.
 */
@ActiveProfiles("test")
@SpringBootTest
class TopicAliasesConfigurationIntegrationTest {

    @Autowired
    private TopicAliases topicAliases;

    @ParameterizedTest
    @CsvSource({
            "동시성, concurrency",
            "JWT, auth",
            "블루그린, zero_downtime",
            "n+1, n_plus_one",
            "카프카, messaging"
    })
    @DisplayName("대표 검색어는 프로토타입 동의어를 옮긴 별칭으로 해당 주제에 매핑된다.")
    void mapsRepresentativeKeywords(String keyword, String topicId) {
        assertThat(topicAliases.match(NormalizedKeyword.of(keyword))).contains(topicId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"롤백", "프로토타입", "eda", "db"})
    @DisplayName("여러 주제에 걸쳐 뺀 별칭과 맞는 주제가 없는 검색어는 매핑하지 않는다.")
    void doesNotMapDroppedOrUnknownKeywords(String keyword) {
        assertThat(topicAliases.match(NormalizedKeyword.of(keyword))).isEmpty();
    }
}
