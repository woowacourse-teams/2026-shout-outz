import { Link } from '@tanstack/react-router';
import { IconBrandGithub, IconBulb, IconFileText, IconGitPullRequest } from '@tabler/icons-react';

import { Badge, type BadgeTone } from '@/components/Badge';
import { prototypeLinks } from '@/pages/about/links';
import {
  STEPS,
  cleanTitle,
  formatKoreanDate,
  teamName,
  undot,
  youtubeId,
  type MergedResource,
  type MergedResources,
  type ResourceSource,
} from '@/pages/about/resources';
import { toProjectSlugParam } from '@/utils/project';

const SOURCE_LABELS: Record<ResourceSource, [string, BadgeTone]> = {
  blog: ['블로그', 'primary'],
  pr: ['PR', 'primary'],
  talk: ['테코톡', 'green'],
  doc: ['팀 문서', 'green'],
};

const BLOG_LOGOS = [
  ['tistory.com', 'tistory.svg', '티스토리'],
  ['velog.io', 'velog.ico', 'velog'],
  ['rilog.kr', 'rilog.svg', 'Rilog'],
] as const;

const STRETCHED_LINK =
  'after:absolute after:inset-0 after:rounded-xl focus-visible:outline-none focus-visible:after:outline-2 focus-visible:after:outline-primary-600';

export interface ResourcePathProps {
  merged: MergedResources;
  /** 단계마다 앞에서부터 보여 줄 자료 수 */
  perStep: number;
}

/** 블로그 → PR과 팀 문서 → 테코톡 세 칸에 단계마다 앞의 몇 개만 보여 준다. */
export function ResourcePath({ merged, perStep }: ResourcePathProps) {
  return (
    <div className="grid gap-6 lg:grid-cols-3">
      {STEPS.map((step, index) => (
        <section key={step.src} className="min-w-0">
          <div className="flex items-center gap-3 border-b border-gray-200 pb-3">
            <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-gray-900 text-sm font-bold text-white">
              {index + 1}
            </span>
            <div className="min-w-0">
              <p className="font-bold">{step.title}</p>
              <p className="text-xs text-gray-500">{step.sub}</p>
            </div>
            <span className="ml-auto text-sm font-semibold text-gray-500">
              {merged[step.src].length}
            </span>
          </div>
          <div className="relative mt-4 space-y-4">
            {merged[step.src].length ? (
              merged[step.src]
                .slice(0, perStep)
                .map((resource) => <ResourceCard key={resource.id} resource={resource} />)
            ) : (
              <p className="py-6 text-center text-sm text-gray-500">이 단계 자료는 아직 없어요.</p>
            )}
          </div>
        </section>
      ))}
    </div>
  );
}

function ResourceCard({ resource }: { resource: MergedResource }) {
  const [label, tone] = SOURCE_LABELS[resource.source];
  const title = undot(cleanTitle(resource.title));
  const team = undot(teamName(resource.project));
  const line = resource.line && undot(resource.line);
  const date = formatKoreanDate(resource.date);
  const href = prototypeLinks.doc(resource.url, resource.slug);
  const videoId = resource.source === 'talk' ? youtubeId(resource.url) : undefined;

  if (videoId) {
    return (
      <article className="group relative min-w-0">
        <div className="relative aspect-video overflow-hidden rounded-xl bg-gray-100">
          <img
            src={`https://i.ytimg.com/vi/${videoId}/mqdefault.jpg`}
            alt=""
            loading="lazy"
            className="size-full object-cover transition-transform group-hover:scale-[1.02]"
          />
        </div>
        <div className="mt-3 flex items-center gap-1.5">
          <Badge tone={tone}>{label}</Badge>
          <span className="truncate text-xs text-gray-500">
            {[team, date].filter(Boolean).join(' ')}
          </span>
        </div>
        <h3 className="mt-1.5 line-clamp-2 text-sm font-bold text-gray-900 group-hover:underline">
          <a href={href} className={STRETCHED_LINK}>
            {title}
          </a>
        </h3>
        {line && <Takeaway line={line} size="sm" />}
      </article>
    );
  }

  return (
    <article className="group relative flex min-w-0 flex-col rounded-xl border border-gray-200 p-4 transition-colors hover:bg-gray-50">
      <div className="flex items-center gap-1.5">
        <SourceLogo source={resource.source} url={resource.url} />
        <Badge tone={tone}>{label}</Badge>
        {resource.slug ? (
          <>
            <Link
              to="/projects/$slug"
              params={{ slug: toProjectSlugParam(resource.slug) }}
              className="text-primary-600 relative z-10 truncate text-xs font-semibold hover:underline"
            >
              {team}
            </Link>
            <span className="text-xs text-gray-400">{date}</span>
          </>
        ) : (
          <span className="truncate text-xs text-gray-500">
            {[team, date].filter(Boolean).join(' ')}
          </span>
        )}
      </div>
      <h3 className="mt-2 line-clamp-2 font-bold text-gray-900 group-hover:underline">
        <a href={href} className={STRETCHED_LINK}>
          {title}
        </a>
      </h3>
      {line && <Takeaway line={line} size="md" />}
    </article>
  );
}

/** 자료별 한 줄: "읽으면 무엇을 얻는지"가 카드에서 가장 먼저 읽히도록 강조한다. */
function Takeaway({ line, size }: { line: string; size: 'sm' | 'md' }) {
  return (
    <div className={size === 'sm' ? 'mt-2' : 'mt-3'}>
      <p className="text-primary-600 flex items-center gap-1 text-xs font-bold">
        <IconBulb className="size-3.5 shrink-0" aria-hidden="true" />
        이런 걸 얻을 수 있어요
      </p>
      <p
        className={`${size === 'sm' ? 'text-[13px]' : 'text-sm'} mt-1 leading-relaxed font-medium text-gray-900`}
      >
        <span className="bg-[linear-gradient(transparent_58%,var(--color-primary-100)_58%)] [box-decoration-break:clone] px-0.5">
          {line}
        </span>
      </p>
    </div>
  );
}

/** 자료 옆 출처 로고. PR과 팀 문서는 아이콘, 블로그는 플랫폼 아이콘 파일 */
export function SourceLogo({ source, url }: { source: string; url: string }) {
  if (source === 'pr')
    return (
      <IconGitPullRequest
        role="img"
        aria-label="Pull Request"
        className="text-primary-600 size-4 shrink-0"
      />
    );
  if (source === 'doc')
    return (
      <IconFileText role="img" aria-label="팀 문서" className="size-4 shrink-0 text-green-700" />
    );
  if (source === 'issue') return <GithubMark label="GitHub" />;
  if (source !== 'blog') return null;
  let host = '';
  try {
    host = new URL(url).hostname;
  } catch {
    return null;
  }
  if (host.endsWith('.github.io')) return <GithubMark label="GitHub 블로그" />;
  const logo = BLOG_LOGOS.find(([domain]) => host === domain || host.endsWith(`.${domain}`));
  return logo ? (
    <img
      src={`/about-assets/assets/${logo[1]}`}
      alt={logo[2]}
      title={logo[2]}
      className="size-4 shrink-0 rounded-sm"
      loading="lazy"
    />
  ) : null;
}

function GithubMark({ label }: { label: string }) {
  return (
    <IconBrandGithub role="img" aria-label={label} className="size-4 shrink-0 text-gray-900" />
  );
}
