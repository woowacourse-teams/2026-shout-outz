import { Field } from '@/components/Field';
import { Input } from '@/components/Input';
import type { ProfileIntroErrors, ProfileIntroInput } from '@/utils/user';

export interface ProfileIntroFieldsProps {
  value: ProfileIntroInput;
  onChange: (next: ProfileIntroInput) => void;
  errors?: ProfileIntroErrors;
  disabled?: boolean;
  /** 회원가입처럼 비워도 되는 자리임을 라벨에 드러낼 때 쓴다. */
  optional?: boolean;
}

/**
 * 한 줄 소개와 GitHub·블로그 주소 입력.
 *
 * 프로필 수정 모달과 회원가입 화면이 함께 쓴다. 검사는 `validateProfileIntro`로 한다.
 * 주소 칸은 모바일 키보드를 위해 `type="url"`을 쓰므로, 감싸는 폼에 `noValidate`를 둔다.
 */
export function ProfileIntroFields({
  value,
  onChange,
  errors,
  disabled,
  optional,
}: ProfileIntroFieldsProps) {
  const suffix = optional ? ' (선택)' : '';
  const set = (key: keyof ProfileIntroInput) => (next: string) =>
    onChange({ ...value, [key]: next });

  return (
    <>
      <Field label={`한 줄 소개${suffix}`} error={errors?.bio}>
        {(id) => (
          <Input
            id={id}
            value={value.bio}
            placeholder="나를 한 줄로 소개해 주세요"
            aria-invalid={errors?.bio ? true : undefined}
            disabled={disabled}
            onChange={(event) => set('bio')(event.target.value)}
          />
        )}
      </Field>
      <Field label={`GitHub 주소${suffix}`} error={errors?.githubProfileUrl}>
        {(id) => (
          <Input
            id={id}
            type="url"
            value={value.githubProfileUrl}
            placeholder="https://github.com/아이디"
            aria-invalid={errors?.githubProfileUrl ? true : undefined}
            disabled={disabled}
            onChange={(event) => set('githubProfileUrl')(event.target.value)}
          />
        )}
      </Field>
      <Field label={`블로그 주소${suffix}`} error={errors?.blogUrl}>
        {(id) => (
          <Input
            id={id}
            type="url"
            value={value.blogUrl}
            placeholder="https://"
            aria-invalid={errors?.blogUrl ? true : undefined}
            disabled={disabled}
            onChange={(event) => set('blogUrl')(event.target.value)}
          />
        )}
      </Field>
    </>
  );
}
