import { IconArrowUpRight } from '@tabler/icons-react';
import { ABOUT_STATS } from '@/constants/about';
import {
  ABOUT_CONTAINER,
  ABOUT_COPY,
  ABOUT_EYEBROW,
  ABOUT_HEADING,
  ABOUT_TEXT_LINK,
  type AboutSectionProps,
} from '@/components/about/shared';

const PROJECTS = [
  {
    slug: '@sssok',
    name: '쏙',
    image: 'sssok.webp',
    description: '링크 하나로 빠르게 사진과 동영상을 공유해요.',
    tags: ['React', 'TypeScript', 'Spring'],
  },
  {
    slug: '@gachi-gacha',
    name: '가치가챠',
    image: 'gachi.webp',
    description: '원하는 가챠 매장을 찾고, 캐릭터를 교환해요.',
    tags: ['React', 'Spring Boot', 'AWS'],
  },
  {
    slug: '@mapmory',
    name: '맵모리',
    image: 'mapmory.webp',
    description: '여행의 순간을 지도 위에 기억하는 아카이브.',
    tags: ['Android', 'iOS', 'Kotlin'],
  },
];

export function AboutProjects({ LinkComponent: Link }: AboutSectionProps) {
  return (
    <section id="projects" className="scroll-mt-28 bg-gray-50 py-12 md:pt-18 md:pb-20">
      <div className={ABOUT_CONTAINER}>
        <div className="mb-8 flex flex-col gap-6 md:mb-10 md:flex-row md:items-center md:justify-between md:gap-10">
          <div>
            <p className={ABOUT_EYEBROW}>01 / DISCOVER</p>
            <h2 className={`${ABOUT_HEADING} mb-0`}>
              <span className="text-primary-500 mb-5 block text-8xl leading-none font-bold tracking-tighter tabular-nums">
                {ABOUT_STATS.projects}
                <span className="ml-3 text-base font-semibold tracking-tight md:text-xl">
                  개의 프로젝트
                </span>
              </span>
              다음 시도의 힌트가
              <br />
              여기 모여 있어요.
            </h2>
          </div>
          <div className="max-w-96">
            <p className={ABOUT_COPY}>
              누군가는 일상의 불편을, 누군가는 좋아하는 것을
              <br />
              하나의 서비스로 만들었어요.
              <br />
              무엇을 만들었고 어떤 기술을 썼는지 살펴보세요.
            </p>
            <Link href="/projects" className={`${ABOUT_TEXT_LINK} mt-3`}>
              {ABOUT_STATS.projects}개 프로젝트 둘러보기{' '}
              <IconArrowUpRight className="size-4" aria-hidden="true" />
            </Link>
          </div>
        </div>
        <div className="grid gap-5 md:grid-cols-3 md:gap-6">
          {PROJECTS.map((project) => (
            <Link
              key={project.slug}
              href={`/projects/${project.slug}`}
              className="bg-background group overflow-hidden rounded-2xl border border-gray-100 transition-transform hover:-translate-y-1 hover:shadow-lg hover:shadow-gray-950/5 motion-reduce:transform-none"
            >
              <div className="aspect-video overflow-hidden bg-gray-100">
                <img
                  src={`/about-assets/${project.image}`}
                  alt={`${project.name} 프로젝트 미리보기`}
                  loading="lazy"
                  width={800}
                  height={450}
                  className="size-full object-cover transition-transform duration-300 group-hover:scale-105 motion-reduce:transform-none"
                />
              </div>
              <div className="p-6">
                <span className="text-xs text-gray-500">우아한테크코스 8기</span>
                <h3 className="my-3 flex items-center justify-between text-xl font-bold tracking-tight">
                  {project.name}
                  <IconArrowUpRight className="size-5 text-gray-500" aria-hidden="true" />
                </h3>
                <p className="min-h-11 text-sm leading-6 text-gray-700">{project.description}</p>
                <div className="mt-4 flex flex-wrap gap-1.5">
                  {project.tags.map((tag) => (
                    <span
                      key={tag}
                      className="rounded-md bg-gray-50 px-2 py-1 text-xs text-gray-600"
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              </div>
            </Link>
          ))}
        </div>
        <p className="mt-6 text-center text-xs leading-6 text-gray-500">
          등록된 {ABOUT_STATS.projects}개 프로젝트 중 일부예요. 카드를 누르면 자세히 볼 수 있어요. ·{' '}
          {ABOUT_STATS.asOf} 기준
        </p>
      </div>
    </section>
  );
}
