package com.shoutoutz.api.project.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.shoutoutz.api.project.domain.DeploymentUrl;
import com.shoutoutz.api.project.domain.GithubRepositoryUrl;
import com.shoutoutz.api.project.domain.ServiceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.hibernate.validator.constraints.CodePointLength;

/**
 * 프로젝트 수정 요청 객체
 * 대부분의 필드는 전체 교체 방식이며, 비우는 값은 null 로 보낸다.
 * slug는 등록 시점 값으로 고정이라 받지 않는다.
 * techTagIds와 memberHandles도 전체 교체이며, 목록 순서가 그대로 노출 순서가 된다.
 * memberHandles 에는 등록 요청과 같이 작성자 본인을 넣지 않는다.
 *
 * <p>thumbnailImageId는 필드의 존재 여부에 따라 다음처럼 처리한다.</p>
 * <ul>
 *     <li>필드 생략: 기존 썸네일 유지</li>
 *     <li>미디어 ID 전달: 썸네일 교체</li>
 *     <li>null 전달: 썸네일 제거</li>
 * </ul>
 */
public final class ProjectUpdateRequest {

    @NotBlank(message = "title은 필수입니다.")
    @CodePointLength(max = 100, message = "title은 100자를 초과할 수 없습니다.")
    private String title;

    @NotBlank(message = "teamName은 필수입니다.")
    @CodePointLength(max = 50, message = "teamName은 50자를 초과할 수 없습니다.")
    private String teamName;

    @NotBlank(message = "tagline은 필수입니다.")
    @CodePointLength(max = 200, message = "tagline은 200자를 초과할 수 없습니다.")
    private String tagline;

    @NotNull(message = "cohort는 필수입니다.")
    private Integer cohort;

    private Long thumbnailImageId;
    private boolean thumbnailImageIdProvided;

    @NotBlank(message = "githubRepositoryUrl은 필수입니다.")
    @CodePointLength(max = 2_048, message = "githubRepositoryUrl은 2,048자를 초과할 수 없습니다.")
    @Pattern(regexp = GithubRepositoryUrl.REGEX,
            message = "githubRepositoryUrl은 https://github.com/{owner}/{repo} 형식이어야 합니다.")
    private String githubRepositoryUrl;

    @CodePointLength(max = 2_048, message = "deploymentUrl은 2,048자를 초과할 수 없습니다.")
    @Pattern(regexp = DeploymentUrl.REGEX,
            message = "deploymentUrl은 http 또는 https URL 형식이어야 합니다.")
    private String deploymentUrl;

    @CodePointLength(max = 100_000, message = "descriptionMd는 100,000자를 초과할 수 없습니다.")
    private String descriptionMd;

    @NotNull(message = "serviceStatus는 필수입니다.")
    private ServiceStatus serviceStatus;

    @NotEmpty(message = "techTagIds는 1개 이상이어야 합니다.")
    private List<@NotNull(message = "techTagIds에 null을 넣을 수 없습니다.") Long> techTagIds;

    @NotEmpty(message = "memberHandles는 1개 이상이어야 합니다.")
    private List<@NotBlank(message = "memberHandles에 빈 값을 넣을 수 없습니다.") String> memberHandles;

    /**
     * Jackson이 JSON 필드 생략과 명시적 null을 구분할 수 있도록 사용하는 기본 생성자.
     */
    @JsonCreator
    public ProjectUpdateRequest() {
    }

    /**
     * 테스트와 내부 호출에서 사용하는 전체 필드 생성자.
     * 생성자에 thumbnailImageId를 전달한 것은 명시적 입력으로 간주한다.
     */
    public ProjectUpdateRequest(
            String title,
            String teamName,
            String tagline,
            Integer cohort,
            Long thumbnailImageId,
            String githubRepositoryUrl,
            String deploymentUrl,
            String descriptionMd,
            ServiceStatus serviceStatus,
            List<Long> techTagIds,
            List<String> memberHandles
    ) {
        this.title = title;
        this.teamName = teamName;
        this.tagline = tagline;
        this.cohort = cohort;
        this.thumbnailImageId = thumbnailImageId;
        this.thumbnailImageIdProvided = true;
        this.githubRepositoryUrl = githubRepositoryUrl;
        this.deploymentUrl = normalizeDeploymentUrl(deploymentUrl);
        this.descriptionMd = descriptionMd;
        this.serviceStatus = serviceStatus;
        this.techTagIds = techTagIds;
        this.memberHandles = memberHandles;
    }

    public String title() {
        return title;
    }

    public String teamName() {
        return teamName;
    }

    public String tagline() {
        return tagline;
    }

    public Integer cohort() {
        return cohort;
    }

    public Long thumbnailImageId() {
        return thumbnailImageId;
    }

    @JsonIgnore
    public boolean isThumbnailImageIdProvided() {
        return thumbnailImageIdProvided;
    }

    public String githubRepositoryUrl() {
        return githubRepositoryUrl;
    }

    public String deploymentUrl() {
        return deploymentUrl;
    }

    public String descriptionMd() {
        return descriptionMd;
    }

    public ServiceStatus serviceStatus() {
        return serviceStatus;
    }

    public List<Long> techTagIds() {
        return techTagIds;
    }

    public List<String> memberHandles() {
        return memberHandles;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public void setCohort(Integer cohort) {
        this.cohort = cohort;
    }

    @JsonSetter(value = "thumbnailImageId", nulls = Nulls.SET)
    public void setThumbnailImageId(Long thumbnailImageId) {
        this.thumbnailImageId = thumbnailImageId;
        this.thumbnailImageIdProvided = true;
    }

    public void setGithubRepositoryUrl(String githubRepositoryUrl) {
        this.githubRepositoryUrl = githubRepositoryUrl;
    }

    public void setDeploymentUrl(String deploymentUrl) {
        this.deploymentUrl = normalizeDeploymentUrl(deploymentUrl);
    }

    public void setDescriptionMd(String descriptionMd) {
        this.descriptionMd = descriptionMd;
    }

    public void setServiceStatus(ServiceStatus serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    public void setTechTagIds(List<Long> techTagIds) {
        this.techTagIds = techTagIds;
    }

    public void setMemberHandles(List<String> memberHandles) {
        this.memberHandles = memberHandles;
    }

    private static String normalizeDeploymentUrl(String deploymentUrl) {
        if (deploymentUrl != null && deploymentUrl.isBlank()) {
            return null;
        }
        return deploymentUrl;
    }
}
