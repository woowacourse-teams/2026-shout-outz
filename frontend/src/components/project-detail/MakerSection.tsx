import { Suspense } from 'react';
import { MakerInfo } from './MakerInfo';
import { MakerErrorBoundary } from './MakerErrorBoundary';

export function MakerSection({ projectId }: { projectId: string }) {
    return (
        // 로딩은 Suspense에서 처리합니다.
        <Suspense fallback={<p className="mt-8 text-gray-400">제작자 정보를 불러오는 중...</p>}>
            <MakerErrorBoundary>
                <MakerInfo projectId={projectId} />
            </MakerErrorBoundary>
        </Suspense>
    );
}
