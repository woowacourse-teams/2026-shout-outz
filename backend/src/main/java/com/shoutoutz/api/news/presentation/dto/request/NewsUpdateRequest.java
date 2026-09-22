package com.shoutoutz.api.news.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.shoutoutz.api.news.presentation.validation.ValidNewsUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * PUT 전체 교체 요청.
 * nullable 필드가 null인지와 필드가 아예 빠졌는지를 구분하기 위해 전달 여부를 함께 보관한다.
 */
@ValidNewsUpdateRequest
public final class NewsUpdateRequest {

    @NotBlank(message = "title은 필수입니다.")
    @Size(max = 100, message = "title은 100자를 초과할 수 없습니다.")
    private String title;

    @NotBlank(message = "summary는 필수입니다.")
    @Size(max = 200, message = "summary는 200자를 초과할 수 없습니다.")
    private String summary;

    @NotBlank(message = "body는 필수입니다.")
    @Size(max = 100_000, message = "body는 100,000자를 초과할 수 없습니다.")
    private String body;

    @NotBlank(message = "authorName은 필수입니다.")
    @Size(max = 50, message = "authorName은 50자를 초과할 수 없습니다.")
    private String authorName;

    private Instant eventStartAt;
    private Instant eventEndAt;

    @Valid
    private Cta cta;

    private boolean eventStartAtProvided;
    private boolean eventEndAtProvided;
    private boolean ctaProvided;

    @JsonCreator
    public NewsUpdateRequest() {
    }

    /**
     * 테스트와 내부 호출에서 사용하는 전체 필드 생성자.
     * nullable 필드도 생성자에 전달한 순간 명시적으로 입력한 것으로 간주한다.
     */
    public NewsUpdateRequest(
            String title,
            String summary,
            String body,
            String authorName,
            Instant eventStartAt,
            Instant eventEndAt,
            Cta cta
    ) {
        setTitle(title);
        setSummary(summary);
        setBody(body);
        setAuthorName(authorName);
        setEventStartAt(eventStartAt);
        setEventEndAt(eventEndAt);
        setCta(cta);
    }

    @JsonSetter(value = "title", nulls = Nulls.SET)
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonSetter(value = "summary", nulls = Nulls.SET)
    public void setSummary(String summary) {
        this.summary = summary;
    }

    @JsonSetter(value = "body", nulls = Nulls.SET)
    public void setBody(String body) {
        this.body = body;
    }

    @JsonSetter(value = "authorName", nulls = Nulls.SET)
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    @JsonSetter(value = "eventStartAt", nulls = Nulls.SET)
    public void setEventStartAt(Instant eventStartAt) {
        this.eventStartAtProvided = true;
        this.eventStartAt = eventStartAt;
    }

    @JsonSetter(value = "eventEndAt", nulls = Nulls.SET)
    public void setEventEndAt(Instant eventEndAt) {
        this.eventEndAtProvided = true;
        this.eventEndAt = eventEndAt;
    }

    @JsonSetter(value = "cta", nulls = Nulls.SET)
    public void setCta(Cta cta) {
        this.ctaProvided = true;
        this.cta = cta;
    }

    public String title() {
        return title;
    }

    public String summary() {
        return summary;
    }

    public String body() {
        return body;
    }

    public String authorName() {
        return authorName;
    }

    public Instant eventStartAt() {
        return eventStartAt;
    }

    public Instant eventEndAt() {
        return eventEndAt;
    }

    public Cta cta() {
        return cta;
    }

    @JsonIgnore
    public boolean isEventStartAtProvided() {
        return eventStartAtProvided;
    }

    @JsonIgnore
    public boolean isEventEndAtProvided() {
        return eventEndAtProvided;
    }

    @JsonIgnore
    public boolean isCtaProvided() {
        return ctaProvided;
    }

    public record Cta(
            @NotBlank(message = "cta.label은 필수입니다.")
            @Size(max = 100, message = "cta.label은 100자를 초과할 수 없습니다.")
            String label,

            @NotBlank(message = "cta.url은 필수입니다.")
            @Size(max = 2_048, message = "cta.url은 2,048자를 초과할 수 없습니다.")
            String url
    ) {
    }
}
