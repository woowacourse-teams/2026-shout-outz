import { AboutHeader, AboutHero } from '@/components/about/AboutHero';
import { AboutProjects } from '@/components/about/AboutProjects';
import { AboutCommunity } from '@/components/about/AboutCommunity';
import { AboutStories } from '@/components/about/AboutStories';
import { AboutMission, AboutClosing, AboutFooter } from '@/components/about/AboutClosing';
import type { AboutSectionProps } from '@/components/about/shared';

export type { AboutLinkProps } from '@/components/about/shared';

export function AboutPage({ LinkComponent }: AboutSectionProps) {
  return (
    <div
      id="about-top"
      className="bg-background [&_a]:focus-visible:outline-primary-500 min-h-dvh font-medium break-keep text-gray-900 [&_a]:focus-visible:outline-2 [&_a]:focus-visible:outline-offset-4"
    >
      <title>샤라웃 소개 — 개발과 취업 준비를 함께</title>
      <meta
        name="description"
        content="프로젝트를 살펴보고, 개발과 취업 고민을 나누세요. IT 취준생을 위한 커뮤니티, 샤라웃."
      />
      <link rel="canonical" href="https://shout-ou.tz/about" />
      <LinkComponent
        href="#main"
        className="bg-primary-500 fixed -top-20 left-4 z-30 rounded-lg p-4 text-white focus:top-3"
      >
        본문으로 바로가기
      </LinkComponent>
      <AboutHeader LinkComponent={LinkComponent} />
      <main id="main" className="scroll-mt-28">
        <AboutHero LinkComponent={LinkComponent} />
        <AboutProjects LinkComponent={LinkComponent} />
        <AboutCommunity LinkComponent={LinkComponent} />
        <AboutStories LinkComponent={LinkComponent} />
        <AboutMission LinkComponent={LinkComponent} />
        <AboutClosing LinkComponent={LinkComponent} />
      </main>
      <AboutFooter LinkComponent={LinkComponent} />
    </div>
  );
}
