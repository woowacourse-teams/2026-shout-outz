import { IconChevronRight, IconExternalLink } from '@tabler/icons-react';

import { Avatar } from '@/components/Avatar';
import { Badge } from '@/components/Badge';
import { tabItemBaseStyle, tabItemSizeStyles, tabItemStyles } from '@/components/Tab';
import showcaseData from '@/pages/about/data/showcase.json';
import { prototypeLinks } from '@/pages/about/links';
import { SourceLogo } from '@/pages/about/ResourcePath';
import { cn } from '@/utils/cn';

interface CareerItem {
  title: string;
  bullets: string[];
  sources: { type: string; url: string; label: string }[];
  tech: string[];
}

interface Showcase {
  slug: string;
  title: string;
  tagline: string;
  techTags: { id: number; displayName: string }[];
  members: {
    handle?: string | null;
    displayName: string;
    userType?: string | null;
    cohort?: number | null;
    track?: string | null;
    avatarUrl?: string | null;
  }[];
  career: { tracks: { track: string; role: string; items: CareerItem[] }[] };
}

const showcase = showcaseData as Showcase;

const TRACK_LABELS: Record<string, string> = {
  ANDROID: '안드로이드',
  BACKEND: '백엔드',
  FRONTEND: '프론트엔드',
};

const affiliation = (member: Showcase['members'][number]) =>
  member.userType === 'WOOWACOURSE_COACH'
    ? '코치'
    : member.cohort != null
      ? [`${member.cohort}기`, member.track && TRACK_LABELS[member.track], '크루']
          .filter(Boolean)
          .join(' ')
      : member.userType === 'WOOWACOURSE_CREW'
        ? '크루'
        : '';

const normalize = (text: string) => text.toLowerCase().replace(/[\s.-]/g, '');

/** 프로젝트에 등록된 기술 태그 중 이력서 항목의 기술과 이름이 겹치는 것만. 없으면 전부 */
function resumeTags(items: CareerItem[]) {
  const used = items.flatMap((item) => item.tech).map(normalize);
  const all = showcase.techTags.map((tag) => tag.displayName);
  const hit = all.filter((name) =>
    name
      .split('/')
      .map(normalize)
      .some((part) => part && used.some((tech) => tech.includes(part) || part.includes(tech))),
  );
  return hit.length ? hit : all;
}

const tabClass = (variant: 'underline' | 'subnav', size: 'sm' | 'lg') =>
  cn(tabItemBaseStyle, tabItemSizeStyles[size], tabItemStyles[variant]);

