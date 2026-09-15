import type { ComponentProps } from 'react';

import { type FeedAuthor } from '@/types/feed';

/**
 * 피드 한 건. 홈과 피드 페이지에서 공통으로 쓴다.
 *
 * 좋아요·댓글 수와 OG 링크 카드는 응답에 데이터가 없어 아직 받지 않는다.
 */
export interface FeedCardProps extends Omit<ComponentProps<'article'>, 'children'> {
  author: FeedAuthor;
  content: string;
  createdAt: string;
}

export function FeedCard(props: FeedCardProps) {
  return null;
}
