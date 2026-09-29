import { type ProjectFormValues } from '@/types/project';
import { toProjectCreateRequest, toProjectFormErrors, validateProjectForm } from '@/utils/project';

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
    { userId: 1, handle: 'dhyepark', displayName: '두리', userType: 'WOOWACOURSE_CREW' },
    { userId: 2, handle: 'zzaekkii', displayName: '재키', userType: 'WOOWACOURSE_CREW' },
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
        ['cohort', 'githubRepositoryUrl', 'tagline', 'techTags', 'title'].sort(),
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
    expect(toProjectCreateRequest(FILLED, 'woojin')).toEqual({
      title: '루프 (Loop)',
      teamName: '루프팀',
      tagline: '스프린트 회고와 액션 아이템을 하나로 엮은 실시간 협업 도구',
      cohort: 6,
      thumbnailImageId: 12,
      githubRepositoryUrl: 'https://github.com/woowacourse-teams/2026-loop',
      deploymentUrl: 'https://loop.team',
      descriptionMd: '## 문제\n회고 도구와 액션 아이템 관리가 흩어져 있습니다.',
      techTagIds: [1, 2],
      memberHandles: ['woojin', 'dhyepark', 'zzaekkii'],
    });
  });

  it('비어 있는 배포 URL은 null로 보낸다', () => {
    expect(
      toProjectCreateRequest({ ...FILLED, deploymentUrl: '' }, 'woojin').deploymentUrl,
    ).toBeNull();
  });

  it('앞뒤 공백은 잘라서 보낸다', () => {
    expect(toProjectCreateRequest({ ...FILLED, title: '  루프 (Loop)  ' }, 'woojin').title).toBe(
      '루프 (Loop)',
    );
  });

  it('검증을 통과하지 않은 값이 오면 던진다', () => {
    expect(() => toProjectCreateRequest({ ...FILLED, cohort: null }, 'woojin')).toThrow();
  });

  // 서버가 앞뒤 공백을 자르는 대상에서 descriptionMd만 빠져 있다.
  // 여기서 자르면 코드블록 들여쓰기처럼 의미 있는 공백이 사라진다.
  it('상세 설명은 앞뒤 공백을 자르지 않고 그대로 보낸다', () => {
    const descriptionMd = '    코드블록으로 시작하는 본문\n';

    expect(toProjectCreateRequest({ ...FILLED, descriptionMd }, 'woojin').descriptionMd).toBe(
      descriptionMd,
    );
  });
});

describe('GitHub 레포지토리 URL', () => {
  const errorFor = (githubRepositoryUrl: string) =>
    validateProjectForm({ ...FILLED, githubRepositoryUrl }).githubRepositoryUrl;

  it.each([
    'https://github.com/owner/repo',
    'https://www.github.com/owner/repo',
    'https://github.com/owner/repo.git',
    'https://github.com/owner/repo/',
  ])('통과: %s', (url) => {
    expect(errorFor(url)).toBeUndefined();
  });

  // 호스트만 보던 예전 검사가 전부 통과시켜 서버 400으로 넘기던 주소들이다.
  it.each([
    'http://github.com/owner/repo',
    'https://github.com',
    'https://github.com/owner',
    'https://github.com/owner/repo/tree/main',
    'https://github.com/owner/repo?tab=readme',
    'https://gitlab.com/owner/repo',
  ])('차단: %s', (url) => {
    expect(errorFor(url)).toBeDefined();
  });
});

describe('글자 수 상한', () => {
  it.each([
    ['title', 100],
    ['teamName', 50],
    ['tagline', 200],
    ['descriptionMd', 100_000],
  ] as const)('%s는 %d자까지 받는다', (field, max) => {
    expect(validateProjectForm({ ...FILLED, [field]: 'ㄱ'.repeat(max) })[field]).toBeUndefined();
    expect(validateProjectForm({ ...FILLED, [field]: 'ㄱ'.repeat(max + 1) })[field]).toBeDefined();
  });

  // 서버는 코드포인트로 센다. str.length(UTF-16)로 세면 이 이모지가 200자로 잡혀 막힌다.
  it('이모지는 서버와 같이 코드포인트로 센다', () => {
    const tagline = '😀'.repeat(100);

    expect(tagline.length).toBe(200);
    expect(validateProjectForm({ ...FILLED, tagline }).tagline).toBeUndefined();
  });
});

describe('toProjectFormErrors', () => {
  const httpError = (data: unknown) =>
    Object.assign(new Error('HTTPError'), {
      name: 'HTTPError',
      response: new Response(null, { status: 400 }),
      request: new Request('http://localhost/api/v1/projects'),
      options: {},
      data,
    });

  it('VALIDATION_FAILED의 details를 필드별 문구로 편다', () => {
    const error = httpError({
      status: 'error',
      code: 'VALIDATION_FAILED',
      message: '입력값이 올바르지 않습니다.',
      details: [
        { field: 'teamName', message: '팀 이름은 필수입니다.' },
        { field: 'memberHandles', message: '팀원을 1명 이상 선택해 주세요.' },
      ],
    });

    expect(toProjectFormErrors(error)).toEqual({
      teamName: '팀 이름은 필수입니다.',
      members: '팀원을 1명 이상 선택해 주세요.',
    });
  });

  it('details가 없는 코드는 표에 적힌 입력칸에 붙인다', () => {
    const error = httpError({
      status: 'error',
      code: 'PROJECT_DUPLICATE_SLUG',
      message: '같은 주소의 프로젝트가 이미 있습니다.',
    });

    expect(toProjectFormErrors(error)).toEqual({
      githubRepositoryUrl: '같은 주소의 프로젝트가 이미 있습니다.',
    });
  });

  it('입력칸과 무관한 코드는 빈 객체를 준다', () => {
    const error = httpError({
      status: 'error',
      code: 'PROJECT_REGISTRATION_FORBIDDEN',
      message: '등록 권한이 없습니다.',
    });

    expect(toProjectFormErrors(error)).toEqual({});
  });

  it('우리 서버 에러가 아니면 빈 객체를 준다', () => {
    expect(toProjectFormErrors(new Error('network'))).toEqual({});
  });
});
