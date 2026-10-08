import { useSuspenseQuery } from '@tanstack/react-query';

import { cohortsQueryOptions } from '@/api/project';
import type { VerificationTrack } from '@/apis/verification';
import { Field } from '@/components/Field';
import { Select } from '@/components/Select';

export interface CrewInfoFieldsProps {
  cohort: string | null;
  track: VerificationTrack | null;
  onCohortChange: (cohort: string) => void;
  onTrackChange: (track: VerificationTrack) => void;
  /** 수정 불가 정책 문구의 id. 폼의 `aria-describedby`로 가리킨다. */
  policyId: string;
  disabled?: boolean;
}

/**
 * 크루 인증에 필요한 기수와 트랙 입력.
 *
 * 구성원 인증 화면과 회원가입 화면이 함께 쓴다. 기수 목록을 불러오는 동안 suspend한다.
 */
export function CrewInfoFields({
  cohort,
  track,
  onCohortChange,
  onTrackChange,
  policyId,
  disabled,
}: CrewInfoFieldsProps) {
  const { data: cohorts } = useSuspenseQuery(cohortsQueryOptions());

  return (
    <>
      <Field label="기수">
        {(id) => (
          <>
            <Select
              id={id}
              value={cohort}
              placeholder="기수를 선택하세요"
              onValueChange={onCohortChange}
              aria-describedby={`${id}-help`}
              disabled={disabled}
            >
              {cohorts.map((item) => (
                <Select.Item key={item.cohort} value={String(item.cohort)}>
                  {item.cohort}기 ({item.year})
                </Select.Item>
              ))}
            </Select>
            <ul id={`${id}-help`} className="list-disc pl-4 text-xs leading-5 text-gray-500">
              <li>본인의 우아한테크코스 기수를 정확히 선택해 주세요.</li>
            </ul>
          </>
        )}
      </Field>
      <Field label="트랙">
        {(id) => (
          <>
            <Select
              id={id}
              value={track}
              placeholder="트랙을 선택하세요"
              onValueChange={(value) => onTrackChange(value as VerificationTrack)}
              aria-describedby={`${id}-help`}
              disabled={disabled}
            >
              <Select.Item value="BACKEND">백엔드</Select.Item>
              <Select.Item value="FRONTEND">프론트엔드</Select.Item>
              <Select.Item value="ANDROID">안드로이드</Select.Item>
            </Select>
            <ul
              id={`${id}-help`}
              className="flex list-disc flex-col gap-1 pl-4 text-xs leading-5 text-gray-500"
            >
              <li>본인의 파트(백엔드·프론트엔드·안드로이드)를 정확히 선택해 주세요.</li>
              <li id={policyId} className="text-gray-600">
                크루 인증 이후에는{' '}
                <strong className="font-semibold">
                  닉네임과 파트·기수 정보를 수정할 수 없습니다.
                </strong>
              </li>
            </ul>
          </>
        )}
      </Field>
    </>
  );
}
