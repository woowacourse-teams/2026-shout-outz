import { Component, type ReactNode } from 'react';

// 에러는 Error Boundary에서 처리합니다.
export class MakerErrorBoundary extends Component<{ children: ReactNode }, { hasError: boolean }> {
    state = { hasError: false };

    static getDerivedStateFromError() {
        return { hasError: true };
    }

    render() {
        if (this.state.hasError) {
            return (
                <p role="alert" className="mt-8 text-red-500">
                    제작자 정보를 불러오지 못했습니다.
                </p>
            );
        }
        return this.props.children;
    }
}
