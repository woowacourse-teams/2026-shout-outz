import { useEffect, useRef } from 'react';
import { Link } from '@tanstack/react-router';
import { aboutBeforeProjects, aboutAfterProjects } from '@/pages/about/markup';
import { initAbout } from '@/pages/about/interactive';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Button } from '@/components/Button';
import { ClientOnly } from '@/components/ClientOnly';
import { LatestProjectList } from '@/components/projects/LatestProjectList';

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
            {/* 좋아요 여부처럼 사용자별로 달라지는 데이터라 정적 HTML에 넣지 않고 브라우저에서 불러옵니다. */}
            <div className="min-h-80">
              <ClientOnly>
                <AsyncBoundary
                  fallback={
                    <p role="status" className="py-16 text-center text-gray-500">
                      프로젝트를 불러오는 중입니다.
                    </p>
                  }
                  errorFallback={(_error, reset) => (
                    <div role="alert" className="space-y-4 py-16 text-center">
                      <p className="text-gray-600">프로젝트를 불러오지 못했습니다.</p>
                      <Button variant="outline" onClick={reset}>
                        다시 시도
                      </Button>
                    </div>
                  )}
                >
                  <LatestProjectList limit={3} surface="about" />
                </AsyncBoundary>
              </ClientOnly>
            </div>
            <Link to="/projects" className="inline-link mt-5">
              프로젝트 전체 보기 <span aria-hidden="true">→</span>
            </Link>
          </section>
          <div className="about-content" dangerouslySetInnerHTML={{ __html: aboutAfterProjects }} />
        </main>
      </div>
    </>
  );
}
