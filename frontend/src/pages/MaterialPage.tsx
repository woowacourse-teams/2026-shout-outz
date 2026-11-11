import { useSuspenseQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';

import { materialDetailQuery } from '@/apis/explore';
import { AppGnb } from '@/components/AppGnb';
import { AsyncBoundary } from '@/components/AsyncBoundary';
import { MaterialSummary } from '@/components/explore/MaterialSummaryModal';
import { Footer } from '@/components/Footer';

/** 자료 정리 보기. 자료 요약 모달의 내용을 한 페이지로 보여 준다. */
export function MaterialPage({ resourceId }: { resourceId: number }) {
  return (
    <div className="bg-background flex min-h-dvh flex-col text-gray-900">
      <AppGnb />
      <main className="mx-auto w-full max-w-3xl flex-1 px-6 pt-10 pb-24 break-keep">
        <AsyncBoundary key={resourceId}>
          <MaterialContent resourceId={resourceId} />
        </AsyncBoundary>
      </main>
      <Footer />
    </div>
  );
}

function MaterialContent({ resourceId }: { resourceId: number }) {
  const { data: material } = useSuspenseQuery(materialDetailQuery(resourceId));

  return (
    <article>
      <title>{`${material.title} | shout-outz`}</title>
      <MaterialSummary material={material} Title={PageTitle} variant="page" />
    </article>
  );
}

function PageTitle({ className, children }: { className?: string; children: ReactNode }) {
  return <h1 className={className}>{children}</h1>;
}
