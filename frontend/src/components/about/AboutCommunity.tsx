import { useState, type KeyboardEvent } from 'react';
import {
  IconArrowBackUp,
  IconArrowUpRight,
  IconArrowsHorizontal,
  IconCheck,
  IconHeart,
} from '@tabler/icons-react';
import { Tab } from '@/components/Tab';
import { ABOUT_STATS } from '@/constants/about';
import {
  ABOUT_CONTAINER,
  ABOUT_COPY,
  ABOUT_EYEBROW,
  ABOUT_HEADING,
  ABOUT_TEXT_LINK,
  type AboutSectionProps,
} from '@/components/about/shared';

type PreviewTab = 'question' | 'feed';
const PREVIEWS = {
  question: {
    category: '취업 준비',
    kind: '익명',
    title: '프로젝트 경험, 면접에서 어떻게 이야기하나요?',
    content:
      '구현한 기능보다 문제를 해결한 과정을 잘 전달하고 싶어요.\n여러분은 어떤 경험을 중심으로 준비하고 있나요?',
    author: '함께 준비하는 개발자',
    reaction: '궁금해요',
    replyTitle: '나의 시행착오가 누군가에겐 힌트가 돼요.',
    reply: '어떤 고민을 했고, 왜 그렇게 선택했는지부터 나눠 봐요.',
    nextCategory: '개발 고민',
    nextTitle: '코드 리뷰에서 의견이 다를 땐 어떻게 하나요?',
    nextContent: '정답이 없는 고민일수록, 다양한 경험이 도움이 되니까.',
  },
  feed: {
    category: '프로젝트 회고',
    kind: '경험 공유',
    title: '첫 팀 프로젝트를 마치고, 다시 생각해 본 협업',
    content:
      '기능을 완성하는 것만큼 서로의 생각을 맞추는 일이 중요했어요.\n다음 프로젝트에서 다시 해보고 싶은 것들을 기록했어요.',
    author: '오늘도 배우는 개발자',
    reaction: '좋아요',
    replyTitle: '완벽한 글이 아니어도 괜찮아요.',
    reply: '오늘 배운 것과 직접 겪은 작은 경험부터 나눠요.',
    nextCategory: '트러블슈팅',
    nextTitle: '느린 첫 화면, 어디부터 살펴봤을까?',
    nextContent: '문제를 발견하고 해결하기까지의 과정을 공유해요.',
  },
};

export function AboutCommunity({ LinkComponent: Link }: AboutSectionProps) {
  return (
    <section
      id="community"
      className={`${ABOUT_CONTAINER} grid scroll-mt-28 items-center gap-9 py-16 md:grid-cols-2 md:gap-12 md:pt-32 md:pb-24 lg:gap-20`}
    >
      <div>
        <p className={ABOUT_EYEBROW}>02 / ASK &amp; ANSWER</p>
        <h2 className={ABOUT_HEADING}>
          질문이 모이고,
          <br />
          경험이 답이 되는 곳.
        </h2>
        <div
          className="my-6 flex items-center gap-8 border-y border-gray-200 py-5 md:my-8 md:py-6"
          aria-label={`질문 ${ABOUT_STATS.questions}개, 답변 ${ABOUT_STATS.answers}개`}
        >
          <ConversationCount label="함께 나눈 질문" count={ABOUT_STATS.questions} />
          <IconArrowsHorizontal className="size-6 shrink-0 text-gray-500" aria-hidden="true" />
          <ConversationCount label="서로 건넨 답변" count={ABOUT_STATS.answers} />
        </div>
        <p className={ABOUT_COPY}>
          혼자 붙잡고 있던 고민을 질문으로 꺼내고,
          <br />
          먼저 겪어 본 경험을 답변으로 건네요.
          <br />
          당신의 궁금증도 이 대화에 더해 보세요.
        </p>
        <ul className="my-6 space-y-3 text-sm text-gray-700 md:my-7 md:text-sm">
          {[
            '말 꺼내기 어려운 고민은 익명으로',
            '궁금했던 질문에는 공감과 답변으로',
            '나누고 싶은 이야기는 자유롭게',
          ].map((text) => (
            <li key={text} className="flex items-center gap-3">
              <IconCheck className="size-4 shrink-0" aria-hidden="true" />
              {text}
            </li>
          ))}
        </ul>
        <Link href="/community" className={ABOUT_TEXT_LINK}>
          나도 질문과 답변 나누기 <IconArrowUpRight className="size-4" aria-hidden="true" />
        </Link>
        <p className="mt-4 text-xs text-gray-500">질문·답변 수는 {ABOUT_STATS.asOf} 기준</p>
      </div>
      <CommunityPreview />
    </section>
  );
}

