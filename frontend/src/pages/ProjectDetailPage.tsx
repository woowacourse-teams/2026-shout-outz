import { Footer } from '@/components/Footer';
import { AppGnb } from '@/components/AppGnb';
import { ProjectDetailBoundary } from '@/components/projects/ProjectDetailBoundary';
import { ProjectDetailContent } from '@/components/projects/ProjectDetailContent';

export function ProjectDetailPage({ slug }: { slug: string }) {
  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <AppGnb />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 md:py-10">
        <ProjectDetailBoundary key={slug}>
          <ProjectDetailContent slug={slug} />
        </ProjectDetailBoundary>
      </main>
      <Footer />
    </div>
  );
}
