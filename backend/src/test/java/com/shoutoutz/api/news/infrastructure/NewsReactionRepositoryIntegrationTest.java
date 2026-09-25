package com.shoutoutz.api.news.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.news.domain.NewsReactionRepository;
import com.shoutoutz.api.news.domain.NewsReactionType;
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
class NewsReactionRepositoryIntegrationTest {

    @Autowired
    private NewsReactionRepository newsReactionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 같은_사용자의_좋아요를_중복_추가해도_한_건만_저장한다() {
        long firstUserId = insertUser();
        long secondUserId = insertUser();
        long newsId = insertNews(firstUserId);

        newsReactionRepository.add(newsId, firstUserId, NewsReactionType.LIKE);
        newsReactionRepository.add(newsId, firstUserId, NewsReactionType.LIKE);
        newsReactionRepository.add(newsId, secondUserId, NewsReactionType.LIKE);

        assertThat(newsReactionRepository.countByNewsId(newsId)).isEqualTo(2L);

        newsReactionRepository.remove(newsId, firstUserId, NewsReactionType.LIKE);
        newsReactionRepository.remove(newsId, firstUserId, NewsReactionType.LIKE);

        assertThat(newsReactionRepository.countByNewsId(newsId)).isEqualTo(1L);
    }

    private long insertUser() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (handle) VALUES (?) RETURNING id",
                Long.class,
                "news_reaction_" + token
        );
    }

    private long insertNews(long authorId) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO news (
                            type, title, summary, body, author_id, author_name, published_at,
                            is_pinned, pin_order, cta_label, cta_url
                        ) VALUES (
                            'NOTICE', '반응 테스트 소식', '반응 테스트 요약', '반응 테스트 본문', ?, '작성자', now(),
                            false, null, null, null
                        )
                        RETURNING id
                        """,
                Long.class,
                authorId
        );
    }
}
