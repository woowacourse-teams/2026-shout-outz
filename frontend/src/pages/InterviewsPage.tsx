import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Footer } from '@/components/Footer';
import { InterviewList } from '@/components/interviews/InterviewList';

export function InterviewsPage() {
  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>인터뷰 | shout-outz</title>
      <AppGnb />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 pt-6 pb-12 md:pt-10 md:pb-20">
        <h1 className="text-xl font-bold tracking-tight text-gray-900 md:text-2xl">인터뷰</h1>
        <p className="mt-2 text-sm text-gray-500">크루들의 이야기를 인터뷰로 만나보세요.</p>
        <div className="mt-6">
          <AsyncBoundary>
            <InterviewList />
          </AsyncBoundary>
        </div>
      </main>
      <Footer />
    </div>
  );
}
