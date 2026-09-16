import {
  type ProjectCreateRequest,
  type ProjectFormErrors,
  type ProjectFormValues,
} from '@/types/project';

/**
 * 폼 값을 검사해 필드별 오류 문구를 돌려준다. 오류가 없으면 빈 객체다.
 *
 * 필수 여부는 디자인의 `*` 표시를 따른다.
 * - 필수: 프로젝트 이름, 한 줄 소개, 기수, GitHub 레포지토리 URL, 기술 스택, 참여 팀원
 * - 선택: 팀 이름, 상세 설명, 서비스 배포 URL, 썸네일
 */
export function validateProjectForm(values: ProjectFormValues): ProjectFormErrors {
  throw new Error('validateProjectForm은 아직 구현되지 않았습니다.');
}

/** 검증을 통과한 폼 값을 등록 요청 본문으로 바꾼다. 비어 있는 선택 입력은 null로 보낸다. */
export function toProjectCreateRequest(values: ProjectFormValues): ProjectCreateRequest {
  throw new Error('toProjectCreateRequest는 아직 구현되지 않았습니다.');
}

// 메모.. 타입 가드 함수는 type 폴더에 둬서 같은 맥락으 type 폴더로 둘까 고민했는데 필드가 추가될 때 유효성 검사 함수와 같이 변경되고 규칙에 따른 판단이 함수 안에 들어가서 일단 유틸로
