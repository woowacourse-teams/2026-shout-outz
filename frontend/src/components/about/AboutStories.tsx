import { IconArrowUpRight, IconCommand, IconPlus } from '@tabler/icons-react';
import {
  ABOUT_CONTAINER,
  ABOUT_COPY,
  ABOUT_EYEBROW,
  ABOUT_HEADING,
  ABOUT_TEXT_LINK,
  type AboutSectionProps,
} from '@/components/about/shared';

const STORIES = [
  {
    type: 'LEARNING',
    title: '오늘 새롭게 알게 된 것',
    text: '작은 배움도 기록하면 나만의 경험이 돼요.',
    Icon: IconPlus,
  },
  {
    type: 'TROUBLESHOOTING',
    title: '한참을 붙잡았던 문제',
    text: '해결까지의 고민이 누군가에겐 지름길이 돼요.',
    Icon: IconCommand,
  },
  {
    type: 'RETROSPECTIVE',
    title: '함께 만들며 배운 것',
    text: '결과 뒤에 있던 과정도 들려주세요.',
    Icon: IconArrowUpRight,
  },
];

export function AboutStories({ LinkComponent: Link }: AboutSectionProps) {
  return (
    <div className={ABOUT_CONTAINER}>
      <section
        id="stories"
        className="grid scroll-mt-28 items-center gap-8 border-t border-gray-200 py-16 md:grid-cols-2 md:gap-14 md:py-24 lg:gap-24"
      >
        <div
          className="rounded-3xl bg-gray-50 px-6 py-3 md:px-8 md:py-5"
          aria-label="경험을 나누는 세 가지 주제"
        >
          {STORIES.map(({ type, title, text, Icon }, index) => (
            <div
              key={type}
              className="flex items-center gap-4 border-b border-gray-100 py-6 last:border-0 md:gap-5 md:py-7"
            >
              <span className="text-xs text-gray-500">0{index + 1}</span>
              <div>
                <small className="text-xs tracking-widest text-gray-500">{type}</small>
                <h3 className="my-2.5 text-base font-semibold tracking-tight">{title}</h3>
                <p className="text-sm leading-6 text-gray-700">{text}</p>
              </div>
              <span className="bg-background ml-auto grid size-10 shrink-0 place-items-center rounded-xl border border-gray-100 text-gray-500">
                <Icon className="size-5" aria-hidden="true" />
              </span>
            </div>
          ))}
        </div>
        <div className="max-md:row-start-1">
          <p className={ABOUT_EYEBROW}>03 / YOUR TURN</p>
          <h2 className={ABOUT_HEADING}>
            둘러보는 것에서,
            <br />내 경험을 나누는 것으로.
          </h2>
          <p className={ABOUT_COPY}>
            프로젝트를 구경하다 떠오른 생각도,
            <br />
            질문에 답하다 돌아본 나의 시행착오도.
            <br />
            기술 글과 회고로 남겨 다음 사람에게 건네주세요.
          </p>
          <Link href="/community?type=POST" className={`${ABOUT_TEXT_LINK} mt-5`}>
            다른 사람들의 경험 읽기 <IconArrowUpRight className="size-4" aria-hidden="true" />
          </Link>
        </div>
      </section>
    </div>
  );
}
