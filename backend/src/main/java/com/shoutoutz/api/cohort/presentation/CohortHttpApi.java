package com.shoutoutz.api.cohort.presentation;

import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.cohort.presentation.dto.response.CohortFindAllResponse;
import com.shoutoutz.api.common.response.SuccessResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 우아한테크코스 기수 조회 API
 */
@RestController
@RequestMapping("/api/v1/cohorts")
public class CohortHttpApi {

    @GetMapping
    public ResponseEntity<SuccessResponse<CohortFindAllResponse>> findAll() {
        CohortFindAllResponse response = CohortFindAllResponse.from(Cohort.descending());
        return ResponseEntity.ok(SuccessResponse.success(response));
    }
}
