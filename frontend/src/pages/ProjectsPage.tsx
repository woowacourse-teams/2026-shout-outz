import { Footer } from '@/components/Footer';
import { AppGnb } from '@/components/AppGnb';
import { useQuery } from '@tanstack/react-query';
import { Link } from '@tanstack/react-router';
import { sessionQuery } from '@/apis/session';
import { getButtonStyles } from '@/components/Button';
import { ProjectList } from '@/components/projects/ProjectList';
import { ProjectListBoundary } from '@/components/projects/ProjectListBoundary';

export function ProjectsPage() {
  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>프로젝트 | shout-outz</title>
      <AppGnb />
      <main
        id="projects-content"
        tabIndex={-1}
        className="mx-auto w-full max-w-6xl flex-1 px-4 pt-6 pb-12 md:pt-10 md:pb-20"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-xl font-bold md:text-2xl">프로젝트</h1>
            <p className="mt-2 text-sm leading-relaxed text-gray-500">
              우아한테크코스 크루들이 제작한 프로젝트 모음
            </p>
          </div>
          <ProjectRegistrationLink />
        </div>
        <div className="mt-8">
          <ProjectListBoundary>
            <ProjectList />
          </ProjectListBoundary>
        </div>
      </main>
      <Footer />
    </div>
  );
}

function ProjectRegistrationLink() {
  const session = useQuery({ ...sessionQuery, enabled: typeof window !== 'undefined' });
  if (session.data?.status !== 'AUTHENTICATED') return null;

  return (
    <Link to="/projects/new" className={getButtonStyles({ size: 'sm' })}>
      프로젝트 등록
    </Link>
  );
}
