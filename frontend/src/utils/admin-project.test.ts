import type { AdminProjectDetail } from '@/types/admin';
import {
  toAdminProjectFormValues,
  toAdminProjectUpdateBody,
  validateAdminProjectForm,
} from '@/utils/admin-project';

const project: AdminProjectDetail = {
  id: 200,
  slug: 'shout-outz',
  title: 'shout-outz',
  teamName: '샤웃아웃',
  tagline: '우테코 프로젝트 아카이브',
  cohort: 8,
  thumbnailImageId: 10,
  imageUrl: 'https://cdn.example.com/thumb.webp',
  githubRepositoryUrl: 'https://github.com/woowacourse-teams/2026-shout-outz',
  deploymentUrl: null,
  descriptionMd: null,
  serviceStatus: 'OPERATING',
  approvalStatus: 'APPROVED',
  techTags: [
    { id: 1, displayName: 'React' },
    { id: 2, displayName: 'Spring' },
  ],
  members: [],
};

describe('toAdminProjectUpdateBody', () => {
  const initial = toAdminProjectFormValues(project);

  it('아무것도 바꾸지 않으면 빈 요청이다', () => {
    expect(toAdminProjectUpdateBody(initial, initial)).toEqual({});
  });

  it('바꾼 필드만 담고, 앞뒤 공백은 잘라서 보낸다', () => {
    expect(
      toAdminProjectUpdateBody(initial, { ...initial, title: '  새 이름 ', slug: 'new-slug' }),
    ).toEqual({ title: '새 이름', slug: 'new-slug' });
  });

  it('공백만 더한 입력은 바뀐 것으로 보지 않는다', () => {
    expect(toAdminProjectUpdateBody(initial, { ...initial, title: 'shout-outz  ' })).toEqual({});
  });

  it('비운 배포 URL과 썸네일은 null로 보낸다', () => {
    expect(
      toAdminProjectUpdateBody(
        { ...initial, deploymentUrl: 'https://shout-ou.tz' },
        { ...initial, deploymentUrl: '  ', thumbnailImageId: null },
      ),
    ).toEqual({ deploymentUrl: null, thumbnailImageId: null });
  });

  it('기술 스택은 순서만 바뀌면 보내지 않고, 구성이 바뀌면 ID 전체를 보낸다', () => {
    const reversed = [...initial.techTags].reverse();
    expect(toAdminProjectUpdateBody(initial, { ...initial, techTags: reversed })).toEqual({});
    expect(
      toAdminProjectUpdateBody(initial, {
        ...initial,
        techTags: [...initial.techTags, { id: 3, displayName: 'Kotlin' }],
      }),
    ).toEqual({ techTagIds: [1, 2, 3] });
  });
});

describe('validateAdminProjectForm', () => {
  const initial = toAdminProjectFormValues(project);

  it('처음 값은 통과한다', () => {
    expect(validateAdminProjectForm(initial)).toEqual({});
  });

  it.each(['', 'Shout', 'shout_outz', '-shout', 'shout--outz'])('slug "%s"는 막는다', (slug) => {
    expect(validateAdminProjectForm({ ...initial, slug })).toHaveProperty('slug');
  });
});
