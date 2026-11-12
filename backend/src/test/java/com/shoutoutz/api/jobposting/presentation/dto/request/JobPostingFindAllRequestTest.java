package com.shoutoutz.api.jobposting.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.jobposting.domain.JobPostingSort;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JobPostingFindAllRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("정렬과 size 가 없으면 최신순 20개를 쓰고, 빈 검색어와 빈 커서는 null 로 둔다.")
    void usesDefaults() {
        JobPostingFindAllRequest request = new JobPostingFindAllRequest(null, " ", null, " ", null);

        assertThat(request.resolvedSort()).isEqualTo(JobPostingSort.LATEST);
        assertThat(request.resolvedSize()).isEqualTo(20);
        assertThat(request.keyword()).isNull();
        assertThat(request.cursor()).isNull();
    }

    @Test
    @DisplayName("검색어 앞뒤 공백을 지운다.")
    void trimsKeyword() {
        JobPostingFindAllRequest request = new JobPostingFindAllRequest(null, "  토스  ", null, null, null);

        assertThat(request.keyword()).isEqualTo("토스");
    }

    @Test
    @DisplayName("검색어는 앞뒤 공백을 지운 뒤 100자까지 허용한다.")
    void validatesKeywordLength() {
        assertThat(validator.validate(request("가".repeat(100), null))).isEmpty();
        assertThat(validator.validate(request(" " + "가".repeat(100) + " ", null))).isEmpty();
        assertThat(validator.validate(request("가".repeat(101), null))).hasSize(1);
    }

    @Test
    @DisplayName("검색어 길이는 이모지도 한 글자로 센다.")
    void countsEmojiAsOneCharacter() {
        assertThat(validator.validate(request("😀".repeat(100), null))).isEmpty();
        assertThat(validator.validate(request("😀".repeat(101), null))).hasSize(1);
    }

    @Test
    @DisplayName("size 는 1부터 50까지 허용한다.")
    void validatesSizeRange() {
        assertThat(validator.validate(request(null, 1))).isEmpty();
        assertThat(validator.validate(request(null, 50))).isEmpty();
        assertThat(validator.validate(request(null, 0))).hasSize(1);
        assertThat(validator.validate(request(null, 51))).hasSize(1);
    }

    private JobPostingFindAllRequest request(String keyword, Integer size) {
        return new JobPostingFindAllRequest(null, keyword, null, null, size);
    }
}
