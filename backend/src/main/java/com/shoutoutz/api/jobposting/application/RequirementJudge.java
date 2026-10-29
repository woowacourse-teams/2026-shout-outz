package com.shoutoutz.api.jobposting.application;

import com.shoutoutz.api.jobposting.application.dto.ClaimedJobRequirement;
import com.shoutoutz.api.jobposting.application.dto.RequirementJudgement;
import com.shoutoutz.api.jobposting.application.dto.ResourceCandidate;
import java.util.List;

/**
 * 후보 자료가 역량을 실제로 뒷받침하는지 판정하고, 추천할 자료의 설명을 함께 만든다.
 * 역량 하나의 후보를 한 번에 판정한다 (후보마다 따로 부르면 호출 수가 후보 수만큼 는다).
 * 실패는 JobRequirementMatchingException 으로 분류해 던진다.
 */
public interface RequirementJudge {

    /**
     * @param candidates 비어 있지 않은 후보 목록
     * @return 후보마다 판정 하나
     */
    List<RequirementJudgement> judge(ClaimedJobRequirement requirement, List<ResourceCandidate> candidates);
}
