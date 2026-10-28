import { Title } from '@/components/Title';
import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { Footer } from '@/components/Footer';
import { MagazineList } from '@/components/magazines/MagazineList';

export function MagazinesPage() {
  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <Title>매거진 | shout-outz</Title>
      <AppGnb />
      <main className="mx-auto w-full max-w-252 flex-1 px-6 pt-14 pb-30">
        <h1 className="text-3xl font-extrabold tracking-tight text-gray-900">매거진</h1>
        <p className="mt-2 text-base text-gray-600">크루들의 이야기를 인터뷰로 만나보세요.</p>
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
