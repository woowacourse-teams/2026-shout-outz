import { Footer } from '@/components/Footer';
import { Gnb } from '@/components/Gnb';
import { ProjectList } from '@/components/projects/ProjectList';
import { ProjectListBoundary } from '@/components/projects/ProjectListBoundary';

export function ProjectsPage() {
  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>프로젝트 | shout-outz</title>
      <Gnb />
      <main
        id="projects-content"
        tabIndex={-1}
        className="mx-auto w-full max-w-6xl flex-1 px-4 pt-6 pb-12 md:pt-10 md:pb-20"
      >
        <h1 className="text-xl font-bold md:text-2xl">프로젝트</h1>
        <p className="mt-2 text-sm leading-relaxed text-gray-500">
          우아한테크코스 크루들이 제작한 프로젝트 모음
        </p>
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
