import { type TechTag } from '@/types/project';

/**
 * 기술 스택 선택 필드.
 *
 * TODO 기술 스택 선택 시트/모달이 구현되면 그것으로 교체한다. 지금은 개발용으로 태그 id를 직접
 * 입력받아 `GET /api/v1/tech-tags`에서 찾은 태그를 칩으로 쌓는다.
 */
export interface TechTagFieldProps {
  value: TechTag[];
  onChange: (next: TechTag[]) => void;
  error?: string;
}

export function TechTagField(props: TechTagFieldProps) {
  return null;
}