function ConversationCount({ label, count }: { label: string; count: number }) {
  return (
    <div className="flex flex-col gap-2.5">
      <span className="text-xs text-gray-600">{label}</span>
      <strong className="text-primary-500 text-6xl leading-none tracking-tighter tabular-nums">
        {count}
        <small className="ml-1.5 text-base font-medium tracking-normal">개</small>
      </strong>
    </div>
  );
}

function CommunityPreview() {
  const [activeTab, setActiveTab] = useState<PreviewTab>('question');
  const preview = PREVIEWS[activeTab];
  function handleTabKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    let next: PreviewTab;
    if (event.key === 'Home') next = 'question';
    else if (event.key === 'End') next = 'feed';
    else if (event.key === 'ArrowRight' || event.key === 'ArrowLeft')
      next = activeTab === 'question' ? 'feed' : 'question';
    else return;
    event.preventDefault();
    setActiveTab(next);
    event.currentTarget.parentElement
      ?.querySelector<HTMLButtonElement>(`#about-tab-${next}`)
      ?.focus();
  }
  return (
    <div className="rounded-3xl border border-gray-100 bg-gray-50 p-5 md:p-6">
      <div className="mb-5 flex items-center justify-between gap-2">
        <span className="text-xs font-semibold">우리의 이야기가 모이는 곳</span>
        <span className="text-xs text-gray-500">이해를 돕기 위한 예시</span>
      </div>
      <div className="mb-4 flex items-center justify-between gap-2 border-b border-gray-100">
        <Tab
          variant="subnav"
          size="sm"
          value={activeTab}
          onChange={(value) => setActiveTab(value as PreviewTab)}
          aria-label="커뮤니티 미리보기"
        >
          {(['question', 'feed'] as const).map((value) => (
            <Tab.Item
              key={value}
              value={value}
              id={`about-tab-${value}`}
              aria-controls="about-community-preview"
              tabIndex={activeTab === value ? 0 : -1}
              onKeyDown={handleTabKeyDown}
            >
              {value === 'question' ? '질문' : '피드'}
            </Tab.Item>
          ))}
        </Tab>
        <span className="pb-2 text-xs text-gray-500 max-sm:hidden">
          {activeTab === 'question' ? '작은 궁금증도 괜찮아요.' : '당신의 경험을 들려주세요.'}
        </span>
      </div>
      <div
        key={activeTab}
        id="about-community-preview"
        role="tabpanel"
        aria-labelledby={`about-tab-${activeTab}`}
      >
        <article className="bg-background rounded-xl border border-gray-100 p-5">
          <div className="mb-3 flex justify-between text-xs text-gray-500">
            <span>{preview.category}</span>
            <span>{preview.kind}</span>
          </div>
          <h3 className="mb-2 text-sm leading-6 font-semibold tracking-tight">
            {activeTab === 'question' && <b className="text-primary-500 mr-1">Q.</b>}
            {preview.title}
          </h3>
          <p className="text-sm leading-6 whitespace-pre-line text-gray-700">{preview.content}</p>
          <div className="mt-5 flex flex-wrap items-center gap-2 border-t border-gray-100 pt-4 text-xs text-gray-500">
            <span
              className="grid size-6 place-items-center rounded-full bg-gray-100"
              aria-hidden="true"
            >
              {activeTab === 'question' ? '?' : <IconArrowUpRight className="size-3" />}
            </span>
            <span>{preview.author}</span>
            <span className="ml-auto flex items-center gap-1">
              <IconHeart className="size-3" aria-hidden="true" />
              {preview.reaction}
            </span>
          </div>
        </article>
        <div className="mx-2 my-5 flex gap-3 text-gray-700">
          <IconArrowBackUp className="size-4 shrink-0 text-gray-500" aria-hidden="true" />
          <div>
            <strong className="text-sm font-semibold">{preview.replyTitle}</strong>
            <p className="mt-1.5 text-xs leading-5 text-gray-500">{preview.reply}</p>
          </div>
        </div>
        <article className="bg-background rounded-xl border border-gray-100 p-5">
          <p className="mb-2 text-xs text-gray-500">{preview.nextCategory}</p>
          <h3 className="mb-2 text-sm leading-6 font-semibold">
            {activeTab === 'question' && <b className="text-primary-500 mr-1">Q.</b>}
            {preview.nextTitle}
          </h3>
          <p className="text-sm leading-6 text-gray-700">{preview.nextContent}</p>
        </article>
      </div>
    </div>
  );
}
