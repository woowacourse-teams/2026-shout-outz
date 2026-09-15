package com.shoutoutz.api.project.domain;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 프로젝트 본문 속 이미지 참조
 * 본문 이미지는 Markdown 안에 media://{mediaId} 형식으로 참조한다.
 */
public final class DescriptionMediaReferences {

    /**
     * 19 자리 이상의 숫자는 존재할 수 없는 id이므로, 참조로 보지 않는다.
     */
    private static final Pattern REFERENCE = Pattern.compile("media://(\\d{1,18})(?!\\d)");

    private DescriptionMediaReferences() {
    }

    /**
     * 본문에서 참조한 미디어 id 를 처음 등장한 순서대로 뽑는다. 같은 이미지를 여러 번 참조해도 한 번만 담는다.
     */
    public static List<Long> extractMediaIds(String descriptionMd) {
        if (descriptionMd == null) {
            return List.of();
        }
        return REFERENCE.matcher(descriptionMd).results()
                .map(result -> Long.parseLong(result.group(1)))
                .distinct()
                .toList();
    }
}
