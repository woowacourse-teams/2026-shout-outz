package com.shoutoutz.api.techtag.presentation;

import com.shoutoutz.api.common.response.SuccessResponse;
import com.shoutoutz.api.techtag.domain.TechTag;
import com.shoutoutz.api.techtag.domain.TechTagRepository;
import com.shoutoutz.api.techtag.presentation.dto.response.TechTagFindAllResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기술 스택 조회(Query) API
 */
@RestController
@RequestMapping("/api/v1/tech-tags")
@RequiredArgsConstructor
public class TechTagHttpApi {

    private final TechTagRepository techTagRepository;

    /**
     * 선택 가능한 기술 스택을 조회한다.
     * keyword 가 없거나 비어 있으면 전체를 반환한다.
     */
    @GetMapping
    public ResponseEntity<SuccessResponse<TechTagFindAllResponse>> findAll(
            @RequestParam(required = false) String keyword
    ) {
        List<TechTag> techTags = findTechTags(keyword);
        return ResponseEntity.ok(SuccessResponse.success(TechTagFindAllResponse.from(techTags)));
    }

    private List<TechTag> findTechTags(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return techTagRepository.findAllActive();
        }
        return techTagRepository.findAllActiveByKeyword(keyword.strip());
    }
}
