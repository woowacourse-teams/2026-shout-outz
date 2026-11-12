import type { Feed } from '@/apis/feed';
import { formatSpokenRelativeTime } from '@/utils/date';
import { toPlainText } from '@/utils/markdown';
import { formatUserAffiliation } from '@/utils/user';

const FIRST_URL_PATTERN = /https?:\/\/[^\s<>()]+/;

/** 본문에서 첫 번째 주소를 찾는다. 링크 미리보기에 쓰며, 문장 끝 구두점은 떼어낸다. */
export function findFirstUrl(content: string) {
  return content.match(FIRST_URL_PATTERN)?.[0].replace(/[.,!?;:]+$/, '');
}

/** 카드의 분류 문구. 고른 분류가 없으면 질문·이야기로 보여 준다. */
export function formatFeedCategory(feed: Feed) {
  return (
    feed.categories.map((category) => category.displayName).join(' · ') ||
    (feed.feedType === 'QUESTION' ? '질문' : '이야기')
  );
}

/**
 * 커뮤니티 카드를 스크린리더가 한 번에 읽을 링크 이름.
 * 화면의 제목으로 시작하고, 카드에 보이는 분류·답변 상태·작성자·날짜·반응 수를 이어 붙인다.
 */
export function formatFeedCardLabel(feed: Feed, now: Date = new Date()) {
  const isQuestion = feed.feedType === 'QUESTION';
  const commentCount = feed.commentCount ?? 0;
  const likeCount = feed.likeCount ?? 0;
  const author = [
    feed.author.handle ? feed.author.displayName : '익명',
    formatUserAffiliation({ ...feed.author, anonymous: !feed.author.handle }),
  ]
    .filter(Boolean)
    .join(' ');

  return [
    feed.title,
    formatFeedCategory(feed),
    isQuestion && (commentCount === 0 ? '답변 대기' : `답변 ${commentCount}개`),
    author,
    formatSpokenRelativeTime(feed.createdAt, now),
    `${isQuestion ? '저도 궁금해요' : '좋아요'} ${likeCount}개${feed.likedByMe ? ', 내가 누름' : ''}`,
    !isQuestion && `댓글 ${commentCount}개`,
    feed.media.length > 0 && `이미지 ${feed.media.length}장`,
  ]
    .filter(Boolean)
    .join(', ');
}

/** 스크린리더가 카드의 미리보기로 읽을 본문 길이. 화면처럼 앞부분만 읽고 넘어갈 수 있게 자른다. */
const CARD_PREVIEW_LENGTH = 100;

/** 일반·AI 추천 카드의 링크나 버튼 설명으로 연결할 본문 미리보기. */
export function formatFeedCardPreview(feed: Pick<Feed, 'content'>) {
  const preview = toPlainText(feed.content);
  return preview.length > CARD_PREVIEW_LENGTH
    ? `${preview.slice(0, CARD_PREVIEW_LENGTH)}…`
    : preview;
}
