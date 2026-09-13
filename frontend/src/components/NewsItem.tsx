import type { ComponentProps } from 'react';

/** `NewsListItem.type`. 공지사항과 이벤트가 같은 목록에 섞여 내려온다. */
export type NewsType = 'NOTICE' | 'EVENT';

/**
 * 소식 하나의 내용.
 *
 * 목록에서의 배치(리스트 시맨틱, 항목 사이 구분선과 여백)는 목록이 정한다.
 * `.pen`에서 마지막 항목만 구분선이 없는 것도 형제를 아는 쪽의 규칙이다.
 *
 * ```tsx
 * <ul className="flex flex-col gap-5">
 *   {news.map((item) => (
 *     <li key={item.id} className="border-b border-gray-100 pb-4 last:border-b-0 last:pb-0 md:pb-6">
 *       <NewsItem {...item} />
 *     </li>
 *   ))}
 * </ul>
 * ```
 */
export interface NewsItemProps extends Omit<ComponentProps<'article'>, 'children'> {
  type: NewsType;
  title: string;
  summary: string;
  publishedAt: string;
}
