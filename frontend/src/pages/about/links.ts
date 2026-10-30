// 랜딩에서 프로토타입으로 이동하는 주소는 한 곳에서 관리합니다.
// 아직 실제 서비스에 없는 화면(자료 상세, 기술 이야기 검색, 공고, 전체 경험)만 프로토타입으로 보냅니다.
const PROTOTYPE_ORIGIN = 'https://project-after-psi.vercel.app/';

export const PROTOTYPE_URL = `${PROTOTYPE_ORIGIN}#/projects`;

const prototype = (hash: string) => `${PROTOTYPE_ORIGIN}#${hash}`;

export const prototypeLinks = {
  search: (query: string) => prototype(`/projects/search?q=${encodeURIComponent(query)}`),
  jobs: () => prototype('/projects/jobs'),
  job: (id: string) => prototype(`/jobs/${encodeURIComponent(id)}`),
  project: (slug: string) => prototype(`/projects/@${slug}`),
  doc: (url: string, slug?: string | null) =>
    prototype(
      `${slug ? `/projects/@${encodeURIComponent(slug)}/doc` : '/doc'}?u=${encodeURIComponent(url)}`,
    ),
};
