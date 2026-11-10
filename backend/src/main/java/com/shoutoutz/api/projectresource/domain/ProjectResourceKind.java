package com.shoutoutz.api.projectresource.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 기술 이야기의 팀 자료 종류. 테코톡은 팀이 없어 팀 자료가 아니다.
 * 팀 문서는 레포 문서, Wiki, Discussion 을 합한다(프로토타입과 같음).
 */
public enum ProjectResourceKind {

    BLOG(List.of("blog-post")),
    PR(List.of("github-pr")),
    TEAM_DOC(List.of("github-teamdoc", "github-wiki", "github-discussion"));

    public static final String TALK_SOURCE = "youtube-tekotalk";

    private final List<String> sources;

    ProjectResourceKind(List<String> sources) {
        this.sources = sources;
    }

    public static Optional<ProjectResourceKind> fromSource(String source) {
        return Arrays.stream(values())
                .filter(kind -> kind.sources.contains(source))
                .findFirst();
    }

    /**
     * 이 팀 자료와 다른 팀 자료가 되는 원천
     */
    public static List<String> teamSources() {
        return Arrays.stream(values())
                .flatMap(kind -> kind.sources.stream())
                .toList();
    }
}
