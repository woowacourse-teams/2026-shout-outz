import { type ProjectFormValues } from '@/types/project';
import { toProjectCreateRequest, validateProjectForm } from '@/utils/project';

const FILLED: ProjectFormValues = {
  title: '루프 (Loop)',
  teamName: '루프팀',
  tagline: '스프린트 회고와 액션 아이템을 하나로 엮은 실시간 협업 도구',
  cohort: 6,
  thumbnailImageId: 12,
  githubRepositoryUrl: 'https://github.com/woowacourse-teams/2026-loop',
  deploymentUrl: 'https://loop.team',
  descriptionMd: '## 문제\n회고 도구와 액션 아이템 관리가 흩어져 있습니다.',
  techTags: [
    { id: 1, displayName: 'React' },
    { id: 2, displayName: 'TypeScript' },
  ],
  members: [
    { handle: 'dhyepark', displayName: '두리', userType: 'WOOWACOURSE_CREW' },
    { handle: 'zzaekkii', displayName: '재키', userType: 'WOOWACOURSE_CREW' },
  ],
};

describe('validateProjectForm', () => {
  it('모두 채우면 오류가 없다', () => {
    expect(validateProjectForm(FILLED)).toEqual({});
  });

  describe('필수 입력', () => {
    it('비어 있으면 필드마다 오류를 알린다', () => {
      const errors = validateProjectForm({
        ...FILLED,
        title: '',
        tagline: '',
        cohort: null,
        githubRepositoryUrl: '',
        techTags: [],
        members: [],
      });

      expect(Object.keys(errors).sort()).toEqual(
        ['cohort', 'githubRepositoryUrl', 'members', 'tagline', 'techTags', 'title'].sort(),
      );
    });

    it('공백만 입력한 것은 입력하지 않은 것으로 본다', () => {
      expect(validateProjectForm({ ...FILLED, title: '   ' })).toHaveProperty('title');
    });
  });

  describe('선택 입력', () => {
    it('팀 이름, 상세 설명, 배포 URL, 썸네일은 비어 있어도 된다', () => {
      expect(
        validateProjectForm({
          ...FILLED,
          teamName: '',
          descriptionMd: '',
          deploymentUrl: '',
          thumbnailImageId: null,
        }),
      ).toEqual({});
    });
  });

  describe('URL 형식', () => {
    it('GitHub 레포지토리 URL은 github.com 주소여야 한다', () => {
      expect(validateProjectForm({ ...FILLED, githubRepositoryUrl: '2026-loop' })).toHaveProperty(
        'githubRepositoryUrl',
      );
      expect(
        validateProjectForm({ ...FILLED, githubRepositoryUrl: 'https://gitlab.com/team/loop' }),
      ).toHaveProperty('githubRepositoryUrl');
    });

    it('배포 URL은 입력했을 때만 형식을 본다', () => {
      expect(validateProjectForm({ ...FILLED, deploymentUrl: '' })).toEqual({});
      expect(validateProjectForm({ ...FILLED, deploymentUrl: 'loop.team' })).toHaveProperty(
        'deploymentUrl',
      );
    });
  });
});

describe('toProjectCreateRequest', () => {
  it('폼 값을 등록 요청 본문으로 바꾼다', () => {
    expect(toProjectCreateRequest(FILLED)).toEqual({
      title: '루프 (Loop)',
      teamName: '루프팀',
      tagline: '스프린트 회고와 액션 아이템을 하나로 엮은 실시간 협업 도구',
      cohort: 6,
      thumbnailImageId: 12,
      githubRepositoryUrl: 'https://github.com/woowacourse-teams/2026-loop',
      deploymentUrl: 'https://loop.team',
      descriptionMd: '## 문제\n회고 도구와 액션 아이템 관리가 흩어져 있습니다.',
      techTagIds: [1, 2],
      memberHandles: ['dhyepark', 'zzaekkii'],
    });
  });

  it('비어 있는 배포 URL은 null로 보낸다', () => {
    expect(toProjectCreateRequest({ ...FILLED, deploymentUrl: '' }).deploymentUrl).toBeNull();
  });

  it('앞뒤 공백은 잘라서 보낸다', () => {
    expect(toProjectCreateRequest({ ...FILLED, title: '  루프 (Loop)  ' }).title).toBe(
      '루프 (Loop)',
    );
  });

  it('검증을 통과하지 않은 값이 오면 던진다', () => {
    expect(() => toProjectCreateRequest({ ...FILLED, cohort: null })).toThrow();
  });
});
