import { IconArrowDown, IconArrowUpRight, IconCode, IconSparkles } from '@tabler/icons-react';
import { getButtonStyles } from '@/components/Button';
import { ABOUT_STATS } from '@/constants/about';
import {
  ABOUT_CONTAINER,
  ABOUT_EYEBROW,
  ABOUT_TEXT_LINK,
  type AboutSectionProps,
} from '@/components/about/shared';

export function AboutHeader({ LinkComponent: Link }: AboutSectionProps) {
  return (
    <header className="bg-background/90 sticky top-0 z-20 border-b border-gray-200 backdrop-blur-lg">
      <div className={`${ABOUT_CONTAINER} flex h-18 items-center justify-between gap-3 md:h-22`}>
        <Link
          href="#about-top"
          aria-label="샤라웃 처음으로"
          className="inline-flex items-center gap-2 text-xl font-extrabold tracking-tight md:text-2xl"
        >
          <img
            src="/about-assets/logo.png"
            width={34}
            height={34}
            alt=""
            className="size-7 object-contain md:size-8.5"
          />
          shout-outz<span className="text-primary-500 -ml-2">.</span>
        </Link>
        <nav
          aria-label="주요 메뉴"
          className="hidden items-center gap-8 text-sm font-semibold text-gray-700 md:flex"
        >
          <Link href="#projects" className="hover:text-primary-700">
            프로젝트
          </Link>
          <Link href="#community" className="hover:text-primary-700">
            커뮤니티
          </Link>
          <Link href="#about" className="hover:text-primary-700">
            샤라웃 소개
          </Link>
        </nav>
        <Link href="/" className={getButtonStyles({ size: 'sm', className: 'gap-2' })}>
          샤라웃 시작하기 <IconArrowUpRight className="size-4" aria-hidden="true" />
        </Link>
      </div>
    </header>
  );
}

export function AboutHero({ LinkComponent: Link }: AboutSectionProps) {
  return (
    <>
      <section
        className={`${ABOUT_CONTAINER} grid items-center gap-5 pt-14 pb-8 text-center md:min-h-178 md:grid-cols-2 md:gap-0 md:pt-9 md:pb-11 md:text-left`}
      >
        <div className="relative z-2 min-w-0">
          <p className={`${ABOUT_EYEBROW} flex items-center justify-center gap-2 md:justify-start`}>
            <span className="bg-primary-500 size-1.5 rounded-full" /> IT 취준생을 위한, 취업
            커뮤니티
          </p>
          <h1 className="text-3xl leading-snug font-extrabold tracking-tight sm:text-4xl lg:text-5xl">
            개발과 취업 준비,
            <br />
            <em className="text-primary-500 not-italic">
              <span className="text-5xl tracking-tighter tabular-nums md:text-6xl lg:text-7xl">
                {ABOUT_STATS.members}
              </span>
              명과
            </em>
            <br />
            경험을 나누며 함께해요.
          </h1>
          <p className="mt-7 mb-8 text-sm leading-7 text-gray-600 lg:text-base lg:leading-8">
            다른 사람의 프로젝트를 살펴보고,
            <br />
            개발과 취업 고민을 물어보세요.
            <br />
            먼저 겪어 본 사람들의 경험이 도움이 될 거예요.
          </p>
          <div className="flex flex-col items-center gap-5 md:items-start xl:flex-row xl:items-center">
            <Link href="/" className={getButtonStyles({ size: 'lg', className: 'gap-4' })}>
              지금 샤라웃 둘러보기 <IconArrowUpRight className="size-5" aria-hidden="true" />
            </Link>
            <Link href="#projects" className={`${ABOUT_TEXT_LINK} text-xs`}>
              어떤 경험들이 모였을까요 <IconArrowDown className="size-4" aria-hidden="true" />
            </Link>
          </div>
          <div className="mt-6 flex items-center justify-center gap-3 text-xs text-gray-600 md:justify-start md:text-xs">
            <span className="flex -space-x-1" aria-hidden="true">
              <span className="border-background grid size-7 place-items-center rounded-full border-2 bg-gray-100 font-bold">
                Q
              </span>
              <span className="border-background grid size-7 place-items-center rounded-full border-2 bg-gray-100 text-gray-600">
                <IconCode className="size-3.5" />
              </span>
              <span className="border-background grid size-7 place-items-center rounded-full border-2 bg-gray-100 text-gray-600">
                <IconArrowUpRight className="size-3.5" />
              </span>
            </span>
            <span>
              회원 {ABOUT_STATS.members}명 · {ABOUT_STATS.asOf} 기준
            </span>
          </div>
        </div>
        <div className="relative mx-auto flex h-90 w-full max-w-110 min-w-0 items-end justify-center md:h-110 lg:h-128 lg:max-w-none">
          <div
            aria-hidden="true"
            className="absolute right-1/2 bottom-4 size-76 translate-x-1/2 rounded-full bg-gray-50 after:absolute after:inset-8 after:rounded-full after:border after:border-gray-100 md:right-0 md:size-90 md:translate-x-0 lg:size-112"
          />
          <span className="absolute top-5 right-4 z-2 text-xs tracking-wide text-gray-500">
            작은 경험도, 크게 나눠요.
          </span>
          <img
            src="/about-assets/character.webp"
            width={960}
            height={791}
            fetchPriority="high"
            alt="파란 후드를 입고 확성기로 이야기를 전하는 샤라웃 캐릭터"
            className="relative z-1 max-h-85 w-full object-contain md:max-h-100 lg:max-h-118"
          />
          <div className="bg-background absolute bottom-5 left-0 z-3 flex items-center gap-2 rounded-xl border border-gray-100 px-4 py-3 text-xs font-semibold shadow-lg shadow-gray-950/5 md:bottom-7 lg:px-5 lg:py-4 lg:text-sm">
            <IconSparkles className="text-primary-500 size-5" aria-hidden="true" /> 내 경험이,
            누군가의 다음 한 걸음!
          </div>
          <span
            aria-hidden="true"
            className="absolute right-3 bottom-0 text-xs tracking-widest text-gray-500"
          >
            SHARE YOUR NEXT.
          </span>
        </div>
      </section>
      <div className={ABOUT_CONTAINER}>
        <div className="mb-10 grid items-center gap-4 border-y border-gray-200 py-6 text-xs md:mb-16 md:grid-cols-4 md:gap-6 md:py-7">
          <span className="font-semibold text-gray-700">경험이 연결되는 방식</span>
          {[
            ['#projects', '만든 것을 발견하고'],
            ['#community', '궁금한 것을 나누고'],
            ['#stories', '내 경험을 더하고'],
          ].map(([href, label], index) => (
            <Link key={href} href={href} className="hover:text-primary-600 flex items-center gap-3">
              <b className="text-xs tracking-wider text-gray-500">0{index + 1}</b>
              {label}
              <IconArrowDown className="ml-auto size-4 text-gray-500" aria-hidden="true" />
            </Link>
          ))}
        </div>
      </div>
    </>
  );
}