/** 샤라웃 프로젝트의 "전체 경험" 탭을 미리 보여 준다. 누르면 프로토타입의 실제 화면으로 간다. */
export function CareerPreview() {
  const tracks = showcase.career.tracks.filter((track) => track.items.length);
  const track = tracks[0];
  if (!track) return <p className="text-sm text-gray-500">예시 경험을 불러오지 못했어요.</p>;
  const href = prototypeLinks.project(showcase.slug);

  return (
    <>
      <figure
        aria-label="샤라웃 전체 경험 탭 미리보기"
        className="relative h-[30rem] overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm md:h-[36rem]"
      >
        <div
          aria-hidden="true"
          inert
          className="pointer-events-none px-5 pt-5 select-none md:px-8 md:pt-7"
        >
          <div className="flex items-center justify-between gap-3 border-b border-gray-100 pb-4">
            <p className="text-sm font-bold text-gray-900">{showcase.title}</p>
            <span className="text-xs text-gray-500">전체 경험 미리보기</span>
          </div>
          <div className="mt-2 flex gap-4 overflow-hidden border-b border-gray-200">
            {['전체 경험', '기술 이야기', '프로젝트 소개'].map((label, index) => (
              <span
                key={label}
                data-status={index === 0 ? 'active' : 'inactive'}
                className={tabClass('underline', 'lg')}
              >
                {label}
              </span>
            ))}
          </div>
          <div className="pt-6 pb-10 lg:pb-16">
            <div className="grid min-w-0 gap-10 lg:grid-cols-3 lg:gap-12">
              <div className="min-w-0 lg:col-span-2">
                {tracks.length > 1 && (
                  <div className="flex gap-2 border-b border-gray-200">
                    {tracks.map((item, index) => (
                      <span
                        key={item.track}
                        data-status={index === 0 ? 'active' : 'inactive'}
                        className={tabClass('subnav', 'sm')}
                      >
                        {item.track}
                      </span>
                    ))}
                  </div>
                )}
                <article className="mt-6 min-w-0">
                  <h3 className="text-xl font-bold text-gray-900">
                    {showcase.title}{' '}
                    <span className="ml-1 text-sm font-medium text-gray-500">{track.role}</span>
                  </h3>
                  <p className="mt-2 text-sm leading-6 text-gray-600">{showcase.tagline}</p>
                  <div className="mt-4 flex flex-wrap gap-1.5">
                    {resumeTags(track.items).map((tech) => (
                      <Badge key={tech}>{tech}</Badge>
                    ))}
                  </div>
                  <div className="mt-6 space-y-2">
                    {track.items.map((item) => (
                      <section key={item.title} className="-mx-4 rounded-xl px-4 py-4">
                        <h4 className="text-sm font-bold text-gray-900">{item.title}</h4>
                        <ul className="mt-2 list-disc space-y-2 pl-4 text-sm leading-7 text-gray-700">
                          {item.bullets.map((bullet) => (
                            <li key={bullet} className="pl-1">
                              {bullet}
                            </li>
                          ))}
                        </ul>
                        <div className="mt-2 flex flex-wrap items-center justify-between gap-2">
                          <div className="flex min-w-0 flex-wrap items-center gap-x-3 gap-y-1.5">
                            {item.sources.map((source) => (
                              <span
                                key={source.url}
                                className="text-primary-600 inline-flex min-w-0 items-center gap-1 text-xs font-medium"
                              >
                                <SourceLogo source={source.type} url={source.url} />
                                <span className="truncate">{source.label}</span>
                                <IconExternalLink className="size-3.5 shrink-0" />
                              </span>
                            ))}
                          </div>
                          <span className="shrink-0 text-xs font-medium text-gray-400">
                            포트폴리오 보기 →
                          </span>
                        </div>
                      </section>
                    ))}
                  </div>
                </article>
              </div>
              <div className="min-w-0 space-y-10">
                <section>
                  <h4 className="text-lg font-bold">참여 크루 ({showcase.members.length}명)</h4>
                  <ul className="mt-4 space-y-3">
                    {showcase.members.map((member) => (
                      <li
                        key={member.handle ?? member.displayName}
                        className="flex min-w-0 items-center gap-3"
                      >
                        <Avatar src={member.avatarUrl} name={member.displayName} alt="" size="md" />
                        <div className="min-w-0 text-sm">
                          <p className="flex min-w-0 flex-wrap items-baseline gap-x-1.5 font-semibold break-words">
                            <span>{member.displayName}</span>
                            {member.handle && (
                              <span className="text-xs font-normal text-gray-500">
                                {member.handle}
                              </span>
                            )}
                          </p>
                          {affiliation(member) && (
                            <span className="block min-w-0 text-xs leading-5 text-gray-500">
                              {affiliation(member)}
                            </span>
                          )}
                        </div>
                      </li>
                    ))}
                  </ul>
                </section>
                <section>
                  <h4 className="text-lg font-bold">기술 스택</h4>
                  <div className="mt-4 flex flex-wrap gap-2">
                    {showcase.techTags.map((tag) => (
                      <Badge key={tag.id}>{tag.displayName}</Badge>
                    ))}
                  </div>
                </section>
              </div>
            </div>
          </div>
        </div>
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-x-0 bottom-0 h-28 bg-gradient-to-t from-white to-transparent"
        />
        <a
          href={href}
          aria-label="샤라웃의 실제 이력서와 포트폴리오 열기"
          className="focus-visible:outline-primary-600 absolute inset-0 rounded-2xl focus-visible:outline-2 focus-visible:outline-offset-[-3px]"
        />
      </figure>
      <AboutMoreLink href={href}>샤라웃의 전체 경험 보기</AboutMoreLink>
    </>
  );
}

export const MORE_LINK_CLASS =
  'hover:text-primary-700 mt-6 inline-flex items-center gap-1 text-sm font-medium text-gray-600';

/** 섹션 아래 "더 보기" 링크. 프로토타입 화면으로 가는 외부 링크에 쓴다. */
export function AboutMoreLink({ href, children }: { href: string; children: string }) {
  return (
    <a href={href} className={MORE_LINK_CLASS}>
      {children} <IconChevronRight className="size-4" aria-hidden="true" />
    </a>
  );
}
