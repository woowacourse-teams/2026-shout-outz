import { useEffect, useRef } from 'react';
import { aboutBeforeProjects, aboutAfterProjects } from '@/pages/about/markup';
import { initAbout } from '@/pages/about/interactive';
import { ProjectCard } from '@/components/projects/ProjectCard';
import projects from '@/pages/about/project-snapshot.json';

export function AboutPage() {
  const root = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (root.current) return initAbout(root.current);
  }, []);
  return (
    <>
      <title>샤라웃 소개 — 경험에서 찾는 다음 힌트</title>
      <meta
        name="description"
        content="우아한테크코스 크루의 경험과 개발 기록에서 다음 힌트를 찾아보세요."
      />
      <link rel="canonical" href="https://shout-ou.tz/about" />
      <link rel="stylesheet" href="/about-assets/style.f9f1e8ef69.css" precedence="about" />
      <link rel="stylesheet" href="/about-assets/technology.e9221c42fa.css" precedence="about" />
      <div ref={root} className="about-page">
        <a className="skip" href="#main">
          본문으로 바로가기
        </a>
        <main id="main">
          <div
            className="about-content"
            dangerouslySetInnerHTML={{ __html: aboutBeforeProjects }}
          />
          <section id="experience" className="ref-section wrap ref-divider">
            <div className="ref-heading">
              <h2>프로젝트를 둘러보세요</h2>
              <p>크루들이 만든 프로젝트를 살펴보세요.</p>
            </div>
            <ul aria-label="프로젝트 예시" className="grid grid-cols-1 gap-6 md:grid-cols-3">
              {projects.map((project) => (
                <li key={project.slug}>
                  <ProjectCard {...project} />
                </li>
              ))}
            </ul>
            <p className="mt-5 text-sm text-gray-700">프로토타입에 수집된 프로젝트 스냅샷입니다.</p>
          </section>
          <div className="about-content" dangerouslySetInnerHTML={{ __html: aboutAfterProjects }} />
        </main>
      </div>
    </>
  );
}
