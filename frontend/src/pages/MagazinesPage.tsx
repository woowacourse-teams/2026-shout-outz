import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Footer } from '@/components/Footer';
import { MagazineList } from '@/components/magazines/MagazineList';

export function MagazinesPage() {
  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <title>매거진 | shout-outz</title>
      <AppGnb />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 pt-6 pb-12 md:pt-10 md:pb-20">
        <h1 className="text-xl font-bold tracking-tight text-gray-900 md:text-2xl">매거진</h1>
        <p className="mt-2 text-sm text-gray-500">크루들의 이야기를 인터뷰로 만나보세요.</p>
        <div className="mt-6">
          <AsyncBoundary>
            <MagazineList />
          </AsyncBoundary>
        </div>
      </main>
      <Footer />
    </div>
  );
}
