import { IconArrowUpRight } from '@tabler/icons-react';
import { getButtonStyles } from '@/components/Button';
import { ABOUT_STATS } from '@/constants/about';
import {
  ABOUT_CONTAINER,
  ABOUT_EYEBROW,
  ABOUT_HEADING,
  ABOUT_TEXT_LINK,
  type AboutSectionProps,
} from '@/components/about/shared';

export function AboutMission({ LinkComponent: Link }: AboutSectionProps) {
  return (
    <div className={ABOUT_CONTAINER}>
      <section
        id="about"
        className="scroll-mt-28 border-y border-gray-200 pt-12 pb-7 text-center md:pt-16 md:pb-8"
      >
        <span className="text-xs tracking-widest text-gray-500">WHY SHOUT-OUTZ</span>
        <h2 className={`${ABOUT_HEADING} my-6`}>
          좋은 경험은
          <br />더 멀리 들려야 하니까.
        </h2>
        <p className="text-sm leading-7 text-gray-700 md:leading-8">
          우아한테크코스 크루들의 프로젝트와 이야기를 모으는 데서 시작했어요.
          <br />
          이제 개발과 취업을 준비하는 여정에서, 서로에게 힘이 되는 공간을 만들어 가요.
        </p>
        <div className="mt-8 flex flex-col justify-between gap-4 border-t border-gray-200 pt-6 text-xs text-gray-500 md:mt-12 md:flex-row">
          <span>당신의 이야기도 기다리고 있어요.</span>
          <Link
            href="https://github.com/woowacourse-teams/2026-shout-outz"
            target="_blank"
            rel="noopener noreferrer"
            className={`${ABOUT_TEXT_LINK} justify-center text-xs`}
          >
            샤라웃을 만드는 이야기 <IconArrowUpRight className="size-4" aria-hidden="true" />
          </Link>
        </div>
      </section>
    </div>
  );
}

export function AboutClosing({ LinkComponent: Link }: AboutSectionProps) {
  return (
    <section className={`${ABOUT_CONTAINER} py-18 text-center md:py-24`}>
      <img
        src="/about-assets/logo.png"
        width={64}
        height={64}
        alt=""
        className="mx-auto mb-4 size-13 object-contain md:size-16"
      />
      <p className={ABOUT_EYEBROW}>YOUR NEXT STARTS HERE</p>
      <h2 className={ABOUT_HEADING}>
        {ABOUT_STATS.members}명의 이야기에,
        <br />
        당신의 경험을 더해 주세요.
      </h2>
      <p className="mb-7 text-sm leading-6 text-gray-700">
        프로젝트 하나, 질문 하나, 짧은 답변 하나부터 시작해요.
      </p>
      <Link href="/" className={getButtonStyles({ size: 'lg', className: 'gap-4' })}>
        함께 이야기 나누기 <IconArrowUpRight className="size-5" aria-hidden="true" />
      </Link>
    </section>
  );
}

export function AboutFooter({ LinkComponent: Link }: AboutSectionProps) {
  return (
    <footer className="bg-gray-50 pt-11 pb-6">
      <div className={ABOUT_CONTAINER}>
        <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
          <div>
            <Link
              href="#about-top"
              className="inline-flex items-center gap-2 text-xl font-extrabold tracking-tight"
            >
              <img src="/about-assets/logo.png" width={30} height={30} alt="" />
              shout-outz<span className="text-primary-500 -ml-2">.</span>
            </Link>
            <p className="mt-3 text-xs text-gray-500">IT 취준생을 위한, 취업 커뮤니티</p>
          </div>
          <nav aria-label="서비스 바로가기" className="flex flex-wrap gap-6 text-sm text-gray-700">
            {[
              ['/community', '커뮤니티'],
              ['/projects', '프로젝트'],
              ['/news', '소식'],
              ['https://github.com/woowacourse-teams/2026-shout-outz', 'GitHub'],
            ].map(([href, label]) => (
              <Link
                key={href}
                href={href}
                target={href?.startsWith('https:') ? '_blank' : undefined}
                rel={href?.startsWith('https:') ? 'noopener noreferrer' : undefined}
                className="hover:text-primary-700 inline-flex items-center gap-1"
              >
                {label}
                <IconArrowUpRight className="size-3" aria-hidden="true" />
              </Link>
            ))}
          </nav>
        </div>
        <div className="mt-9 flex justify-between gap-5 border-t border-gray-100 pt-5 text-xs leading-5 text-gray-500">
          <span>© 2026 shout-outz. All rights reserved.</span>
          <span className="hidden md:inline">
            회원·프로젝트·질문·답변 수: {ABOUT_STATS.asOf} 기준
          </span>
        </div>
      </div>
    </footer>
  );
}
