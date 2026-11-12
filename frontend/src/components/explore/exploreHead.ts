import type { ExploreTab } from '@/components/explore/ExploreHero';

interface ExploreHeadInput {
  tab: ExploreTab;
  /** 검색어 */
  query: string;
  /** 프로젝트 탭의 기술 주제 */
  topic: string | null;
}

export interface ExploreHead {
  title: string;
  description: string;
  /** 검색 결과처럼 색인하지 않을 화면이면 null */
  canonicalPath: string | null;
}

const TAB_HEADS: Record<ExploreTab, Omit<ExploreHead, 'canonicalPath'> & { path: string }> = {
  keyword: {
    path: '/',
    title: 'shout-outz | 우테코 크루들의 프로젝트 경험과 취업 과정',
    description:
      '우테코 크루들의 프로젝트 경험과 취업 과정을 모았어요. 크루 블로그, PR과 팀 문서, 테코톡을 기술 키워드로 찾아보세요.',
  },
  job: {
    path: '/?tab=job',
    title: '채용 공고 | shout-outz',
    description: '채용 공고가 찾는 역량과, 그 역량을 다룬 우테코 크루들의 자료를 함께 보여 줘요.',
  },
  project: {
    path: '/?tab=project',
    title: '프로젝트 | shout-outz',
    description: '우테코 크루들이 만든 프로젝트와 그 안에서 쌓은 경험을 기술 주제별로 모았어요.',
  },
};

/**
 * 홈(탐색 화면)의 제목·설명·canonical.
 * 탭 첫 화면만 색인하고, 검색어나 기술 주제로 거른 화면은 주소가 끝없이 늘어나 색인하지 않는다.
 */
export function exploreHead({ tab, query, topic }: ExploreHeadInput): ExploreHead {
  const { path, ...head } = TAB_HEADS[tab];
  if (query) return { ...head, title: `${query} 검색 결과 | shout-outz`, canonicalPath: null };
  if (tab === 'project' && topic) {
    return { ...head, title: `${topic} 프로젝트 | shout-outz`, canonicalPath: null };
  }
  return { ...head, canonicalPath: path };
}
