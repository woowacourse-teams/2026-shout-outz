package com.shoutoutz.api.feed.infrastructure;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LinkPreviewUriTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://github.com/woowacourse-teams/2021-botobo/pull/378",
            "https://2022-momo.github.io/Backend/20220821-%EC%8A%A4%ED%94%84%EB%A7%81",
            "https://kskim.notion.site/AspectJ-AOP-4dd93b3a1c5e?source=copy_link&p=1#top",
            "https://www.youtube.com/watch?v=nER0Z_99OmQ",
            "https://example.com/search?tags[]=java&q=a+b",
            "https://example.com/a;b,c:d@e/~f*'()!$",
            "https://[2001:db8::1]/path",
    })
    @DisplayName("이미 올바른 주소는 한 글자도 바꾸지 않는다")
    void keepsValidUrl(String url) {
        assertThat(LinkPreviewUri.parse(url).toString()).isEqualTo(url);
    }

    @Test
    @DisplayName("경로에 그대로 들어간 대괄호와 한글을 인코딩한다")
    void encodesBracketsAndKoreanInPath() {
        assertThat(LinkPreviewUri.parse("https://github.com/woowacourse-teams/2021-zzimkkong/wiki/[BE]-DB-백업"))
                .hasToString("https://github.com/woowacourse-teams/2021-zzimkkong/wiki/%5BBE%5D-DB-%EB%B0%B1%EC%97%85");
    }

    @Test
    @DisplayName("이모지와 공백은 UTF-8 바이트로 인코딩한다")
    void encodesEmojiAndSpace() {
        assertThat(LinkPreviewUri.parse("https://github.com/a/b/wiki/🤖AN DI"))
                .hasToString("https://github.com/a/b/wiki/%F0%9F%A4%96AN%20DI");
    }

    @Test
    @DisplayName("이미 인코딩된 %XX 는 그대로 두고, 뒤에 16진수가 없는 % 만 인코딩한다")
    void encodesOnlyBarePercent() {
        assertThat(LinkPreviewUri.parse("https://example.com/100%-%ED%95%9C"))
                .hasToString("https://example.com/100%25-%ED%95%9C");
    }

    @Test
    @DisplayName("# 뒤에 다시 나온 # 은 인코딩한다")
    void encodesSecondHash() {
        assertThat(LinkPreviewUri.parse("https://example.com/a#b#c")).hasToString("https://example.com/a#b%23c");
    }

    @Test
    @DisplayName("Location 에 UTF-8 바이트 그대로 온 한글을 되살려 현재 주소 기준으로 푼다")
    void resolvesRawUtf8Location() {
        URI current = URI.create("https://2022-momo.github.io/Backend/20220821-%EC%8A%A4%ED%94%84%EB%A7%81");
        String location = new String("/Backend/20220821-스프링/".getBytes(UTF_8), ISO_8859_1);

        assertThat(LinkPreviewUri.resolveLocation(current, location))
                .hasToString("https://2022-momo.github.io/Backend/20220821-%EC%8A%A4%ED%94%84%EB%A7%81/");
    }

    @Test
    @DisplayName("UTF-8 로 읽을 수 없는 Location 은 받은 글자 그대로 인코딩한다")
    void keepsLatin1LocationThatIsNotUtf8() {
        URI current = URI.create("https://example.com/");

        assertThat(LinkPreviewUri.resolveLocation(current, "/café"))
                .hasToString("https://example.com/caf%C3%A9");
    }

    @Test
    @DisplayName("페이지 안의 상대 주소도 인코딩해서 페이지 주소 기준으로 푼다")
    void resolvesRelativeReference() {
        URI page = URI.create("https://blog.example.com/posts/1");

        assertThat(LinkPreviewUri.resolve(page, " ../assets/대표 이미지.png "))
                .hasToString("https://blog.example.com/assets/%EB%8C%80%ED%91%9C%20%EC%9D%B4%EB%AF%B8%EC%A7%80.png");
    }
}
