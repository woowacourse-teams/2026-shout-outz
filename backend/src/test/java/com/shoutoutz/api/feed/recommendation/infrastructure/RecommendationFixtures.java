package com.shoutoutz.api.feed.recommendation.infrastructure;

import com.shoutoutz.api.feed.recommendation.infrastructure.quality.RecommendationCheck;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import tools.jackson.databind.ObjectMapper;

/**
 * 규칙 검증을 통과하는 서로 다른 추천 후보 8개와 judge 응답을 만든다.
 */
final class RecommendationFixtures {

    static final List<Map<String, String>> CANDIDATES = List.of(
            candidate("DEEPEN", "캐시 무효화 시점은 어떻게 정하시나요?",
                    "조회가 많은 API에 캐시를 붙이려고 해요. 데이터가 바뀌었을 때 캐시를 바로 지울지, TTL에 맡길지 고민입니다. 정합성이 중요한 데이터와 덜 중요한 데이터를 나누는 기준이 있다면 알려주세요."),
            candidate("DEEPEN", "트랜잭션 안에서 외부 API를 호출해도 될까요?",
                    "결제 승인처럼 외부 API를 부르는 로직을 트랜잭션 안에 두면 커넥션을 오래 잡게 되더라고요. 호출을 트랜잭션 밖으로 빼면 실패했을 때 보상 처리가 필요해집니다. 실무에서는 어떤 구조를 많이 쓰시나요?"),
            candidate("ADJACENT", "메시지 큐 소비자가 중복 메시지를 받으면 어떻게 하나요?",
                    "재시도 때문에 같은 메시지가 두 번 처리되는 일이 생겼어요. 멱등 키를 저장하는 방식과 상태를 확인하는 방식 중 무엇이 관리하기 쉬웠는지 궁금합니다. 처리 기록은 얼마나 오래 보관하시나요?"),
            candidate("ADJACENT", "로컬 캐시와 분산 캐시는 언제 함께 쓰나요?",
                    "서버가 여러 대로 늘면서 인스턴스마다 다른 값을 들고 있는 문제가 보였어요. 로컬 캐시를 앞단에 두고 Redis를 뒤에 두는 2단 구성을 써 보신 분이 있다면, 무효화를 어떻게 전파하셨는지 듣고 싶어요."),
            candidate("COMPARE", "낙관적 락과 비관적 락은 어떤 기준으로 고르시나요?",
                    "재고 차감처럼 동시에 같은 행을 수정하는 기능을 만들고 있어요. 충돌이 드물면 낙관적 락이 낫다고 들었는데, 충돌 빈도를 어떻게 가늠하고 선택하셨는지 궁금합니다. 재시도 비용도 함께 고려하시나요?"),
            candidate("COMPARE", "커서 페이지네이션과 오프셋 방식의 차이가 체감되나요?",
                    "목록 API를 오프셋으로 만들었는데 뒤쪽 페이지가 느려졌어요. 커서 방식으로 바꾸면 정렬 조건이 여러 개일 때 구현이 복잡해지더라고요. 두 방식을 써 보시고 어떤 상황에서 바꾸셨는지 알려주세요."),
            candidate("PRACTICE", "느린 쿼리를 찾을 때 무엇부터 확인하시나요?",
                    "운영 중인 API 응답이 갑자기 느려졌는데 원인을 찾는 순서가 막막했어요. 실행 계획, 커넥션 풀 지표, 애플리케이션 로그 중 무엇을 먼저 보시는지, 그리고 개선 전후를 어떤 수치로 비교하시는지 궁금해요."),
            candidate("PRACTICE", "팀에서 장애 회고는 어떤 형식으로 남기시나요?",
                    "작은 장애가 반복되는데 회고 문서가 매번 형식이 달라 쌓이지 않아요. 원인, 영향 범위, 재발 방지 항목처럼 꼭 남기는 항목이 있는지, 그리고 회고 후 후속 작업을 어떻게 추적하시는지 알고 싶습니다."));

    private RecommendationFixtures() {
    }

    static String generation(ObjectMapper mapper, List<Map<String, String>> candidates) {
        return mapper.writeValueAsString(Map.of("decision", "GENERATE", "skipReason", "NONE", "questions", candidates));
    }

    static String skip(ObjectMapper mapper, String reason) {
        return mapper.writeValueAsString(Map.of("decision", "SKIP", "skipReason", reason, "questions", List.of()));
    }

    /**
     * failedHardIds에 든 후보는 SAFE를 통과하지 못한 것으로 판정한다.
     */
    static String judgement(int candidateCount, List<String> failedHardIds) {
        String results = IntStream.rangeClosed(1, candidateCount)
                .mapToObj(index -> "c" + index)
                .map(id -> "{\"id\":\"" + id + "\",\"checks\":[" + Arrays.stream(RecommendationCheck.values())
                        .map(check -> "{\"item\":\"" + check.name() + "\",\"why\":\"근거\",\"pass\":"
                                + !(check == RecommendationCheck.SAFE && failedHardIds.contains(id)) + "}")
                        .collect(Collectors.joining(",")) + "]}")
                .collect(Collectors.joining(","));
        return "{\"results\":[" + results + "],\"duplicateGroups\":[]}";
    }

    static Map<String, String> candidate(String angle, String title, String content) {
        Map<String, String> candidate = new LinkedHashMap<>();
        candidate.put("angle", angle);
        candidate.put("title", title);
        candidate.put("content", content);
        return candidate;
    }
}
